package com.example.clothingshop.service.inventory;

import static com.example.clothingshop.dto.inventory.InventoryRequests.*;

import com.example.clothingshop.repository.admin.AdminRepository;
import com.example.clothingshop.repository.inventory.InventoryOperationsRepository;
import java.util.*;
import org.springframework.web.bind.annotation.*;

@org.springframework.stereotype.Service
public class InventoryQueryService {
  private final InventoryOperationsRepository queries;
  private final AdminRepository db;

  public InventoryQueryService(AdminRepository db, InventoryOperationsRepository queries) {
    this.queries = queries;
    this.db = db;
  }

  public Object orderIssues(String q) {
    return queries.listBill("q", "%" + q.trim().toLowerCase(Locale.ROOT) + "%");
  }

  public Object variants(String q) {
    return queries.listProductDetail("q", "%" + q.toLowerCase(Locale.ROOT) + "%");
  }

  public Object list(String q, String type, String status, int page) {
    return queries.pageInventoryDocumentLine(
        page, 20, "t", type, "s", status, "q", "%" + q.toLowerCase(Locale.ROOT) + "%");
  }

  public Object detail(long id) {
    return Map.of(
        "document",
        queries.findInventoryDocument("i", id),
        "lines",
        queries.listInventoryDocumentLine("i", id));
  }
}
