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

  public JsonNode quote(Map<String, Object> parcel) {
    if (!ready())
      throw new ResponseStatusException(
          HttpStatus.SERVICE_UNAVAILABLE, "Viettel Post chưa cấu hình tài khoản API và kho gửi");
    var payload = new LinkedHashMap<>(parcel);
    payload.put("SENDER_ADDRESS", origin);
    try {
      var request =
          HttpRequest.newBuilder(URI.create(base + "/v2/order/getPriceAllNlp"))
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
            HttpStatus.BAD_GATEWAY, "Chưa lấy được báo giá Viettel Post; vui lòng thử lại");
      JsonNode data = json.readTree(response.body());
      if (data.path("error").asBoolean(false)
          || data.has("status") && data.path("status").asInt() != 200)
        throw new ResponseStatusException(
            HttpStatus.BAD_GATEWAY,
            "Viettel Post chưa báo giá được tuyến này; kiểm tra địa chỉ và cấu hình kho gửi");
      return data;
    } catch (ResponseStatusException e) {
      throw e;
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Tính cước bị gián đoạn");
    } catch (Exception e) {
      throw new ResponseStatusException(
          HttpStatus.BAD_GATEWAY,
          "Không kết nối được Viettel Post; không dùng phí giả hoặc phí 0 thay thế");
    }
  }
}
