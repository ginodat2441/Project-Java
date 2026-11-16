package com.example.clothingshop.service.report;

import com.example.clothingshop.repository.report.ReportRepository;
import java.time.LocalDate;
import java.util.*;
import org.springframework.stereotype.Service;

/** Application boundary for report read models. SQL projections live in the repository. */
@Service
public class ReportService {
  private final ReportRepository repository;

  public ReportService(ReportRepository repository) {
    this.repository = repository;
  }

  public static final String EVENTS = ReportRepository.EVENTS;

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
    return repository.report(period, year, month, quarter, from, to, productId, q, page);
  }

  public List<Map<String, Object>> topProducts(int year, int limit, int minSold) {
    return repository.topProducts(year, limit, minSold);
  }

  public List<Map<String, Object>> lowStock(int threshold) {
    return repository.lowStock(threshold);
  }
}
