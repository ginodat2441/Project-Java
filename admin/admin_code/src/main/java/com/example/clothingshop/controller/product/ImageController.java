package com.example.clothingshop.controller.product;

import com.example.clothingshop.service.admin.AdminService;
import com.example.clothingshop.service.product.ImageService;
import java.io.IOException;
import java.util.*;
import org.springframework.core.io.*;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
public class ImageController {
  final ImageService images;
  final AdminService admin;

  public ImageController(ImageService images, AdminService admin) {
    this.images = images;
    this.admin = admin;
  }

  @PostMapping("/api/admin/products/{product}/images")
  public long upload(
      @PathVariable long product,
      @RequestParam(required = false) Long colorId,
      @RequestParam MultipartFile file,
      Authentication a)
      throws IOException {
    return images.upload(product, colorId, file, admin.actor(a.getName()));
  }

  @PutMapping("/api/admin/products/{product}/images/{id}")
  public void edit(
      @PathVariable long product,
      @PathVariable long id,
      @RequestParam(required = false) Long colorId,
      @RequestParam(defaultValue = "0") int order,
      @RequestParam(defaultValue = "false") boolean cover,
      Authentication a) {
    images.edit(product, id, colorId, order, cover, admin.actor(a.getName()));
  }

  @DeleteMapping("/api/admin/products/{product}/images/{id}")
  public void remove(@PathVariable long product, @PathVariable long id, Authentication a) {
    images.remove(product, id, admin.actor(a.getName()));
  }

  @GetMapping("/uploads/{name}")
  public ResponseEntity<Resource> get(@PathVariable String name) {
    var path = images.path(name);
    if (!java.nio.file.Files.isRegularFile(path)) return ResponseEntity.notFound().build();
    String type =
        name.endsWith(".png") ? "image/png" : name.endsWith(".webp") ? "image/webp" : "image/jpeg";
    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType(type))
        .header("X-Content-Type-Options", "nosniff")
        .body(new FileSystemResource(path));
  }
}
