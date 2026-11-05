package com.example.clothingshop.dev;

import com.example.clothingshop.common.ApiResponse;
import com.example.clothingshop.common.BusinessException;
import com.example.clothingshop.common.SqlHelper;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/dev")
@Profile("!prod")
public class SqlConsoleController {

    private final SqlHelper sql;

    public SqlConsoleController(SqlHelper sql) { this.sql = sql; }

    public record SqlRequest(String sql) {}

    @PostMapping("/sql")
    public ApiResponse<List<Map<String, Object>>> run(@RequestBody SqlRequest req) {
        String s = req.sql() == null ? "" : req.sql().trim();
        if (s.endsWith(";")) s = s.substring(0, s.length() - 1);
        String lower = s.toLowerCase();
        if (!(lower.startsWith("select") || lower.startsWith("with")) || s.contains(";")) {
            throw new BusinessException("Chỉ cho phép một câu SELECT");
        }
        return ApiResponse.ok(sql.maps(s, Map.of()));
    }
}
