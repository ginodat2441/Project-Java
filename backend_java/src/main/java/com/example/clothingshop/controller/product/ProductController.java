package com.example.clothingshop.controller.product;

import com.example.clothingshop.common.ApiResponse;
import com.example.clothingshop.common.NotFoundException;
import com.example.clothingshop.common.PageResult;
import com.example.clothingshop.dto.product.ProductDetailView;
import com.example.clothingshop.dto.product.ProductView;
import com.example.clothingshop.repository.product.ProductRepository;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/products")
public class ProductController {

  private final ProductRepository repo;

  public ProductController(ProductRepository repo) {
    this.repo = repo;
  }

  @GetMapping
  public ApiResponse<PageResult<ProductView>> search(
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false) Long brandId,
      @RequestParam(required = false) Integer gender,
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(defaultValue = "10") int size) {
    return ApiResponse.ok(repo.search(keyword, brandId, gender, page, size));
  }

  @GetMapping("/{id}")
  public ApiResponse<ProductView> get(@PathVariable long id) {
    return ApiResponse.ok(
        repo.findById(id)
            .orElseThrow(() -> new NotFoundException("Không tìm thấy sản phẩm id=" + id)));
  }

  @GetMapping("/{id}/details")
  public ApiResponse<List<ProductDetailView>> details(@PathVariable long id) {
    return ApiResponse.ok(repo.findDetails(id));
  }
}
