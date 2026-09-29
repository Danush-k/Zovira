package com.zovira.user.mapper;

import com.zovira.seller.entity.SellerStatus;
import com.zovira.user.dto.AddressResponse;
import com.zovira.user.dto.UserResponse;
import com.zovira.user.entity.Address;
import com.zovira.user.entity.Role;
import com.zovira.user.entity.User;

public final class UserMapper {

    private UserMapper() {
    }

    public static UserResponse toResponse(User user, SellerStatus sellerStatus) {
        return new UserResponse(user.getId(), user.getEmail(), user.getFullName(), user.getPhone(),
                user.getAvatarUrl(), user.isEmailVerified(),
                user.getRoles().stream().map(Role::getName).sorted().toList(),
                sellerStatus == null ? null : sellerStatus.name(), user.getCreatedAt());
    }

    public static AddressResponse toResponse(Address a) {
        return new AddressResponse(a.getId(), a.getFullName(), a.getPhone(), a.getLine1(), a.getLine2(),
                a.getLandmark(), a.getCity(), a.getState(), a.getPincode(), a.getCountry(), a.getType(),
                a.isDefaultAddress());
    }
}
