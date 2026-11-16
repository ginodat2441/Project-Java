package com.example.clothingshop.service.order;

import static com.example.clothingshop.dto.admin.AdminRequests.*;

import com.example.clothingshop.repository.admin.AdminRepository;
import com.example.clothingshop.repository.order.OrderOperationsRepository;
import java.util.*;
import org.springframework.web.bind.annotation.*;

@org.springframework.stereotype.Service
public class AdminOrderQueryService {
  private final OrderOperationsRepository queries;
  private final AdminRepository db;
  private final BillService bills;

  public AdminOrderQueryService(
      AdminRepository db, BillService bills, OrderOperationsRepository queries) {
    this.queries = queries;
    this.db = db;
    this.bills = bills;
  }

  public Object order(long id) {
    return Map.of(
        "order",
        queries.findBill("i", id),
        "lines",
        bills.lines(id),
        "history",
        queries.listBillHistory("i", id),
        "inventoryDocuments",
        queries.listInventoryDocument("i", id));
  }
}
