package com.example.clothingshop.service.product;

import static com.example.clothingshop.dto.admin.AdminRequests.*;

import com.example.clothingshop.repository.admin.AdminRepository;
import com.example.clothingshop.repository.product.ProductOperationsRepository;
import java.util.*;
import org.springframework.web.bind.annotation.*;

@org.springframework.stereotype.Service
public class AdminProductQueryService {
  private final ProductOperationsRepository queries;
  private final AdminRepository db;

  public AdminProductQueryService(AdminRepository db, ProductOperationsRepository queries) {
    this.queries = queries;
    this.db = db;
  }

  public Map<String, Object> lookups() {
    return Map.of(
        "brands", queries.listBrand(),
        "materials", queries.listMaterial(),
        "colors", queries.listColor(),
        "categories", queries.listCategory());
  }
}
