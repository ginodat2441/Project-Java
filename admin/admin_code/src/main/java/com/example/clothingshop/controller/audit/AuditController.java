package com.example.clothingshop.controller.audit;

import static com.example.clothingshop.dto.admin.AdminRequests.*;

import com.example.clothingshop.service.admin.AdminSearchService;
import java.util.*;
import org.springframework.web.bind.annotation.*;

@RestController
public class AuditController {
  private final AdminSearchService search;

  public AuditController(AdminSearchService search) {
    this.search = search;
  }

  @GetMapping("/api/admin/audit")
  public Object audit(@RequestParam Map<String, String> filters) {
    return search.audit(filters);
  }
}
