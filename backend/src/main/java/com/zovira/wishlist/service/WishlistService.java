package com.zovira.wishlist.service;

import com.zovira.cart.dto.CartLineRequest;
import com.zovira.cart.service.CartService;
import com.zovira.catalog.entity.Product;
import com.zovira.catalog.entity.ProductStatus;
import com.zovira.catalog.entity.ProductVariant;
import com.zovira.catalog.repository.ProductRepository;
import com.zovira.common.exception.BusinessException;
import com.zovira.common.exception.ErrorCodes;
import com.zovira.common.exception.NotFoundException;
import com.zovira.user.repository.UserRepository;
import com.zovira.wishlist.dto.WishlistItemResponse;
import com.zovira.wishlist.entity.Wishlist;
import com.zovira.wishlist.entity.WishlistItem;
import com.zovira.wishlist.repository.WishlistRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WishlistService {

    static final int MAX_ITEMS = 200;

    private final WishlistRepository wishlists;
    private final ProductRepository products;
    private final UserRepository users;
    private final CartService cartService;
    private final Clock clock;

    public WishlistService(WishlistRepository wishlists, ProductRepository products, UserRepository users,
            CartService cartService, Clock clock) {
        this.wishlists = wishlists;
        this.products = products;
        this.users = users;
        this.cartService = cartService;
        this.clock = clock;
    }

    @Transactional
    public List<WishlistItemResponse> list(Long userId) {
        return wishlistFor(userId).getItems().stream().map(WishlistService::toResponse).toList();
    }

    @Transactional
    public List<Long> productIds(Long userId) {
        return wishlistFor(userId).getItems().stream().map(i -> i.getProduct().getId()).toList();
    }

    @Transactional
    public List<WishlistItemResponse> add(Long userId, Long productId, Long variantId) {
        Wishlist wishlist = wishlistFor(userId);
        if (wishlist.findByProduct(productId).isEmpty()) {
            if (wishlist.getItems().size() >= MAX_ITEMS) {
                throw new BusinessException(ErrorCodes.QUANTITY_LIMIT, "Your wishlist is full.");
            }
            Product product = products.findById(productId)
                    .filter(p -> p.getStatus() == ProductStatus.ACTIVE || p.getStatus() == ProductStatus.INACTIVE)
                    .orElseThrow(() -> NotFoundException.of("Product"));
            ProductVariant variant = variantId == null ? null
                    : product.getVariants().stream().filter(v -> v.getId().equals(variantId)).findFirst().orElse(null);
            BigDecimal price = variant != null ? variant.getPrice() : product.getMinPrice();
            wishlist.addItem(new WishlistItem(product, variant, price, clock.instant()));
        }
        return list(userId);
    }

    @Transactional
    public List<WishlistItemResponse> remove(Long userId, Long productId) {
        Wishlist wishlist = wishlistFor(userId);
        wishlist.findByProduct(productId).ifPresent(wishlist::removeItem);
        return list(userId);
    }

    /** Adds the saved option (or the default) to the cart and removes it from the wishlist. */
    @Transactional
    public List<WishlistItemResponse> moveToCart(Long userId, Long productId) {
        Wishlist wishlist = wishlistFor(userId);
        WishlistItem item = wishlist.findByProduct(productId).orElseThrow(() -> NotFoundException.of("Wishlist item"));
        ProductVariant variant = item.getVariant() != null && item.getVariant().isActive() ? item.getVariant()
                : item.getProduct().defaultVariant();
        if (variant == null) {
            throw new BusinessException(ErrorCodes.PRODUCT_UNAVAILABLE, "This product is currently unavailable.");
        }
        cartService.add(userId, new CartLineRequest(variant.getId(), 1));
        wishlist.removeItem(item);
        return list(userId);
    }

    private Wishlist wishlistFor(Long userId) {
        return wishlists.findByUserId(userId)
                .orElseGet(() -> wishlists.save(new Wishlist(users.getReferenceById(userId), clock.instant())));
    }

    private static WishlistItemResponse toResponse(WishlistItem item) {
        Product p = item.getProduct();
        ProductVariant v = item.getVariant() != null && item.getVariant().isActive() ? item.getVariant() : p.defaultVariant();
        BigDecimal price = v != null ? v.getPrice() : p.getMinPrice();
        BigDecimal mrp = v != null ? v.getMrp() : p.getMinMrp();
        BigDecimal drop = item.getPriceAtAdd().subtract(price).max(BigDecimal.ZERO);
        return new WishlistItemResponse(p.getId(), p.getSlug(), p.getTitle(),
                p.getBrand() == null ? null : p.getBrand().getName(), p.primaryImageUrl(), price, mrp,
                Product.discountPercent(price, mrp), item.getPriceAtAdd(), drop, p.getTotalStock() > 0,
                p.isPurchasable(), v == null ? null : v.getId(),
                (int) p.getVariants().stream().filter(ProductVariant::isActive).count(), item.getCreatedAt());
    }
}
