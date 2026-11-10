package com.example.clothingshop.repository.admin;

import com.example.clothingshop.common.*;
import java.util.*;
import org.springframework.stereotype.Repository;

@Repository
public class AdminRepository {
  private final SqlHelper sql;

  public AdminRepository(SqlHelper sql) {
    this.sql = sql;
  }

  public List<Map<String, Object>> rows(String q, Object... args) {
    return sql.maps(q, params(args));
  }

  public Map<String, Object> one(String q, Object... args) {
    return rows(q, args).stream()
        .findFirst()
        .orElseThrow(() -> new NotFoundException("Không tìm thấy dữ liệu"));
  }

  public int update(String q, Object... args) {
    return sql.update(q, params(args));
  }

  public long insert(String q, Object... args) {
    return sql.insert(q, params(args));
  }

  public static Map<String, Object> params(Object... args) {
    Map<String, Object> p = new HashMap<>();
    for (int i = 0; i < args.length; i += 2) p.put(args[i].toString(), args[i + 1]);
    return p;
  }

  public static long number(Map<String, Object> m, String key) {
    return ((Number) m.get(key)).longValue();
  }

  public void audit(Long actor, String action, String type, Long id) {
    update(
        "INSERT INTO ADMIN_AUDIT(MANAGER_ID,ACTION,OBJECT_TYPE,OBJECT_ID) VALUES(:a,:b,:c,:d)",
        "a",
        actor,
        "b",
        action,
        "c",
        type,
        "d",
        id);
  }

  public void auditChange(
      Long actor, String action, String type, Long id, Object before, Object after, String reason) {
    try {
      var json = new com.fasterxml.jackson.databind.ObjectMapper().findAndRegisterModules();
      update(
          "INSERT INTO"
              + " ADMIN_AUDIT(MANAGER_ID,ACTION,OBJECT_TYPE,OBJECT_ID,BEFORE_DATA,AFTER_DATA,REASON)"
              + " VALUES(:a,:b,:c,:d,:before,:after,:reason)",
          "a",
          actor,
          "b",
          action,
          "c",
          type,
          "d",
          id,
          "before",
          json.writeValueAsString(before),
          "after",
          json.writeValueAsString(after),
          "reason",
          reason);
    } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
      throw new IllegalArgumentException("Không ghi được nhật ký", e);
    }
  }

  public Map<String, Object> page(String query, int page, int size, Object... args) {
    int s = Math.max(1, Math.min(size, 100)), p = Math.max(1, page);
    long total = number(one("SELECT COUNT(*) N FROM (" + query + ")", args), "N");
    var values = new ArrayList<>(Arrays.asList(args));
    values.addAll(Arrays.asList("off", (long) (p - 1) * s, "lim", s));
    return Map.of(
        "items",
        rows(query + " OFFSET :off ROWS FETCH NEXT :lim ROWS ONLY", values.toArray()),
        "total",
        total,
        "page",
        p,
        "size",
        s);
  }
}
