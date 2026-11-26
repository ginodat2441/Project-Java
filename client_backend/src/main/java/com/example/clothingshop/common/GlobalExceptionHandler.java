package com.example.clothingshop.common;

import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
    public record AccountError(boolean success, String message, java.util.Map<String,String> fieldErrors) {}

    @ExceptionHandler(AccountInputPolicy.Invalid.class)
    public ResponseEntity<AccountError> accountInput(AccountInputPolicy.Invalid e) {
        return ResponseEntity.badRequest().body(new AccountError(false,e.getMessage(),e.fields()));
    }

    @ExceptionHandler(org.springframework.web.server.ResponseStatusException.class)
    public ResponseEntity<ApiResponse<Void>> status(org.springframework.web.server.ResponseStatusException e) {
        return ResponseEntity.status(e.getStatusCode()).body(ApiResponse.error(e.getReason()==null?"Yêu cầu không hợp lệ.":e.getReason()));
    }

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> notFound(NotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(e.getMessage()));
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> business(BusinessException e) {
        return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> validation(MethodArgumentNotValidException e) {
        Object target=e.getBindingResult().getTarget();
        if(target instanceof com.example.clothingshop.dto.request.RegisterRequest || target instanceof com.example.clothingshop.dto.request.LoginRequest) {
            var fields=new java.util.LinkedHashMap<String,String>();
            e.getBindingResult().getFieldErrors().forEach(f->fields.putIfAbsent(f.getField(),f.getDefaultMessage()));
            return ResponseEntity.badRequest().body(new AccountError(false,"Vui lòng kiểm tra các thông tin được đánh dấu.",fields));
        }
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + ": " + f.getDefaultMessage())
                .reduce((a, b) -> a + "; " + b).orElse("Dữ liệu không hợp lệ");
        return ResponseEntity.badRequest().body(ApiResponse.error(msg));
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ApiResponse<Void>> sqlError(DataAccessException e) {
        Throwable root = e.getMostSpecificCause();
        logger.error("Database request failed: {}", root.getMessage(), e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Máy chủ gặp sự cố khi xử lý dữ liệu. Vui lòng thử lại sau."));
    }
}
