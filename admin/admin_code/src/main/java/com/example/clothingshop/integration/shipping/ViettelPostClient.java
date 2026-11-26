package com.example.clothingshop.integration.shipping;

import com.fasterxml.jackson.databind.*;
import java.net.*;
import java.net.http.*;
import java.time.Duration;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class ViettelPostClient {
  private final ObjectMapper json;
  private final String token, origin, base;
  private final boolean enabled;
  private final HttpClient http =
      HttpClient.newBuilder()
          .connectTimeout(Duration.ofSeconds(5))
          .followRedirects(HttpClient.Redirect.NEVER)
          .build();

  @org.springframework.beans.factory.annotation.Autowired
  public ViettelPostClient(
      ObjectMapper json,
      @Value("${shop.shipping.enabled:false}") boolean enabled,
      @Value("${shop.shipping.viettel.token:}") String token,
      @Value("${shop.shipping.origin:}") String origin,
      @Value("${shop.shipping.viettel.environment:production}") String environment) {
    this.json = json;
    this.enabled = enabled;
    this.token = token;
    this.origin = origin;
    base =
        switch (environment) {
          case "production" -> "https://partner.viettelpost.vn";
          case "development" -> "https://partnerdev.viettelpost.vn";
          default ->
              throw new IllegalArgumentException(
                  "Chọn production hoặc development cho Viettel Post");
        };
  }

  // Local fake-server constructor for tests only; browser cannot set a destination URL.
  ViettelPostClient(ObjectMapper json, String base) {
    this.json = json;
    this.base = base;
    this.token = "test-token";
    this.origin = "Kho thử, Hà Nội";
    this.enabled = true;
  }

  public boolean ready() {
    return enabled && !token.isBlank() && !origin.isBlank();
  }

  public Map<String, Object> status() {
    return Map.of(
        "provider",
        "VIETTEL_POST",
        "enabled",
        enabled,
        "configured",
        ready(),
        "originConfigured",
        !origin.isBlank(),
        "tokenConfigured",
        !token.isBlank(),
        "environment",
        base.contains("partnerdev") ? "development" : "production");
  }

  public List<String> wards(String provinceCode) {
    String code = "ADM1-" + provinceCode;
    for (JsonNode province : readCategory("/v3/categories/listProvinceNew")) {
      int id = province.path("PROVINCE_ID").asInt(-1);
      if (id > 0 && code.equals(province.path("PROVINCE_CODE").asText())) {
        var wards = new TreeSet<String>(java.text.Collator.getInstance(Locale.forLanguageTag("vi")));
        for (JsonNode ward : readCategory("/v3/categories/listWardsNew?provinceId=" + id)) {
          String name = ward.path("WARDS_NAME").asText("").strip();
          if (ward.path("PROVINCE_ID").asInt(-1) == id && !name.isBlank()) wards.add(name);
        }
        return List.copyOf(wards);
      }
    }
    throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
        "Viettel Post chưa có danh mục Phường/Xã cho Tỉnh/Thành phố đã chọn.");
  }

  private JsonNode readCategory(String path) {
    try {
      var request = HttpRequest.newBuilder(URI.create(base + path)).timeout(Duration.ofSeconds(8))
          .header("Accept", "application/json").GET().build();
      var response = http.send(request, HttpResponse.BodyHandlers.ofString());
      if (response.statusCode() == 200 && response.body().length() <= 1_000_000) {
        JsonNode result = json.readTree(response.body());
        if (result != null && result.path("status").asInt() == 200
            && !result.path("error").asBoolean() && result.path("data").isArray())
          return result.path("data");
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    } catch (java.io.IOException e) {
      // The caller can retry without losing the selected province.
    }
    throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
        "Không tải được Phường/Xã từ Viettel Post. Vui lòng thử lại.");
  }

  public JsonNode quote(Map<String, Object> parcel) {
    try {
      return send("/v2/order/getPriceAllNlp", parcel, "báo giá");
    } catch (ResponseStatusException failure) {
      if (failure.getStatusCode() != HttpStatus.BAD_GATEWAY || Thread.currentThread().isInterrupted())
        throw failure;
      // A quote is read-only. Retry at most once; creating a shipment must never use this path.
      return send("/v2/order/getPriceAllNlp", parcel, "báo giá");
    }
  }

  public String createOrder(Map<String, Object> order) {
    String code = send("/v2/order/createOrderNlp", order, "tạo vận đơn")
        .path("data").path("ORDER_NUMBER").asText("");
    if (!code.matches("[A-Za-z0-9-]{1,50}"))
      throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
          "Chưa xác nhận được mã vận đơn. Hãy tra cứu mã đơn của shop trên Viettel Post.");
    return code;
  }

  private JsonNode send(String path, Map<String, Object> parcel, String operation) {
    if (!ready())
      throw new ResponseStatusException(
          HttpStatus.SERVICE_UNAVAILABLE, "Viettel Post chưa cấu hình tài khoản API và kho gửi");
    var payload = new LinkedHashMap<>(parcel);
    payload.put("SENDER_ADDRESS", origin);
    try {
      var request =
          HttpRequest.newBuilder(URI.create(base + path))
              .timeout(Duration.ofSeconds(12))
              .header("Content-Type", "application/json")
              .header("Token", token)
              .POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(payload)))
              .build();
      var response = http.send(request, HttpResponse.BodyHandlers.ofString());
      if (response.statusCode() == 401 || response.statusCode() == 403)
        throw new ResponseStatusException(
            HttpStatus.SERVICE_UNAVAILABLE,
            "Viettel Post chưa cấp quyền API hoặc token đã hết hạn");
      if (response.statusCode() != 200 || response.body().length() > 1_000_000)
        throw new ResponseStatusException(
            HttpStatus.BAD_GATEWAY, "Viettel Post chưa phản hồi khi " + operation + ".");
      JsonNode data = json.readTree(response.body());
      if (data.path("error").asBoolean(false)
          || data.has("status") && data.path("status").asInt() != 200)
        throw new ResponseStatusException(
            HttpStatus.BAD_GATEWAY,
            "Viettel Post từ chối " + operation + ". Kiểm tra địa chỉ, dịch vụ và cấu hình tài khoản.");
      return data;
    } catch (ResponseStatusException e) {
      throw e;
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Kết nối Viettel Post bị gián đoạn khi " + operation + ".");
    } catch (Exception e) {
      throw new ResponseStatusException(
          HttpStatus.BAD_GATEWAY,
          "Không kết nối được Viettel Post khi " + operation + ".");
    }
  }
}
