package com.example.clothingshop.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.example.clothingshop.common.BusinessException;
import com.example.clothingshop.dto.request.BillRequest;
import com.example.clothingshop.repositories.BillRepository;
import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.Test;

class PurchaseQuoteTest {
    final BillRepository repo=mock(BillRepository.class);
    final BillService service=new BillService(repo,mock(SePayService.class),new BigDecimal("30000"));
    @Test void quoteReadsOraclePriceAndNeverReservesOrAddsToCart() {
        when(repo.findPrice(12)).thenReturn(Optional.of(new BigDecimal("200000")));
        when(repo.availableStock(12)).thenReturn(10);
        var quote=service.quote(List.of(new BillRequest.Item(12L,2)));
        assertEquals(new BigDecimal("430000"),quote.get("totalAmount"));
        verify(repo,never()).reserveStock(anyLong(),anyInt());
        verify(repo,never()).insertBill(anyMap());
    }
    @Test void insufficientStockCannotBeQuoted() {
        when(repo.findPrice(12)).thenReturn(Optional.of(BigDecimal.TEN));
        when(repo.availableStock(12)).thenReturn(0);
        assertThrows(BusinessException.class,()->service.quote(List.of(new BillRequest.Item(12L,1))));
    }
    @Test void duplicateOrBadQuantityRejectedBeforeReservation() {
        var duplicate=List.of(new BillRequest.Item(12L,1),new BillRequest.Item(12L,1));
        assertThrows(BusinessException.class,()->service.quote(duplicate));
        assertThrows(BusinessException.class,()->service.create(1,new BillRequest(1L,"COD",null,duplicate)));
        assertThrows(BusinessException.class,()->service.quote(List.of(new BillRequest.Item(12L,0))));
        verifyNoInteractions(repo);
    }
}
