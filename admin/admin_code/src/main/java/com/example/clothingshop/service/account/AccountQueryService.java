package com.example.clothingshop.service.account;

import static com.example.clothingshop.dto.admin.AdminRequests.*;

import com.example.clothingshop.repository.account.AccountOperationsRepository;
import com.example.clothingshop.repository.admin.AdminRepository;
import java.util.*;
import org.springframework.web.bind.annotation.*;

@org.springframework.stereotype.Service
public class AccountQueryService {
  private final AccountOperationsRepository queries;
  private final AdminRepository db;

  public AccountQueryService(AdminRepository db, AccountOperationsRepository queries) {
    this.queries = queries;
    this.db = db;
  }

  public Object roles() {
    return queries.listManagerRole();
  }

  public Object customers(String q, int page) {
    return queries.pageCustomer(page, 20, "q", "%" + q.toLowerCase(Locale.ROOT) + "%");
  }

  public Object customer(long id) {
    return Map.of("customer", queries.findCustomer("i", id), "orders", queries.listBill("i", id));
  }
}
