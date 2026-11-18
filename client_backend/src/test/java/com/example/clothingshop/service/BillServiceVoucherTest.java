package com.example.clothingshop.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.example.clothingshop.common.BusinessException;
import com.example.clothingshop.dto.request.BillRequest;
import com.example.clothingshop.dto.request.VoucherQuoteRequest;
import com.example.clothingshop.repositories.BillRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class BillServiceVoucherTest {
    @Test
    void quoteDiscountsOnlySelectedProductsAndAddsShippingToTheServerTotal() {
        var repo = new VoucherRepository();
        var service = new BillService(repo, new SePayService(""), BigDecimal.valueOf(30000));
        var quote = service.quote(7L, new VoucherQuoteRequest("sale10", List.of(
                new BillRequest.Item(11L, 1),
                new BillRequest.Item(22L, 1))));

        assertEquals(0, quote.subtotal().compareTo(BigDecimal.valueOf(300000)));
        assertEquals(0, quote.eligibleSubtotal().compareTo(BigDecimal.valueOf(100000)));
        assertEquals(0, quote.discount().compareTo(BigDecimal.valueOf(10000)));
        assertEquals(0, quote.total().compareTo(BigDecimal.valueOf(320000)));
        assertEquals(0, repo.claimCount);
    }

    @Test
    void quoteRejectsVoucherWhenTheCartHasNoEligibleProducts() {
        var service = new BillService(
                new VoucherRepository(), new SePayService(""), BigDecimal.valueOf(30000));

        assertThrows(
                BusinessException.class,
                () -> service.quote(7L, new VoucherQuoteRequest(
                        "SALE10", List.of(new BillRequest.Item(22L, 1)))));
    }

    @Test
    void quoteRejectsVoucherForAnotherCustomerAudience() {
        var repo = new VoucherRepository();
        var service = new BillService(repo, new SePayService(""), BigDecimal.valueOf(30000));

        assertThrows(
                BusinessException.class,
                () -> service.quote(8L, new VoucherQuoteRequest(
                        "SALE10", List.of(new BillRequest.Item(11L, 1)))));
        assertEquals(8L, repo.checkedCustomerId);
    }

    private static final class VoucherRepository extends BillRepository {
        private int claimCount;
        private long checkedCustomerId;

        private VoucherRepository() {
            super(null);
        }

        @Override
        public Optional<BigDecimal> findPrice(long productDetailId) {
            return Optional.of(BigDecimal.valueOf(productDetailId == 11L ? 100000 : 200000));
        }

        @Override
        public List<Long> findVoucherProductIds(String code) {
            return List.of(1L);
        }

        @Override
        public Optional<Long> findProductId(long productDetailId) {
            return Optional.of(productDetailId == 11L ? 1L : 2L);
        }

        @Override
        public Optional<Map<String, Object>> findEligibleVoucher(
                String code, long customerId, BigDecimal eligibleSubtotal, boolean lock) {
            checkedCustomerId = customerId;
            if (eligibleSubtotal.signum() == 0 || customerId != 7L) return Optional.empty();
            return Optional.of(Map.of(
                    "ID", 9L,
                    "DISCOUNT_TYPE", "PERCENT",
                    "DISCOUNT_VALUE", BigDecimal.TEN,
                    "MAX_DISCOUNT", BigDecimal.valueOf(50000)));
        }

        @Override
        public int claimVoucher(long voucherId, long customerId, BigDecimal eligibleSubtotal) {
            claimCount++;
            return 1;
        }
    }
}
