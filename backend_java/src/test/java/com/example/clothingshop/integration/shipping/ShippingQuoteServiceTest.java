package com.example.clothingshop.integration.shipping;

import static org.junit.jupiter.api.Assertions.*;

import com.example.clothingshop.dto.order.BillRequest;
import com.example.clothingshop.service.shipping.ShippingQuoteService;
import com.example.clothingshop.util.shipping.ProvinceCatalog;
import com.fasterxml.jackson.databind.*;
import com.sun.net.httpserver.HttpServer;
import java.math.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.junit.jupiter.api.*;

class ShippingQuoteServiceTest {
  ObjectMapper json = new ObjectMapper();
  HttpServer server;
  JsonNode payload;
  String token;
  int status = 200;
  String body =
      "{\"status\":200,\"error\":false,\"data\":{\"RESULT\":[{\"MA_DV_CHINH\":\"VCN\",\"TEN_DICHVU\":\"Chuyển"
          + " phát nhanh\",\"GIA_CUOC\":36000,\"THOI_GIAN\":\"2 ngày\"}]}}";
  ShippingQuoteService service;

  @BeforeEach
  void start() throws Exception {
    server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext(
        "/v2/order/getPriceAllNlp",
        exchange -> {
          try {
            token = exchange.getRequestHeaders().getFirst("Token");
            payload = json.readTree(exchange.getRequestBody());
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(status, bytes.length);
            exchange.getResponseBody().write(bytes);
          } finally {
            exchange.close();
          }
        });
    server.start();
    service =
        new ShippingQuoteService(
            new ViettelPostClient(json, "http://127.0.0.1:" + server.getAddress().getPort()),
            300,
            100,
            25,
            20,
            10);
  }

  @AfterEach
  void stop() {
    server.stop(0);
  }

  List<ShippingQuoteService.Offer> quote(String payment) {
    return service.quote(
        "Hà Nội",
        "Phường Cầu Giấy",
        "12 Đường Demo",
        payment,
        List.of(new BillRequest.Item(1L, 2)),
        new BigDecimal("400000"),
        new BigDecimal("50000"));
  }

  @Test
  void sendsServerParcelAndParsesServicesWithoutCustomerCredentials() {
    var offers = quote("COD");
    assertEquals(new BigDecimal("36000"), offers.get(0).fee());
    assertEquals("VCN", offers.get(0).serviceCode());
    assertEquals("test-token", token);
    assertEquals(700, payload.get("PRODUCT_WEIGHT").asInt());
    assertEquals(350000, payload.get("MONEY_COLLECTION").asInt());
    assertTrue(payload.get("RECEIVER_ADDRESS").asText().contains("Hà Nội"));
    assertFalse(payload.has("RECEIVER_PHONE"));
    assertFalse(service.status().toString().contains("test-token"));
  }

  @Test
  void bankHasNoCodCollection() {
    quote("BANK");
    assertEquals(0, payload.get("MONEY_COLLECTION").asInt());
  }

  @Test
  void disabledDoesNotCallNetworkOrInventFee() {
    var disabled =
        new ShippingQuoteService(
            new ViettelPostClient(json, false, "", "", "production"), 300, 0, 20, 20, 10);
    assertFalse(disabled.ready());
    assertThrows(
        org.springframework.web.server.ResponseStatusException.class,
        () ->
            disabled.quote(
                "Hà Nội",
                "Cầu Giấy",
                "12 Demo",
                "COD",
                List.of(new BillRequest.Item(1L, 1)),
                BigDecimal.TEN,
                BigDecimal.ZERO));
    assertNull(payload);
  }

  @Test
  void errorsAreSanitizedAndNeverBecomeZeroFee() {
    status = 401;
    body = "secret-account-error";
    var e =
        assertThrows(
            org.springframework.web.server.ResponseStatusException.class, () -> quote("COD"));
    assertFalse(e.getMessage().contains(body));
    assertFalse(e.getMessage().contains("test-token"));
  }

  @Test
  void rejectsMalformedNegativeAndMissingPrices() {
    body =
        "{\"status\":200,\"data\":{\"RESULT\":[{\"MA_DV_CHINH\":\"VCN\",\"TEN_DICHVU\":\"Demo\",\"GIA_CUOC\":-10}]}}";
    assertThrows(IllegalArgumentException.class, () -> quote("COD"));
    body = "{\"status\":200,\"data\":{}}";
    assertThrows(IllegalArgumentException.class, () -> quote("COD"));
  }

  @Test
  void invalidProvinceAndOversizeNeverCallCarrier() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            service.quote(
                "Không có tỉnh này",
                "Demo",
                "Demo",
                "COD",
                List.of(new BillRequest.Item(1L, 1)),
                BigDecimal.TEN,
                BigDecimal.ZERO));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            service.quote(
                "Hà Nội",
                "Demo",
                "Demo",
                "COD",
                List.of(new BillRequest.Item(1L, 1000)),
                BigDecimal.TEN,
                BigDecimal.ZERO));
    assertNull(payload);
    assertEquals(34, ProvinceCatalog.ALL.size());
  }
}
