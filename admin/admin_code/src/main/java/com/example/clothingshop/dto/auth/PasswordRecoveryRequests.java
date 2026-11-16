package com.example.clothingshop.dto.auth;

import jakarta.validation.constraints.*;
import org.springframework.web.bind.annotation.*;

public final class PasswordRecoveryRequests {
  private PasswordRecoveryRequests() {}

  public record Request(@NotBlank @Size(max = 255) String identifier) {}

  public record Reset(
      @NotBlank @Size(max = 200) String token,
      @NotBlank @Size(min = 12, max = 72) String password) {}
}
