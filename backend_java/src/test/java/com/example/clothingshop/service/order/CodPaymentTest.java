package com.example.clothingshop.service.order;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.example.clothingshop.common.BusinessException;
import com.example.clothingshop.dto.admin.AdminRequests;
import com.example.clothingshop.dto.order.BillView;
import com.example.clothingshop.repository.admin.AdminRepository;
import com.example.clothingshop.repository.order.*;
import com.example.clothingshop.service.admin.AdminService;
import com.example.clothingshop.service.inventory.InventoryService;
import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.Test;

class CodPaymentTest {
  final BillRepository repo=mock(BillRepository.class);
  final OrderOperationsRepository queries=mock(OrderOperationsRepository.class);
  final BillService service=new BillService(repo,mock(AdminRepository.class),mock(AdminService.class),mock(InventoryService.class),queries);
  @Test void shippingEditorCannotChangeMethodOrMarkCodPaid() {
    when(queries.findBill3("i",1L)).thenReturn(Map.of("STATUS",1,"PAYMENT_STATUS",0,"PAYMENT_METHOD","COD"));
    assertThrows(BusinessException.class,()->service.shipping(1L,new AdminRequests.Shipping("BANK",0,"Carrier","TRACK"),7L));
    assertThrows(BusinessException.class,()->service.shipping(1L,new AdminRequests.Shipping("COD",1,"Carrier","TRACK"),7L));
    verify(queries,never()).updateBill6(any(Object[].class));
  }
  @Test void shippingDetailsCanChangeWhileCodPaymentRemainsPending() {
    when(queries.findBill3("i",1L)).thenReturn(Map.of("STATUS",1,"PAYMENT_STATUS",0,"PAYMENT_METHOD","COD"));
    service.shipping(1L,new AdminRequests.Shipping("COD",0,"Carrier","TRACK"),7L);
    verify(queries).updateBill6("p","COD","s",0,"c","Carrier","t","TRACK","a",7L,"i",1L);
  }
  @Test void completedOrderCannotBeEdited() {
    when(queries.findBill3("i",1L)).thenReturn(Map.of("STATUS",3,"PAYMENT_STATUS",1,"PAYMENT_METHOD","COD"));
    assertThrows(BusinessException.class,()->service.shipping(1L,new AdminRequests.Shipping("COD",1,"Changed","X"),7L));
    verify(queries,never()).updateBill6(any(Object[].class));
  }
  @Test void successfulDeliveryRecordsCodCollectionOnceAndRejectsRepeatedTransition() {
    var pending=new BillView(1L,"TEST",9L,"Name","Phone","Address",BigDecimal.TEN,BigDecimal.ZERO,BigDecimal.ZERO,BigDecimal.TEN,"COD",0,2,null);
    var completed=new BillView(1L,"TEST",9L,"Name","Phone","Address",BigDecimal.TEN,BigDecimal.ZERO,BigDecimal.ZERO,BigDecimal.TEN,"COD",1,3,null);
    when(repo.findById(1L)).thenReturn(Optional.of(pending),Optional.of(completed),Optional.of(completed));
    when(repo.updateStatus(1L,2,3,7L)).thenReturn(1);when(queries.updateBill5("i",1L)).thenReturn(1);
    service.changeStatus(1L,3,7L,"Delivered and collected");
    verify(queries).recordCodPayment(1L);
    assertThrows(BusinessException.class,()->service.changeStatus(1L,3,7L,"Retry"));
    verify(queries,times(1)).recordCodPayment(1L);
  }
}
