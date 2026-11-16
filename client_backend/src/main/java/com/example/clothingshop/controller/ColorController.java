package com.example.clothingshop.controller;

import com.example.clothingshop.common.ApiResponse;
import com.example.clothingshop.common.PageResult;
import com.example.clothingshop.dto.request.ColorRequest;
import com.example.clothingshop.service.ColorService;
import com.example.clothingshop.view.ColorView;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/colors")
public class ColorController {

    private final ColorService service;

    public ColorController(ColorService service) { this.service = service; }

    @GetMapping
    public ApiResponse<PageResult<ColorView>> search(@RequestParam(required = false) String keyword,
                                                     @RequestParam(defaultValue = "1") int page,
                                                     @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.ok(service.search(keyword, page, size));
    }

    @GetMapping("/{id}")
    public ApiResponse<ColorView> get(@PathVariable long id) { return ApiResponse.ok(service.get(id)); }

    @PostMapping
    public ApiResponse<ColorView> create(@Valid @RequestBody ColorRequest r) {
        return ApiResponse.ok("Tạo thành công", service.create(r));
    }

    @PutMapping("/{id}")
    public ApiResponse<ColorView> update(@PathVariable long id, @Valid @RequestBody ColorRequest r) {
        return ApiResponse.ok("Cập nhật thành công", service.update(id, r));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable long id) {
        service.delete(id);
        return ApiResponse.ok("Đã xóa", null);
    }
}
