package com.example.clothingshop.controller.report;

import static com.example.clothingshop.dto.admin.AdminRequests.*;

import com.example.clothingshop.service.report.DashboardQueryService;
import java.util.*;
import org.springframework.web.bind.annotation.*;

@RestController
public class DashboardController {
  private final DashboardQueryService queries;

  public DashboardController(DashboardQueryService queries) {
    this.queries = queries;
  }

  @SuppressWarnings("unchecked")
  @GetMapping("/api/admin/dashboard")
  public Object dashboard(
      @RequestParam(defaultValue = "5") int threshold,
      @RequestParam(required = false) Integer year) {
    return queries.dashboard(threshold, year);
  }
}
