package com.example.clothingshop.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.clothingshop.common.BusinessException;
import com.example.clothingshop.entity.Bill;
import com.example.clothingshop.repositories.BillRepository;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class BillServiceCancellationTest {
    @Test
    void customerCanCancelPaidUnconfirmedOrderAndManualRefundIsRecorded() {
        var repo = new CancellationRepository(bill(0, 1));
        var service = new BillService(repo, new SePayService(""), BigDecimal.valueOf(30000));

        var cancelled = service.cancel(7L, 15L);

        assertEquals(4, cancelled.status());
        assertEquals(1, cancelled.paymentStatus());
        assertEquals(1, repo.releasedReservations);
        assertTrue(repo.historyNote.contains("cần hoàn tiền thủ công"));
    }

    @Test
    void customerCanCancelUnpaidUnconfirmedOrder() {
        var repo = new CancellationRepository(bill(0, 0));
        var service = new BillService(repo, new SePayService(""), BigDecimal.valueOf(30000));

        var cancelled = service.cancel(7L, 15L);

        assertEquals(4, cancelled.status());
        assertEquals("Khách hàng hủy đơn", repo.historyNote);
        assertEquals(1, repo.releasedReservations);
    }

    @Test
    void customerCannotCancelAfterAdminHasConfirmedOrder() {
        var repo = new CancellationRepository(bill(1, 0));
        var service = new BillService(repo, new SePayService(""), BigDecimal.valueOf(30000));

        assertThrows(BusinessException.class, () -> service.cancel(7L, 15L));
        assertEquals(0, repo.releasedReservations);
        assertEquals(0, repo.statusUpdates);
    }

    private static Bill bill(int status, int paymentStatus) {
        return new Bill(
                15L,
                "HD000000000000015",
                7L,
                3L,
                "Customer",
                "0900000000",
                "Address",
                BigDecimal.valueOf(100000),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.valueOf(100000),
                "SEPAY",
                paymentStatus,
                status,
                null,
                null,
                null,
                null);
    }

    private static final class CancellationRepository extends BillRepository {
        private Bill bill;
        private int releasedReservations;
        private int statusUpdates;
        private String historyNote;

        private CancellationRepository(Bill bill) {
            super(null);
            this.bill = bill;
        }

        @Override
        public Optional<Bill> lockByIdAndCustomer(long id, long customerId) {
            return id == bill.id() && customerId == bill.customerId()
                    ? Optional.of(bill)
                    : Optional.empty();
        }

        @Override
        public int updateStatus(long billId, int oldStatus, int newStatus, Long managerId) {
            statusUpdates++;
            if (billId != bill.id() || bill.status() != oldStatus) return 0;
            bill = new Bill(
                    bill.id(),
                    bill.billCode(),
                    bill.customerId(),
                    bill.addressId(),
                    bill.receiverName(),
                    bill.receiverPhone(),
                    bill.shippingAddress(),
                    bill.subTotal(),
                    bill.shippingFee(),
                    bill.discountAmount(),
                    bill.totalAmount(),
                    bill.paymentMethod(),
                    bill.paymentStatus(),
                    newStatus,
                    bill.note(),
                    bill.handledBy(),
                    bill.createdAt(),
                    bill.updatedAt());
            return 1;
        }

        @Override
        public void releaseReservedStockForBill(long billId) {
            releasedReservations++;
        }

        @Override
        public void insertHistory(
                long billId, Integer oldStatus, int newStatus, String note, Long managerId) {
            historyNote = note;
        }

        @Override
        public Optional<Bill> findByIdAndCustomer(long id, long customerId) {
            return id == bill.id() && customerId == bill.customerId()
                    ? Optional.of(bill)
                    : Optional.empty();
        }
    }
}
