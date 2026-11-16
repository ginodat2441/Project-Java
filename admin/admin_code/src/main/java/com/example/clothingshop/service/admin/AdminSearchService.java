package com.example.clothingshop.service.admin;

import com.example.clothingshop.repository.admin.AdminOperationsRepository;
import com.example.clothingshop.repository.admin.AdminRepository;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Service;

@Service
public class AdminSearchService {
  private final AdminOperationsRepository queries;
  private final AdminRepository db;

  public AdminSearchService(AdminRepository db, AdminOperationsRepository queries) {
    this.queries = queries;
    this.db = db;
  }

  Object v(Map<String, String> f, String k) {
    String s = f.get(k);
    return s == null || s.isBlank() ? null : s;
  }

  Object day(Map<String, String> f, String k, boolean end) {
    Object s = v(f, k);
    return s == null
        ? null
        : java.sql.Date.valueOf(
            end ? LocalDate.parse(s.toString()).plusDays(1) : LocalDate.parse(s.toString()));
  }

  int page(Map<String, String> f) {
    return Integer.parseInt(f.getOrDefault("page", "1"));
  }

  String like(Map<String, String> f, String key) {
    return "%" + f.getOrDefault(key, "").toLowerCase(Locale.ROOT) + "%";
  }

  public Object managers(Map<String, String> f) {
    return queries.listManager("q", like(f, "q"), "r", v(f, "role"), "s", v(f, "status"));
  }

  public Object categories(String q) {
    return queries.listCategory("q", "%" + q.toLowerCase(Locale.ROOT) + "%");
  }

  public Object categoryImpact(long id) {
    return queries.findCategory("i", id);
  }

  public Object vouchers(Map<String, String> f) {
    return queries.listVoucher("q", like(f, "q"), "s", v(f, "effective"));
  }

  public Object voucherOrders(long id) {
    return queries.listBill("i", id);
  }

  public Object orders(Map<String, String> f) {
    return queries.pageBill(
        page(f),
        20,
        "q",
        like(f, "q"),
        "s",
        v(f, "status"),
        "pm",
        v(f, "paymentMethod"),
        "ps",
        v(f, "paymentStatus"),
        "rs",
        v(f, "returnStatus"),
        "f",
        day(f, "from", false),
        "t",
        day(f, "to", true));
  }

  public Object history(Map<String, String> f) {
    return queries.pageInventoryHistory(
        page(f),
        30,
        "q",
        like(f, "q"),
        "v",
        v(f, "variantId"),
        "kind",
        v(f, "type"),
        "f",
        day(f, "from", false),
        "t",
        day(f, "to", true),
        "doc",
        like(f, "document"),
        "bill",
        like(f, "bill"));
  }

  public Object audit(Map<String, String> f) {
    return queries.pageAdminAudit(
        page(f),
        30,
        "q",
        like(f, "q"),
        "act",
        v(f, "action"),
        "obj",
        v(f, "object"),
        "f",
        day(f, "from", false),
        "t",
        day(f, "to", true));
  }
}
