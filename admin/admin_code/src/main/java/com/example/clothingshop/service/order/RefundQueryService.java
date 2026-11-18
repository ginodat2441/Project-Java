package com.example.clothingshop.service.order;

import com.example.clothingshop.repository.admin.AdminRepository;
import com.example.clothingshop.repository.order.OrderOperationsRepository;
import org.springframework.web.bind.annotation.*;

@org.springframework.stereotype.Service
public class RefundQueryService {
  private final OrderOperationsRepository queries;
  private final AdminRepository db;

  public RefundQueryService(AdminRepository db, OrderOperationsRepository queries) {
    this.queries = queries;
    this.db = db;
  }

  public Object refunds(long id) {
    return queries.listBillRefund("b", id);
  }
}
