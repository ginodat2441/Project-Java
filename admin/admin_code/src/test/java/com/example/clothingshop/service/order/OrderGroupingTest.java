package com.example.clothingshop.service.order;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import com.example.clothingshop.common.BusinessException;
import com.example.clothingshop.dto.admin.AdminRequests;
import com.example.clothingshop.repository.admin.*;
import com.example.clothingshop.repository.order.*;
import com.example.clothingshop.service.admin.*;
import com.example.clothingshop.service.inventory.InventoryService;
import java.util.*;
import org.junit.jupiter.api.Test;

class OrderGroupingTest {
  @Test void unpaidPrepaidOrderCannotBeApprovedOrDispatched() {
    var repo=mock(BillRepository.class);var db=mock(AdminRepository.class);
    var inventory=mock(InventoryService.class);var queries=mock(OrderOperationsRepository.class);
    var bill=new com.example.clothingshop.dto.order.BillView(1L,"TEST",1L,"Name","Phone","Address",
      java.math.BigDecimal.TEN,java.math.BigDecimal.ZERO,java.math.BigDecimal.ZERO,java.math.BigDecimal.TEN,"SEPAY",0,0,null);
    when(repo.findById(1L)).thenReturn(Optional.of(bill));
    var service=new BillService(repo,db,mock(AdminService.class),inventory,queries);
    assertThrows(BusinessException.class,()->service.changeStatus(1,1,1L,"test"));
    verifyNoInteractions(inventory);
    verify(repo,never()).updateStatus(anyLong(),anyInt(),anyInt(),anyLong());
  }
  @Test void groupsAreAppliedInSqlBeforePaging() {
    var db=mock(AdminRepository.class);
    var queries=new AdminOperationsRepository(db);
    new AdminSearchService(db,queries).orders(Map.of("orderGroup","shipping","page","2"));
    var sql=org.mockito.ArgumentCaptor.forClass(String.class);
    verify(db).page(sql.capture(),eq(2),eq(20),any(Object[].class));
    assertTrue(sql.getValue().contains(":og='shipping' AND STATUS=2"));
    assertTrue(sql.getValue().contains("PAYMENT_METHOD<>'COD' AND PAYMENT_STATUS=0"));
    assertTrue(sql.getValue().contains("STATUS IN (3,4,5,6)"));
  }
  @Test void invalidGroupRejected() {
    var db=mock(AdminRepository.class);var queries=mock(AdminOperationsRepository.class);
    assertThrows(BusinessException.class,()->new AdminSearchService(db,queries).orders(Map.of("orderGroup","invalid")));
    verifyNoInteractions(queries);
  }
  @Test void shippingNeverRewritesFinancialFields() {
    var db=mock(AdminRepository.class);var queries=mock(OrderOperationsRepository.class);
    var service=new BillService(mock(BillRepository.class),db,mock(AdminService.class),mock(InventoryService.class),queries);
    when(queries.findBill3(any(Object[].class))).thenReturn(Map.of("STATUS",1,"PAYMENT_STATUS",0,"PAYMENT_METHOD","COD"));
    assertThrows(BusinessException.class,()->service.shipping(1,new AdminRequests.Shipping("SEPAY",0,"A","B"),1));
    assertThrows(BusinessException.class,()->service.shipping(1,new AdminRequests.Shipping("COD",1,"A","B"),1));
    verify(queries,never()).updateBill6(any(Object[].class));
    service.shipping(1,new AdminRequests.Shipping("COD",0,"A","B"),1);
    verify(queries).updateBill6(any(Object[].class));
    var captured=new OrderOperationsRepository(db);
    captured.updateBill6("c","A","t","B","a",1L,"i",1L);
    var sql=org.mockito.ArgumentCaptor.forClass(String.class);
    verify(db).update(sql.capture(),any(Object[].class));
    assertFalse(sql.getValue().contains("PAYMENT_STATUS="));
    assertFalse(sql.getValue().contains("PAYMENT_METHOD="));
  }
}
