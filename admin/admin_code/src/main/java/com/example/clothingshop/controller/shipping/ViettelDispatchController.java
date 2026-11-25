package com.example.clothingshop.controller.shipping;

import com.example.clothingshop.common.BusinessException;
import com.example.clothingshop.integration.shipping.ViettelPostClient;
import com.example.clothingshop.service.order.BillService;
import com.example.clothingshop.service.admin.AdminService;
import org.springframework.security.core.Authentication;
import com.example.clothingshop.service.shipping.ShippingQuoteService;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.bind.annotation.*;

@RestController
public class ViettelDispatchController {
  private final BillService bills;
  private final AdminService admin;
  private final ShippingQuoteService shipping;
  private final ViettelPostClient carrier;
  private final ViettelTrackingController tracking;
  private final JdbcTemplate db;
  private final TransactionTemplate transaction;
  private final String senderName;
  private final String senderPhone;

  public ViettelDispatchController(BillService bills, AdminService admin, ShippingQuoteService shipping,
      ViettelPostClient carrier, ViettelTrackingController tracking, JdbcTemplate db,
      TransactionTemplate transaction,
      @Value("${shop.shipping.sender-name:}") String senderName,
      @Value("${shop.shipping.sender-phone:}") String senderPhone) {
    this.bills = bills;
    this.admin = admin;
    this.shipping = shipping;
    this.carrier = carrier;
    this.tracking = tracking;
    this.db = db;
    this.transaction = transaction;
    this.senderName = senderName;
    this.senderPhone = senderPhone;
  }

  @PostMapping("/api/admin/shipping/orders/{id}/quote")
  public Object quote(@PathVariable long id) {
    var bill = bills.get(id);
    long count = bills.lines(id).stream().mapToLong(line -> line.quantity()).sum();
    boolean collect = "COD".equals(bill.paymentMethod()) && bill.paymentStatus() == 0;
    return shipping.quoteOrder(bill.shippingAddress(), count, bill.subTotal(),
        collect ? bill.totalAmount() : java.math.BigDecimal.ZERO);
  }

  @PostMapping("/api/admin/shipping/orders/{id}/create")
  public Map<String, Object> create(@PathVariable long id, @RequestBody Request input) {
    var linked = tracking.tracking(id);
    if (!linked.isEmpty()) return linked;
    String service = input.serviceCode() == null ? "" : input.serviceCode().strip();
    if (!service.matches("[A-Z0-9]{1,20}"))
      throw new BusinessException("Mã dịch vụ Viettel Post phải gồm chữ in hoa hoặc số.");
    var bill = bills.get(id);
    if (bill.status() != 2)
      throw new BusinessException("Đơn phải được duyệt phiếu xuất giao trước khi tạo vận đơn.");
    boolean collect = "COD".equals(bill.paymentMethod()) && bill.paymentStatus() == 0;
    if (!collect && bill.paymentStatus() != 1)
      throw new BusinessException("Đơn chưa được xác nhận thanh toán.");
    if (senderName.isBlank() || !senderPhone.matches("[0-9]{9,12}"))
      throw new BusinessException("Cần cấu hình Tên người gửi và Số điện thoại người gửi trên máy chủ.");
    String receiverPhone = bill.receiverPhone() == null ? "" :
        bill.receiverPhone().replaceAll("[ .()-]", "");
    if (!receiverPhone.matches("\\+?[0-9]{9,15}"))
      throw new BusinessException("Số điện thoại người nhận không hợp lệ.");
    if (bill.shippingAddress() == null || bill.shippingAddress().isBlank())
      throw new BusinessException("Địa chỉ giao hàng là trường bắt buộc.");
    var lines = bills.lines(id);
    long count = lines.stream().mapToLong(line -> line.quantity()).sum();
    var offers = shipping.quoteOrder(bill.shippingAddress(), count, bill.subTotal(),
        collect ? bill.totalAmount() : java.math.BigDecimal.ZERO);
    if (offers.stream().noneMatch(offer -> service.equals(offer.serviceCode())))
      throw new BusinessException("Dịch vụ Viettel Post đã chọn không còn khả dụng. Hãy lấy cước lại.");
    var payload = new LinkedHashMap<String, Object>();
    shipping.addParcelDimensions(payload, count);
    payload.put("ORDER_NUMBER", bill.billCode());
    payload.put("SENDER_FULLNAME", senderName);
    payload.put("SENDER_PHONE", senderPhone);
    payload.put("RECEIVER_FULLNAME", bill.receiverName());
    payload.put("RECEIVER_PHONE", receiverPhone);
    payload.put("RECEIVER_ADDRESS", bill.shippingAddress());
    payload.put("PRODUCT_TYPE", "HH");
    payload.put("PRODUCT_NAME", "Quần áo " + bill.billCode());
    payload.put("PRODUCT_QUANTITY", count);
    payload.put("PRODUCT_DETAIL", lines.stream().map(line -> Map.of(
        "PRODUCT_NAME", line.productName(),
        "PRODUCT_QUANTITY", line.quantity(),
        "PRODUCT_PRICE", line.unitPrice().setScale(0, RoundingMode.HALF_UP),
        "PRODUCT_WEIGHT", shipping.gramsPerItem())).toList());
    payload.put("PRODUCT_PRICE", bill.subTotal().setScale(0, RoundingMode.HALF_UP));
    payload.put("ORDER_SERVICE", service);
    // The shop bears the carrier charge. Collect the exact outstanding shop total once.
    payload.put("ORDER_PAYMENT", collect ? 3 : 1);
    payload.put("MONEY_COLLECTION", collect ? bill.totalAmount().setScale(0, RoundingMode.HALF_UP) : 0);
    payload.put("CHECK_UNIQUE", true);

    transaction.executeWithoutResult(ignored -> {
      var order = db.queryForList("""
          SELECT ID FROM BILL WHERE ID=? AND STATUS=2 AND DELETE_FLAG=0
            AND TRACKING_CODE IS NULL FOR UPDATE
          """, id);
      if (order.isEmpty() || !tracking.tracking(id).isEmpty())
        throw new BusinessException("Đơn đã đổi trạng thái hoặc đã có vận đơn. Hãy tải lại.");
      Long document = db.queryForObject("""
          SELECT MAX(ID) FROM INVENTORY_DOCUMENT WHERE BILL_ID=?
            AND DOC_TYPE='DISPATCH' AND STATUS='POSTED'
          """, Long.class, id);
      if (document == null)
        throw new BusinessException("Cần duyệt phiếu xuất giao trước khi gửi đơn giao hàng.");
      db.update("""
          MERGE INTO BILL_VIETTEL_DISPATCH x USING (SELECT ? BILL_ID FROM DUAL) source
          ON (x.BILL_ID=source.BILL_ID)
          WHEN MATCHED THEN UPDATE SET x.DOCUMENT_ID=?
          WHEN NOT MATCHED THEN INSERT (BILL_ID,DOCUMENT_ID,SEND_STATE)
          VALUES (source.BILL_ID,?,'DRAFT')
          """, id, document, document);
      int claimed = db.update("""
          UPDATE BILL_VIETTEL_DISPATCH SET SEND_STATE='SENDING', ATTEMPTS=ATTEMPTS+1,
            SERVICE_CODE=?, WEIGHT_GRAMS=?, REQUESTED_AT=LOCALTIMESTAMP,LAST_ERROR=NULL
          WHERE BILL_ID=? AND (SEND_STATE IN ('DRAFT','RETRY') OR
            (SEND_STATE='SENDING' AND REQUESTED_AT<LOCALTIMESTAMP-INTERVAL '2' MINUTE))
          """, service, payload.get("PRODUCT_WEIGHT"), id);
      if (claimed != 1)
        throw new BusinessException("Đơn giao đang được gửi hoặc đã xử lý. Hãy tải lại sau.");
    });
    try {
      String code = carrier.createOrder(payload);
      return tracking.link(id, new ViettelTrackingController.Link(code));
    } catch (RuntimeException error) {
      db.update("""
          UPDATE BILL_VIETTEL_DISPATCH SET SEND_STATE='RETRY', LAST_ERROR=?
          WHERE BILL_ID=? AND SEND_STATE='SENDING'
          """, "Chưa xác nhận được mã vận đơn từ Viettel Post.", id);
      throw error;
    }
  }
  @PostMapping("/api/admin/shipping/orders/{id}/cash-received")
  public Object cashReceived(@PathVariable long id, Authentication actor) {
    return bills.confirmDeliveredCash(id, admin.actor(actor.getName()));
  }

  public record Request(String serviceCode) {}
}
