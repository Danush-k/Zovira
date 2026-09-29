package com.zovira.cart.service;

import com.zovira.cart.dto.CartLineRequest;
import com.zovira.cart.dto.CartResponse;
import com.zovira.cart.dto.UpdateCartItemRequest;
import com.zovira.cart.entity.Cart;
import com.zovira.cart.entity.CartItem;
import com.zovira.cart.repository.CartRepository;
import com.zovira.cart.service.CartPricer.PricedCart;
import com.zovira.cart.service.CartPricer.PricingLine;
import com.zovira.catalog.entity.ProductVariant;
import com.zovira.catalog.repository.ProductVariantRepository;
import com.zovira.common.exception.BusinessException;
import com.zovira.common.exception.ErrorCodes;
import com.zovira.common.exception.NotFoundException;
import com.zovira.coupon.entity.Coupon;
import com.zovira.inventory.repository.InventoryRepository;
import com.zovira.order.entity.DeliveryOption;
import com.zovira.settings.service.SettingsService;
import com.zovira.user.repository.UserRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CartService {

    static final int MAX_LINES = 50;

    private final CartRepository carts;
    private final UserRepository users;
    private final ProductVariantRepository variants;
    private final InventoryRepository inventory;
    private final CartPricer pricer;
    private final SettingsService settings;

    public CartService(CartRepository carts, UserRepository users, ProductVariantRepository variants,
            InventoryRepository inventory, CartPricer pricer, SettingsService settings) {
        this.carts = carts;
        this.users = users;
        this.variants = variants;
        this.inventory = inventory;
        this.pricer = pricer;
        this.settings = settings;
    }

    @Transactional
    public CartResponse get(Long userId) {
        return respond(cartFor(userId), userId);
    }

    @Transactional
    public CartResponse add(Long userId, CartLineRequest request) {
        Cart cart = cartFor(userId);
        ProductVariant variant = purchasableVariant(request.variantId());
        CartItem existing = cart.findByVariant(variant.getId()).orElse(null);
        int desired = (existing == null || existing.isSavedForLater() ? 0 : existing.getQuantity()) + request.quantity();
        int allowed = checkQuantity(variant, desired);
        if (existing != null) {
            existing.setQuantity(allowed);
            existing.setSavedForLater(false);
            existing.setPriceAtAdd(variant.getPrice());
        } else {
            if (cart.getItems().size() >= MAX_LINES) {
                throw new BusinessException(ErrorCodes.QUANTITY_LIMIT, "Your cart is full. Remove an item to add more.");
            }
            cart.addItem(new CartItem(variant, allowed, variant.getPrice()));
        }
        return respond(cart, userId);
    }

    @Transactional
    public CartResponse update(Long userId, Long itemId, UpdateCartItemRequest request) {
        Cart cart = cartFor(userId);
        CartItem item = cart.findItem(itemId).orElseThrow(() -> NotFoundException.of("Cart item"));
        if (request.variantId() != null && !request.variantId().equals(item.getVariant().getId())) {
            ProductVariant next = purchasableVariant(request.variantId());
            if (!next.getProduct().getId().equals(item.getVariant().getProduct().getId())) {
                throw new BusinessException(ErrorCodes.BAD_REQUEST, "You can only switch between options of the same product");
            }
            cart.findByVariant(next.getId()).filter(other -> other != item).ifPresent(cart::removeItem);
            item.setVariant(next);
            item.setPriceAtAdd(next.getPrice());
        }
        int quantity = request.quantity() != null ? request.quantity() : item.getQuantity();
        item.setQuantity(item.isSavedForLater() ? quantity : checkQuantity(item.getVariant(), quantity));
        return respond(cart, userId);
    }

    @Transactional
    public CartResponse remove(Long userId, Long itemId) {
        Cart cart = cartFor(userId);
        cart.findItem(itemId).ifPresent(cart::removeItem);
        return respond(cart, userId);
    }

    @Transactional
    public CartResponse setSavedForLater(Long userId, Long itemId, boolean saved) {
        Cart cart = cartFor(userId);
        CartItem item = cart.findItem(itemId).orElseThrow(() -> NotFoundException.of("Cart item"));
        if (!saved) {
            checkQuantity(purchasableVariant(item.getVariant().getId()), item.getQuantity());
            item.setPriceAtAdd(item.getVariant().getPrice());
        }
        item.setSavedForLater(saved);
        return respond(cart, userId);
    }

    @Transactional
    public CartResponse applyCoupon(Long userId, String code) {
        Cart cart = cartFor(userId);
        PricedCart priced = price(cart, userId, Coupon.normalizeCode(code));
        if (priced.couponResult() == null || !priced.couponResult().valid()) {
            throw new BusinessException(ErrorCodes.COUPON_INVALID,
                    priced.couponResult() == null ? "This coupon code isn't valid." : priced.couponResult().message());
        }
        cart.setCouponCode(priced.couponCode());
        return pricer.toResponse(priced, settings.current().maxQuantityPerItem());
    }

    @Transactional
    public CartResponse removeCoupon(Long userId) {
        Cart cart = cartFor(userId);
        cart.setCouponCode(null);
        return respond(cart, userId);
    }

    /** Folds a guest cart into the account cart after sign-in; unavailable lines are skipped. */
    @Transactional
    public CartResponse merge(Long userId, List<CartLineRequest> lines) {
        Cart cart = cartFor(userId);
        for (CartLineRequest line : lines) {
            if (cart.getItems().size() >= MAX_LINES) {
                break;
            }
            variants.findWithProductById(line.variantId())
                    .filter(v -> v.isActive() && v.getProduct().isPurchasable())
                    .ifPresent(variant -> {
                        int available = available(variant);
                        CartItem existing = cart.findByVariant(variant.getId()).orElse(null);
                        int desired = Math.max(line.quantity(), existing == null ? 0 : existing.getQuantity());
                        int quantity = Math.min(desired, Math.min(available, settings.current().maxQuantityPerItem()));
                        if (quantity <= 0) {
                            return;
                        }
                        if (existing != null) {
                            existing.setQuantity(quantity);
                            existing.setSavedForLater(false);
                        } else {
                            cart.addItem(new CartItem(variant, quantity, variant.getPrice()));
                        }
                    });
        }
        return respond(cart, userId);
    }

    /** Prices a guest cart held in the browser without persisting anything. */
    @Transactional(readOnly = true)
    public CartResponse preview(List<CartLineRequest> lines, String couponCode) {
        List<PricingLine> input = lines.stream().limit(MAX_LINES)
                .map(l -> new PricingLine(null, l.variantId(), l.quantity(), null, false)).toList();
        return pricer.toResponse(pricer.price(input, null, couponCode, DeliveryOption.STANDARD),
                settings.current().maxQuantityPerItem());
    }

    @Transactional
    public Cart cartFor(Long userId) {
        return carts.findByUserId(userId).orElseGet(() -> carts.save(new Cart(users.getReferenceById(userId))));
    }

    public PricedCart price(Cart cart, Long userId, String couponCode) {
        return price(cart, userId, couponCode, DeliveryOption.STANDARD);
    }

    public PricedCart price(Cart cart, Long userId, String couponCode, DeliveryOption delivery) {
        List<PricingLine> lines = cart.getItems().stream()
                .map(i -> new PricingLine(i.getId(), i.getVariant().getId(), i.getQuantity(), i.getPriceAtAdd(),
                        i.isSavedForLater()))
                .toList();
        return pricer.price(lines, userId, couponCode, delivery);
    }

    private CartResponse respond(Cart cart, Long userId) {
        carts.flush();
        // A coupon that stops applying stays attached and is reported with the reason (couponApplied=false).
        PricedCart priced = price(cart, userId, cart.getCouponCode());
        return pricer.toResponse(priced, settings.current().maxQuantityPerItem());
    }

    private ProductVariant purchasableVariant(Long variantId) {
        ProductVariant variant = variants.findWithProductById(variantId)
                .orElseThrow(() -> NotFoundException.of("Product option"));
        if (!variant.isActive() || !variant.getProduct().isPurchasable()) {
            throw new BusinessException(ErrorCodes.PRODUCT_UNAVAILABLE, "This product is currently unavailable.");
        }
        return variant;
    }

    private int checkQuantity(ProductVariant variant, int desired) {
        int available = available(variant);
        int max = settings.current().maxQuantityPerItem();
        if (available <= 0) {
            throw new BusinessException(ErrorCodes.OUT_OF_STOCK, "This item is out of stock.");
        }
        if (desired > max) {
            throw new BusinessException(ErrorCodes.QUANTITY_LIMIT, "You can buy up to " + max + " of this item per order.");
        }
        if (desired > available) {
            throw new BusinessException(ErrorCodes.OUT_OF_STOCK, "Only " + available + " left in stock.");
        }
        return desired;
    }

    private int available(ProductVariant variant) {
        return inventory.findByVariantId(variant.getId()).map(i -> i.getQuantityAvailable()).orElse(0);
    }
}
