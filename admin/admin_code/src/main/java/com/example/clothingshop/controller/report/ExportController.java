package com.example.clothingshop.controller.report;

import com.example.clothingshop.service.admin.AdminSearchService;
import com.example.clothingshop.service.inventory.InventoryReportService;
import com.example.clothingshop.service.report.ReportService;
import com.example.clothingshop.util.admin.Csv;
import java.time.*;
import java.util.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class ExportController {
  private final ReportService reports;
  private final AdminSearchService search;
  private final InventoryReportService inventory;

  public ExportController(ReportService r, AdminSearchService s, InventoryReportService i) {
    reports = r;
    search = s;
    inventory = i;
  }

  LocalDate date(Map<String, String> f, String k) {
    String v = f.get(k);
    return v == null || v.isBlank() ? null : LocalDate.parse(v);
  }

  Integer integer(Map<String, String> f, String k) {
    String v = f.get(k);
    return v == null || v.isBlank() ? null : Integer.valueOf(v);
  }

  @GetMapping("/api/admin/reports/export.xlsx")
  public ResponseEntity<byte[]> salesWorkbook(
      @RequestParam(required = false) Integer year,
      @RequestParam(required = false) Integer month,
      @RequestParam(required = false) Integer quarter,
      @RequestParam(required = false) LocalDate from,
      @RequestParam(required = false) LocalDate to,
      @RequestParam(required = false) Long productId,
      @RequestParam(defaultValue = "") String q) throws java.io.IOException {
    var bytes = reports.salesWorkbook(year == null ? LocalDate.now().getYear() : year,
        month, quarter, from, to, productId, q);
    return ResponseEntity.ok()
        .header("Content-Disposition", "attachment; filename=bao-cao-ban-hang.xlsx")
        .cacheControl(org.springframework.http.CacheControl.noStore())
        .contentType(org.springframework.http.MediaType.parseMediaType(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
        .body(bytes);
  }

  @SuppressWarnings("unchecked")
  @GetMapping("/api/admin/reports/export")
  public ResponseEntity<String> reports(@RequestParam Map<String, String> f) {
    Integer year = integer(f, "year");
    Long product = f.get("productId") == null ? null : Long.valueOf(f.get("productId"));
    String detail = f.getOrDefault("detail", "period");
    var rows = new ArrayList<Map<String, Object>>();
    for (int page = 1; ; page++) {
      var d =
          reports.report(
              f.getOrDefault("period", "MONTH"),
              year == null ? LocalDate.now().getYear() : year,
              integer(f, "month"),
              integer(f, "quarter"),
              date(f, "from"),
              date(f, "to"),
              product,
              f.getOrDefault("q", ""),
              page);
      if (detail.equals("sku")) {
        rows.addAll((List<Map<String, Object>>) d.get("variants"));
        break;
      }
      if (detail.equals("period")) {
        rows.addAll((List<Map<String, Object>>) d.get("series"));
        break;
      }
      if (!detail.equals("product"))
        throw new IllegalArgumentException("Chọn period, product hoặc sku");
      var products = (Map<String, Object>) d.get("products");
      var items = (List<Map<String, Object>>) products.get("items");
      rows.addAll(items);
      if (items.isEmpty() || rows.size() >= ((Number) products.get("total")).longValue()) break;
    }
    return Csv.response(rows, "doanh-thu-" + detail);
  }

  @SuppressWarnings("unchecked")
  @GetMapping("/api/admin/inventory/export")
  public ResponseEntity<String> inventory(@RequestParam Map<String, String> f) {
    var rows = new ArrayList<Map<String, Object>>();
    String detail = f.getOrDefault("detail", "balance");
    for (int page = 1; ; page++) {
      Map<String, Object> d;
      if (detail.equals("history")) {
        var filters = new HashMap<>(f);
        filters.put("page", Integer.toString(page));
        d = (Map<String, Object>) search.history(filters);
      } else d = inventory.report(date(f, "from"), date(f, "to"), f.getOrDefault("q", ""), page);
      var items = (List<Map<String, Object>>) d.get("items");
      rows.addAll(items);
      if (items.isEmpty() || rows.size() >= ((Number) d.get("total")).longValue()) break;
    }
    return Csv.response(rows, "ton-kho-" + detail);
  }
}
