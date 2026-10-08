package com.zovira.returns.controller;

import com.zovira.common.web.PageResponse;
import com.zovira.returns.dto.CreateReturnRequest;
import com.zovira.returns.dto.ReturnResponse;
import com.zovira.returns.service.ReturnService;
import com.zovira.security.AuthUser;
import com.zovira.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Returns", description = "Customer return requests")
public class ReturnController {

    private final ReturnService returnService;

    public ReturnController(ReturnService returnService) {
        this.returnService = returnService;
    }

    @PostMapping("/api/v1/orders/{orderNumber}/items/{itemId}/return")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Request a return for a delivered item within its return window")
    public ReturnResponse request(@CurrentUser AuthUser user, @PathVariable String orderNumber, @PathVariable Long itemId,
            @Valid @RequestBody CreateReturnRequest body) {
        return returnService.request(user.id(), orderNumber, itemId, body);
    }

    @GetMapping("/api/v1/returns")
    @Operation(summary = "The customer's return requests")
    public PageResponse<ReturnResponse> mine(@CurrentUser AuthUser user, @RequestParam(required = false) Integer page) {
        return returnService.mine(user.id(), page);
    }
}
