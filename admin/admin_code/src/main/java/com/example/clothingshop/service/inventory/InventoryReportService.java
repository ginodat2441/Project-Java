package com.example.clothingshop.service.inventory;

import com.example.clothingshop.repository.inventory.InventoryReportRepository;
import java.time.LocalDate;
import java.util.*;
import org.springframework.stereotype.Service;

/** Application boundary for inventory read models. SQL projections live in the repository. */
@Service
public class InventoryReportService {
  private final InventoryReportRepository repository;

  public InventoryReportService(InventoryReportRepository repository) {
    this.repository = repository;
  }

  public Map<String, Object> report(LocalDate from, LocalDate to, String q, int page) {
    return repository.report(from, to, q, page);
  }
}
