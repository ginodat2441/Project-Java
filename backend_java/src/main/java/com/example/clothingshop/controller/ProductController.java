package com.example.clothingshop.controller;

import com.example.clothingshop.common.ApiResponse;
import com.example.clothingshop.common.PageResult;
import com.example.clothingshop.service.ProductService;
import com.example.clothingshop.view.ProductDetailView;
import com.example.clothingshop.view.ProductView;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService service;

    public ProductController(ProductService service) { this.service = service; }

    @GetMapping
    public ApiResponse<PageResult<ProductView>> search(@RequestParam(required = false) String keyword,
                                                       @RequestParam(required = false) Long brandId,
                                                       @RequestParam(required = false) Integer gender,
                                                       @RequestParam(defaultValue = "1") int page,
                                                       @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.ok(service.search(keyword, brandId, gender, page, size));
    }

    @GetMapping("/{id}")
    public ApiResponse<ProductView> get(@PathVariable long id) {
        return ApiResponse.ok(service.get(id));
    }

    @GetMapping("/{id}/details")
    public ApiResponse<List<ProductDetailView>> details(@PathVariable long id) {
        return ApiResponse.ok(service.details(id));
    }
}
