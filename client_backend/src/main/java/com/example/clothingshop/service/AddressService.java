package com.example.clothingshop.service;

import com.example.clothingshop.common.NotFoundException;
import com.example.clothingshop.common.ReceiverPhone;
import com.example.clothingshop.common.BusinessException;
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

    private final DeliveryDirectory directory;

    public AddressService(AddressRepository repo, DeliveryDirectory directory) {
        this.repo = repo;
        this.directory = directory;
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
        var address = values(request, makeDefault, null);
        if (makeDefault) {
            repo.clearDefault(customerId);
        }
        long id = repo.insert(customerId, address);
        return AddressView.from(getAddress(id, customerId));
    }

    @Transactional
    public AddressView update(long customerId, long id, AddressRequest request) {
        Address previous = getAddress(id, customerId);
        boolean makeDefault = Boolean.TRUE.equals(request.isDefault());
        var address = values(request, makeDefault, previous);
        if (makeDefault) {
            repo.clearDefault(customerId);
        }
        repo.update(id, customerId, address);
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

    private Map<String, Object> values(AddressRequest request, boolean isDefault, Address previous) {
        Map<String, Object> values = new HashMap<>();
        values.put("receiverName", request.receiverName().trim());
        values.put("receiverPhone", ReceiverPhone.normalize(request.receiverPhone()));
        String province = request.province().strip();
        String ward = request.ward().strip();
        if (previous == null || !province.equals(previous.province()) || !ward.equals(previous.ward())) {
            var selected = directory.resolve(province);
            String requestedWard = ward;
            ward = directory.wards(selected).stream().filter(name -> name.equalsIgnoreCase(requestedWard))
                    .findFirst().orElseThrow(() -> new BusinessException("Phường/Xã không thuộc Tỉnh/Thành phố đã chọn."));
            province = selected.name();
        }
        values.put("province", province);
        values.put("ward", ward);
        values.put("detailAddress", request.detailAddress().trim());
        values.put("isDefault", isDefault ? 1 : 0);
        return values;
    }
}
