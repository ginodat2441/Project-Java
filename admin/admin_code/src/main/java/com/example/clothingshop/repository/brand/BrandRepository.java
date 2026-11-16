package com.example.clothingshop.repository.brand;

import com.example.clothingshop.common.PageResult;
import com.example.clothingshop.common.SqlHelper;
import com.example.clothingshop.dto.brand.BrandRequest;
import com.example.clothingshop.model.brand.Brand;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class BrandRepository {

  private final SqlHelper sql;

  public BrandRepository(SqlHelper sql) {
    this.sql = sql;
  }

  private static final String SELECT_COLS =
      "SELECT ID, BRAND_NAME, COUNTRY, LOGO_URL, DESCRIPTION, CREATED_AT, UPDATED_AT FROM BRAND ";

  public PageResult<Brand> search(String keyword, int page, int size) {
    StringBuilder q = new StringBuilder(SELECT_COLS).append("WHERE DELETE_FLAG = 0 ");
    Map<String, Object> p = new HashMap<>();
    if (keyword != null && !keyword.isBlank()) {
      q.append("AND LOWER(BRAND_NAME) LIKE '%' || LOWER(:keyword) || '%' ");
      p.put("keyword", keyword.trim());
    }
    q.append("ORDER BY ID DESC");
    return sql.page(q.toString(), p, page, size, Brand.class);
  }

  public Optional<Brand> findById(long id) {
    return sql.one(
        SELECT_COLS + "WHERE ID = :id AND DELETE_FLAG = 0", Map.of("id", id), Brand.class);
  }

  public long insert(BrandRequest r) {
    String q =
        """
        INSERT INTO BRAND (BRAND_NAME, COUNTRY, LOGO_URL, DESCRIPTION)
        VALUES (:name, :country, :logo, :desc)
        """;
    return sql.insert(q, params(r));
  }

  public int update(long id, BrandRequest r) {
    String q =
        """
        UPDATE BRAND
           SET BRAND_NAME = :name, COUNTRY = :country,
               LOGO_URL = :logo, DESCRIPTION = :desc
         WHERE ID = :id AND DELETE_FLAG = 0
        """;
    Map<String, Object> p = params(r);
    p.put("id", id);
    return sql.update(q, p);
  }

  /** Xóa mềm */
  public int softDelete(long id) {
    return sql.update(
        "UPDATE BRAND SET DELETE_FLAG = 1 WHERE ID = :id AND DELETE_FLAG = 0", Map.of("id", id));
  }

  private Map<String, Object> params(BrandRequest r) {
    Map<String, Object> p = new HashMap<>(); // HashMap vì cho phép giá trị null
    p.put("name", r.brandName());
    p.put("country", r.country());
    p.put("logo", r.logoUrl());
    p.put("desc", r.description());
    return p;
  }
}
