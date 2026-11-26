package com.example.clothingshop.controller.admin;

import com.example.clothingshop.common.ApiResponse;
import com.example.clothingshop.common.BusinessException;
import com.example.clothingshop.common.NotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import com.example.clothingshop.common.FormErrors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
  private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);
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
    var errors = new java.util.LinkedHashMap<String, org.springframework.validation.FieldError>();
    for (var error : e.getBindingResult().getFieldErrors()) {
      errors.merge(error.getField(), error, (first, next) ->
          java.util.Set.of("NotBlank", "NotEmpty", "NotNull").contains(
              next.getCode() == null ? "" : next.getCode()) ? next : first);
    }
    String message = errors.values().stream()
        .map(FormErrors::validation)
        .distinct()
        .collect(java.util.stream.Collectors.joining(" "));
    return ResponseEntity.badRequest().body(ApiResponse.error(
        message.isEmpty() ? "Dữ liệu không hợp lệ." : message));
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ApiResponse<Void>> unreadable(HttpMessageNotReadableException e) {
    return ResponseEntity.badRequest().body(ApiResponse.error(FormErrors.unreadable(e)));
  }

  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  public ResponseEntity<ApiResponse<Void>> typeMismatch(MethodArgumentTypeMismatchException e) {
    return ResponseEntity.badRequest().body(ApiResponse.error(FormErrors.invalidValue(e.getName())));
  }

  @ExceptionHandler(MissingServletRequestParameterException.class)
  public ResponseEntity<ApiResponse<Void>> missingParameter(MissingServletRequestParameterException e) {
    String label = FormErrors.label(e.getParameterName());
    return ResponseEntity.badRequest().body(ApiResponse.error(
        label == null ? "Yêu cầu thiếu thông tin bắt buộc." : label + " là trường bắt buộc."));
  }

  @ExceptionHandler(DataAccessException.class)
  public ResponseEntity<ApiResponse<Void>> sqlError(DataAccessException e, HttpServletRequest request) {
    String message = FormErrors.database(e);
    if (message != null) {
      return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiResponse.error(message));
    }
    logger.error("Database request failed for {} {}", request.getMethod(), request.getRequestURI(), e);
    String fallback = "GET".equals(request.getMethod())
        ? "Không tải được dữ liệu. Vui lòng thử lại sau."
        : "Không xử lý được dữ liệu. Vui lòng thử lại sau.";
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.error(fallback));
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ApiResponse<Void>> invalid(IllegalArgumentException e) {
    return ResponseEntity.badRequest()
        .body(ApiResponse.error("Dữ liệu gửi lên không hợp lệ."));
  }
}
