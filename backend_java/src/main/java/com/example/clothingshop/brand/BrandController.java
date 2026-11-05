package com.example.clothingshop.brand;

import com.example.clothingshop.common.ApiResponse;
import com.example.clothingshop.common.PageResult;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/brands")
public class BrandController {

    private final BrandService service;

    public BrandController(BrandService service) { this.service = service; }

    @GetMapping
    public ApiResponse<PageResult<Brand>> search(@RequestParam(required = false) String keyword,
                                                 @RequestParam(defaultValue = "1") int page,
                                                 @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.ok(service.search(keyword, page, size));
    }

    @GetMapping("/{id}")
    public ApiResponse<Brand> get(@PathVariable long id) { return ApiResponse.ok(service.get(id)); }

    @PostMapping
    public ApiResponse<Brand> create(@Valid @RequestBody BrandRequest r) {
        return ApiResponse.ok("Tạo thành công", service.create(r));
    }

    @PutMapping("/{id}")
    public ApiResponse<Brand> update(@PathVariable long id, @Valid @RequestBody BrandRequest r) {
        return ApiResponse.ok("Cập nhật thành công", service.update(id, r));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable long id) {
        service.delete(id);
        return ApiResponse.ok("Đã xóa", null);
    }
}
