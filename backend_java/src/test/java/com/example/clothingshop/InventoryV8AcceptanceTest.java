package com.example.clothingshop;

import static com.example.clothingshop.repository.admin.AdminRepository.number;
import static org.junit.jupiter.api.Assertions.*;

import com.example.clothingshop.dto.admin.AdminRequests;
import com.example.clothingshop.dto.inventory.InventoryRequests;
import com.example.clothingshop.dto.order.BillRequest;
import com.example.clothingshop.integration.auth.RecoveryDelivery;
import com.example.clothingshop.repository.admin.AdminRepository;
import com.example.clothingshop.service.admin.AdminService;
import com.example.clothingshop.service.auth.PasswordRecoveryService;
import com.example.clothingshop.service.inventory.InventoryService;
import com.example.clothingshop.service.inventory.StockCostService;
import com.example.clothingshop.service.order.BillService;
import com.example.clothingshop.service.order.RefundService;
import com.example.clothingshop.service.product.ProductMetricsService;
import com.example.clothingshop.service.report.ReportService;
import com.fasterxml.jackson.databind.*;
import java.math.*;
import java.net.*;
import java.net.http.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@EnabledIfEnvironmentVariable(named = "RUN_V8_TESTS", matches = "true")
class InventoryV8AcceptanceTest {
  @Autowired AdminRepository db;
  @Autowired AdminService admin;
  @Autowired InventoryService inventory;
  @Autowired BillService bills;
  @Autowired StockCostService costs;
  @Autowired ProductMetricsService metrics;
  @Autowired ReportService reports;
  @Autowired PasswordEncoder encoder;
  @Autowired ObjectMapper json;
  @Autowired RefundService refunds;
  @Autowired PasswordRecoveryService recovery;
  @Autowired RecoveryDelivery delivery;
  @LocalServerPort int port;
  final String tag = "V8" + UUID.randomUUID().toString().substring(0, 8),
      password = UUID.randomUUID() + "Aa1!";
  long actor, staff, product, first, second, customer, address;
  List<Long> products = new ArrayList<>();

  BigDecimal n(long v) {
    return BigDecimal.valueOf(v);
  }

  long count(String sql, Object... params) {
    return number(db.one(sql, params), "N");
  }

  long q(long id, String column) {
    return number(db.one("SELECT " + column + " N FROM PRODUCT_DETAIL WHERE ID=:i", "i", id), "N");
  }

  BigDecimal cost(long id) {
    return costs.current(id);
  }

  long receive(long v, int q, BigDecimal cost) {
    long id =
        inventory.save(
            null,
            new InventoryRequests.Document(
                "RECEIPT", tag, null, List.of(new InventoryRequests.Line(v, q, true, tag, cost))),
            actor);
    inventory.approve(id, actor);
    return id;
  }

  long manual(String type, long v, int q, BigDecimal cost) {
    return inventory.save(
        null,
        new InventoryRequests.Document(
            type, tag, null, List.of(new InventoryRequests.Line(v, q, true, tag, cost))),
        actor);
  }

  long order(long v, int q) {
    return bills
        .create(
            new BillRequest(
                customer, address, "COD", n(30000), tag, null, List.of(new BillRequest.Item(v, q))))
        .id();
  }

  void ship(long bill) {
    bills.changeStatus(bill, 1, actor, tag);
    bills.changeStatus(bill, 2, actor, tag);
  }

  long returned(long bill, long v, int q, boolean sellable) {
    long doc =
        inventory.save(
            null,
            new InventoryRequests.Document(
                "RETURN",
                tag,
                bill,
                List.of(new InventoryRequests.Line(v, q, sellable, tag, null))),
            actor);
    inventory.approve(doc, actor);
    return doc;
  }

  @BeforeEach
  void fixtures() {
    assertTrue(
        System.getenv("SPRING_DATASOURCE_USERNAME").endsWith("_STAGE"),
        "Acceptance tests must target the isolated schema");
    actor = manager("ADMIN");
    staff = manager("STAFF");
    long role = number(db.one("SELECT MIN(ID) N FROM ROLES"), "N");
    long u =
        db.insert(
            "INSERT INTO USERS(ROLE_ID,USERNAME,EMAIL,PASSWORD_HASH) VALUES(:r,:u,:e,:p)",
            "r",
            role,
            "u",
            tag,
            "e",
            tag + "@example.test",
            "p",
            encoder.encode(password));
    customer =
        db.insert(
            "INSERT INTO CUSTOMER(USER_ID,FULL_NAME,PHONE) VALUES(:u,:n,'0901234567')",
            "u",
            u,
            "n",
            tag);
    address =
        db.insert(
            "INSERT INTO"
                + " ADDRESS(CUSTOMER_ID,RECEIVER_NAME,RECEIVER_PHONE,PROVINCE,WARD,DETAIL_ADDRESS)"
                + " VALUES(:c,:n,'0901234567','Demo','Demo','Demo')",
            "c",
            customer,
            "n",
            tag);
    long c1 = number(db.rows("SELECT ID FROM COLOR WHERE DELETE_FLAG=0 ORDER BY ID").get(0), "ID"),
        c2 = number(db.rows("SELECT ID FROM COLOR WHERE DELETE_FLAG=0 ORDER BY ID").get(1), "ID");
    product =
        admin.product(
            null,
            new AdminRequests.Product(
                null,
                tag,
                null,
                null,
                null,
                2,
                tag,
                "/store/assets/img/product/product-1.webp",
                1,
                List.of(
                    new AdminRequests.Variant(null, c1, "XL", null, n(199000), 0, 1),
                    new AdminRequests.Variant(null, c2, "M", null, n(299000), 0, 1)),
                tag,
                tag),
            actor);
    products.add(product);
    var vs = db.rows("SELECT ID FROM PRODUCT_DETAIL WHERE PRODUCT_ID=:p ORDER BY ID", "p", product);
    first = number(vs.get(0), "ID");
    second = number(vs.get(1), "ID");
  }

  long manager(String role) {
    long r = number(db.one("SELECT ID FROM MANAGER_ROLE WHERE ROLE_CODE=:r", "r", role), "ID");
    return db.insert(
        "INSERT INTO MANAGER(MANAGER_ROLE_ID,USERNAME,FULL_NAME,EMAIL,PASSWORD_HASH)"
            + " VALUES(:r,:u,:u,:e,:p)",
        "r",
        r,
        "u",
        tag + role,
        "e",
        tag + role + "@example.test",
        "p",
        encoder.encode(password));
  }

  @Test
  void physicalReserveDispatchCostsReturnsAndReports() throws Exception {
    receive(first, 90, n(107000));
    receive(second, 20, n(80000));
    assertEquals(0, cost(first).compareTo(n(107000)));
    assertEquals(0, cost(second).compareTo(n(80000)));
    long bill = order(first, 5);
    assertEquals(85, q(first, "QUANTITY"));
    assertEquals(90, q(first, "PHYSICAL_QUANTITY"));
    assertEquals(5, q(first, "RESERVED_QUANTITY"));
    @SuppressWarnings("unchecked")
    var stock =
        (List<Map<String, Object>>)
            metrics.stock(tag, product, Integer.MAX_VALUE, 1, null, null).get("items");
    var row = stock.stream().filter(r -> number(r, "ID") == first).findFirst().orElseThrow();
    assertEquals(9630000, number(row, "INVENTORY_VALUE"));
    assertEquals(9095000, number(row, "AVAILABLE_VALUE"));
    assertNull(
        db.one("SELECT COST_UNIT FROM BILL_DETAIL WHERE BILL_ID=:b", "b", bill).get("COST_UNIT"));
    bills.changeStatus(bill, 4, actor, tag);
    assertEquals(90, q(first, "PHYSICAL_QUANTITY"));
    assertEquals(90, q(first, "QUANTITY"));
    long canceledBill = bill;
    assertThrows(RuntimeException.class, () -> bills.changeStatus(canceledBill, 4, actor, tag));
    bill = order(first, 5);
    bills.changeStatus(bill, 1, actor, tag);
    long dispatch = inventory.prepareDispatch(bill, staff);
    assertEquals(90, q(first, "PHYSICAL_QUANTITY"));
    assertThrows(
        org.springframework.security.access.AccessDeniedException.class,
        () -> inventory.approve(dispatch, staff));
    inventory.approve(dispatch, actor);
    assertEquals(85, q(first, "PHYSICAL_QUANTITY"));
    assertEquals(0, q(first, "RESERVED_QUANTITY"));
    assertEquals(85, q(first, "QUANTITY"));
    assertThrows(RuntimeException.class, () -> inventory.approve(dispatch, actor));
    receive(first, 10, n(200000));
    assertEquals(116789.47368421, cost(first).doubleValue(), 0.0000001);
    assertEquals(
        107000,
        ((Number)
                db.one("SELECT COST_UNIT FROM BILL_DETAIL WHERE BILL_ID=:b", "b", bill)
                    .get("COST_UNIT"))
            .longValue());
    bills.changeStatus(bill, 3, actor, tag);
    long returned = returned(bill, first, 2, true);
    assertEquals(97, q(first, "PHYSICAL_QUANTITY"));
    assertEquals(116587.62886598, cost(first).doubleValue(), 0.0000001);
    var report =
        reports.report(
            "DAY",
            2026,
            null,
            null,
            java.time.LocalDate.now(),
            java.time.LocalDate.now(),
            product,
            tag,
            1);
    @SuppressWarnings("unchecked")
    var totals = (Map<String, Object>) report.get("totals");
    assertEquals(597000, number(totals, "NET_AFTER_RETURN"));
    assertEquals(321000, number(totals, "COGS"));
    assertEquals(276000, number(totals, "GROSS_PROFIT"));
    long reversed = inventory.reverse(returned, tag, actor);
    inventory.approve(reversed, actor);
    assertEquals(95, q(first, "PHYSICAL_QUANTITY"));
    assertEquals(
        "NONE",
        db.one("SELECT RETURN_STATUS FROM BILL WHERE ID=:b", "b", bill).get("RETURN_STATUS"));
    returned(bill, first, 1, false);
    assertEquals(95, q(first, "PHYSICAL_QUANTITY"));
    long oldBill = bill;
    assertThrows(RuntimeException.class, () -> returned(oldBill, first, 5, true));
    long badCount = manual("COUNT", first, 94, null);
    long pending = order(first, 1);
    assertThrows(RuntimeException.class, () -> inventory.approve(badCount, actor));
    bills.changeStatus(pending, 4, actor, tag);
    long issue = manual("ISSUE", first, 2, null);
    inventory.approve(issue, actor);
    receive(first, 1, n(100000));
    long undo = inventory.reverse(issue, tag, actor);
    assertThrows(RuntimeException.class, () -> inventory.approve(undo, actor));
    long failed = order(second, 2);
    ship(failed);
    bills.changeStatus(failed, 6, actor, tag);
    assertEquals(18, q(second, "PHYSICAL_QUANTITY"));
    returned(failed, second, 2, true);
    assertEquals(20, q(second, "PHYSICAL_QUANTITY"));
    @SuppressWarnings("unchecked")
    var afterFailed =
        (Map<String, Object>)
            reports
                .report(
                    "DAY",
                    2026,
                    null,
                    null,
                    java.time.LocalDate.now(),
                    java.time.LocalDate.now(),
                    product,
                    tag,
                    1)
                .get("totals");
    assertEquals(
        796000,
        number(afterFailed, "NET_AFTER_RETURN"),
        "Failed delivery/receipt does not create or subtract completed revenue");
    assertEquals(
        0,
        count(
            "SELECT COUNT(*) N FROM PRODUCT_DETAIL WHERE ID IN (:a,:b) AND"
                + " (QUANTITY<>PHYSICAL_QUANTITY-RESERVED_QUANTITY OR RESERVED_QUANTITY<0)",
            "a",
            first,
            "b",
            second));
  }

  @Test
  void legacyNk216AndZeroCostAreExplicit() {
    db.update(
        "UPDATE PRODUCT_DETAIL SET PHYSICAL_QUANTITY=20,QUANTITY=20,UNKNOWN_QUANTITY=20 WHERE"
            + " ID=:i",
        "i",
        first);
    receive(first, 100, n(100000));
    assertEquals(120, q(first, "PHYSICAL_QUANTITY"));
    assertNull(cost(first));
    assertEquals(
        10000000,
        number(
            db.one("SELECT KNOWN_VALUE FROM PRODUCT_DETAIL WHERE ID=:i", "i", first),
            "KNOWN_VALUE"));
    receive(second, 20, n(80000));
    receive(second, 100, n(100000));
    assertEquals(
        11600000,
        number(
            db.one("SELECT KNOWN_VALUE FROM PRODUCT_DETAIL WHERE ID=:i", "i", second),
            "KNOWN_VALUE"));
    assertEquals(96666.66666667, cost(second).doubleValue(), 0.0000001);
  }

  @Test
  void concurrentReservationAndRollback() throws Exception {
    receive(first, 10, n(0));
    assertNotNull(cost(first));
    assertEquals(0, cost(first).signum());
    var pool = Executors.newFixedThreadPool(2);
    try {
      var a =
          pool.submit(
              () -> {
                try {
                  return order(first, 6);
                } catch (RuntimeException e) {
                  return -1L;
                }
              });
      var b =
          pool.submit(
              () -> {
                try {
                  return order(first, 6);
                } catch (RuntimeException e) {
                  return -1L;
                }
              });
      long x = a.get(), y = b.get();
      assertTrue((x > 0) ^ (y > 0));
      assertEquals(4, q(first, "QUANTITY"));
      bills.changeStatus(x > 0 ? x : y, 4, actor, tag);
    } finally {
      pool.shutdownNow();
    }
    assertThrows(
        RuntimeException.class,
        () ->
            bills.create(
                new BillRequest(
                    customer,
                    address,
                    "COD",
                    n(30000),
                    tag,
                    null,
                    List.of(new BillRequest.Item(first, 1), new BillRequest.Item(second, 1)))));
    assertEquals(10, q(first, "QUANTITY"));
    assertEquals(0, q(first, "RESERVED_QUANTITY"));
  }

  @Test
  void countsRespectHeldStockAndUnknownIncrementAndZero() {
    receive(first, 10, n(100));
    long bill = order(first, 5);
    long tooLow = manual("COUNT", first, 4, null);
    assertThrows(RuntimeException.class, () -> inventory.approve(tooLow, actor));
    assertEquals(10, q(first, "PHYSICAL_QUANTITY"));
    long increase = manual("COUNT", first, 12, n(200));
    inventory.approve(increase, actor);
    assertEquals(12, q(first, "PHYSICAL_QUANTITY"));
    assertEquals(7, q(first, "QUANTITY"));
    assertEquals(116.66666667, cost(first).doubleValue(), 0.0000001);
    long unknown = manual("COUNT", first, 14, null);
    inventory.approve(unknown, actor);
    assertNull(cost(first));
    assertEquals(
        1400,
        number(
            db.one("SELECT KNOWN_VALUE FROM PRODUCT_DETAIL WHERE ID=:i", "i", first),
            "KNOWN_VALUE"));
    bills.changeStatus(bill, 4, actor, tag);
    long stale = manual("COUNT", first, 14, null);
    long temp = order(first, 1);
    bills.changeStatus(temp, 4, actor, tag);
    assertThrows(RuntimeException.class, () -> inventory.approve(stale, actor));
    long clear = manual("ISSUE", first, 14, null);
    inventory.approve(clear, actor);
    assertEquals(0, q(first, "PHYSICAL_QUANTITY"));
    assertEquals(0, cost(first).signum());
    long zero = receive(first, 5, n(0));
    assertNotNull(cost(first));
    assertEquals(0, cost(first).signum());
    long reverse = inventory.reverse(zero, tag, actor);
    inventory.approve(reverse, actor);
    assertEquals(0, q(first, "PHYSICAL_QUANTITY"));
  }

  @Test
  void httpPermissionsFiltersAndCostSnapshot() throws Exception {
    receive(first, 10, n(90000));
    Browser adminBrowser = new Browser(tag + "ADMIN"), staffBrowser = new Browser(tag + "STAFF");
    assertEquals(403, staffBrowser.call("/api/admin/sku-cost/" + first, "GET", null).statusCode());
    assertEquals(403, staffBrowser.call("/api/admin/audit", "GET", null).statusCode());
    long pendingOrder = order(first, 1);
    assertEquals(
        200,
        staffBrowser
            .call("/api/admin/orders/" + pendingOrder + "/refunds", "GET", null)
            .statusCode());
    assertEquals(
        403,
        staffBrowser
            .call(
                "/api/admin/orders/" + pendingOrder + "/refunds",
                "POST",
                Map.of(
                    "documentId",
                    1,
                    "amount",
                    1,
                    "status",
                    "PAID",
                    "reference",
                    tag,
                    "reason",
                    tag))
            .statusCode());
    bills.changeStatus(pendingOrder, 4, actor, tag);
    var preview = adminBrowser.get("/api/admin/sku-cost/" + first);
    assertEquals(10, preview.get("stock").get("PHYSICAL_QUANTITY").asLong());
    var snapshot =
        Map.of(
            "expectedQuantity",
            10,
            "unitCost",
            100000,
            "reason",
            tag,
            "evidence",
            "Supplier invoice DEMO-216");
    assertEquals(
        200, adminBrowser.call("/api/admin/sku-cost/" + first, "POST", snapshot).statusCode());
    assertEquals(10, q(first, "PHYSICAL_QUANTITY"));
    assertEquals(100000, cost(first).longValue());
    assertEquals(
        400,
        adminBrowser
            .call(
                "/api/admin/sku-cost/" + first,
                "POST",
                Map.of("expectedQuantity", 9, "unitCost", 100000, "reason", tag, "evidence", tag))
            .statusCode());
    assertEquals(
        200,
        adminBrowser
            .call(
                "/api/admin/sku-cost/" + second,
                "POST",
                Map.of("expectedQuantity", 0, "unitCost", 170000, "reason", tag, "evidence", tag))
            .statusCode());
    assertEquals(0, cost(second).signum());
    assertEquals(
        0,
        number(
            db.one(
                "SELECT AFTER_COST N FROM STOCK_COST_ADJUSTMENT WHERE PRODUCT_DETAIL_ID=:i",
                "i",
                second),
            "N"));
    for (String url :
        List.of(
            "/api/admin/products?q=" + tag,
            "/api/admin/stock?q=" + tag,
            "/api/admin/reports?q=" + tag,
            "/api/admin/orders?paymentMethod=COD&paymentStatus=0",
            "/api/admin/inventory-history?q=" + tag,
            "/api/admin/audit?q=" + tag,
            "/api/admin/managers?role=STAFF&status=1",
            "/api/admin/vouchers?effective=ACTIVE",
            "/api/admin/categories?q=demo",
            "/api/admin/reports/export?detail=sku&q=" + tag,
            "/api/admin/reports/export?detail=product&q=" + tag,
            "/api/admin/inventory/export?detail=balance&q=" + tag,
            "/api/admin/inventory/export?detail=history&q=" + tag))
      assertEquals(200, adminBrowser.call(url, "GET", null).statusCode(), url);
    JsonNode firstPage = adminBrowser.get("/api/admin/stock?q=" + tag),
        emptyPage = adminBrowser.get("/api/admin/stock?q=" + tag + "&page=99");
    assertTrue(emptyPage.get("items").isEmpty());
    assertEquals(firstPage.get("totals"), emptyPage.get("totals"));
  }

  @Test
  void concurrentDispatchReturnAndRefundAreSingleEntry() throws Exception {
    receive(first, 20, n(100000));
    long bill = order(first, 3);
    bills.changeStatus(bill, 1, actor, tag);
    long dispatch = inventory.prepareDispatch(bill, staff);
    var pool = Executors.newFixedThreadPool(2);
    try {
      var a =
          pool.submit(
              () -> {
                try {
                  inventory.approve(dispatch, actor);
                  return true;
                } catch (RuntimeException e) {
                  return false;
                }
              });
      var b =
          pool.submit(
              () -> {
                try {
                  inventory.approve(dispatch, actor);
                  return true;
                } catch (RuntimeException e) {
                  return false;
                }
              });
      assertTrue(a.get() ^ b.get());
      assertEquals(17, q(first, "PHYSICAL_QUANTITY"));
      bills.changeStatus(bill, 3, actor, tag);
      long
          x =
              inventory.save(
                  null,
                  new InventoryRequests.Document(
                      "RETURN",
                      tag,
                      bill,
                      List.of(new InventoryRequests.Line(first, 2, true, tag, null))),
                  actor),
          y =
              inventory.save(
                  null,
                  new InventoryRequests.Document(
                      "RETURN",
                      tag,
                      bill,
                      List.of(new InventoryRequests.Line(first, 2, true, tag, null))),
                  actor);
      var c =
          pool.submit(
              () -> {
                try {
                  inventory.approve(x, actor);
                  return x;
                } catch (RuntimeException e) {
                  return -1L;
                }
              });
      var d =
          pool.submit(
              () -> {
                try {
                  inventory.approve(y, actor);
                  return y;
                } catch (RuntimeException e) {
                  return -1L;
                }
              });
      long cx = c.get(), dy = d.get();
      assertTrue((cx > 0) ^ (dy > 0));
      assertEquals(19, q(first, "PHYSICAL_QUANTITY"));
      long returned = cx > 0 ? cx : dy;
      long pending =
          refunds.save(
              bill,
              null,
              new RefundService.Request(returned, n(398000), "PENDING", tag + "REF", tag),
              actor);
      assertThrows(
          RuntimeException.class,
          () ->
              refunds.save(
                  bill,
                  null,
                  new RefundService.Request(returned, n(1), "PAID", tag + "EXCESS", tag),
                  actor));
      refunds.save(
          bill,
          pending,
          new RefundService.Request(returned, n(398000), "PAID", tag + "REF", tag),
          actor);
      assertThrows(RuntimeException.class, () -> inventory.reverse(returned, tag, actor));
      @SuppressWarnings("unchecked")
      var t =
          (Map<String, Object>)
              reports
                  .report(
                      "DAY",
                      2026,
                      null,
                      null,
                      java.time.LocalDate.now(),
                      java.time.LocalDate.now(),
                      product,
                      tag,
                      1)
                  .get("totals");
      assertEquals(199000, number(t, "NET_AFTER_RETURN"));
    } finally {
      pool.shutdownNow();
    }
  }

  @Test
  void voucherLastUseAndWholeVndAllocation() throws Exception {
    receive(first, 20, n(0));
    receive(second, 20, n(0));
    long voucher =
        admin.voucher(
            null,
            new AdminRequests.Voucher(
                tag + "LAST",
                "PERCENT",
                n(10),
                n(0),
                n(100000),
                java.time.LocalDateTime.now().minusDays(1),
                java.time.LocalDateTime.now().plusDays(1),
                1,
                1),
            actor);
    var request =
        new BillRequest(
            customer,
            address,
            "COD",
            n(30000),
            tag,
            tag + "LAST",
            List.of(new BillRequest.Item(first, 1)));
    var pool = Executors.newFixedThreadPool(2);
    try {
      var a =
          pool.submit(
              () -> {
                try {
                  return bills.create(request).id();
                } catch (RuntimeException e) {
                  return -1L;
                }
              });
      var b =
          pool.submit(
              () -> {
                try {
                  return bills.create(request).id();
                } catch (RuntimeException e) {
                  return -1L;
                }
              });
      long x = a.get(), y = b.get();
      assertTrue((x > 0) ^ (y > 0));
      assertEquals(1, q(first, "RESERVED_QUANTITY"));
      bills.changeStatus(x > 0 ? x : y, 4, actor, tag);
      assertEquals(
          1,
          number(db.one("SELECT USED_COUNT FROM VOUCHER WHERE ID=:i", "i", voucher), "USED_COUNT"));
    } finally {
      pool.shutdownNow();
    }
    db.update("UPDATE PRODUCT_DETAIL SET PRICE=101 WHERE ID=:i", "i", first);
    db.update("UPDATE PRODUCT_DETAIL SET PRICE=102 WHERE ID=:i", "i", second);
    admin.voucher(
        null,
        new AdminRequests.Voucher(
            tag + "ONE",
            "FIXED",
            n(1),
            n(0),
            n(1),
            java.time.LocalDateTime.now().minusDays(1),
            java.time.LocalDateTime.now().plusDays(1),
            10,
            1),
        actor);
    long bill =
        bills
            .create(
                new BillRequest(
                    customer,
                    address,
                    "COD",
                    n(30000),
                    tag,
                    tag + "ONE",
                    List.of(new BillRequest.Item(first, 2), new BillRequest.Item(second, 3))))
            .id();
    ship(bill);
    bills.changeStatus(bill, 3, actor, tag);
    @SuppressWarnings("unchecked")
    var t =
        (Map<String, Object>)
            reports
                .report(
                    "DAY",
                    2026,
                    null,
                    null,
                    java.time.LocalDate.now(),
                    java.time.LocalDate.now(),
                    product,
                    tag,
                    1)
                .get("totals");
    assertEquals(507, number(t, "NET_AFTER_RETURN"));
    returned(bill, first, 1, true);
    returned(bill, first, 1, true);
    returned(bill, second, 1, true);
    returned(bill, second, 1, true);
    returned(bill, second, 1, true);
    @SuppressWarnings("unchecked")
    var end =
        (Map<String, Object>)
            reports
                .report(
                    "DAY",
                    2026,
                    null,
                    null,
                    java.time.LocalDate.now(),
                    java.time.LocalDate.now(),
                    product,
                    tag,
                    1)
                .get("totals");
    assertEquals(0, number(end, "NET_AFTER_RETURN"));
    assertEquals(507, number(end, "RETURN_VALUE"));
    long color =
        number(db.one("SELECT COLOR_ID FROM PRODUCT_DETAIL WHERE ID=:i", "i", first), "COLOR_ID");
    long third =
        db.insert(
            "INSERT INTO PRODUCT_DETAIL(PRODUCT_ID,COLOR_ID,SIZE_NAME,SKU,PRICE,QUANTITY)"
                + " VALUES(:p,:c,'L',:sku,101,0)",
            "p",
            product,
            "c",
            color,
            "sku",
            tag + "THIRD");
    receive(third, 5, n(0));
    db.update("UPDATE PRODUCT_DETAIL SET PRICE=101 WHERE ID IN (:a,:b)", "a", first, "b", second);
    long triple =
        bills
            .create(
                new BillRequest(
                    customer,
                    address,
                    "COD",
                    n(30000),
                    tag,
                    tag + "ONE",
                    List.of(
                        new BillRequest.Item(first, 1),
                        new BillRequest.Item(second, 1),
                        new BillRequest.Item(third, 1))))
            .id();
    ship(triple);
    bills.changeStatus(triple, 3, actor, tag);
    var detailed =
        reports.report(
            "DAY",
            2026,
            null,
            null,
            java.time.LocalDate.now(),
            java.time.LocalDate.now(),
            product,
            tag,
            1);
    @SuppressWarnings("unchecked")
    var grand = (Map<String, Object>) detailed.get("totals");
    assertEquals(302, number(grand, "NET_AFTER_RETURN"));
    @SuppressWarnings("unchecked")
    var skuRows = (List<Map<String, Object>>) detailed.get("variants");
    assertEquals(302, skuRows.stream().mapToLong(r -> number(r, "NET_AFTER_RETURN")).sum());
    assertEquals(
        number(grand, "SHIPPING"), skuRows.stream().mapToLong(r -> number(r, "SHIPPING")).sum());
  }

  @Test
  void sessionsCategoryVisibilityAndRecovery() throws Exception {
    Browser staffBrowser = new Browser(tag + "STAFF");
    long staffRole =
        number(
            db.one("SELECT MANAGER_ROLE_ID FROM MANAGER WHERE ID=:i", "i", staff),
            "MANAGER_ROLE_ID");
    admin.access(staff, new AdminRequests.Access(staffRole, 0), actor);
    assertEquals(401, staffBrowser.call("/api/admin/me", "GET", null).statusCode());
    admin.access(staff, new AdminRequests.Access(staffRole, 1), actor);
    Browser adminBrowser = new Browser(tag + "ADMIN");
    assertThrows(
        RuntimeException.class,
        () -> admin.access(actor, new AdminRequests.Access(staffRole, 0), actor));
    long parent = admin.category(null, new AdminRequests.Category(tag + "ROOT", null, 1), actor),
        child = admin.category(null, new AdminRequests.Category(tag + "CHILD", parent, 1), actor);
    db.update("UPDATE PRODUCT SET CATEGORY_ID=:c WHERE ID=:i", "c", child, "i", product);
    receive(first, 5, n(100));
    admin.category(parent, new AdminRequests.Category(tag + "ROOT", null, 0), actor);
    assertThrows(RuntimeException.class, () -> order(first, 1));
    assertEquals(
        200,
        adminBrowser.call("/api/admin/categories/" + parent + "/impact", "GET", null).statusCode());
    admin.category(parent, new AdminRequests.Category(tag + "ROOT", null, 1), actor);
    assertTrue(order(first, 1) > 0);
    var before = new HashSet<java.nio.file.Path>();
    if (java.nio.file.Files.exists(delivery.directory()))
      try (var files = java.nio.file.Files.list(delivery.directory())) {
        files.forEach(before::add);
      }
    assertTrue(recovery.request(tag + "ADMIN", tag));
    java.nio.file.Path file;
    try (var files = java.nio.file.Files.list(delivery.directory())) {
      file = files.filter(p -> !before.contains(p)).findFirst().orElseThrow();
    }
    try {
      String body = java.nio.file.Files.readString(file);
      var m = java.util.regex.Pattern.compile("#token=([A-Za-z0-9_-]+)").matcher(body);
      assertTrue(m.find());
      String token = m.group(1);
      recovery.reset(token, password + "new");
      assertThrows(RuntimeException.class, () -> recovery.reset(token, password + "new"));
      assertEquals(401, adminBrowser.call("/api/admin/me", "GET", null).statusCode());
    } finally {
      java.nio.file.Files.deleteIfExists(file);
    }
  }

  class Browser {
    HttpClient client =
        HttpClient.newBuilder()
            .cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL))
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();
    String token;

    Browser(String user) throws Exception {
      token = get("/api/csrf").get("token").asText();
      var response =
          client.send(
              HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/login"))
                  .header("Content-Type", "application/x-www-form-urlencoded")
                  .header("X-XSRF-TOKEN", token)
                  .POST(
                      HttpRequest.BodyPublishers.ofString(
                          "username="
                              + user
                              + "&password="
                              + URLEncoder.encode(
                                  password, java.nio.charset.StandardCharsets.UTF_8)))
                  .build(),
              HttpResponse.BodyHandlers.ofString());
      assertEquals(302, response.statusCode());
      token = get("/api/csrf").get("token").asText();
    }

    HttpResponse<String> call(String path, String method, Object body) throws Exception {
      var b = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path));
      if (token != null) b.header("X-XSRF-TOKEN", token);
      if (body != null) b.header("Content-Type", "application/json");
      return client.send(
          b.method(
                  method,
                  body == null
                      ? HttpRequest.BodyPublishers.noBody()
                      : HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body)))
              .build(),
          HttpResponse.BodyHandlers.ofString());
    }

    JsonNode get(String path) throws Exception {
      var r = call(path, "GET", null);
      assertEquals(200, r.statusCode(), r.body());
      return json.readTree(r.body());
    }
  }

  @AfterEach
  void cleanup() {
    String fixtureProducts = "SELECT ID FROM PRODUCT WHERE PRODUCT_NAME=:n";
    String fixtureVariants =
        "SELECT ID FROM PRODUCT_DETAIL WHERE PRODUCT_ID IN (" + fixtureProducts + ")";
    String fixtureBills =
        "SELECT ID FROM BILL WHERE CUSTOMER_ID IN (SELECT ID FROM CUSTOMER WHERE USER_ID IN (SELECT"
            + " ID FROM USERS WHERE USERNAME=:n))";
    db.update("DELETE FROM BILL_REFUND WHERE BILL_ID IN (" + fixtureBills + ")", "n", tag);
    db.update(
        "DELETE FROM STOCK_COST_ADJUSTMENT WHERE PRODUCT_DETAIL_ID IN (" + fixtureVariants + ")",
        "n",
        tag);
    db.update(
        "DELETE FROM INVENTORY_HISTORY WHERE PRODUCT_DETAIL_ID IN (" + fixtureVariants + ")",
        "n",
        tag);
    String docs =
        "SELECT DISTINCT DOCUMENT_ID FROM INVENTORY_DOCUMENT_LINE WHERE PRODUCT_DETAIL_ID IN ("
            + fixtureVariants
            + ")";
    db.update("UPDATE INVENTORY_DOCUMENT SET SOURCE_ID=NULL WHERE ID IN (" + docs + ")", "n", tag);
    db.update(
        "DELETE FROM INVENTORY_DOCUMENT_LINE WHERE PRODUCT_DETAIL_ID IN (" + fixtureVariants + ")",
        "n",
        tag);
    db.update(
        "DELETE FROM INVENTORY_DOCUMENT WHERE CREATED_BY IN (:a,:s) OR BILL_ID IN ("
            + fixtureBills
            + ")",
        "a",
        actor,
        "s",
        staff,
        "n",
        tag);
    for (String t : List.of("PAYMENT_HISTORY", "BILL_HISTORY", "BILL_DETAIL"))
      db.update("DELETE FROM " + t + " WHERE BILL_ID IN (" + fixtureBills + ")", "n", tag);
    db.update("DELETE FROM BILL WHERE ID IN (" + fixtureBills + ")", "n", tag);
    db.update("DELETE FROM ADDRESS WHERE CUSTOMER_ID=:i", "i", customer);
    db.update("DELETE FROM CUSTOMER WHERE ID=:i", "i", customer);
    db.update("DELETE FROM USERS WHERE USERNAME=:n", "n", tag);
    db.update("DELETE FROM PRODUCT_DETAIL WHERE ID IN (" + fixtureVariants + ")", "n", tag);
    db.update("DELETE FROM PRODUCT WHERE PRODUCT_NAME=:n", "n", tag);
    db.update("DELETE FROM BRAND WHERE BRAND_NAME=:n", "n", tag);
    db.update("DELETE FROM MATERIAL WHERE MATERIAL_NAME=:n", "n", tag);
    db.update("UPDATE CATEGORY SET PARENT_ID=NULL WHERE CATEGORY_NAME LIKE :n", "n", tag + "%");
    db.update("DELETE FROM CATEGORY WHERE CATEGORY_NAME LIKE :n", "n", tag + "%");
    db.update("DELETE FROM VOUCHER WHERE CODE LIKE :n", "n", tag.toUpperCase() + "%");
    db.update(
        "DELETE FROM MANAGER_PASSWORD_RESET WHERE MANAGER_ID IN (:a,:s)", "a", actor, "s", staff);
    db.update("DELETE FROM ADMIN_AUDIT WHERE MANAGER_ID IN (:a,:s)", "a", actor, "s", staff);
    db.update("DELETE FROM MANAGER WHERE ID IN (:a,:s)", "a", actor, "s", staff);
  }
}
