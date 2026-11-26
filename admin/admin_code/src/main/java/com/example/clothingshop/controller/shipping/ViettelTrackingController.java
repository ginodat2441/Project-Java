package com.example.clothingshop.controller.shipping;

import com.example.clothingshop.common.BusinessException;
import com.fasterxml.jackson.databind.JsonNode;
import com.example.clothingshop.service.order.BillService;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class ViettelTrackingController {
  public static final String WEBHOOK = "/api/shipping/viettel-webhook";
  private static final DateTimeFormatter EVENT_DATE =
      DateTimeFormatter.ofPattern("dd/MM/uuuu HH:mm:ss").withResolverStyle(ResolverStyle.STRICT);
  private final JdbcTemplate db;
  private final BillService bills;
  private final String secret;

  public ViettelTrackingController(JdbcTemplate db, BillService bills,
      @Value("${shop.viettel.webhook-secret:}") String secret) {
    this.db = db;
    this.bills = bills;
    this.secret = secret;
  }

  @GetMapping("/api/admin/orders/{id}/viettel-tracking")
  public Map<String, Object> tracking(@PathVariable long id) {
    return db.queryForList("""
        SELECT TRACKING_CODE, STATUS_CODE, STATUS_NAME,
               TO_CHAR(EVENT_AT, 'YYYY-MM-DD HH24:MI:SS') EVENT_AT
        FROM BILL_VIETTEL_TRACKING WHERE BILL_ID=?
        """, id).stream().findFirst().orElseGet(Map::of);
  }

  @GetMapping("/api/admin/orders/{id}/viettel-fulfillment")
  public Map<String, Object> fulfillment(@PathVariable long id) {
    var delivery = db.queryForList("""
        SELECT s.SEND_STATE,s.ATTEMPTS,s.SERVICE_CODE,s.WEIGHT_GRAMS,s.LAST_ERROR,
          d.ID DOCUMENT_ID,d.DOC_CODE,d.STATUS DOCUMENT_STATUS,d.WAREHOUSE_NAME
        FROM BILL_VIETTEL_DISPATCH s LEFT JOIN INVENTORY_DOCUMENT d ON d.ID=s.DOCUMENT_ID
        WHERE s.BILL_ID=?
        """, id).stream().findFirst().orElseGet(Map::of);
    var history = db.queryForList("""
        SELECT STATUS_CODE,STATUS_NAME,TO_CHAR(EVENT_AT,'YYYY-MM-DD HH24:MI:SS') EVENT_AT
        FROM BILL_VIETTEL_EVENT WHERE BILL_ID=? ORDER BY EVENT_AT DESC, STATUS_CODE DESC
        FETCH FIRST 100 ROWS ONLY
        """, id);
    return Map.of("delivery", delivery, "history", history);
  }

  @org.springframework.transaction.annotation.Transactional
  @PutMapping("/api/admin/orders/{id}/viettel-tracking")
  public Map<String, Object> link(@PathVariable long id, @RequestBody Link request) {
    String code = request.trackingCode() == null ? "" : request.trackingCode().strip();
    if (!code.matches("[A-Za-z0-9-]{1,50}"))
      throw new BusinessException("Mã vận đơn Viettel Post chỉ được dùng chữ, số và dấu gạch ngang.");
    code = code.toUpperCase(java.util.Locale.ROOT);
    var orders = db.queryForList(
        "SELECT CARRIER, TRACKING_CODE FROM BILL WHERE ID=? AND DELETE_FLAG=0 FOR UPDATE", id);
    if (orders.isEmpty())
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy đơn hàng.");
    var order = orders.get(0);
    String oldCode = (String) order.get("TRACKING_CODE");
    String oldCarrier = (String) order.get("CARRIER");
    if (oldCode != null && (!oldCode.equalsIgnoreCase(code)
        || oldCarrier != null && !oldCarrier.matches("(?i)VIETTEL[ _]?POST")))
      throw new BusinessException("Đơn đã có thông tin vận chuyển khác. Hãy kiểm tra trước khi gắn mã.");
    Object linkedCode = tracking(id).get("TRACKING_CODE");
    if (linkedCode != null && !code.equals(linkedCode))
      throw new BusinessException("Đơn đã gắn mã vận đơn Viettel Post khác.");
    try {
      int changed = db.update("""
          MERGE INTO BILL_VIETTEL_TRACKING target
          USING (SELECT ID FROM BILL WHERE ID=? AND DELETE_FLAG=0) source
          ON (target.BILL_ID=source.ID)
          WHEN MATCHED THEN UPDATE SET target.TRACKING_CODE=?, target.STATUS_CODE=NULL,
              target.STATUS_NAME=NULL, target.EVENT_AT=NULL, target.RECEIVED_AT=NULL
              WHERE target.TRACKING_CODE<>?
          WHEN NOT MATCHED THEN INSERT (BILL_ID, TRACKING_CODE) VALUES (source.ID, ?)
          """, id, code, code, code);
      if (changed == 0 && tracking(id).isEmpty())
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy đơn hàng.");
    } catch (DuplicateKeyException e) {
      throw new BusinessException("Mã vận đơn Viettel Post đã được gắn với một đơn hàng.");
    }
    db.update("UPDATE BILL SET CARRIER='VIETTEL_POST', TRACKING_CODE=? WHERE ID=?", code, id);
    db.update("UPDATE BILL_VIETTEL_DISPATCH SET SEND_STATE='LINKED',LAST_ERROR=NULL WHERE BILL_ID=?", id);
    return tracking(id);
  }

  @org.springframework.transaction.annotation.Transactional
  @PostMapping(WEBHOOK)
  public Map<String, Object> accept(
      @RequestHeader(value = "Authorization", defaultValue = "") String authorization,
      @RequestBody JsonNode payload) {
    if (secret.length() < 32)
      throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Webhook chưa được cấu hình.");
    String supplied = authorization.isBlank() ? payload.path("TOKEN").asText("")
        : authorization.startsWith("Bearer ") ? authorization.substring(7) : authorization;
    if (!MessageDigest.isEqual(secret.getBytes(StandardCharsets.UTF_8),
        supplied.getBytes(StandardCharsets.UTF_8)))
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Webhook không được xác thực.");

    JsonNode event = payload.path("DATA");
    String code = event.path("ORDER_NUMBER").asText("").strip().toUpperCase(java.util.Locale.ROOT);
    String name = event.path("STATUS_NAME").asText("").strip();
    int status = event.path("ORDER_STATUS").asInt(-1);
    if (!code.matches("[A-Z0-9-]{1,50}") || status < 0 || status > 9999 || name.length() > 300)
      throw new BusinessException("Sự kiện vận chuyển thiếu mã vận đơn hoặc trạng thái hợp lệ.");
    Timestamp timestamp;
    try {
      timestamp = Timestamp.valueOf(LocalDateTime.parse(
          event.path("ORDER_STATUSDATE").asText(), EVENT_DATE));
    } catch (DateTimeParseException e) {
      throw new BusinessException("Thời điểm trạng thái Viettel Post không hợp lệ.");
    }
    var ids = db.queryForList("SELECT BILL_ID FROM BILL_VIETTEL_TRACKING WHERE TRACKING_CODE=?",
        Long.class, code);
    if (ids.isEmpty()) {
      String reference = event.path("ORDER_REFERENCE").asText("").strip();
      ids = db.queryForList("""
          SELECT b.ID FROM BILL b JOIN BILL_VIETTEL_DISPATCH d ON d.BILL_ID=b.ID
          WHERE b.BILL_CODE=? AND b.STATUS=2 AND b.DELETE_FLAG=0
            AND b.TRACKING_CODE IS NULL AND d.SEND_STATE IN ('SENDING','RETRY')
          """, Long.class, reference);
      if (ids.size() == 1) link(ids.get(0), new Link(code));
    }
    if (ids.size() != 1) return Map.of("status", 200, "error", false, "message", "OK");
    long bill = ids.get(0);
    if (db.queryForList("SELECT ID FROM BILL WHERE ID=? AND DELETE_FLAG=0 FOR UPDATE", bill).isEmpty())
      return Map.of("status", 200, "error", false, "message", "OK");
    if (!code.equals(tracking(bill).get("TRACKING_CODE")))
      return Map.of("status", 200, "error", false, "message", "OK");
    db.update("""
        MERGE INTO BILL_VIETTEL_EVENT e
        USING (SELECT ? BILL_ID, ? EVENT_AT, ? STATUS_CODE FROM DUAL) source
        ON (e.BILL_ID=source.BILL_ID AND e.EVENT_AT=source.EVENT_AT AND e.STATUS_CODE=source.STATUS_CODE)
        WHEN NOT MATCHED THEN INSERT (BILL_ID,EVENT_AT,STATUS_CODE,STATUS_NAME)
        VALUES (source.BILL_ID,source.EVENT_AT,source.STATUS_CODE,?)
        """, bill, timestamp, status, name);
    int updated = db.update("""
        UPDATE BILL_VIETTEL_TRACKING SET STATUS_CODE=?, STATUS_NAME=?, EVENT_AT=?,
               RECEIVED_AT=LOCALTIMESTAMP
        WHERE TRACKING_CODE=? AND (EVENT_AT IS NULL OR EVENT_AT<?)
        """, status, name, timestamp, code, timestamp);
    if (updated == 1 && status == 501) bills.completeCarrierDelivery(bill);
    return Map.of("status", 200, "error", false, "message", "OK");
  }

  public record Link(String trackingCode) {}
}
