package com.example.clothingshop.controller.auth;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AdminEntryController {
  @GetMapping("/")
  public void redirectToAdmin(HttpServletResponse response) {
    response.setStatus(HttpServletResponse.SC_FOUND);
    response.setHeader("Location", "/admin");
  }
}
