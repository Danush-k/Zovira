package com.zovira.cart.service;

import com.zovira.cart.dto.CartItemResponse;
import com.zovira.cart.dto.CartResponse;
import com.zovira.cart.dto.CartSummary;
import com.zovira.catalog.entity.Product;
import com.zovira.catalog.entity.ProductVariant;
import com.zovira.catalog.mapper.CatalogMapper;
import com.zovira.catalog.repository.ProductVariantRepository;
import com.zovira.coupon.entity.Coupon;
import com.zovira.coupon.repository.CouponRedemptionRepository;
import com.zovira.coupon.repository.CouponRepository;
import com.zovira.coupon.service.CouponEvaluator;
import com.zovira.inventory.entity.Inventory;
import com.zovira.inventory.repository.InventoryRepository;
import com.zovira.order.entity.DeliveryOption;
import com.zovira.settings.dto.PlatformSettings;
import com.zovira.settings.service.SettingsService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The single source of truth for cart and checkout money: current prices, stock checks, coupon
 * discount (allocated across eligible lines), shipping and the GST included in the total.
 */
@Service
public class CartPricer {

    public static final String PRICE_INCREASED = "PRICE_INCREASED";
    public static final String PRICE_DROPPED = "PRICE_DROPPED";
    public static final String OUT_OF_STOCK = "OUT_OF_STOCK";
    public static final String LIMITED_STOCK = "LIMITED_STOCK";
    public static final String UNAVAILABLE = "UNAVAILABLE";

    public record PricingLine(Long itemId, Long variantId, int quantity, BigDecimal priceAtAdd, boolean savedForLater) {
    }

    public record PricedLine(Long itemId, ProductVariant variant, Product product, int quantity, int available,
            BigDecimal lineTotal, BigDecimal couponShare, boolean purchasable, List<String> issues,
            boolean savedForLater, BigDecimal priceAtAdd) {
    }

    public record PricedCart(List<PricedLine> lines, BigDecimal mrpTotal, BigDecimal subtotal, Coupon coupon,
            CouponEvaluator.Result couponResult, String couponCode, BigDecimal couponDiscount,
            DeliveryOption deliveryOption, BigDecimal shippingFee, BigDecimal freeShippingThreshold,
            BigDecimal taxIncluded, BigDecimal total) {

        public List<PricedLine> purchasable() {
            return lines.stream().filter(l -> !l.savedForLater() && l.purchasable()).toList();
        }

        public boolean hasIssues() {
            return lines.stream().anyMatch(l -> !l.savedForLater()
                    && l.issues().stream().anyMatch(i -> !i.equals(PRICE_DROPPED)));
        }
    }

    private final ProductVariantRepository variants;
    private final InventoryRepository inventory;
    private final CouponRepository coupons;
    private final CouponRedemptionRepository redemptions;
    private final SettingsService settingsService;
    private final Clock clock;

    public CartPricer(ProductVariantRepository variants, InventoryRepository inventory, CouponRepository coupons,
            CouponRedemptionRepository redemptions, SettingsService settingsService, Clock clock) {
        this.variants = variants;
        this.inventory = inventory;
        this.coupons = coupons;
        this.redemptions = redemptions;
        this.settingsService = settingsService;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public PricedCart price(List<PricingLine> input, Long userId, String couponCode, DeliveryOption delivery) {
        PlatformSettings settings = settingsService.current();
        List<Long> ids = input.stream().map(PricingLine::variantId).distinct().toList();
        Map<Long, ProductVariant> byId = ids.isEmpty() ? Map.of()
                : variants.findWithProductByIdIn(ids).stream().collect(Collectors.toMap(ProductVariant::getId, Function.identity()));
        Map<Long, Inventory> stock = ids.isEmpty() ? Map.of() : CatalogMapper.byVariant(inventory.findByVariantIdIn(ids));

        List<PricedLine> lines = new ArrayList<>();
        for (PricingLine in : input) {
            ProductVariant variant = byId.get(in.variantId());
            if (variant == null) {
                continue; // deleted from the catalog: silently dropped
            }
            Product product = variant.getProduct();
            int available = stock.containsKey(variant.getId()) ? stock.get(variant.getId()).getQuantityAvailable() : 0;
            List<String> issues = new ArrayList<>();
            boolean purchasable = true;
            if (!product.isPurchasable() || !variant.isActive()) {
                issues.add(UNAVAILABLE);
                purchasable = false;
            } else if (available <= 0) {
                issues.add(OUT_OF_STOCK);
                purchasable = false;
            } else if (in.quantity() > Math.min(available, settings.maxQuantityPerItem())) {
                issues.add(LIMITED_STOCK);
                purchasable = false;
            }
            if (in.priceAtAdd() != null) {
                int cmp = variant.getPrice().compareTo(in.priceAtAdd());
                if (cmp > 0) {
                    issues.add(PRICE_INCREASED);
                } else if (cmp < 0) {
                    issues.add(PRICE_DROPPED);
                }
            }
            lines.add(new PricedLine(in.itemId(), variant, product, in.quantity(), available,
                    variant.getPrice().multiply(BigDecimal.valueOf(in.quantity())), BigDecimal.ZERO, purchasable,
                    List.copyOf(issues), in.savedForLater(), in.priceAtAdd()));
        }

        List<PricedLine> active = lines.stream().filter(l -> !l.savedForLater() && l.purchasable()).toList();
        BigDecimal mrpTotal = active.stream()
                .map(l -> l.variant().getMrp().multiply(BigDecimal.valueOf(l.quantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal subtotal = active.stream().map(PricedLine::lineTotal).reduce(BigDecimal.ZERO, BigDecimal::add);

        // Coupon
        Coupon coupon = null;
        CouponEvaluator.Result couponResult = null;
        BigDecimal couponDiscount = BigDecimal.ZERO;
        String normalized = couponCode == null || couponCode.isBlank() ? null : Coupon.normalizeCode(couponCode);
        if (normalized != null) {
            coupon = coupons.findByCodeIgnoreCase(normalized).orElse(null);
            long used = coupon == null || userId == null ? 0 : redemptions.countByCouponIdAndUserId(coupon.getId(), userId);
            List<CouponEvaluator.Line> couponLines = active.stream()
                    .map(l -> new CouponEvaluator.Line(l.product().getId(), l.product().getCategory(), l.lineTotal()))
                    .toList();
            couponResult = CouponEvaluator.evaluate(coupon, couponLines, userId, used, clock.instant());
            if (couponResult.valid()) {
                couponDiscount = couponResult.discount();
                lines = allocate(lines, coupon, couponDiscount, couponResult.eligibleSubtotal());
            }
        }

        // Shipping
        BigDecimal payable = subtotal.subtract(couponDiscount);
        BigDecimal shipping;
        if (active.isEmpty()) {
            shipping = BigDecimal.ZERO;
        } else if (delivery == DeliveryOption.EXPRESS) {
            shipping = settings.expressShippingFee();
        } else {
            shipping = payable.compareTo(settings.freeShippingThreshold()) >= 0 ? BigDecimal.ZERO
                    : settings.standardShippingFee();
        }

        // GST is included in selling prices; report the tax component of what the customer pays for goods.
        BigDecimal tax = lines.stream().filter(l -> !l.savedForLater() && l.purchasable())
                .map(l -> {
                    BigDecimal rate = l.product().getCategory().getTaxRate();
                    BigDecimal net = l.lineTotal().subtract(l.couponShare());
                    return net.multiply(rate).divide(rate.add(BigDecimal.valueOf(100)), 2, RoundingMode.HALF_UP);
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new PricedCart(lines, mrpTotal, subtotal, coupon, couponResult, normalized, couponDiscount, delivery,
                shipping, settings.freeShippingThreshold(), tax, payable.add(shipping));
    }

    /** Splits the coupon discount across eligible lines in proportion to their value (last line absorbs rounding). */
    private static List<PricedLine> allocate(List<PricedLine> lines, Coupon coupon, BigDecimal discount,
            BigDecimal eligibleSubtotal) {
        List<PricedLine> eligible = lines.stream()
                .filter(l -> !l.savedForLater() && l.purchasable() && CouponEvaluator.isEligible(coupon,
                        new CouponEvaluator.Line(l.product().getId(), l.product().getCategory(), l.lineTotal())))
                .toList();
        BigDecimal remaining = discount;
        List<PricedLine> result = new ArrayList<>();
        for (PricedLine l : lines) {
            int index = eligible.indexOf(l);
            if (index < 0) {
                result.add(l);
                continue;
            }
            BigDecimal share = index == eligible.size() - 1 ? remaining
                    : discount.multiply(l.lineTotal()).divide(eligibleSubtotal, 2, RoundingMode.DOWN);
            remaining = remaining.subtract(share);
            result.add(new PricedLine(l.itemId(), l.variant(), l.product(), l.quantity(), l.available(), l.lineTotal(),
                    share, l.purchasable(), l.issues(), l.savedForLater(), l.priceAtAdd()));
        }
        return result;
    }

    public CartResponse toResponse(PricedCart cart, int maxPerItem) {
        List<CartItemResponse> items = new ArrayList<>();
        List<CartItemResponse> saved = new ArrayList<>();
        for (PricedLine l : cart.lines()) {
            CartItemResponse item = toItem(l, maxPerItem);
            (l.savedForLater() ? saved : items).add(item);
        }
        int count = cart.purchasable().stream().mapToInt(PricedLine::quantity).sum();
        BigDecimal payable = cart.subtotal().subtract(cart.couponDiscount());
        BigDecimal toFree = cart.freeShippingThreshold().subtract(payable).max(BigDecimal.ZERO);
        CouponEvaluator.Result cr = cart.couponResult();
        return new CartResponse(items, saved, new CartSummary(count, cart.mrpTotal(), cart.subtotal(),
                cart.mrpTotal().subtract(cart.subtotal()), cart.couponCode(), cr != null && cr.valid(),
                cart.couponDiscount(), cr == null ? null : cr.message(), cart.deliveryOption().name(),
                cart.shippingFee(), cart.freeShippingThreshold(), toFree, cart.taxIncluded(), cart.total(),
                cart.hasIssues()));
    }

    private static CartItemResponse toItem(PricedLine l, int maxPerItem) {
        Product p = l.product();
        ProductVariant v = l.variant();
        String image = p.getImages().stream()
                .filter(i -> i.getVariant() != null && v.getOptions().get("Colour") != null
                        && v.getOptions().get("Colour").equals(i.getVariant().getOptions().get("Colour")))
                .map(i -> i.getUrl()).findFirst().orElse(p.primaryImageUrl());
        String stockStatus = l.available() <= 0 ? CatalogMapper.OUT_OF_STOCK
                : l.available() <= 5 ? CatalogMapper.LOW_STOCK : CatalogMapper.IN_STOCK;
        return new CartItemResponse(l.itemId(), p.getId(), p.getSlug(), p.getTitle(),
                p.getBrand() == null ? null : p.getBrand().getName(), image, v.getId(), v.getName(), v.getOptions(),
                v.getSku(), l.quantity(), Math.max(1, Math.min(l.available(), maxPerItem)), v.getPrice(), v.getMrp(),
                l.lineTotal(), l.priceAtAdd(), stockStatus, l.purchasable(), l.issues(), p.getSeller().getStoreName(),
                p.isCodAvailable());
    }
}
