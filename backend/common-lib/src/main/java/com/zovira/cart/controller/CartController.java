package com.zovira.cart.controller;

import com.zovira.cart.dto.ApplyCouponRequest;
import com.zovira.cart.dto.CartLineRequest;
import com.zovira.cart.dto.CartLinesRequest;
import com.zovira.cart.dto.CartResponse;
import com.zovira.cart.dto.UpdateCartItemRequest;
import com.zovira.cart.service.CartService;
import com.zovira.security.AuthUser;
import com.zovira.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/cart")
@Tag(name = "Cart", description = "The signed-in shopper's cart, coupons and saved-for-later items")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping
    @Operation(summary = "Get the cart with live prices, stock checks and totals")
    public CartResponse get(@CurrentUser AuthUser user) {
        return cartService.get(user.id());
    }

    @PostMapping("/items")
    @Operation(summary = "Add a product option to the cart")
    public CartResponse add(@CurrentUser AuthUser user, @Valid @RequestBody CartLineRequest request) {
        return cartService.add(user.id(), request);
    }

    @PatchMapping("/items/{id}")
    @Operation(summary = "Change quantity or switch option")
    public CartResponse update(@CurrentUser AuthUser user, @PathVariable Long id,
            @Valid @RequestBody UpdateCartItemRequest request) {
        return cartService.update(user.id(), id, request);
    }

    @DeleteMapping("/items/{id}")
    @Operation(summary = "Remove an item")
    public CartResponse remove(@CurrentUser AuthUser user, @PathVariable Long id) {
        return cartService.remove(user.id(), id);
    }

    @PostMapping("/items/{id}/save-for-later")
    @Operation(summary = "Move an item to saved for later")
    public CartResponse saveForLater(@CurrentUser AuthUser user, @PathVariable Long id) {
        return cartService.setSavedForLater(user.id(), id, true);
    }

    @PostMapping("/items/{id}/move-to-cart")
    @Operation(summary = "Move a saved item back into the cart")
    public CartResponse moveToCart(@CurrentUser AuthUser user, @PathVariable Long id) {
        return cartService.setSavedForLater(user.id(), id, false);
    }

    @PostMapping("/coupon")
    @Operation(summary = "Apply a coupon (validated on the server)")
    public CartResponse applyCoupon(@CurrentUser AuthUser user, @Valid @RequestBody ApplyCouponRequest request) {
        return cartService.applyCoupon(user.id(), request.code());
    }

    @DeleteMapping("/coupon")
    @Operation(summary = "Remove the applied coupon")
    public CartResponse removeCoupon(@CurrentUser AuthUser user) {
        return cartService.removeCoupon(user.id());
    }

    @PostMapping("/merge")
    @Operation(summary = "Merge a guest cart into the account cart after sign-in")
    public CartResponse merge(@CurrentUser AuthUser user, @Valid @RequestBody CartLinesRequest request) {
        return cartService.merge(user.id(), request.items());
    }

    @PostMapping("/preview")
    @Operation(summary = "Price a guest cart without an account")
    public CartResponse preview(@Valid @RequestBody CartLinesRequest request) {
        return cartService.preview(request.items(), request.couponCode());
    }
}
