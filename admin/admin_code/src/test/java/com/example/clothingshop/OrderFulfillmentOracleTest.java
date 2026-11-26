package com.example.clothingshop;

import static org.junit.jupiter.api.Assertions.*;
import com.example.clothingshop.controller.shipping.ViettelTrackingController;
import com.example.clothingshop.service.inventory.InventoryService;
import com.example.clothingshop.service.order.BillService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(properties = "shop.viettel.webhook-secret=fulfillment-test-only-secret-123456789")
@EnabledIfEnvironmentVariable(named = "RUN_FULFILLMENT_ORACLE_TEST", matches = "true")
@Transactional
class OrderFulfillmentOracleTest {
  @Autowired JdbcTemplate db;
  @Autowired BillService bills;
  @Autowired InventoryService inventory;
  @Autowired ViettelTrackingController tracking;
  @Autowired ObjectMapper json;

  @Test
  void confirmationDispatchAndDeliveredCallbackUseOneLinkedOrder() {
    long id = db.queryForObject("""
        SELECT MIN(ID) FROM BILL WHERE BILL_CODE LIKE 'DEMO-HD-%'
          AND STATUS=0 AND PAYMENT_METHOD='COD' AND STOCK_RESERVED=1 AND DELETE_FLAG=0
        """, Long.class);
    long actor = db.queryForObject("""
        SELECT MIN(m.ID) FROM MANAGER m JOIN MANAGER_ROLE r ON r.ID=m.MANAGER_ROLE_ID
        WHERE m.STATUS=1 AND m.DELETE_FLAG=0 AND r.ROLE_CODE='ADMIN' AND r.DELETE_FLAG=0
        """, Long.class);
    bills.changeStatus(id, 1, actor, "Kiểm thử có rollback");
    bills.changeStatus(id, 1, actor, "Gọi lại thao tác xác nhận");
    var document = db.queryForList("""
        SELECT ID,WAREHOUSE_NAME FROM INVENTORY_DOCUMENT
        WHERE BILL_ID=? AND DOC_TYPE='DISPATCH' AND STATUS='DRAFT'
        """, id);
    assertEquals(1, document.size());
    assertNotNull(document.get(0).get("WAREHOUSE_NAME"));
    long doc = ((Number) document.get(0).get("ID")).longValue();
    assertEquals(doc, ((Number) db.queryForMap(
        "SELECT DOCUMENT_ID FROM BILL_VIETTEL_DISPATCH WHERE BILL_ID=? AND SEND_STATE='DRAFT'",
        id).get("DOCUMENT_ID")).longValue());
    inventory.approve(doc, actor);
    assertEquals(2, bills.get(id).status());

    String code = "TEST-" + UUID.randomUUID().toString().substring(0, 20).toUpperCase();
    tracking.link(id, new ViettelTrackingController.Link(code));
    var event = json.createObjectNode();
    event.put("TOKEN", "fulfillment-test-only-secret-123456789");
    event.putObject("DATA").put("ORDER_NUMBER", code).put("ORDER_STATUS", 501)
        .put("STATUS_NAME", "Giao thành công").put("ORDER_STATUSDATE", "21/11/2026 10:00:00");
    tracking.accept("", event);
    tracking.accept("", event);
    assertEquals(3, bills.get(id).status());
    assertEquals(0, bills.get(id).paymentStatus());
    assertEquals(1, db.queryForObject(
        "SELECT COUNT(*) FROM BILL_VIETTEL_EVENT WHERE BILL_ID=?", Integer.class, id));
    assertEquals(1, bills.confirmDeliveredCash(id, actor).paymentStatus());
    assertEquals(1, bills.confirmDeliveredCash(id, actor).paymentStatus());
    assertEquals(1, db.queryForObject("""
        SELECT COUNT(*) FROM PAYMENT_HISTORY
        WHERE BILL_ID=? AND PAYMENT_METHOD='COD' AND STATUS=1
        """, Integer.class, id));
  }
}
