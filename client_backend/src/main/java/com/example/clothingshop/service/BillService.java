package com.example.clothingshop.service;

import com.example.clothingshop.common.BusinessException;
import com.example.clothingshop.common.NotFoundException;
import com.example.clothingshop.dto.request.BillRequest;
import com.example.clothingshop.dto.request.SePayIpnRequest;
import com.example.clothingshop.entity.Bill;
import com.example.clothingshop.entity.SePayPaymentAttempt;
import com.example.clothingshop.repositories.BillRepository;
import com.example.clothingshop.view.BillLineView;
import com.example.clothingshop.view.BillHistoryView;
import com.example.clothingshop.view.BillView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger logger = LoggerFactory.getLogger(BillService.class);

    private final BillRepository repo;
    private final BigDecimal shippingFee;
    private final SePayService sePayService;

    public BillService(BillRepository repo, SePayService sePayService,
                       @Value("${app.order.shipping-fee:30000}") BigDecimal shippingFee) {
        this.repo = repo;
        this.sePayService = sePayService;
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

        // 1. Giữ chỗ tồn kho + tính tiền
        BigDecimal subTotal = BigDecimal.ZERO;
        Map<Long, BigDecimal> priceMap = new HashMap<>();
        for (var item : req.items()) {
            BigDecimal price = repo.findPrice(item.productDetailId())
                    .orElseThrow(() -> new BusinessException("Sản phẩm chi tiết không tồn tại: " + item.productDetailId()));
            if (repo.reserveStock(item.productDetailId(), item.quantity()) == 0) {
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
        if ("SEPAY".equals(req.paymentMethod())) {
            repo.insertSePayPayment(billId, subTotal.add(ship), (String) p.get("code"));
        }

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
        Bill bill = repo.lockByIdAndCustomer(billId, customerId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy hóa đơn id=" + billId));
        if (bill.status() == null || bill.status() != 0) {
            throw new BusinessException("Chỉ có thể hủy đơn đang chờ xác nhận");
        }
        if (bill.paymentStatus() != null && bill.paymentStatus() == 1) {
            throw new BusinessException("Đơn hàng đã thanh toán; vui lòng liên hệ cửa hàng để được hỗ trợ");
        }
        if (repo.updateStatus(billId, bill.status(), 4, null) == 0) {
            throw new BusinessException("Trạng thái đơn đã bị thay đổi, vui lòng tải lại");
        }
        repo.releaseReservedStockForBill(billId);
        repo.insertHistory(billId, bill.status(), 4, "Khách hàng hủy đơn", null);
        return get(customerId, billId);
    }

    public List<BillHistoryView> history(long customerId, long billId) {
        getEntity(billId, customerId);
        return repo.findHistory(billId);
    }

    @Transactional
    public void createSePayPayment(long customerId, long billId) {
        Bill bill = repo.lockByIdAndCustomer(billId, customerId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy hóa đơn id=" + billId));
        if (!"SEPAY".equals(bill.paymentMethod())) {
            throw new BusinessException("Đơn hàng không sử dụng thanh toán SePay");
        }
        if (bill.status() == null || bill.status() == 4) {
            throw new BusinessException("Không thể thanh toán đơn hàng đã hủy");
        }
        if (bill.paymentStatus() == null || bill.paymentStatus() != 0) {
            throw new BusinessException("Đơn hàng không còn ở trạng thái chờ thanh toán");
        }

        SePayPaymentAttempt attempt = repo.findPendingSePayPayment(billId).orElse(null);
        if (attempt == null) {
            repo.insertSePayPayment(billId, bill.totalAmount(), bill.billCode());
        }
    }

    @Transactional
    public boolean confirmSePayPayment(SePayIpnRequest notification) {
        if (!sePayService.isValidPaymentNotification(notification)) {
            return false;
        }
        String invoiceNumber = sePayService.extractBillCode(notification).orElse(null);
        if (invoiceNumber == null) {
            return false;
        }
        SePayPaymentAttempt attempt = repo.findSePayPayment(invoiceNumber).orElse(null);
        if (attempt == null) {
            return false;
        }
        Bill bill = repo.lockById(attempt.billId()).orElse(null);
        if (bill == null) {
            return false;
        }
        attempt = repo.findSePayPayment(invoiceNumber).orElse(null);
        if (attempt == null || !"SEPAY".equals(bill.paymentMethod())) {
            return false;
        }
        BigDecimal receivedAmount = notification.transferAmount();
        if (attempt.amount().compareTo(receivedAmount) != 0
                || bill.totalAmount().compareTo(receivedAmount) != 0) {
            return false;
        }
        if (attempt.status() != null && attempt.status() == 1
                && bill.paymentStatus() != null && bill.paymentStatus() == 1) {
            return true;
        }
        if (attempt.status() == null || attempt.status() != 0
                || bill.paymentStatus() == null || bill.paymentStatus() != 0) {
            return false;
        }

        String note = "SePay xác nhận giao dịch " + notification.id()
                + (notification.referenceCode() == null || notification.referenceCode().isBlank()
                ? "" : " (" + notification.referenceCode() + ")");
        if (bill.status() != null && bill.status() == 4) {
            note += "; đơn đã hủy, cần kiểm tra hoàn tiền";
            logger.warn("SePay paid cancelled bill {}; review refund", bill.id());
        }
        if (repo.updateSePayPaymentStatus(invoiceNumber, 0, 1, note) == 0) {
            SePayPaymentAttempt latest = repo.findSePayPayment(invoiceNumber).orElse(null);
            return latest != null && latest.status() != null && latest.status() == 1
                    && bill.paymentStatus() == 1;
        }
        if (repo.markSePayBillPaid(bill.id()) == 0) {
            throw new BusinessException("Không thể cập nhật trạng thái thanh toán đơn hàng SePay");
        }
        return true;
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
        Bill bill = repo.lockById(billId)
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
        if (newStatus == 4 && bill.paymentStatus() != null && bill.paymentStatus() == 1) {
            throw new BusinessException("Đơn hàng đã thanh toán; cần hoàn tiền trước khi hủy đơn");
        }
        if (repo.updateStatus(billId, oldStatus, newStatus, managerId) == 0) {
            throw new BusinessException("Trạng thái đơn đã bị thay đổi, vui lòng tải lại");
        }
        if (newStatus == 4) {
            repo.releaseReservedStockForBill(billId);
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
