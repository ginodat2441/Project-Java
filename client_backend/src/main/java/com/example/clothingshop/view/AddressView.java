package com.example.clothingshop.view;

import com.example.clothingshop.entity.Address;

public record AddressView(Long id, String receiverName, String receiverPhone, String province,
                          String ward, String detailAddress, boolean isDefault) {

    public static AddressView from(Address address) {
        return new AddressView(address.id(), address.receiverName(), address.receiverPhone(),
                address.province(), address.ward(), address.detailAddress(),
                address.isDefault() != null && address.isDefault() == 1);
    }
}
