package com.zovira.admin.controller;

import com.zovira.admin.dto.AdminCouponRow;
import com.zovira.admin.dto.AdminDecisionRequest;
import com.zovira.admin.dto.AdminOrderRow;
import com.zovira.admin.dto.AdminOverview;
import com.zovira.admin.dto.AdminReviewRow;
import com.zovira.admin.dto.AdminSellerRow;
import com.zovira.admin.dto.AdminUserRow;
import com.zovira.admin.dto.AuditLogRow;
import com.zovira.admin.dto.CategoryRequest;
import com.zovira.admin.dto.CouponRequest;
import com.zovira.admin.service.AdminAnalyticsService;
import com.zovira.admin.service.AdminCatalogService;
import com.zovira.admin.service.AdminCouponService;
import com.zovira.admin.service.AdminModerationService;
import com.zovira.admin.service.AdminSettingsService;
import com.zovira.admin.service.AdminUserService;
import com.zovira.catalog.dto.CategoryNode;
import com.zovira.catalog.entity.ProductStatus;
import com.zovira.common.web.PageResponse;
import com.zovira.returns.dto.ReturnDecisionRequest;
import com.zovira.returns.dto.ReturnResponse;
import com.zovira.returns.service.ReturnService;
import com.zovira.review.entity.ReviewStatus;
import com.zovira.security.AuthUser;
import com.zovira.security.CurrentUser;
import com.zovira.seller.dto.SellerProductSummary;
import com.zovira.seller.entity.SellerStatus;
import com.zovira.settings.dto.SettingResponse;
import com.zovira.user.entity.UserStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Administrative operations. The whole controller requires the ADMIN role in addition to the
 * route rule in the security config, so a missing rule alone can never expose it.
 */
@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin", description = "Platform analytics, users, sellers, catalog, orders and moderation")
public class AdminController {

    private final AdminAnalyticsService analytics;
    private final AdminUserService userService;
    private final AdminCatalogService catalog;
    private final AdminCouponService coupons;
    private final AdminModerationService moderation;
    private final AdminSettingsService settings;
    private final ReturnService returns;

    public AdminController(AdminAnalyticsService analytics, AdminUserService userService, AdminCatalogService catalog,
            AdminCouponService coupons, AdminModerationService moderation, AdminSettingsService settings,
            ReturnService returns) {
        this.analytics = analytics;
        this.userService = userService;
        this.catalog = catalog;
        this.coupons = coupons;
        this.moderation = moderation;
        this.settings = settings;
        this.returns = returns;
    }

    @GetMapping("/overview")
    @Operation(summary = "Platform metrics, revenue and signup trends, category mix and alerts")
    public AdminOverview overview() {
        return analytics.overview();
    }

    // --- Users ------------------------------------------------------------------------------

    @GetMapping("/users")
    @Operation(summary = "Search and filter user accounts")
    public PageResponse<AdminUserRow> users(@RequestParam(required = false) String q,
            @RequestParam(required = false) String role, @RequestParam(required = false) String status,
            @RequestParam(required = false) Integer page, @RequestParam(required = false) Integer size) {
        return userService.listUsers(q, role, status, page, size);
    }

    @PatchMapping("/users/{id}/status")
    @Operation(summary = "Suspend or reactivate an account (suspension ends all sessions)")
    public AdminUserRow setUserStatus(@CurrentUser AuthUser admin, @PathVariable Long id,
            @RequestParam String status, @Valid @RequestBody(required = false) AdminDecisionRequest body) {
        return userService.setUserStatus(admin.id(), id, UserStatus.valueOf(status.toUpperCase(Locale.ROOT)),
                body == null ? null : body.reason());
    }

    // --- Sellers ----------------------------------------------------------------------------

    @GetMapping("/sellers")
    @Operation(summary = "Seller accounts and applications")
    public PageResponse<AdminSellerRow> sellers(@RequestParam(required = false) String status,
            @RequestParam(required = false) String q, @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return userService.listSellers(status, q, page, size);
    }

    @PostMapping("/sellers/{id}/decision")
    @Operation(summary = "Approve, reject or suspend a seller")
    public AdminSellerRow decideSeller(@PathVariable Long id, @RequestParam String status,
            @Valid @RequestBody(required = false) AdminDecisionRequest body) {
        return userService.decideSeller(id, SellerStatus.valueOf(status.toUpperCase(Locale.ROOT)),
                body == null ? null : body.reason());
    }

    // --- Catalog ----------------------------------------------------------------------------

    @GetMapping("/categories")
    @Operation(summary = "Category tree")
    public List<CategoryNode> categories() {
        return catalog.tree();
    }

    @PostMapping("/categories")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a category")
    public CategoryNode createCategory(@Valid @RequestBody CategoryRequest request) {
        return catalog.create(request);
    }

    @PutMapping("/categories/{id}")
    @Operation(summary = "Update a category")
    public CategoryNode updateCategory(@PathVariable Long id, @Valid @RequestBody CategoryRequest request) {
        return catalog.update(id, request);
    }

    @GetMapping("/products")
    @Operation(summary = "Every seller's listings")
    public PageResponse<SellerProductSummary> products(@RequestParam(required = false) String status,
            @RequestParam(required = false) String q, @RequestParam(required = false) Long sellerId,
            @RequestParam(required = false) Integer page, @RequestParam(required = false) Integer size) {
        return catalog.listProducts(status, q, sellerId, page, size);
    }

    @PatchMapping("/products/{id}/status")
    @Operation(summary = "Take down or restore a listing")
    public SellerProductSummary setProductStatus(@PathVariable Long id, @RequestParam String status,
            @Valid @RequestBody(required = false) AdminDecisionRequest body) {
        return catalog.setProductStatus(id, ProductStatus.valueOf(status.toUpperCase(Locale.ROOT)),
                body == null ? null : body.reason());
    }

    // --- Orders, returns and reviews --------------------------------------------------------

    @GetMapping("/orders")
    @Operation(summary = "All orders across the platform")
    public PageResponse<AdminOrderRow> orders(@RequestParam(required = false) String status,
            @RequestParam(required = false) String q, @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return moderation.listOrders(status, q, page, size);
    }

    @PostMapping("/orders/{orderNumber}/cancel")
    @Operation(summary = "Cancel an order on a customer's behalf")
    public ResponseEntity<Void> cancelOrder(@CurrentUser AuthUser admin, @PathVariable String orderNumber,
            @Valid @RequestBody(required = false) AdminDecisionRequest body) {
        moderation.cancelOrder(admin.id(), orderNumber, body == null ? null : body.reason());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/returns")
    @Operation(summary = "Return requests across all sellers")
    public PageResponse<ReturnResponse> returns(@RequestParam(defaultValue = "true") boolean openOnly,
            @RequestParam(required = false) Integer page) {
        return returns.all(openOnly, page);
    }

    @PostMapping("/returns/{id}/decision")
    @Operation(summary = "Resolve a return on a seller's behalf")
    public ReturnResponse decideReturn(@CurrentUser AuthUser admin, @PathVariable Long id,
            @Valid @RequestBody ReturnDecisionRequest request) {
        return returns.decide(id, null, admin.id(), request);
    }

    @GetMapping("/reviews")
    @Operation(summary = "Reviews for moderation")
    public PageResponse<AdminReviewRow> reviews(@RequestParam(required = false) String status,
            @RequestParam(required = false) Integer rating, @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return moderation.listReviews(status, rating, page, size);
    }

    @PatchMapping("/reviews/{id}")
    @Operation(summary = "Publish or reject a review")
    public AdminReviewRow moderateReview(@CurrentUser AuthUser admin, @PathVariable Long id,
            @RequestParam String status, @Valid @RequestBody(required = false) AdminDecisionRequest body) {
        return moderation.moderateReview(admin.id(), id, ReviewStatus.valueOf(status.toUpperCase(Locale.ROOT)),
                body == null ? null : body.reason());
    }

    // --- Coupons and settings ---------------------------------------------------------------

    @GetMapping("/coupons")
    @Operation(summary = "All coupons with their usage")
    public PageResponse<AdminCouponRow> coupons(@RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return coupons.list(page, size);
    }

    @PostMapping("/coupons")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a coupon")
    public AdminCouponRow createCoupon(@Valid @RequestBody CouponRequest request) {
        return coupons.create(request);
    }

    @PutMapping("/coupons/{id}")
    @Operation(summary = "Update a coupon")
    public AdminCouponRow updateCoupon(@PathVariable Long id, @Valid @RequestBody CouponRequest request) {
        return coupons.update(id, request);
    }

    @DeleteMapping("/coupons/{id}")
    @Operation(summary = "Deactivate a coupon (history is kept)")
    public AdminCouponRow deactivateCoupon(@PathVariable Long id) {
        return coupons.setActive(id, false);
    }

    @GetMapping("/settings")
    @Operation(summary = "Platform settings")
    public List<SettingResponse> settings() {
        return settings.list();
    }

    @PutMapping("/settings")
    @Operation(summary = "Update platform settings")
    public List<SettingResponse> updateSettings(@CurrentUser AuthUser admin, @RequestBody Map<String, String> values) {
        return settings.update(admin.id(), values);
    }

    @GetMapping("/audit-log")
    @Operation(summary = "Recent administrative and security events")
    public PageResponse<AuditLogRow> auditLog(@RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return moderation.auditLog(page, size);
    }
}
