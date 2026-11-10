package com.example.clothingshop.service.inventory;

import static com.example.clothingshop.dto.inventory.InventoryRequests.*;
import static com.example.clothingshop.repository.admin.AdminRepository.number;

import com.example.clothingshop.common.BusinessException;
import com.example.clothingshop.repository.admin.AdminRepository;
import com.example.clothingshop.repository.inventory.InventoryOperationsRepository;
import java.math.*;
import java.util.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InventoryService {
  private final InventoryOperationsRepository queries;
  private final AdminRepository db;
  private final StockCostService costs;

  public InventoryService(
      AdminRepository db, StockCostService costs, InventoryOperationsRepository queries) {
    this.queries = queries;
    this.db = db;
    this.costs = costs;
  }

  public boolean isAdmin(long actor) {
    return !queries.listManager("i", actor).isEmpty();
  }

  private void approver(long actor) {
    if (!isAdmin(actor)) throw new AccessDeniedException("Chỉ ADMIN được xác nhận/đảo phiếu kho");
  }

  private long header(
      String type, String reason, Long bill, Long source, Long actor, String status) {
    String prefix =
        switch (type) {
          case "RECEIPT" -> "NK";
          case "ISSUE", "DISPATCH" -> "XK";
          case "COUNT" -> "KK";
          case "RETURN" -> "HT";
          case "REVERSAL" -> "DP";
          case "RESERVE" -> "GH";
          case "RELEASE" -> "HG";
          default -> "BH";
        };
    String code = prefix + String.format(Locale.ROOT, "%06d", number(queries.findDual(), "N"));
    return queries.insertInventoryDocument(
        "c", code, "t", type, "r", reason, "b", bill, "s", source, "a", actor, "st", status);
  }

  public void lockProducts(Collection<Long> variants) {
    costs.lockVariants(variants);
  }

  private List<Map<String, Object>> billLines(long bill) {
    return queries.listBillDetail("b", bill);
  }

  private void lockBill(long bill) {
    queries.findBill("b", bill);
    lockProducts(billLines(bill).stream().map(l -> number(l, "PRODUCT_DETAIL_ID")).toList());
  }

  private Map<String, Object> stock(long variant) {
    return queries.findProductDetail("i", variant);
  }

  @Transactional
  public long save(Long id, Document r, long actor) {
    if (r.type().equals("ISSUE") && r.billId() != null)
      throw new BusinessException(
          "Dùng phiếu xuất giao theo đơn; xuất khác không được trừ lặp hàng đã giữ");
    if (!Set.of("RECEIPT", "ISSUE", "COUNT", "RETURN").contains(r.type()))
      throw new BusinessException("Loại phiếu thủ công không hợp lệ");
    if (r.type().equals("RETURN")) {
      if (r.billId() == null) throw new BusinessException("Phiếu hoàn cần đơn gốc");
      queries.findBill2("b", r.billId());
    }
    if (id == null)
      id =
          header(
              r.type(),
              r.reason().strip(),
              r.type().equals("RETURN") ? r.billId() : null,
              null,
              actor,
              "DRAFT");
    else {
      var old = queries.findInventoryDocument2("i", id);
      if (!old.get("STATUS").equals("DRAFT")
          || Set.of("REVERSAL", "DISPATCH").contains(old.get("DOC_TYPE")))
        throw new BusinessException("Chỉ sửa phiếu nháp thủ công");
      if (number(old, "CREATED_BY") != actor && !isAdmin(actor))
        throw new AccessDeniedException("Không được sửa phiếu người khác");
      queries.updateInventoryDocument(
          "t",
          r.type(),
          "r",
          r.reason().strip(),
          "b",
          r.type().equals("RETURN") ? r.billId() : null,
          "i",
          id);
      queries.deleteInventoryDocumentLine("i", id);
    }
    var seen = new HashSet<Long>();
    for (var l : r.lines()) {
      if (!seen.add(l.variantId()))
        throw new BusinessException("Một SKU chỉ được có một dòng trên phiếu");
      if (!r.type().equals("COUNT") && l.quantity() == 0)
        throw new BusinessException("Số lượng phải lớn hơn 0");
      if (r.type().equals("RECEIPT") && l.unitCost() == null)
        throw new BusinessException("Phiếu nhập cần đơn giá, có thể bằng 0 nếu có chứng từ");
      var s = queries.findProductDetail2("i", l.variantId());
      if (r.type().equals("RETURN")
          && queries.listBillDetail2("b", r.billId(), "p", l.variantId()).isEmpty())
        throw new BusinessException("SKU không thuộc đơn gốc");
      queries.updateInventoryDocumentLine(
          "d",
          id,
          "p",
          l.variantId(),
          "q",
          l.quantity(),
          "e",
          number(s, "PHYSICAL_QUANTITY"),
          "er",
          number(s, "RESERVED_QUANTITY"),
          "version",
          number(s, "STOCK_VERSION"),
          "r",
          l.restock() ? 1 : 0,
          "n",
          l.conditionNote(),
          "c",
          Set.of("RECEIPT", "COUNT").contains(r.type()) ? l.unitCost() : null);
    }
    db.audit(actor, "SAVE_DRAFT", "INVENTORY_DOCUMENT", id);
    return id;
  }

  private long line(long doc, long variant, long qty) {
    return queries.insertInventoryDocumentLine("d", doc, "p", variant, "q", qty);
  }

  private void record(
      long doc,
      long line,
      long variant,
      long dp,
      long dr,
      BigDecimal unit,
      String type,
      Long bill,
      Long actor,
      String reason) {
    var before = stock(variant);
    long p = number(before, "PHYSICAL_QUANTITY"), r = number(before, "RESERVED_QUANTITY");
    if (p + dp < 0 || r + dr < 0 || r + dr > p + dp || p + dp > Integer.MAX_VALUE)
      throw new BusinessException("Không đủ tồn tại kho/sẵn bán hoặc số giữ không hợp lệ");
    if (dp != 0) costs.movement(variant, dp, unit);
    queries.updateProductDetail("p", dp, "r", dr, "i", variant);
    var after = stock(variant);
    BigDecimal
        vb =
            before.get("AVERAGE_COST") == null && p > 0
                ? null
                : (BigDecimal) before.get("KNOWN_VALUE"),
        va =
            after.get("AVERAGE_COST") == null && p + dp > 0
                ? null
                : (BigDecimal) after.get("KNOWN_VALUE");
    queries.updateInventoryDocumentLine2(
        "u",
        unit,
        "ab",
        p - r,
        "aa",
        p + dp - r - dr,
        "d",
        dp - dr,
        "pb",
        p,
        "pa",
        p + dp,
        "rb",
        r,
        "ra",
        r + dr,
        "vb",
        vb,
        "va",
        va,
        "i",
        line);
    queries.updateInventoryHistory(
        "p",
        variant,
        "q",
        dp - dr,
        "reason",
        reason,
        "actor",
        actor,
        "b",
        bill,
        "d",
        doc,
        "t",
        type,
        "ab",
        p - r,
        "aa",
        p + dp - r - dr,
        "dp",
        dp,
        "dr",
        dr,
        "pb",
        p,
        "pa",
        p + dp,
        "rb",
        r,
        "ra",
        r + dr,
        "vb",
        vb,
        "va",
        va);
  }

  /** Called after atomic reservation in BillRepository; physical stock/cost are unchanged. */
  public void sale(long bill) {
    long doc = header("RESERVE", "Giữ hàng khi đặt đơn", bill, null, null, "POSTED");
    for (var l : billLines(bill)) {
      long v = number(l, "PRODUCT_DETAIL_ID"), q = number(l, "QTY");
      var s = stock(v);
      queries.updateProductDetail2("q", q, "i", v);
      record(doc, line(doc, v, q), v, 0, q, null, "RESERVE", bill, null, "Đặt đơn giữ hàng");
    }
  }

  public void cancelSale(long bill, Long actor) {
    lockBill(bill);
    long doc = header("RELEASE", "Giải phóng giữ khi hủy trước xuất", bill, null, actor, "POSTED");
    for (var l : billLines(bill)) {
      long v = number(l, "PRODUCT_DETAIL_ID"), q = number(l, "QTY");
      record(doc, line(doc, v, q), v, 0, -q, null, "RELEASE", bill, actor, "Hủy đơn trước xuất");
    }
    queries.updateInventoryDocument2("a", actor, "b", bill);
  }

  @Transactional
  public long prepareDispatch(long bill, long actor) {
    lockBill(bill);
    var b = queries.findBill3("b", bill);
    if (number(b, "DISPATCHED") == 1)
      throw new BusinessException("Đơn đã xuất giao, không lập xuất lần hai");
    if (number(b, "STATUS") != 1 || number(b, "STOCK_RESERVED") != 1)
      throw new BusinessException(
          "Đơn phải được xác nhận và giữ đủ hàng trước khi lập phiếu xuất giao");
    var existing = queries.listInventoryDocument("b", bill);
    if (!existing.isEmpty()) return number(existing.get(0), "ID");
    long doc = header("DISPATCH", "Xuất giao theo đơn", bill, null, actor, "DRAFT");
    for (var l : billLines(bill)) line(doc, number(l, "PRODUCT_DETAIL_ID"), number(l, "QTY"));
    db.audit(actor, "PREPARE_DISPATCH", "BILL", bill);
    return doc;
  }

  @Transactional
  public void approve(long id, long actor) {
    approver(actor);
    var pointer = queries.findInventoryDocument3("i", id);
    Long bill = pointer.get("BILL_ID") == null ? null : number(pointer, "BILL_ID");
    if (bill != null) lockBill(bill);
    var doc = queries.findInventoryDocument4("i", id);
    if (!doc.get("STATUS").equals("DRAFT"))
      throw new BusinessException("Phiếu đã ghi sổ hoặc hủy, không duyệt trùng");
    String type = doc.get("DOC_TYPE").toString();
    var lines = queries.listInventoryDocumentLine2("i", id);
    lockProducts(lines.stream().map(l -> number(l, "PRODUCT_DETAIL_ID")).toList());
    if (type.equals("DISPATCH")) {
      var b = queries.findBill3("b", bill);
      if (number(b, "STATUS") != 1
          || number(b, "DISPATCHED") != 0
          || number(b, "STOCK_RESERVED") != 1)
        throw new BusinessException("Đơn không còn đủ điều kiện xuất giao");
      costs.snapshotSale(bill);
    }
    BigDecimal returnedValue = BigDecimal.ZERO;
    for (var l : lines) {
      long v = number(l, "PRODUCT_DETAIL_ID"), q = number(l, "QUANTITY");
      var s = stock(v);
      long dp = 0, dr = 0;
      BigDecimal unit = costs.current(v), ret = BigDecimal.ZERO, cogs = null;
      switch (type) {
        case "RECEIPT" -> {
          if (l.get("UNIT_COST") == null) throw new BusinessException("Thiếu đơn giá nhập");
          dp = q;
          unit = (BigDecimal) l.get("UNIT_COST");
        }
        case "ISSUE" -> dp = -q;
        case "COUNT" -> {
          if (l.get("EXPECTED_VERSION") == null
              || number(s, "STOCK_VERSION") != number(l, "EXPECTED_VERSION")
              || number(s, "PHYSICAL_QUANTITY") != number(l, "EXPECTED_QUANTITY")
              || number(s, "RESERVED_QUANTITY") != number(l, "EXPECTED_RESERVED"))
            throw new BusinessException(
                "Tồn tại kho hoặc lượng giữ đã thay đổi từ lúc lập kiểm kê; lập lại phiếu");
          dp = q - number(s, "PHYSICAL_QUANTITY");
          if (dp > 0) unit = (BigDecimal) l.get("UNIT_COST");
        }
        case "DISPATCH" -> {
          dp = -q;
          dr = -q;
        }
        case "RETURN" -> {
          var b = queries.findBill4("b", bill);
          if (number(b, "DISPATCHED") != 1 || !Set.of(3L, 5L, 6L).contains(number(b, "STATUS")))
            throw new BusinessException("Đơn chưa xuất hoặc chưa đủ điều kiện nhận hoàn");
          var sold = queries.findRecord("b", bill, "p", v);
          var previous = queries.findInventoryDocumentLine("b", bill, "p", v);
          if (q > number(sold, "SOLD") - number(previous, "N"))
            throw new BusinessException("Hoàn vượt số đã xuất còn được hoàn");
          ret =
              ((BigDecimal) sold.get("LINE_NET"))
                  .multiply(BigDecimal.valueOf(number(previous, "N") + q))
                  .divide(BigDecimal.valueOf(number(sold, "SOLD")), 0, RoundingMode.HALF_UP)
                  .subtract((BigDecimal) previous.get("V"));
          dp = number(l, "RESTOCK") == 1 ? q : 0;
          unit = costs.sold(bill, v);
          returnedValue = returnedValue.add(ret);
          if (unit != null) {
            BigDecimal total = (BigDecimal) queries.findBillDetail("b", bill, "v", v).get("V");
            cogs =
                total
                    .multiply(BigDecimal.valueOf(number(previous, "N") + q))
                    .divide(BigDecimal.valueOf(number(sold, "SOLD")), 0, RoundingMode.HALF_UP)
                    .subtract((BigDecimal) previous.get("COGS"));
          }
        }
        case "REVERSAL" -> {
          var original = queries.findInventoryDocumentLine2("d", doc.get("SOURCE_ID"), "p", v);
          if (original.get("PHYSICAL_AFTER") == null)
            throw new BusinessException(
                "Phiếu cũ chỉ ghi tồn sẵn bán; cần đối chiếu trước khi đảo");
          if (number(s, "PHYSICAL_QUANTITY") != number(original, "PHYSICAL_AFTER")
              || number(s, "RESERVED_QUANTITY") != number(original, "RESERVED_AFTER")
              || !queries.listInventoryHistory("p", v, "d", doc.get("SOURCE_ID")).isEmpty())
            throw new BusinessException(
                "SKU có giao dịch sau phiếu gốc; đảo không còn an toàn. Lập chứng từ điều chỉnh có"
                    + " đối chiếu");
          dp = number(original, "PHYSICAL_BEFORE") - number(original, "PHYSICAL_AFTER");
          dr = number(original, "RESERVED_BEFORE") - number(original, "RESERVED_AFTER");
          unit = (BigDecimal) original.get("COST_UNIT");
          cogs = (BigDecimal) original.get("COGS_VALUE");
        }
        default -> throw new BusinessException("Không duyệt thủ công loại phiếu này");
      }
      record(id, number(l, "ID"), v, dp, dr, unit, type, bill, actor, doc.get("REASON").toString());
      queries.updateInventoryDocumentLine3(
          "v",
          ret,
          "c",
          cogs != null
              ? cogs
              : unit == null
                  ? null
                  : unit.multiply(BigDecimal.valueOf(q)).setScale(0, RoundingMode.HALF_UP),
          "i",
          l.get("ID"));
    }
    queries.updateInventoryDocument3("a", actor, "i", id);
    if (type.equals("DISPATCH")) {
      queries.updateBill("a", actor, "b", bill);
      queries.updateBillHistory("b", bill, "n", "Xuất giao theo phiếu #" + id, "a", actor);
    }
    if (type.equals("RETURN")) refreshReturn(bill, actor);
    if (type.equals("REVERSAL") && bill != null) {
      var original = queries.findInventoryDocument5("s", doc.get("SOURCE_ID"));
      if (original.get("DOC_TYPE").equals("RETURN")) {
        var b = queries.findBill5("b", bill);
        BigDecimal amount =
            (BigDecimal) queries.findInventoryDocumentLine3("d", doc.get("SOURCE_ID")).get("V");
        if (b.get("COMPLETED_AT") != null) queries.updateInventoryDocument4("v", amount, "i", id);
        refreshReturn(bill, actor);
      }
    }
    db.auditChange(
        actor,
        "POST_" + type,
        "INVENTORY_DOCUMENT",
        id,
        Map.of("STATUS", "DRAFT", "DOC_TYPE", type),
        Map.of("STATUS", "POSTED", "lines", queries.listInventoryDocumentLine3("d", id)),
        doc.get("REASON").toString());
  }

  private void refreshReturn(long bill, long actor) {
    var r = queries.findInventoryDocumentLine4("b", bill);
    long sold = number(queries.findBillDetail2("b", bill), "Q"), qty = number(r, "Q");
    queries.updateBill2(
        "s",
        qty == 0 ? "NONE" : qty == sold ? "FULL" : "PARTIAL",
        "v",
        r.get("V"),
        "q",
        qty,
        "sold",
        sold,
        "a",
        actor,
        "b",
        bill);
    db.audit(actor, "RECEIVE_RETURN", "BILL", bill);
  }

  @Transactional
  public void cancel(long id, String reason, long actor) {
    var d = queries.findInventoryDocument6("i", id);
    if (!d.get("STATUS").equals("DRAFT"))
      throw new BusinessException("Chỉ hủy nháp; phiếu đã ghi sổ cần đảo");
    if (number(d, "CREATED_BY") != actor && !isAdmin(actor))
      throw new AccessDeniedException("Không được hủy phiếu người khác");
    queries.updateInventoryDocument5("a", actor, "r", reason, "i", id);
    db.audit(actor, "CANCEL_DRAFT", "INVENTORY_DOCUMENT", id);
  }

  @Transactional
  public long reverse(long source, String reason, long actor) {
    approver(actor);
    var p = queries.findInventoryDocument3("i", source);
    Long bill = p.get("BILL_ID") == null ? null : number(p, "BILL_ID");
    if (bill != null) lockBill(bill);
    queries.findInventoryDocument7("i", source);
    if (!queries.listInventoryDocument2("i", source).isEmpty())
      throw new BusinessException("Phiếu đã có phiếu đảo");
    if (!queries.listBillRefund("d", source).isEmpty())
      throw new BusinessException(
          "Phiếu đã có khoản hoàn tiền, cần đối soát tiền trước khi đảo nhận hoàn");
    long id = header("REVERSAL", reason, bill, source, actor, "DRAFT");
    queries.updateInventoryDocumentLine4("d", id, "s", source);
    db.audit(actor, "CREATE_REVERSAL", "INVENTORY_DOCUMENT", id);
    return id;
  }
}
