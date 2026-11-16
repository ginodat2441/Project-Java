package com.example.clothingshop.controller.product;

import static com.example.clothingshop.dto.admin.AdminRequests.*;

import com.example.clothingshop.service.admin.AdminService;
import com.example.clothingshop.service.product.ProductFormService;
import jakarta.validation.Valid;
import java.io.IOException;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
public class ProductFormController {
  private final ProductFormService forms;
  private final AdminService admin;

  public ProductFormController(ProductFormService forms, AdminService admin) {
    this.forms = forms;
    this.admin = admin;
  }

  @PostMapping(value = "/api/admin/products/with-images", consumes = "multipart/form-data")
  public long create(
      @Valid @RequestPart("product") Product product,
      @RequestPart("files") List<MultipartFile> files,
      @Valid @RequestPart("images") List<ImageInput> images,
      Authentication a)
      throws IOException {
    return forms.save(null, product, files, images, admin.actor(a.getName()));
  }

  @PutMapping(value = "/api/admin/products/{id}/with-images", consumes = "multipart/form-data")
  public long edit(
      @PathVariable long id,
      @Valid @RequestPart("product") Product product,
      @RequestPart("files") List<MultipartFile> files,
      @Valid @RequestPart("images") List<ImageInput> images,
      Authentication a)
      throws IOException {
    return forms.save(id, product, files, images, admin.actor(a.getName()));
  }
}
