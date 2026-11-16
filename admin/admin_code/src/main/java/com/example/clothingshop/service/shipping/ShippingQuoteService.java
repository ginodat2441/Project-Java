package com.example.clothingshop.service.shipping;

import com.example.clothingshop.dto.order.BillRequest;
import com.example.clothingshop.integration.shipping.ViettelPostClient;
import com.example.clothingshop.util.shipping.ProvinceCatalog;
import com.fasterxml.jackson.databind.JsonNode;
import java.math.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class ShippingQuoteService {
  private final ViettelPostClient carrier;
  private final int grams, packing, length, width, height;

  public ShippingQuoteService(
      ViettelPostClient c,
      @Value("${shop.shipping.grams-per-item:0}") int grams,
      @Value("${shop.shipping.packing-grams:0}") int packing,
      @Value("${shop.shipping.length-cm:0}") int length,
      @Value("${shop.shipping.width-cm:0}") int width,
      @Value("${shop.shipping.height-cm:0}") int height) {
    carrier = c;
    this.grams = grams;
    this.packing = packing;
    this.length = length;
    this.width = width;
    this.height = height;
  }

  public boolean ready() {
    return carrier.ready() && grams > 0 && packing >= 0 && length > 0 && width > 0 && height > 0;
  }

  public Map<String, Object> status() {
    var s = new LinkedHashMap<>(carrier.status());
    s.put("configured", ready());
    s.put("parcelConfigured", grams > 0 && length > 0 && width > 0 && height > 0);
    s.put(
        "weightPolicy",
        "Gram mỗi sản phẩm cộng bao bì theo cấu hình shop; cần cân thực tế trước khi gửi");
    return s;
  }

  public record Offer(
      String serviceCode, String serviceName, BigDecimal fee, String deliveryTime) {}

  public List<Offer> quote(
      String province,
      String ward,
      String address,
      String payment,
      List<BillRequest.Item> items,
      BigDecimal subtotal,
      BigDecimal discount) {
    if (!ready())
      throw new org.springframework.web.server.ResponseStatusException(
          org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE,
          "Shop chưa hoàn tất cấu hình Viettel Post, kho gửi và trọng lượng/kích thước đóng gói");
    var p = ProvinceCatalog.resolve(province);
    if (ward == null || ward.isBlank() || address == null || address.isBlank())
      throw new IllegalArgumentException("Nhập đủ phường/xã và số nhà/đường để tính cước");
    long count = items.stream().mapToLong(BillRequest.Item::quantity).sum(),
        weight = Math.addExact(Math.multiplyExact(count, grams), packing);
    if (weight <= 0 || weight > 30000)
      throw new IllegalArgumentException(
          "Kiện vượt cấu hình giao online 30kg; liên hệ shop để chia kiện");
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("RECEIVER_ADDRESS", address.strip() + ", " + ward.strip() + ", " + p.name());
    body.put("PRODUCT_TYPE", "HH");
    body.put("PRODUCT_WEIGHT", weight);
    body.put("PRODUCT_PRICE", subtotal.setScale(0, RoundingMode.HALF_UP));
    body.put(
        "MONEY_COLLECTION",
        "COD".equals(payment)
            ? subtotal.subtract(discount).setScale(0, RoundingMode.HALF_UP)
            : BigDecimal.ZERO);
    body.put("PRODUCT_LENGTH", length);
    body.put("PRODUCT_WIDTH", width);
    body.put("PRODUCT_HEIGHT", height);
    body.put("TYPE", 1);
    return parse(carrier.quote(body));
  }

  static List<Offer> parse(JsonNode response) {
    JsonNode data = response.has("data") ? response.get("data") : response,
        rows = data.path("RESULT");
    if (data.isArray()) rows = data;
    if (!rows.isArray())
      throw new IllegalArgumentException(
          "Phản hồi cước Viettel Post chưa đúng hợp đồng API; cần xác minh tài liệu tài khoản tích"
              + " hợp");
    var offers = new LinkedHashMap<String, Offer>();
    for (var row : rows) {
      String code = row.path("MA_DV_CHINH").asText(), name = row.path("TEN_DICHVU").asText();
      JsonNode amount = row.get("GIA_CUOC");
      if (code.isBlank() || name.isBlank() || amount == null || !amount.isNumber()) continue;
      BigDecimal fee = amount.decimalValue();
      if (fee.signum() < 0 || fee.stripTrailingZeros().scale() > 0) continue;
      offers.putIfAbsent(
          code, new Offer(code, name, fee.setScale(0), row.path("THOI_GIAN").asText()));
    }
    if (offers.isEmpty())
      throw new IllegalArgumentException("Viettel Post chưa có dịch vụ cho địa chỉ này");
    return List.copyOf(offers.values());
  }
}
