package com.zovira.user.service;

import com.zovira.common.exception.BusinessException;
import com.zovira.common.exception.ErrorCodes;
import com.zovira.common.exception.NotFoundException;
import com.zovira.user.dto.AddressRequest;
import com.zovira.user.dto.AddressResponse;
import com.zovira.user.entity.Address;
import com.zovira.user.entity.User;
import com.zovira.user.mapper.UserMapper;
import com.zovira.user.repository.AddressRepository;
import com.zovira.user.repository.UserRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Address book. Every lookup is scoped to the caller's user id, so one customer can never read or
 * modify another's addresses by guessing ids.
 */
@Service
public class AddressService {

    static final int MAX_ADDRESSES = 10;

    private final AddressRepository addressRepository;
    private final UserRepository userRepository;

    public AddressService(AddressRepository addressRepository, UserRepository userRepository) {
        this.addressRepository = addressRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<AddressResponse> list(Long userId) {
        return addressRepository.findByUserIdOrderByDefaultAddressDescUpdatedAtDesc(userId).stream()
                .map(UserMapper::toResponse)
                .toList();
    }

    @Transactional
    public AddressResponse create(Long userId, AddressRequest request) {
        long existing = addressRepository.countByUserId(userId);
        if (existing >= MAX_ADDRESSES) {
            throw new BusinessException(ErrorCodes.QUANTITY_LIMIT,
                    "You can save up to " + MAX_ADDRESSES + " addresses. Remove one to add another.");
        }
        User user = userRepository.getReferenceById(userId);
        Address address = new Address(user);
        apply(address, request);
        boolean makeDefault = request.makeDefault() || existing == 0;
        if (makeDefault) {
            clearDefault(userId);
        }
        address.setDefaultAddress(makeDefault);
        return UserMapper.toResponse(addressRepository.save(address));
    }

    @Transactional
    public AddressResponse update(Long userId, Long addressId, AddressRequest request) {
        Address address = find(userId, addressId);
        apply(address, request);
        if (request.makeDefault() && !address.isDefaultAddress()) {
            clearDefault(userId);
            address.setDefaultAddress(true);
        }
        return UserMapper.toResponse(address);
    }

    @Transactional
    public void setDefault(Long userId, Long addressId) {
        Address address = find(userId, addressId);
        if (!address.isDefaultAddress()) {
            clearDefault(userId);
            address.setDefaultAddress(true);
        }
    }

    @Transactional
    public void delete(Long userId, Long addressId) {
        Address address = find(userId, addressId);
        boolean wasDefault = address.isDefaultAddress();
        addressRepository.delete(address);
        addressRepository.flush();
        if (wasDefault) {
            addressRepository.findByUserIdOrderByDefaultAddressDescUpdatedAtDesc(userId).stream()
                    .findFirst()
                    .ifPresent(next -> next.setDefaultAddress(true));
        }
    }

    @Transactional(readOnly = true)
    public Address find(Long userId, Long addressId) {
        return addressRepository.findByIdAndUserId(addressId, userId)
                .orElseThrow(() -> NotFoundException.of("Address"));
    }

    private void clearDefault(Long userId) {
        addressRepository.findByUserIdOrderByDefaultAddressDescUpdatedAtDesc(userId).stream()
                .filter(Address::isDefaultAddress)
                .forEach(a -> a.setDefaultAddress(false));
        // Flush before setting a new default so the partial unique index never sees two defaults.
        addressRepository.flush();
    }

    private static void apply(Address address, AddressRequest r) {
        address.setFullName(r.fullName().trim());
        address.setPhone(r.phone());
        address.setLine1(r.line1().trim());
        address.setLine2(blankToNull(r.line2()));
        address.setLandmark(blankToNull(r.landmark()));
        address.setCity(r.city().trim());
        address.setState(r.state().trim());
        address.setPincode(r.pincode());
        address.setType(r.type());
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
