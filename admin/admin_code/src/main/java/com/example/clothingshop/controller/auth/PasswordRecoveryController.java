package com.example.clothingshop.controller.auth;

import com.example.clothingshop.dto.auth.PasswordRecoveryRequests.*;
import com.example.clothingshop.service.auth.PasswordRecoveryService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class PasswordRecoveryController {
  private final PasswordRecoveryService service;
  private final String delivery;

  public PasswordRecoveryController(
      PasswordRecoveryService service, @Value("${shop.reset.delivery:local}") String delivery) {
    this.service = service;
    this.delivery = delivery;
  }

  @PostMapping("/api/auth/forgot-password")
  public ResponseEntity<?> request(@Valid @RequestBody Request r, HttpServletRequest request) {
    if (!service.request(r.identifier(), request.getRemoteAddr()))
      return ResponseEntity.status(429)
          .body(Map.of("message", "Bạn gửi yêu cầu quá nhiều lần. Vui lòng thử lại sau 10 phút."));
    String message =
        delivery.equals("smtp")
            ? "Đã tiếp nhận yêu cầu. Nếu tài khoản đang hoạt động tồn tại, bạn sẽ nhận được email"
                + " khôi phục. Hãy kiểm tra cả thư rác."
            : "Đã tiếp nhận yêu cầu. Bản này đang dùng thư khôi phục cục bộ, chưa gửi email. Nếu"
                + " tài khoản hợp lệ, hãy lấy liên kết theo hướng dẫn bên dưới.";
    return ResponseEntity.ok(Map.of("message", message, "delivery", delivery));
  }

  @PostMapping("/api/auth/reset-password")
  public Object reset(@Valid @RequestBody Reset r) {
    service.reset(r.token(), r.password());
    return Map.of(
        "message",
        "Đổi mật khẩu thành công. Đăng nhập bằng mật khẩu mới; các phiên cũ đã được thu hồi.");
  }
}
