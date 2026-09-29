package com.zovira.cart.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/** A guest cart held in the browser, priced by the server or merged into the account cart on login. */
public record CartLinesRequest(@NotNull @Size(max = 50) List<@Valid CartLineRequest> items, String couponCode) {
}
