package com.example.clothingshop.controller;

import com.example.clothingshop.common.ApiResponse;
import com.example.clothingshop.service.DeliveryDirectory;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/delivery-address")
public class DeliveryDirectoryController {
    private final DeliveryDirectory directory;

    public DeliveryDirectoryController(DeliveryDirectory directory) {
        this.directory = directory;
    }

    @GetMapping("/provinces")
    public ApiResponse<List<DeliveryDirectory.Province>> provinces() {
        return ApiResponse.ok(directory.provinces());
    }

    @GetMapping("/wards")
    public ApiResponse<List<String>> wards(@RequestParam String province) {
        return ApiResponse.ok(directory.wards(province));
    }
}
