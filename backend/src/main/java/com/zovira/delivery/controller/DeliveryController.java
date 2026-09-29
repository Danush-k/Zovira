package com.zovira.delivery.controller;

import com.zovira.delivery.dto.DeliveryEstimate;
import com.zovira.delivery.service.DeliveryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/delivery")
@Tag(name = "Delivery", description = "Serviceability and delivery date estimates")
public class DeliveryController {

    private final DeliveryService deliveryService;

    public DeliveryController(DeliveryService deliveryService) {
        this.deliveryService = deliveryService;
    }

    @GetMapping("/estimate")
    @Operation(summary = "Estimate delivery dates and fees for a PIN code")
    public DeliveryEstimate estimate(@RequestParam String pincode) {
        return deliveryService.estimate(pincode.trim());
    }
}
