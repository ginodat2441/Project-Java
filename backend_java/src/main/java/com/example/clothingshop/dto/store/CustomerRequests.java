package com.example.clothingshop.dto.store;

import jakarta.validation.constraints.*;
import java.time.LocalDate;

public final class CustomerRequests {
  private CustomerRequests() {}

  public record CartItem(@NotNull(message = "Thiếu thông tin bắt buộc.") @Positive(message = "Mã sản phẩm chưa hợp lệ.") Long productDetailId, @Min(value = 1, message = "Số lượng tối thiểu là 1.") @Max(value = 100000, message = "Số lượng tối đa là 100.000.") int quantity) {}
  public record Quantity(@Min(value = 1, message = "Số lượng tối thiểu là 1.") @Max(value = 100000, message = "Số lượng tối đa là 100.000.") int quantity) {}
  public record Profile(
      @NotBlank(message = "Vui lòng điền thông tin bắt buộc.") @Size(max = 150, message = "Độ dài thông tin chưa hợp lệ.") String name,
      @NotBlank(message = "Vui lòng điền thông tin bắt buộc.") @Email(message = "Email chưa đúng định dạng.") @Size(max = 255, message = "Độ dài thông tin chưa hợp lệ.") String email,
      @NotBlank(message = "Vui lòng điền thông tin bắt buộc.") @Pattern(regexp = "[+0-9 ()-]{8,20}", message = "Số điện thoại cần từ 8 đến 20 ký tự hợp lệ.") String phone,
      @Min(value = 0, message = "Giá trị chưa hợp lệ.") @Max(value = 2, message = "Giá trị chưa hợp lệ.") Integer gender,
      @PastOrPresent(message = "Ngày sinh không được ở tương lai.") LocalDate birthDate) {}
  public record Password(
      @NotBlank(message = "Vui lòng điền thông tin bắt buộc.") @Size(max = 72, message = "Độ dài thông tin chưa hợp lệ.") String currentPassword,
      @NotBlank(message = "Vui lòng điền thông tin bắt buộc.") @Size(min = 12, max = 72, message = "Độ dài thông tin chưa hợp lệ.") String newPassword) {}
  public record Address(
      @NotBlank(message = "Vui lòng điền thông tin bắt buộc.") @Size(max = 150, message = "Độ dài thông tin chưa hợp lệ.") String name,
      @NotBlank(message = "Vui lòng điền thông tin bắt buộc.") @Pattern(regexp = "[+0-9 ()-]{8,20}", message = "Số điện thoại cần từ 8 đến 20 ký tự hợp lệ.") String phone,
      @NotBlank(message = "Vui lòng điền thông tin bắt buộc.") @Size(max = 100, message = "Độ dài thông tin chưa hợp lệ.") String province,
      @NotBlank(message = "Vui lòng điền thông tin bắt buộc.") @Size(max = 100, message = "Độ dài thông tin chưa hợp lệ.") String ward,
      @NotBlank(message = "Vui lòng điền thông tin bắt buộc.") @Size(max = 300, message = "Độ dài thông tin chưa hợp lệ.") String address,
      boolean isDefault) {}
}
