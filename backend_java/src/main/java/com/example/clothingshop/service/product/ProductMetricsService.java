package com.example.clothingshop.service.product;

import com.example.clothingshop.repository.product.ProductMetricsRepository;
import java.time.LocalDate;
import java.util.*;
import org.springframework.stereotype.Service;

/** Application boundary for product read models. SQL projections live in the repository. */
@Service
public class ProductMetricsService {
  private final ProductMetricsRepository repository;

  public ProductMetricsService(ProductMetricsRepository repository) {
    this.repository = repository;
  }

  public Map<String, Object> stock(
      String q,
      Long productId,
      int threshold,
      int page,
      java.math.BigDecimal minValue,
      java.math.BigDecimal maxValue) {
    return repository.stock(q, productId, threshold, page, minValue, maxValue);
  }

  public Map<String, Object> stock(
      String q,
      Long productId,
      int threshold,
      int page,
      java.math.BigDecimal minValue,
      java.math.BigDecimal maxValue,
      String stockState) {
    return repository.stock(q, productId, threshold, page, minValue, maxValue, stockState);
  }

  public Map<String, Object> list(
      String q, Long category, Integer status, int page, LocalDate from, LocalDate to) {
    return repository.list(q, category, status, page, from, to);
  }
}
