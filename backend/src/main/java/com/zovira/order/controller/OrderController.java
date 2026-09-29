package com.zovira.order.controller;

import com.zovira.common.web.PageResponse;
import com.zovira.order.dto.CancelOrderRequest;
import com.zovira.order.dto.OrderDetailResponse;
import com.zovira.order.dto.OrderSummaryResponse;
import com.zovira.order.dto.ReorderResponse;
import com.zovira.order.service.InvoiceService;
import com.zovira.order.service.OrderCommandService;
import com.zovira.order.service.OrderQueryService;
import com.zovira.security.AuthUser;
import com.zovira.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/orders")
@Tag(name = "Orders", description = "The signed-in customer's orders")
public class OrderController {

    private final OrderQueryService queryService;
    private final OrderCommandService commandService;
    private final InvoiceService invoiceService;

    public OrderController(OrderQueryService queryService, OrderCommandService commandService,
            InvoiceService invoiceService) {
        this.queryService = queryService;
        this.commandService = commandService;
        this.invoiceService = invoiceService;
    }

    @GetMapping
    @Operation(summary = "List orders (filter: all, active, delivered, cancelled)")
    public PageResponse<OrderSummaryResponse> list(@CurrentUser AuthUser user,
            @RequestParam(defaultValue = "all") String filter, @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return queryService.list(user.id(), filter, page, size);
    }

    @GetMapping("/{orderNumber}")
    @Operation(summary = "Order details, shipments and status timeline")
    public OrderDetailResponse detail(@CurrentUser AuthUser user, @PathVariable String orderNumber) {
        return queryService.detail(user.id(), orderNumber);
    }

    @PostMapping("/{orderNumber}/cancel")
    @Operation(summary = "Cancel an order that has not shipped; paid orders are refunded")
    public OrderDetailResponse cancel(@CurrentUser AuthUser user, @PathVariable String orderNumber,
            @Valid @RequestBody CancelOrderRequest request) {
        return commandService.cancelByCustomer(user.id(), orderNumber, request.reason());
    }

    @PostMapping("/{orderNumber}/reorder")
    @Operation(summary = "Add the items from a past order to the cart")
    public ReorderResponse reorder(@CurrentUser AuthUser user, @PathVariable String orderNumber) {
        return commandService.reorder(user.id(), orderNumber);
    }

    @GetMapping(value = "/{orderNumber}/invoice", produces = MediaType.APPLICATION_PDF_VALUE)
    @Operation(summary = "Download the GST tax invoice as PDF")
    public ResponseEntity<byte[]> invoice(@CurrentUser AuthUser user, @PathVariable String orderNumber) {
        byte[] pdf = invoiceService.invoice(user.id(), orderNumber);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename("Zovira-invoice-" + orderNumber + ".pdf").build().toString())
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }
}
