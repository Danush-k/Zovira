package com.zovira.seller.controller;

import com.zovira.catalog.entity.ProductStatus;
import com.zovira.common.web.PageResponse;
import com.zovira.security.AuthUser;
import com.zovira.security.CurrentUser;
import com.zovira.seller.dto.InventoryRow;
import com.zovira.seller.dto.SellerApplicationRequest;
import com.zovira.seller.dto.SellerDashboard;
import com.zovira.seller.dto.SellerOrderRow;
import com.zovira.seller.dto.SellerProductRequest;
import com.zovira.seller.dto.SellerProductSummary;
import com.zovira.seller.dto.SellerProfileResponse;
import com.zovira.seller.dto.ShipmentUpdateRequest;
import com.zovira.seller.dto.StockUpdateRequest;
import com.zovira.seller.entity.Seller;
import com.zovira.seller.service.SellerAccessService;
import com.zovira.seller.service.SellerAnalyticsService;
import com.zovira.seller.service.SellerCatalogService;
import com.zovira.seller.service.SellerOnboardingService;
import com.zovira.seller.service.SellerOrderService;
import com.zovira.returns.dto.ReturnDecisionRequest;
import com.zovira.returns.dto.ReturnResponse;
import com.zovira.returns.service.ReturnService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
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

@RestController
@RequestMapping("/api/v1/seller")
@PreAuthorize("hasRole('SELLER')")
@Tag(name = "Seller", description = "Seller dashboard, catalog, inventory, orders and returns")
public class SellerController {

    private final SellerAccessService access;
    private final SellerOnboardingService onboarding;
    private final SellerCatalogService catalog;
    private final SellerOrderService orders;
    private final SellerAnalyticsService analytics;
    private final ReturnService returns;

    public SellerController(SellerAccessService access, SellerOnboardingService onboarding,
            SellerCatalogService catalog, SellerOrderService orders, SellerAnalyticsService analytics,
            ReturnService returns) {
        this.access = access;
        this.onboarding = onboarding;
        this.catalog = catalog;
        this.orders = orders;
        this.analytics = analytics;
        this.returns = returns;
    }

    @GetMapping("/profile")
    @Operation(summary = "The signed-in seller's store profile")
    public SellerProfileResponse profile(@CurrentUser AuthUser user) {
        return SellerOnboardingService.toResponse(access.requireApproved(user.id()));
    }

    @PutMapping("/profile")
    @Operation(summary = "Update store details")
    public SellerProfileResponse updateProfile(@CurrentUser AuthUser user,
            @Valid @RequestBody SellerApplicationRequest request) {
        return onboarding.update(access.requireApprovedId(user.id()), request);
    }

    @GetMapping("/dashboard")
    @Operation(summary = "Revenue, orders, top products and stock alerts")
    public SellerDashboard dashboard(@CurrentUser AuthUser user) {
        Seller seller = access.requireApproved(user.id());
        return analytics.dashboard(seller, catalog);
    }

    @GetMapping("/products")
    @Operation(summary = "The seller's listings")
    public PageResponse<SellerProductSummary> products(@CurrentUser AuthUser user,
            @RequestParam(required = false) String status, @RequestParam(required = false) String q,
            @RequestParam(required = false) Integer page, @RequestParam(required = false) Integer size) {
        return catalog.list(access.requireApprovedId(user.id()), status, q, page, size);
    }

    @PostMapping("/products")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a listing with its options, variants and stock")
    public SellerProductSummary create(@CurrentUser AuthUser user, @Valid @RequestBody SellerProductRequest request) {
        return catalog.create(access.requireApproved(user.id()), request);
    }

    @PutMapping("/products/{id}")
    @Operation(summary = "Update a listing")
    public SellerProductSummary update(@CurrentUser AuthUser user, @PathVariable Long id,
            @Valid @RequestBody SellerProductRequest request) {
        return catalog.update(access.requireApprovedId(user.id()), id, request);
    }

    @PatchMapping("/products/{id}/status")
    @Operation(summary = "Publish, unpublish or archive a listing")
    public SellerProductSummary setStatus(@CurrentUser AuthUser user, @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        return catalog.setStatus(access.requireApprovedId(user.id()), id,
                ProductStatus.valueOf(body.getOrDefault("status", "INACTIVE").toUpperCase(java.util.Locale.ROOT)));
    }

    @GetMapping("/inventory")
    @Operation(summary = "Stock levels across every listing")
    public List<InventoryRow> inventory(@CurrentUser AuthUser user, @RequestParam(defaultValue = "false") boolean lowOnly) {
        return catalog.inventory(access.requireApprovedId(user.id()), lowOnly);
    }

    @PutMapping("/inventory")
    @Operation(summary = "Update stock levels in bulk")
    public List<InventoryRow> updateStock(@CurrentUser AuthUser user, @Valid @RequestBody StockUpdateRequest request) {
        return catalog.updateStock(access.requireApprovedId(user.id()), request);
    }

    @GetMapping("/orders")
    @Operation(summary = "Shipments to fulfil (filter: open, pending, processing, shipped, delivered, all)")
    public PageResponse<SellerOrderRow> orders(@CurrentUser AuthUser user,
            @RequestParam(defaultValue = "open") String filter, @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return orders.list(access.requireApprovedId(user.id()), filter, page, size);
    }

    @PatchMapping("/shipments/{id}")
    @Operation(summary = "Advance a shipment (processing, shipped with tracking, out for delivery, delivered)")
    public SellerOrderRow updateShipment(@CurrentUser AuthUser user, @PathVariable Long id,
            @Valid @RequestBody ShipmentUpdateRequest request) {
        return orders.updateStatus(access.requireApprovedId(user.id()), user.id(), id, request);
    }

    @GetMapping("/returns")
    @Operation(summary = "Return requests for this seller's items")
    public PageResponse<ReturnResponse> returns(@CurrentUser AuthUser user,
            @RequestParam(defaultValue = "true") boolean openOnly, @RequestParam(required = false) Integer page) {
        return returns.forSeller(access.requireApprovedId(user.id()), openOnly, page);
    }

    @PostMapping("/returns/{id}/decision")
    @Operation(summary = "Approve, reject or mark a return as received")
    public ReturnResponse decide(@CurrentUser AuthUser user, @PathVariable Long id,
            @Valid @RequestBody ReturnDecisionRequest request) {
        return returns.decide(id, access.requireApprovedId(user.id()), user.id(), request);
    }
}
