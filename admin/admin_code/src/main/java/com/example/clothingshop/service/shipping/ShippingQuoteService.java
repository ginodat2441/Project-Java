package com.example.clothingshop.service.shipping;

import com.example.clothingshop.dto.order.BillRequest;
import com.example.clothingshop.integration.shipping.ViettelPostClient;
import com.example.clothingshop.util.shipping.ProvinceCatalog;
import com.fasterxml.jackson.databind.JsonNode;
import java.math.*;
import java.time.Instant;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class ShippingQuoteService {
  private final ViettelPostClient carrier;
  private final int grams, packing, length, width, height;
  private volatile Instant lastSuccessfulQuote;

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

  public Map<String, Object> adminStatus() {
    var status = status();
    status.put("lastSuccessfulQuote", lastSuccessfulQuote);
    return status;
  }

  public List<String> wards(String province) {
    return carrier.wards(ProvinceCatalog.resolve(province).code());
  }

  public List<Offer> testQuote(String province, String ward, String address) {
    return quote(province, ward, address, "BANK", 1, BigDecimal.valueOf(100000), BigDecimal.ZERO);
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
    long count = items.stream().mapToLong(BillRequest.Item::quantity).sum();
    return quote(province, ward, address, payment, count, subtotal, discount);
  }

  private List<Offer> quote(
      String province, String ward, String address, String payment, long count,
      BigDecimal subtotal, BigDecimal discount) {
    if (!ready())
      throw new org.springframework.web.server.ResponseStatusException(
          org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE,
          "Shop chưa hoàn tất cấu hình Viettel Post, kho gửi và trọng lượng/kích thước đóng gói");
    var p = ProvinceCatalog.resolve(province);
    if (ward == null || ward.isBlank() || address == null || address.isBlank())
      throw new IllegalArgumentException("Nhập đủ phường/xã và số nhà/đường để tính cước");

    long weight = Math.addExact(Math.multiplyExact(count, grams), packing);
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
    var offers = parse(carrier.quote(body));
    lastSuccessfulQuote = Instant.now();
    return offers;
  }

  public List<Offer> quoteOrder(String address, long count, BigDecimal subtotal, BigDecimal cod) {
    if (address == null || address.isBlank())
      throw new com.example.clothingshop.common.BusinessException("Địa chỉ giao hàng là trường bắt buộc.");
    var request = new LinkedHashMap<String, Object>();
    addParcelDimensions(request, count);
    request.put("RECEIVER_ADDRESS", address);
    request.put("PRODUCT_TYPE", "HH");
    request.put("PRODUCT_PRICE", subtotal.setScale(0, RoundingMode.HALF_UP));
    request.put("MONEY_COLLECTION", cod.setScale(0, RoundingMode.HALF_UP));
    request.put("TYPE", 1);
    var offers = parse(carrier.quote(request));
    lastSuccessfulQuote = Instant.now();
    return offers;
  }

  public int gramsPerItem() { return grams; }

  public void addParcelDimensions(Map<String, Object> request, long itemCount) {
    if (!ready()) throw new com.example.clothingshop.common.BusinessException(
        "Cần cấu hình tài khoản Viettel Post, kho gửi và thông số kiện.");
    long weight = itemCount * grams + packing;
    if (itemCount < 1 || weight < 1 || weight > 30000)
      throw new com.example.clothingshop.common.BusinessException("Khối lượng kiện phải từ 1 g đến 30 kg.");
    request.put("PRODUCT_WEIGHT", weight);
    request.put("PRODUCT_LENGTH", length);
    request.put("PRODUCT_WIDTH", width);
    request.put("PRODUCT_HEIGHT", height);
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
