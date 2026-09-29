package com.zovira.wishlist.controller;

import com.zovira.security.AuthUser;
import com.zovira.security.CurrentUser;
import com.zovira.wishlist.dto.AddToWishlistRequest;
import com.zovira.wishlist.dto.WishlistItemResponse;
import com.zovira.wishlist.service.WishlistService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/wishlist")
@Tag(name = "Wishlist", description = "Saved products with price-drop and stock tracking")
public class WishlistController {

    private final WishlistService wishlistService;

    public WishlistController(WishlistService wishlistService) {
        this.wishlistService = wishlistService;
    }

    @GetMapping
    @Operation(summary = "Saved products, newest first")
    public List<WishlistItemResponse> list(@CurrentUser AuthUser user) {
        return wishlistService.list(user.id());
    }

    @GetMapping("/ids")
    @Operation(summary = "Product ids in the wishlist (for heart icons)")
    public List<Long> ids(@CurrentUser AuthUser user) {
        return wishlistService.productIds(user.id());
    }

    @PostMapping
    @Operation(summary = "Save a product")
    public List<WishlistItemResponse> add(@CurrentUser AuthUser user, @Valid @RequestBody AddToWishlistRequest request) {
        return wishlistService.add(user.id(), request.productId(), request.variantId());
    }

    @DeleteMapping("/{productId}")
    @Operation(summary = "Remove a saved product")
    public List<WishlistItemResponse> remove(@CurrentUser AuthUser user, @PathVariable Long productId) {
        return wishlistService.remove(user.id(), productId);
    }

    @PostMapping("/{productId}/move-to-cart")
    @Operation(summary = "Move a saved product into the cart")
    public List<WishlistItemResponse> moveToCart(@CurrentUser AuthUser user, @PathVariable Long productId) {
        return wishlistService.moveToCart(user.id(), productId);
    }
}
