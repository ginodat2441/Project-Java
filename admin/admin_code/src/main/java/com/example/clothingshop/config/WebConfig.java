package com.example.clothingshop.config;

import java.nio.file.Path;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.*;

@Configuration
public class WebConfig implements WebMvcConfigurer {
  @Value("${shop.storefront:../giao dien}")
  String storefront;

  @Override
  public void addResourceHandlers(ResourceHandlerRegistry r) {
    r.addResourceHandler("/store/**")
        .addResourceLocations(
            "classpath:/static/store/",
            Path.of(storefront).toAbsolutePath().normalize().toUri().toString());
  }

  @Override
  public void addViewControllers(ViewControllerRegistry r) {
    r.addViewController("/login").setViewName("forward:/login.html");
    r.addViewController("/forgot-password").setViewName("forward:/forgot-password.html");
    r.addViewController("/reset-password").setViewName("forward:/reset-password.html");
    r.addViewController("/admin").setViewName("forward:/admin/index.html");
    r.addViewController("/").setViewName("redirect:/store/index.html");
  }
}
