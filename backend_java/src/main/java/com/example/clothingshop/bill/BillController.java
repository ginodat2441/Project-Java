package com.example.clothingshop.bill;

import com.example.clothingshop.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import com.example.clothingshop.admin.AdminService;
import org.springframework.security.core.Authentication;

@RestController
@RequestMapping("/api/bills")
public class BillController {

    private final BillService service;
    private final AdminService admin;

    public BillController(BillService service,AdminService admin) { this.service = service;this.admin=admin; }

    @PostMapping
    public ApiResponse<BillView> create(@Valid @RequestBody BillRequest req) {
        return ApiResponse.ok("Đặt hàng thành công", service.create(req));
    }

    @GetMapping("/{id}")
    public ApiResponse<BillView> get(@PathVariable long id) { return ApiResponse.ok(service.get(id)); }

    @GetMapping("/{id}/lines")
    public ApiResponse<List<BillLine>> lines(@PathVariable long id) { return ApiResponse.ok(service.lines(id)); }

    @GetMapping("/customer/{customerId}")
    public ApiResponse<List<BillView>> byCustomer(@PathVariable long customerId) {
        return ApiResponse.ok(service.byCustomer(customerId));
    }

    /** PATCH /api/bills/5/status?status=1&managerId=1&note=... */
    @PatchMapping("/{id}/status")
    public ApiResponse<BillView> changeStatus(@PathVariable long id, @RequestParam int status,
                                              @RequestParam(required = false) Long managerId,
                                              @RequestParam(required = false) String note,Authentication auth) {
        return ApiResponse.ok(service.changeStatus(id, status, admin.actor(auth.getName()), note));
    }
}
