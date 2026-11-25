package com.example.clothingshop;
import static org.junit.jupiter.api.Assertions.*;
import static com.example.clothingshop.repository.admin.AdminRepository.number;
import com.example.clothingshop.repository.admin.AdminRepository;
import com.example.clothingshop.service.order.*;
import com.example.clothingshop.service.inventory.InventoryService;
import com.example.clothingshop.dto.order.BillRequest;
import com.example.clothingshop.common.BusinessException;
import java.util.*;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest @Transactional
@EnabledIfEnvironmentVariable(named="RUN_ORDER_REQUESTS_ORACLE_TEST",matches="true")
class CustomerOrderRequestsOracleTest {
 @Autowired AdminRepository db;@Autowired BillService bills;@Autowired CustomerOrderRequestService requests;
 @Autowired InventoryService inventory;@Autowired RefundService refunds;
 long[] order(){
  var a=db.one("SELECT ID,CUSTOMER_ID FROM ADDRESS WHERE DELETE_FLAG=0 ORDER BY ID FETCH FIRST 1 ROWS ONLY");
  long sku=number(db.one("SELECT ID FROM PRODUCT_DETAIL WHERE DELETE_FLAG=0 AND STATUS=1 AND QUANTITY>2 AND AVERAGE_COST IS NOT NULL ORDER BY ID FETCH FIRST 1 ROWS ONLY"),"ID");
  long actor=number(db.one("SELECT m.ID FROM MANAGER m JOIN MANAGER_ROLE r ON r.ID=m.MANAGER_ROLE_ID WHERE r.ROLE_CODE='ADMIN' AND m.STATUS=1 AND m.DELETE_FLAG=0 FETCH FIRST 1 ROWS ONLY"),"ID");
  var b=bills.create(new BillRequest(number(a,"CUSTOMER_ID"),number(a,"ID"),"COD",null,"Rollback request acceptance",null,List.of(new BillRequest.Item(sku,1))));
  return new long[]{b.id(),number(a,"CUSTOMER_ID"),sku,actor};
 }
 long request(long[] ids,String type){return db.insert("INSERT INTO BILL_CUSTOMER_REQUEST(BILL_ID,CUSTOMER_ID,REQUEST_TYPE,REASON) VALUES(:b,:c,:t,'Khách gửi yêu cầu kiểm tra đơn')","b",ids[0],"c",ids[1],"t",type);}
 long stock(long sku){return number(db.one("SELECT RESERVED_QUANTITY FROM PRODUCT_DETAIL WHERE ID=:id","id",sku),"RESERVED_QUANTITY");}
 @Test void cancellationBlocksDispatchRejectKeepsStockApproveReleasesExactlyOnce(){
  var i=order();long held=stock(i[2]),r=request(i,"CANCEL");
  assertThrows(BusinessException.class,()->inventory.prepareDispatch(i[0],i[3]));
  assertThrows(BusinessException.class,()->bills.changeStatus(i[0],1,i[3],"Không bỏ qua hàng chờ duyệt"));
  requests.review(r,new CustomerOrderRequestService.Review("REJECTED","Vui lòng kiểm tra lại địa chỉ nhận hàng"),i[3]);
  assertEquals(held,stock(i[2]));assertEquals(0,bills.get(i[0]).status());
  r=request(i,"CANCEL");requests.review(r,new CustomerOrderRequestService.Review("APPROVED","Phản hồi: "+"a".repeat(490)),i[3]);
  assertEquals(4,bills.get(i[0]).status());assertEquals(held-1,stock(i[2]));
  long reviewed=r;assertThrows(BusinessException.class,()->requests.review(reviewed,new CustomerOrderRequestService.Review("APPROVED","Không được duyệt lại yêu cầu đã xử lý"),i[3]));
  assertEquals(held-1,stock(i[2]));
 }
 @Test void paidCancellationRequiresRecordedMoneyRefundWithCap(){
  var i=order();db.update("UPDATE BILL SET PAYMENT_STATUS=1 WHERE ID=:id","id",i[0]);
  requests.review(request(i,"CANCEL"),new CustomerOrderRequestService.Review("APPROVED","Chấp nhận hủy và sẽ hoàn khoản đã thu"),i[3]);
  assertEquals(1,bills.get(i[0]).paymentStatus());
  long doc=number(db.one("SELECT ID FROM INVENTORY_DOCUMENT WHERE BILL_ID=:id AND DOC_TYPE='RELEASE' AND STATUS='POSTED'","id",i[0]),"ID");
  BigDecimal total=bills.get(i[0]).totalAmount();
  assertThrows(IllegalArgumentException.class,()->refunds.save(i[0],null,new RefundService.Request(doc,total.add(BigDecimal.ONE),"PAID","TEST-OVER-"+i[0],"Kiểm tra giới hạn tiền được hoàn"),i[3]));
  refunds.save(i[0],null,new RefundService.Request(doc,total,"PAID","TEST-REFUND-"+i[0],"Đã hoàn đủ tiền hủy đơn cho khách"),i[3]);
  assertEquals(2,bills.get(i[0]).paymentStatus());
 }
 @Test void returnApprovalDoesNotReceiveStockOrRefundMoney(){
  var i=order();db.update("UPDATE BILL SET STATUS=3,PAYMENT_STATUS=1,COMPLETED_AT=LOCALTIMESTAMP WHERE ID=:id","id",i[0]);
  long held=stock(i[2]);requests.review(request(i,"RETURN"),new CustomerOrderRequestService.Review("APPROVED","Vui lòng liên hệ cửa hàng để gửi trả hàng"),i[3]);
  assertEquals(3,bills.get(i[0]).status());assertEquals(1,bills.get(i[0]).paymentStatus());assertEquals(held,stock(i[2]));
  assertEquals("REQUESTED",db.one("SELECT RETURN_STATUS FROM BILL WHERE ID=:id","id",i[0]).get("RETURN_STATUS"));
 }
 @Test void deliveredOrderReturnReceivesPhysicalStockBeforeRecordingRefund(){
  var i=order();
  bills.changeStatus(i[0],1,i[3],"Xác nhận đơn kiểm thử");
  long dispatch=inventory.prepareDispatch(i[0],i[3]);inventory.approve(dispatch,i[3]);
  bills.changeStatus(i[0],3,i[3],"Khách đã nhận hàng và thanh toán COD");
  long before=number(db.one("SELECT PHYSICAL_QUANTITY FROM PRODUCT_DETAIL WHERE ID=:id","id",i[2]),"PHYSICAL_QUANTITY");
  long r=request(i,"RETURN");
  var document=new com.example.clothingshop.dto.inventory.InventoryRequests.Document("RETURN","Tiếp nhận sản phẩm khách gửi trả",i[0],List.of(new com.example.clothingshop.dto.inventory.InventoryRequests.Line(i[2],1,true,"Đã kiểm tra, đủ điều kiện nhập lại",null)));
  assertThrows(BusinessException.class,()->inventory.save(null,document,i[3]));
  requests.review(r,new CustomerOrderRequestService.Review("APPROVED","Đồng ý trả hàng, khách gửi theo hướng dẫn"),i[3]);
  long doc=inventory.save(null,document,i[3]);inventory.approve(doc,i[3]);
  assertEquals(before+1,number(db.one("SELECT PHYSICAL_QUANTITY FROM PRODUCT_DETAIL WHERE ID=:id","id",i[2]),"PHYSICAL_QUANTITY"));
  assertEquals(5,bills.get(i[0]).status());
  refunds.save(i[0],null,new RefundService.Request(doc,bills.get(i[0]).subTotal(),"PAID","TEST-RETURN-"+i[0],"Đã nhận hàng và hoàn tiền hàng cho khách"),i[3]);
 }
}
