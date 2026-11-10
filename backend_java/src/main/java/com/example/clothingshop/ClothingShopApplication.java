package com.example.clothingshop;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ClothingShopApplication {
  public static void main(String[] args) {
    var context = SpringApplication.run(ClothingShopApplication.class, args);
    if (java.util.Arrays.stream(args)
        .anyMatch(
            a ->
                a.equals("--create-admin")
                    || a.equals("--cleanup-images")
                    || a.equals("--smtp-check"))) {
      System.exit(SpringApplication.exit(context));
    }
  }
}
