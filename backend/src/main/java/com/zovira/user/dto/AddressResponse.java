package com.zovira.user.dto;

import com.zovira.user.entity.AddressType;

public record AddressResponse(
        Long id,
        String fullName,
        String phone,
        String line1,
        String line2,
        String landmark,
        String city,
        String state,
        String pincode,
        String country,
        AddressType type,
        boolean isDefault) {
}
