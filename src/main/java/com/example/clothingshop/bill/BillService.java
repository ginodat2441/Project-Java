package com.example.clothingshop.bill;

import com.example.clothingshop.common.BusinessException;
import com.example.clothingshop.common.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class BillService {

    private final BillRepository repo;

    public BillService(BillRepository repo) { this.repo = repo; }

    /**
     * Đặt hàng. @Transactional: có lỗi (hết hàng, sai địa chỉ...) -> ROLLBACK toàn bộ,
     * kể cả các lần trừ tồn kho trước đó.
     */
    @Transactional
    public BillView create(BillRequest req) {
        var addr = repo.findAddress(req.addressId(), req.customerId())
                .orElseThrow(() -> new BusinessException("Địa chỉ không hợp lệ"));

        // 1. Trừ kho + tính tiền
        BigDecimal subTotal = BigDecimal.ZERO;
        Map<Long, BigDecimal> priceMap = new HashMap<>();
        for (var item : req.items()) {
            BigDecimal price = repo.findPrice(item.productDetailId())
                    .orElseThrow(() -> new BusinessException("Sản phẩm chi tiết không tồn tại: " + item.productDetailId()));
            if (repo.decreaseStock(item.productDetailId(), item.quantity()) == 0) {
                throw new BusinessException("Không đủ hàng cho sản phẩm chi tiết id=" + item.productDetailId());
            }
            priceMap.put(item.productDetailId(), price);
            subTotal = subTotal.add(price.multiply(BigDecimal.valueOf(item.quantity())));
        }
        BigDecimal ship = req.shippingFee() == null ? BigDecimal.ZERO : req.shippingFee();

        // 2. Insert BILL
        Map<String, Object> p = new HashMap<>();
        p.put("code", "HD" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyMMddHHmmss"))
                + ThreadLocalRandom.current().nextInt(100, 999));
        p.put("customerId", req.customerId());
        p.put("addressId", req.addressId());
        p.put("receiverName", addr.receiverName());
        p.put("receiverPhone", addr.receiverPhone());
        p.put("shippingAddress", addr.fullAddress());
        p.put("subTotal", subTotal);
        p.put("shippingFee", ship);
        p.put("discount", BigDecimal.ZERO);
        p.put("total", subTotal.add(ship));
        p.put("paymentMethod", req.paymentMethod());
        p.put("note", req.note());
        long billId = repo.insertBill(p);

        // 3. Insert BILL_DETAIL + BILL_HISTORY
        for (var item : req.items()) {
            repo.insertBillDetail(billId, item.productDetailId(), item.quantity(), priceMap.get(item.productDetailId()));
        }
        repo.insertHistory(billId, null, 0, "Khách đặt hàng", null);

        return get(billId);
    }

    public BillView get(long id) {
        return repo.findById(id).orElseThrow(() -> new NotFoundException("Không tìm thấy hóa đơn id=" + id));
    }

    public List<BillLine> lines(long id) { return repo.findLines(id); }

    public List<BillView> byCustomer(long customerId) { return repo.findByCustomer(customerId); }

    @Transactional
    public BillView changeStatus(long billId, int newStatus, Long managerId, String note) {
        BillView bill = get(billId);
        if (repo.updateStatus(billId, bill.status(), newStatus, managerId) == 0) {
            throw new BusinessException("Trạng thái đơn đã bị thay đổi, vui lòng tải lại");
        }
        repo.insertHistory(billId, bill.status(), newStatus, note, managerId);
        return get(billId);
    }
}
