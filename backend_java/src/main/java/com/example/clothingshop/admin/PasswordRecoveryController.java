package com.example.clothingshop.admin;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;

@RestController
public class PasswordRecoveryController {
 private final PasswordRecoveryService service;private final String delivery;
 public PasswordRecoveryController(PasswordRecoveryService service,@Value("${shop.reset.delivery:local}") String delivery){this.service=service;this.delivery=delivery;}
 public record Request(@NotBlank @Size(max=255) String identifier){}
 public record Reset(@NotBlank @Size(max=200) String token,@NotBlank @Size(min=12,max=72) String password){}
 @PostMapping("/api/auth/forgot-password") public ResponseEntity<?> request(@Valid @RequestBody Request r,HttpServletRequest request){
  if(!service.request(r.identifier(),request.getRemoteAddr()))return ResponseEntity.status(429).body(Map.of("message","Bạn gửi yêu cầu quá nhiều lần. Vui lòng thử lại sau 10 phút."));
  String message=delivery.equals("smtp")?"Đã tiếp nhận yêu cầu. Nếu tài khoản đang hoạt động tồn tại, bạn sẽ nhận được email khôi phục. Hãy kiểm tra cả thư rác.":"Đã tiếp nhận yêu cầu. Bản này đang dùng thư khôi phục cục bộ, chưa gửi email. Nếu tài khoản hợp lệ, hãy lấy liên kết theo hướng dẫn bên dưới.";
  return ResponseEntity.ok(Map.of("message",message,"delivery",delivery));
 }
 @PostMapping("/api/auth/reset-password") public Object reset(@Valid @RequestBody Reset r){service.reset(r.token(),r.password());return Map.of("message","Đổi mật khẩu thành công. Đăng nhập bằng mật khẩu mới; các phiên cũ đã được thu hồi.");}
}
