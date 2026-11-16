package com.example.clothingshop.controller.admin;

import com.example.clothingshop.common.ApiResponse;
import com.example.clothingshop.common.BusinessException;
import com.example.clothingshop.common.NotFoundException;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
  @ExceptionHandler(org.springframework.web.server.ResponseStatusException.class)
  public ResponseEntity<ApiResponse<Void>> upstream(
      org.springframework.web.server.ResponseStatusException e) {
    return ResponseEntity.status(e.getStatusCode())
        .body(
            ApiResponse.error(
                e.getReason() == null ? "Dịch vụ tạm thời chưa sẵn sàng" : e.getReason()));
  }

  @ExceptionHandler(org.springframework.web.multipart.MaxUploadSizeExceededException.class)
  public ResponseEntity<ApiResponse<Void>> uploadTooLarge(Exception e) {
    return ResponseEntity.status(413)
        .body(ApiResponse.error("Mỗi ảnh tối đa 5 MB; mỗi lần tải tối đa 25 MB."));
  }

  @ExceptionHandler(NotFoundException.class)
  public ResponseEntity<ApiResponse<Void>> notFound(NotFoundException e) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(e.getMessage()));
  }

  @ExceptionHandler(BusinessException.class)
  public ResponseEntity<ApiResponse<Void>> business(BusinessException e) {
    return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiResponse<Void>> validation(MethodArgumentNotValidException e) {
    String msg =
        e.getBindingResult().getFieldErrors().stream()
            .map(f -> f.getField() + ": " + f.getDefaultMessage())
            .reduce((a, b) -> a + "; " + b)
            .orElse("Dữ liệu không hợp lệ");
    return ResponseEntity.badRequest().body(ApiResponse.error(msg));
  }

  /** Trả nguyên lỗi Oracle (ORA-xxxxx) để biết câu SQL sai ở đâu. */
  @ExceptionHandler(DataAccessException.class)
  public ResponseEntity<ApiResponse<Void>> sqlError(DataAccessException e) {
    return ResponseEntity.status(HttpStatus.CONFLICT)
        .body(
            ApiResponse.error(
                "Không thể lưu dữ liệu. Kiểm tra mã trùng, liên kết và các giá trị bắt buộc."));
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ApiResponse<Void>> invalid(IllegalArgumentException e) {
    return ResponseEntity.badRequest()
        .body(ApiResponse.error("Dữ liệu không hợp lệ: " + e.getMessage()));
  }
}
