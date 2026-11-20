package com.example.clothingshop;
import static org.junit.jupiter.api.Assertions.*;
import com.example.clothingshop.common.*;
import com.example.clothingshop.service.*;
import com.example.clothingshop.dto.request.BillRequest;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest @Transactional
@EnabledIfEnvironmentVariable(named="RUN_ORDER_REQUESTS_ORACLE_TEST",matches="true")
class OrderRequestsOracleTest {
 @Autowired SqlHelper sql;@Autowired BillService bills;@Autowired OrderRequestService requests;
 long[] order(){
  var a=sql.maps("SELECT ID,CUSTOMER_ID FROM ADDRESS WHERE DELETE_FLAG=0 ORDER BY ID FETCH FIRST 1 ROWS ONLY",Map.of()).get(0);
  long customer=((Number)a.get("CUSTOMER_ID")).longValue();
  long sku=sql.scalar("SELECT ID FROM PRODUCT_DETAIL WHERE DELETE_FLAG=0 AND STATUS=1 AND QUANTITY>2 ORDER BY ID FETCH FIRST 1 ROWS ONLY",Map.of(),Long.class).orElseThrow();
  var bill=bills.create(customer,new BillRequest(((Number)a.get("ID")).longValue(),"COD","Rollback order request check",List.of(new BillRequest.Item(sku,1))));
  return new long[]{customer,bill.id(),sku};
 }
 @Test void ownedRequestPersistsWithoutCancellingOrReleasingStockAndCannotDuplicate(){
  var ids=order();var p=Map.<String,Object>of("id",ids[1],"sku",ids[2]);
  int held=sql.scalar("SELECT RESERVED_QUANTITY FROM PRODUCT_DETAIL WHERE ID=:sku",p,Integer.class).orElseThrow();
  long id=requests.create(ids[0],new OrderRequestService.Request(ids[1],"CANCEL","Lý do: "+"a".repeat(493)));
  assertTrue(id>0);assertEquals(0,bills.get(ids[0],ids[1]).status());
  assertThrows(BusinessException.class,()->bills.updateManagedStatus(ids[1],1,"Không được bỏ qua yêu cầu",1,true));
  assertEquals(held,sql.scalar("SELECT RESERVED_QUANTITY FROM PRODUCT_DETAIL WHERE ID=:sku",p,Integer.class).orElseThrow());
  assertTrue(requests.overview(ids[0]).stream().anyMatch(r->((Number)r.get("billId")).longValue()==ids[1]&&!Boolean.TRUE.equals(r.get("canCancel"))));
  assertThrows(NotFoundException.class,()->requests.create(-1,new OrderRequestService.Request(ids[1],"CANCEL","Không phải đơn của khách này")));
  assertThrows(BusinessException.class,()->requests.create(ids[0],new OrderRequestService.Request(ids[1],"CANCEL","Gửi trùng yêu cầu hủy đơn")));
 }
 @Test void shippingCannotCancelAndReturnsExpireAfterFifteenDays(){
  var ids=order();var p=Map.<String,Object>of("id",ids[1]);
  sql.update("UPDATE BILL SET STATUS=2,DISPATCHED=1 WHERE ID=:id",p);
  assertThrows(BusinessException.class,()->requests.create(ids[0],new OrderRequestService.Request(ids[1],"CANCEL","Hàng đang giao không được hủy")));
  sql.update("UPDATE BILL SET STATUS=3,COMPLETED_AT=LOCALTIMESTAMP-INTERVAL '16' DAY WHERE ID=:id",p);
  assertThrows(BusinessException.class,()->requests.create(ids[0],new OrderRequestService.Request(ids[1],"RETURN","Yêu cầu sau thời hạn trả hàng")));
  sql.update("UPDATE BILL SET COMPLETED_AT=LOCALTIMESTAMP-INTERVAL '1' DAY WHERE ID=:id",p);
  assertTrue(requests.create(ids[0],new OrderRequestService.Request(ids[1],"RETURN","Sản phẩm bị lỗi đường may"))>0);
 }
}
