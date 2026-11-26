package com.example.clothingshop.controller.dev;

import com.example.clothingshop.common.ApiResponse;
import com.example.clothingshop.dto.dev.SqlRequest;
import com.example.clothingshop.service.dev.SqlConsoleService;
import java.util.List;
import java.util.Map;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * CHỈ DÙNG KHI HỌC/DEMO: chạy SELECT tùy ý và xem kết quả JSON trong Postman. POST /api/dev/sql
 * {"sql": "SELECT * FROM BRAND WHERE ROWNUM <= 5"} Chỉ cho phép SELECT. Tắt bằng profile "prod"
 * (--spring.profiles.active=prod). KHÔNG đưa lên production vì có nguy cơ lộ dữ liệu.
 */
@RestController
@RequestMapping("/api/dev")
@Profile("sql-console & !prod & !admin")
public class SqlConsoleController {

  private final SqlConsoleService service;

  public SqlConsoleController(SqlConsoleService service) {
    this.service = service;
  }

  @PostMapping("/sql")
  public ApiResponse<List<Map<String, Object>>> run(@RequestBody SqlRequest req) {
    return ApiResponse.ok(service.execute(req));
  }
}
