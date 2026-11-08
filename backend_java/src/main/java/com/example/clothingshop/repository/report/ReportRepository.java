package com.example.clothingshop.repository.report;

import com.example.clothingshop.repository.admin.AdminRepository;
import com.example.clothingshop.util.report.SaleAccounting;
import java.math.BigDecimal;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import org.springframework.stereotype.Repository;

@Repository
public class ReportRepository {
  private final AdminRepository db;

  public ReportRepository(AdminRepository db) {
    this.db = db;
  }

  // Revenue excludes shipping. Return events are recognized when a return document is posted,
  // even if the original sale belongs to a different reporting period.
  public static final String EVENTS =
      """
SELECT s.BILL_ID,s.PRODUCT_DETAIL_ID,s.COMPLETED_AT EVENT_AT,
 s.QUANTITY SOLD,0 RETURNED,s.TOTAL_PRICE GROSS,s.ALLOCATED_DISCOUNT DISCOUNT,
 s.TOTAL_PRICE-s.ALLOCATED_DISCOUNT NET,
 0 RETURN_VALUE,s.ALLOCATED_SHIPPING SHIPPING,
 NVL(s.COST_TOTAL,0) COGS,CASE WHEN s.COST_UNIT IS NULL THEN s.QUANTITY ELSE 0 END COST_MISSING,
 CASE WHEN s.COST_UNIT IS NOT NULL THEN s.TOTAL_PRICE-s.ALLOCATED_DISCOUNT ELSE 0 END KNOWN_NET,0 DAMAGED_COST
FROM (
"""
          + SaleAccounting.LINES
          + """
) s
UNION ALL
SELECT h.BILL_ID,l.PRODUCT_DETAIL_ID,h.POSTED_AT,0,l.QUANTITY,0,0,0,ROUND(l.RETURN_VALUE,0),0,
 -NVL(l.COGS_VALUE,0),CASE WHEN l.COST_UNIT IS NULL THEN l.QUANTITY ELSE 0 END,
 CASE WHEN l.COST_UNIT IS NOT NULL THEN -ROUND(l.RETURN_VALUE,0) ELSE 0 END,CASE WHEN l.RESTOCK=0 THEN NVL(l.QUANTITY*l.COST_UNIT,0) ELSE 0 END
FROM INVENTORY_DOCUMENT h JOIN INVENTORY_DOCUMENT_LINE l ON l.DOCUMENT_ID=h.ID JOIN BILL b ON b.ID=h.BILL_ID
WHERE h.DOC_TYPE='RETURN' AND h.STATUS='POSTED' AND b.COMPLETED_AT IS NOT NULL
UNION ALL
SELECT h.BILL_ID,l.PRODUCT_DETAIL_ID,h.POSTED_AT,0,-l.QUANTITY,0,0,ROUND(l.RETURN_VALUE,0),0,0,
 NVL(l.QUANTITY*l.COST_UNIT,0),CASE WHEN l.COST_UNIT IS NULL THEN l.QUANTITY ELSE 0 END,
 CASE WHEN l.COST_UNIT IS NOT NULL THEN ROUND(l.RETURN_VALUE,0) ELSE 0 END,CASE WHEN l.RESTOCK=0 THEN -NVL(l.QUANTITY*l.COST_UNIT,0) ELSE 0 END
FROM INVENTORY_DOCUMENT h JOIN INVENTORY_DOCUMENT original ON original.ID=h.SOURCE_ID
 JOIN INVENTORY_DOCUMENT_LINE l ON l.DOCUMENT_ID=original.ID JOIN BILL b ON b.ID=h.BILL_ID
WHERE h.DOC_TYPE='REVERSAL' AND h.STATUS='POSTED' AND original.DOC_TYPE='RETURN' AND b.COMPLETED_AT IS NOT NULL
""";
  private static final String SUMS =
      """
NVL(SUM(e.SOLD),0) SOLD,NVL(SUM(e.RETURNED),0) RETURNED,
ROUND(NVL(SUM(e.GROSS),0),0) GROSS,ROUND(NVL(SUM(e.DISCOUNT),0),0) DISCOUNT,
ROUND(NVL(SUM(e.NET),0),0) NET,ROUND(NVL(SUM(e.RETURN_VALUE),0),0) RETURN_VALUE,
ROUND(NVL(SUM(e.NET-e.RETURN_VALUE),0),0) NET_AFTER_RETURN,ROUND(NVL(SUM(e.SHIPPING),0),0) SHIPPING,
COUNT(DISTINCT CASE WHEN e.SOLD>0 THEN e.BILL_ID END) ORDERS,
ROUND(NVL(SUM(e.COGS),0),0) KNOWN_COGS,
CASE WHEN NVL(SUM(e.COST_MISSING),0)=0 THEN ROUND(NVL(SUM(e.COGS),0),0) END COGS,
CASE WHEN NVL(SUM(e.COST_MISSING),0)=0 THEN ROUND(NVL(SUM(e.NET-e.RETURN_VALUE-e.COGS),0),0) END GROSS_PROFIT,
ROUND(NVL(SUM(e.KNOWN_NET-e.COGS),0),0) KNOWN_GROSS_PROFIT,
COUNT(DISTINCT CASE WHEN e.COST_MISSING>0 THEN e.PRODUCT_DETAIL_ID END) UNKNOWN_COST_SKUS,
ROUND(NVL(SUM(e.DAMAGED_COST),0),0) DAMAGED_COST
""";

  public Map<String, Object> report(
      String period,
      int year,
      Integer month,
      Integer quarter,
      LocalDate from,
      LocalDate to,
      Long productId,
      String q,
      int page) {
    if (year < 2000 || year > 2200)
      throw new IllegalArgumentException("Năm trong khoảng 2000–2200");
    if (!Set.of("DAY", "MONTH", "QUARTER", "YEAR").contains(period))
      throw new IllegalArgumentException("Chọn ngày, tháng, quý hoặc năm");
    LocalDate start, end;
    if (from != null || to != null) {
      if (from == null || to == null) throw new IllegalArgumentException("Chọn đủ hai ngày");
      start = from;
      end = to;
    } else if (month != null) {
      if (month < 1 || month > 12) throw new IllegalArgumentException("Tháng không hợp lệ");
      start = LocalDate.of(year, month, 1);
      end = start.plusMonths(1).minusDays(1);
    } else if (quarter != null) {
      if (quarter < 1 || quarter > 4) throw new IllegalArgumentException("Quý không hợp lệ");
      start = LocalDate.of(year, (quarter - 1) * 3 + 1, 1);
      end = start.plusMonths(3).minusDays(1);
    } else {
      start = LocalDate.of(year, 1, 1);
      end = LocalDate.of(year, 12, 31);
    }
    if (end.isBefore(start) || ChronoUnit.DAYS.between(start, end) > 3660)
      throw new IllegalArgumentException("Khoảng báo cáo tối đa 10 năm");
    var params =
        AdminRepository.params(
            "f",
            java.sql.Date.valueOf(start),
            "t",
            java.sql.Date.valueOf(end.plusDays(1)),
            "pid",
            productId,
            "q",
            "%" + q.toLowerCase(Locale.ROOT) + "%");
    String source =
        " FROM ("
            + EVENTS
            + ") e JOIN PRODUCT_DETAIL pd ON pd.ID=e.PRODUCT_DETAIL_ID JOIN PRODUCT p ON"
            + " p.ID=pd.PRODUCT_ID WHERE e.EVENT_AT>=:f AND e.EVENT_AT<:t AND (:pid IS NULL OR"
            + " p.ID=:pid) AND (LOWER(p.PRODUCT_NAME) LIKE :q OR LOWER(p.PRODUCT_CODE) LIKE :q OR"
            + " LOWER(pd.SKU) LIKE :q) ";
    String bucket =
        switch (period) {
          case "DAY" -> "TO_CHAR(e.EVENT_AT,'YYYY-MM-DD')";
          case "MONTH" -> "TO_CHAR(e.EVENT_AT,'YYYY-MM')";
          case "QUARTER" -> "TO_CHAR(e.EVENT_AT,'YYYY')||'-Q'||TO_CHAR(e.EVENT_AT,'Q')";
          default -> "TO_CHAR(e.EVENT_AT,'YYYY')";
        };
    var totals = db.one("SELECT " + SUMS + source, paramsToArgs(params));
    var raw =
        db.rows(
            "SELECT "
                + bucket
                + " PERIOD,"
                + SUMS
                + source
                + " GROUP BY "
                + bucket
                + " ORDER BY PERIOD",
            paramsToArgs(params));
    Map<String, Map<String, Object>> byLabel = new HashMap<>();
    raw.forEach(r -> byLabel.put(r.get("PERIOD").toString(), r));
    List<Map<String, Object>> series = new ArrayList<>();
    String previous = "";
    for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
      String label =
          switch (period) {
            case "DAY" -> d.toString();
            case "MONTH" -> d.toString().substring(0, 7);
            case "QUARTER" -> d.getYear() + "-Q" + ((d.getMonthValue() - 1) / 3 + 1);
            default -> Integer.toString(d.getYear());
          };
      if (label.equals(previous)) continue;
      previous = label;
      var row = byLabel.get(label);
      if (row == null) {
        row = new LinkedHashMap<>();
        row.put("PERIOD", label);
        for (String k :
            List.of(
                "SOLD",
                "RETURNED",
                "GROSS",
                "DISCOUNT",
                "NET",
                "RETURN_VALUE",
                "NET_AFTER_RETURN",
                "SHIPPING",
                "ORDERS",
                "COGS",
                "KNOWN_COGS",
                "GROSS_PROFIT",
                "KNOWN_GROSS_PROFIT",
                "UNKNOWN_COST_SKUS",
                "DAMAGED_COST")) row.put(k, BigDecimal.ZERO);
      }
      series.add(row);
    }
    var products =
        db.page(
            "SELECT p.ID,p.PRODUCT_CODE,p.PRODUCT_NAME,"
                + SUMS
                + source
                + " GROUP BY p.ID,p.PRODUCT_CODE,p.PRODUCT_NAME ORDER BY NET_AFTER_RETURN"
                + " DESC,p.ID",
            page,
            20,
            paramsToArgs(params));
    var variants =
        db.rows(
            "SELECT pd.ID,pd.SKU,c.COLOR_NAME,pd.SIZE_NAME,"
                + SUMS
                + source.replace(
                    " WHERE e.EVENT_AT", " JOIN COLOR c ON c.ID=pd.COLOR_ID WHERE e.EVENT_AT")
                + " GROUP BY pd.ID,pd.SKU,c.COLOR_NAME,pd.SIZE_NAME ORDER BY pd.ID",
            paramsToArgs(params));
    return Map.of(
        "from",
        start.toString(),
        "to",
        end.toString(),
        "period",
        period,
        "totals",
        totals,
        "series",
        series,
        "products",
        products,
        "variants",
        variants);
  }

  private Object[] paramsToArgs(Map<String, Object> params) {
    List<Object> args = new ArrayList<>();
    params.forEach(
        (k, v) -> {
          args.add(k);
          args.add(v);
        });
    return args.toArray();
  }

  public List<Map<String, Object>> topProducts(int year, int limit, int minSold) {
    return db.rows(
        "SELECT p.ID,p.PRODUCT_NAME,SUM(e.SOLD-e.RETURNED) SOLD,SUM(e.RETURNED)"
            + " RETURNED,ROUND(SUM(e.NET-e.RETURN_VALUE),0) REVENUE FROM ("
            + EVENTS
            + ") e JOIN PRODUCT_DETAIL pd ON pd.ID=e.PRODUCT_DETAIL_ID JOIN PRODUCT p ON"
            + " p.ID=pd.PRODUCT_ID WHERE e.EVENT_AT>=:f AND e.EVENT_AT<:t GROUP BY"
            + " p.ID,p.PRODUCT_NAME HAVING SUM(e.SOLD-e.RETURNED)>=:min ORDER BY SOLD DESC,p.ID"
            + " FETCH FIRST :lim ROWS ONLY",
        "f",
        java.sql.Date.valueOf(LocalDate.of(year, 1, 1)),
        "t",
        java.sql.Date.valueOf(LocalDate.of(year + 1, 1, 1)),
        "min",
        Math.max(0, minSold),
        "lim",
        Math.max(1, Math.min(limit, 100)));
  }

  public List<Map<String, Object>> lowStock(int threshold) {
    String q =
        """
        SELECT pd.SKU, p.PRODUCT_NAME, c.COLOR_NAME, pd.SIZE_NAME, pd.QUANTITY
          FROM PRODUCT_DETAIL pd
          JOIN PRODUCT p ON p.ID = pd.PRODUCT_ID
          JOIN COLOR c   ON c.ID = pd.COLOR_ID
         WHERE pd.DELETE_FLAG = 0 AND pd.QUANTITY <= :t
         ORDER BY pd.QUANTITY, p.PRODUCT_NAME
        """;

    return db.rows(q, "t", threshold);
  }
}
