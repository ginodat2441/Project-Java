package com.example.clothingshop.controller;

import com.example.clothingshop.common.ApiResponse;
import com.example.clothingshop.common.PageResult;
import com.example.clothingshop.dto.request.MaterialRequest;
import com.example.clothingshop.service.MaterialService;
import com.example.clothingshop.view.MaterialView;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/materials")
public class MaterialController {

    private final MaterialService service;

    public MaterialController(MaterialService service) { this.service = service; }

    @GetMapping
    public ApiResponse<PageResult<MaterialView>> search(@RequestParam(required = false) String keyword,
                                                        @RequestParam(defaultValue = "1") int page,
                                                        @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.ok(service.search(keyword, page, size));
    }

    @GetMapping("/{id}")
    public ApiResponse<MaterialView> get(@PathVariable long id) { return ApiResponse.ok(service.get(id)); }

    @PostMapping
    public ApiResponse<MaterialView> create(@Valid @RequestBody MaterialRequest r) {
        return ApiResponse.ok("Tạo thành công", service.create(r));
    }

    @PutMapping("/{id}")
    public ApiResponse<MaterialView> update(@PathVariable long id, @Valid @RequestBody MaterialRequest r) {
        return ApiResponse.ok("Cập nhật thành công", service.update(id, r));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable long id) {
        service.delete(id);
        return ApiResponse.ok("Đã xóa", null);
    }
}
