package com.example.clothingshop.service;

import com.example.clothingshop.common.NotFoundException;
import com.example.clothingshop.dto.request.AddressRequest;
import com.example.clothingshop.entity.Address;
import com.example.clothingshop.repositories.AddressRepository;
import com.example.clothingshop.view.AddressView;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AddressService {

    private final AddressRepository repo;

    public AddressService(AddressRepository repo) {
        this.repo = repo;
    }

    public List<AddressView> list(long customerId) {
        return repo.findByCustomer(customerId).stream().map(AddressView::from).toList();
    }

    public AddressView get(long customerId, long id) {
        return AddressView.from(getAddress(id, customerId));
    }

    @Transactional
    public AddressView create(long customerId, AddressRequest request) {
        boolean makeDefault = Boolean.TRUE.equals(request.isDefault()) || !repo.hasAddress(customerId);
        if (makeDefault) {
            repo.clearDefault(customerId);
        }
        long id = repo.insert(customerId, values(request, makeDefault));
        return AddressView.from(getAddress(id, customerId));
    }

    @Transactional
    public AddressView update(long customerId, long id, AddressRequest request) {
        getAddress(id, customerId);
        boolean makeDefault = Boolean.TRUE.equals(request.isDefault());
        if (makeDefault) {
            repo.clearDefault(customerId);
        }
        repo.update(id, customerId, values(request, makeDefault));
        return AddressView.from(getAddress(id, customerId));
    }

    @Transactional
    public void delete(long customerId, long id) {
        getAddress(id, customerId);
        repo.softDelete(id, customerId);
    }

    private Address getAddress(long id, long customerId) {
        return repo.findById(id, customerId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy địa chỉ"));
    }

    private Map<String, Object> values(AddressRequest request, boolean isDefault) {
        Map<String, Object> values = new HashMap<>();
        values.put("receiverName", request.receiverName().trim());
        values.put("receiverPhone", request.receiverPhone().trim());
        values.put("province", request.province().trim());
        values.put("ward", request.ward().trim());
        values.put("detailAddress", request.detailAddress().trim());
        values.put("isDefault", isDefault ? 1 : 0);
        return values;
    }
}
