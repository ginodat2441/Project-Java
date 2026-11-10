package com.example.clothingshop.controller.auth;

import static com.example.clothingshop.dto.admin.AdminRequests.*;

import java.util.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;

@RestController
public class AdminSessionController {

  public AdminSessionController() {}

  @GetMapping("/api/csrf")
  public Map<String, String> csrf(CsrfToken token) {
    return Map.of("token", token.getToken(), "headerName", token.getHeaderName());
  }

  @GetMapping("/api/admin/me")
  public Map<String, Object> me(Authentication a) {
    return Map.of("username", a.getName(), "roles", a.getAuthorities());
  }
}
