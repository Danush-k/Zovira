package com.zovira.user.controller;

import com.zovira.security.AuthUser;
import com.zovira.security.CurrentUser;
import com.zovira.user.dto.AddressRequest;
import com.zovira.user.dto.AddressResponse;
import com.zovira.user.service.AddressService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users/me/addresses")
@Tag(name = "Addresses", description = "The signed-in user's saved delivery addresses")
public class AddressController {

    private final AddressService addressService;

    public AddressController(AddressService addressService) {
        this.addressService = addressService;
    }

    @GetMapping
    @Operation(summary = "List saved addresses, default first")
    public List<AddressResponse> list(@CurrentUser AuthUser user) {
        return addressService.list(user.id());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Add an address")
    public AddressResponse create(@CurrentUser AuthUser user, @Valid @RequestBody AddressRequest request) {
        return addressService.create(user.id(), request);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an address")
    public AddressResponse update(@CurrentUser AuthUser user, @PathVariable Long id,
            @Valid @RequestBody AddressRequest request) {
        return addressService.update(user.id(), id, request);
    }

    @PostMapping("/{id}/default")
    @Operation(summary = "Make an address the default")
    public ResponseEntity<Void> makeDefault(@CurrentUser AuthUser user, @PathVariable Long id) {
        addressService.setDefault(user.id(), id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remove an address")
    public ResponseEntity<Void> delete(@CurrentUser AuthUser user, @PathVariable Long id) {
        addressService.delete(user.id(), id);
        return ResponseEntity.noContent().build();
    }
}
