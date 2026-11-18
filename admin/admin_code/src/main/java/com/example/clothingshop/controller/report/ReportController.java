package com.example.clothingshop.controller.report;

import com.example.clothingshop.service.report.ReportService;
import java.time.LocalDate;
import org.springframework.web.bind.annotation.*;

@RestController
public class ReportController {
  private final ReportService service;

  public ReportController(ReportService service) {
    this.service = service;
  }

  @GetMapping("/api/admin/reports")
  public Object reports(
      @RequestParam(defaultValue = "MONTH") String period,
      @RequestParam(required = false) Integer year,
      @RequestParam(required = false) Integer month,
      @RequestParam(required = false) Integer quarter,
      @RequestParam(required = false) LocalDate from,
      @RequestParam(required = false) LocalDate to,
      @RequestParam(required = false) Long productId,
      @RequestParam(defaultValue = "") String q,
      @RequestParam(defaultValue = "1") int page) {
    return service.report(
        period,
        year == null ? LocalDate.now().getYear() : year,
        month,
        quarter,
        from,
        to,
        productId,
        q,
        page);
  }
}
