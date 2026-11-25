package com.example.clothingshop.service;

import com.example.clothingshop.common.BusinessException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class DeliveryDirectory {
    private static final String BASE = "https://partner.viettelpost.vn/v3/categories/";
    private final ObjectMapper json;
    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(4)).build();
    private volatile Cached<Province> provinces;
    private final Map<Integer, Cached<String>> wards = new ConcurrentHashMap<>();

    public DeliveryDirectory(ObjectMapper json) {
        this.json = json;
    }

    public record Province(String code, String name, int id) {}
    private record Cached<T>(List<T> entries, Instant expires) {
        boolean stale() { return !expires.isAfter(Instant.now()); }
    }

    public List<Province> provinces() {
        var current = provinces;
        if (current != null && !current.stale()) return current.entries();
        var result = new ArrayList<Province>();
        for (var row : read("listProvinceNew")) {
            String code = row.path("PROVINCE_CODE").asText();
            String name = row.path("PROVINCE_NAME").asText().strip();
            int id = row.path("PROVINCE_ID").asInt();
            if (code.matches("ADM1-[0-9]{2}") && id > 0 && !name.isBlank())
                result.add(new Province(code.substring(5), name, id));
        }
        result.sort(Comparator.comparing(Province::name, java.text.Collator.getInstance(Locale.forLanguageTag("vi"))));
        if (result.isEmpty()) throw unavailable();
        provinces = cached(result);
        return provinces.entries();
    }

    public List<String> wards(String province) {
        return wards(resolve(province));
    }

    public Province resolve(String input) {
        return provinces().stream().filter(p -> p.code().equals(input.strip())
                || p.name().equalsIgnoreCase(input.strip())).findFirst()
                .orElseThrow(() -> new BusinessException("Tỉnh/Thành phố phải được chọn trong danh sách."));
    }

    public List<String> wards(Province province) {
        return wards.compute(province.id(), (id, old) -> {
            if (old != null && !old.stale()) return old;
            var names = new TreeSet<String>(java.text.Collator.getInstance(Locale.forLanguageTag("vi")));
            for (var row : read("listWardsNew?provinceId=" + id)) {
                String name = row.path("WARDS_NAME").asText().strip();
                if (row.path("PROVINCE_ID").asInt() == id && !name.isBlank()) names.add(name);
            }
            if (names.isEmpty()) throw unavailable();
            return cached(names);
        }).entries();
    }

    private <T> Cached<T> cached(Collection<T> entries) {
        return new Cached<>(List.copyOf(entries), Instant.now().plus(Duration.ofHours(6)));
    }

    private JsonNode read(String path) {
        try {
            var request = HttpRequest.newBuilder(URI.create(BASE + path)).timeout(Duration.ofSeconds(8))
                    .header("Accept", "application/json").GET().build();
            var response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200 && response.body().length() <= 1_000_000) {
                var data = json.readTree(response.body());
                if (data != null && data.path("status").asInt() == 200
                        && !data.path("error").asBoolean() && data.path("data").isArray()) return data.path("data");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (IOException e) {
            // The UI offers a retry without losing other address fields.
        }
        throw unavailable();
    }

    private ResponseStatusException unavailable() {
        return new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                "Không tải được danh mục địa chỉ Viettel Post. Vui lòng thử lại.");
    }
}
