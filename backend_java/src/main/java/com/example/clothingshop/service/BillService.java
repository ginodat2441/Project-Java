package com.example.clothingshop.service;

import com.example.clothingshop.common.BusinessException;
import com.example.clothingshop.common.NotFoundException;
import com.example.clothingshop.dto.request.BillRequest;
import com.example.clothingshop.entity.Bill;
import com.example.clothingshop.repositories.BillRepository;
import com.example.clothingshop.view.BillLineView;
import com.example.clothingshop.view.BillHistoryView;
import com.example.clothingshop.view.BillView;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Value;

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
    private final BigDecimal shippingFee;

    public BillService(BillRepository repo, @Value("${app.order.shipping-fee:30000}") BigDecimal shippingFee) {
        this.repo = repo;
        if (shippingFee.signum() < 0) {
            throw new IllegalArgumentException("app.order.shipping-fee không được âm");
        }
        this.shippingFee = shippingFee;
    }

    /**
     * Đặt hàng. @Transactional: có lỗi (hết hàng, sai địa chỉ...) -> ROLLBACK toàn bộ,
     * kể cả các lần trừ tồn kho trước đó.
     */
    @Transactional
    public BillView create(long customerId, BillRequest req) {
        var addr = repo.findAddress(req.addressId(), customerId)
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
        BigDecimal ship = shippingFee;

        // 2. Insert BILL
        Map<String, Object> p = new HashMap<>();
        p.put("code", "HD" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyMMddHHmmss"))
                + ThreadLocalRandom.current().nextInt(100, 999));
        p.put("customerId", customerId);
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

        return get(customerId, billId);
    }

    // ---------- đọc: repo trả ENTITY -> service đổi sang VIEW ----------

    public BillView get(long customerId, long id) {
        return BillView.from(getEntity(id, customerId));
    }

    public List<BillLineView> lines(long customerId, long id) {
        getEntity(id, customerId);
        return repo.findLineViews(id);       // câu JOIN, repo trả thẳng view
    }

    public List<BillView> byCustomer(long customerId) {
        return repo.findByCustomer(customerId).stream().map(BillView::from).toList();
    }

    public List<BillView> allForStaff() {
        return repo.findAll().stream().map(BillView::from).toList();
    }

    public BillView getForStaff(long billId) {
        return BillView.from(repo.findById(billId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy hóa đơn id=" + billId)));
    }

    public List<BillLineView> linesForStaff(long billId) {
        getForStaff(billId);
        return repo.findLineViews(billId);
    }

    public List<BillHistoryView> historyForStaff(long billId) {
        getForStaff(billId);
        return repo.findHistory(billId);
    }

    @Transactional
    public BillView cancel(long customerId, long billId) {
        Bill bill = getEntity(billId, customerId);
        if (bill.status() == null || bill.status() != 0) {
            throw new BusinessException("Chỉ có thể hủy đơn đang chờ xác nhận");
        }
        if (repo.updateStatus(billId, bill.status(), 4, null) == 0) {
            throw new BusinessException("Trạng thái đơn đã bị thay đổi, vui lòng tải lại");
        }
        repo.restoreStockForBill(billId);
        repo.insertHistory(billId, bill.status(), 4, "Khách hàng hủy đơn", null);
        return get(customerId, billId);
    }

    public List<BillHistoryView> history(long customerId, long billId) {
        getEntity(billId, customerId);
        return repo.findHistory(billId);
    }

    @Transactional
    public BillView updateCashPaymentStatus(long billId, boolean paid, long managerId) {
        Bill bill = repo.findById(billId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy hóa đơn id=" + billId));
        if (!"COD".equals(bill.paymentMethod())) {
            throw new BusinessException("Chỉ hỗ trợ xác nhận thanh toán tiền mặt COD");
        }
        int currentStatus = bill.paymentStatus() == null ? 0 : bill.paymentStatus();
        int newStatus = paid ? 1 : 0;
        if (currentStatus == 2) {
            throw new BusinessException("Không thể thay đổi trạng thái khoản thanh toán đã hoàn tiền");
        }
        if (paid && (bill.status() == null || bill.status() != 2 && bill.status() != 3)) {
            throw new BusinessException("Chỉ xác nhận đã thu tiền khi đơn đang giao hoặc đã hoàn thành");
        }
        if (currentStatus != newStatus) {
            if (repo.updateCashPaymentStatus(billId, currentStatus, newStatus) == 0) {
                throw new BusinessException("Trạng thái thanh toán đã bị thay đổi, vui lòng tải lại");
            }
            String note = paid
                    ? "Admin #" + managerId + " xác nhận đã thu tiền mặt"
                    : "Admin #" + managerId + " cập nhật đơn chưa thanh toán";
            repo.insertCashPaymentHistory(billId, bill.totalAmount(), newStatus, note, paid);
        }
        return BillView.from(repo.findById(billId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy hóa đơn id=" + billId)));
    }

    @Transactional
    public BillView updateManagedStatus(long billId, int newStatus, String note,
                                        long managerId, boolean owner) {
        Bill bill = repo.findById(billId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy hóa đơn id=" + billId));
        int oldStatus = bill.status() == null ? -1 : bill.status();
        if (oldStatus == newStatus) {
            return BillView.from(bill);
        }

        boolean approveOrder = oldStatus == 0 && newStatus == 1;
        if (!approveOrder && !owner) {
            throw new BusinessException("Nhân viên chỉ được duyệt đơn đang chờ xác nhận");
        }
        boolean validTransition = approveOrder
                || oldStatus == 1 && newStatus == 2
                || oldStatus == 2 && newStatus == 3
                || (oldStatus == 0 || oldStatus == 1 || oldStatus == 2) && newStatus == 4
                || oldStatus == 3 && newStatus == 5;
        if (!validTransition) {
            throw new BusinessException("Không thể chuyển trạng thái đơn từ " + oldStatus + " sang " + newStatus);
        }
        if (repo.updateStatus(billId, oldStatus, newStatus, managerId) == 0) {
            throw new BusinessException("Trạng thái đơn đã bị thay đổi, vui lòng tải lại");
        }
        if (newStatus == 4) {
            repo.restoreStockForBill(billId);
        }
        String historyNote = note == null || note.isBlank()
                ? "Nhân viên #" + managerId + " cập nhật trạng thái đơn"
                : note.trim();
        repo.insertHistory(billId, oldStatus, newStatus, historyNote, managerId);
        return getForStaff(billId);
    }

    private Bill getEntity(long id, long customerId) {
        return repo.findByIdAndCustomer(id, customerId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy hóa đơn id=" + id));
    }
}
