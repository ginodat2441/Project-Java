package com.example.clothingshop.service.report;

import static com.example.clothingshop.dto.admin.AdminRequests.*;

import com.example.clothingshop.repository.admin.AdminRepository;
import com.example.clothingshop.repository.report.ReportOperationsRepository;
import com.example.clothingshop.service.product.ProductMetricsService;
import java.util.*;
import org.springframework.web.bind.annotation.*;

@org.springframework.stereotype.Service
public class DashboardQueryService {
  private final ReportOperationsRepository queries;
  private final AdminRepository db;
  private final ReportService reports;
  private final ProductMetricsService metrics;

  public DashboardQueryService(
      AdminRepository db,
      ReportService reports,
      ProductMetricsService metrics,
      ReportOperationsRepository queries) {
    this.queries = queries;
    this.db = db;
    this.reports = reports;
    this.metrics = metrics;
  }

  public Object dashboard(int threshold, Integer year) {
    int y = year == null ? java.time.LocalDate.now().getYear() : year;
    var report = reports.report("MONTH", y, null, null, null, null, null, "", 1);
    var revenue =
        ((List<Map<String, Object>>) report.get("series"))
            .stream()
                .map(r -> Map.of("MONTH", r.get("PERIOD"), "REVENUE", r.get("NET_AFTER_RETURN")))
                .toList();
    var top = reports.topProducts(y, 10, 0);
    return Map.of(
        "period",
        y,
        "totals",
        report.get("totals"),
        "inventory",
        metrics.stock("", null, Integer.MAX_VALUE, 1, null, null).get("globalTotals"),
        "alerts",
        queries.findProductDetail("t", Math.max(0, threshold)),
        "revenue",
        revenue,
        "statuses",
        queries.listBill(),
        "topProducts",
        top,
        "lowStock",
        queries.listProductDetail("t", Math.max(0, threshold)));
  }
}
