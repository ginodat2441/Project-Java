package com.example.clothingshop.controller;

import com.example.clothingshop.common.ApiResponse;
import com.example.clothingshop.dto.request.LoginRequest;
import com.example.clothingshop.dto.request.BillStatusRequest;
import com.example.clothingshop.dto.request.ManagerCreateRequest;
import com.example.clothingshop.dto.request.OwnerBootstrapRequest;
import com.example.clothingshop.dto.request.PaymentStatusRequest;
import com.example.clothingshop.service.AuthenticationService;
import com.example.clothingshop.service.BillService;
import com.example.clothingshop.service.ManagerService;
import com.example.clothingshop.view.AdminAuthView;
import com.example.clothingshop.view.BillHistoryView;
import com.example.clothingshop.view.BillLineView;
import com.example.clothingshop.view.BillView;
import com.example.clothingshop.view.ManagerView;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AuthenticationService authenticationService;
    private final BillService billService;
    private final ManagerService managerService;

    public AdminController(AuthenticationService authenticationService, BillService billService,
                           ManagerService managerService) {
        this.authenticationService = authenticationService;
        this.billService = billService;
        this.managerService = managerService;
    }

    @PostMapping("/auth/login")
    public ApiResponse<AdminAuthView> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.ok("Đăng nhập quản trị thành công", authenticationService.loginAdmin(request));
    }

    @PostMapping("/auth/bootstrap")
    public ApiResponse<ManagerView> bootstrapOwner(@Valid @RequestBody OwnerBootstrapRequest request) {
        return ApiResponse.ok("Đã khởi tạo tài khoản chủ cửa hàng",
                managerService.bootstrapOwner(request));
    }

    @GetMapping("/bills")
    public ApiResponse<List<BillView>> bills() {
        return ApiResponse.ok(billService.allForStaff());
    }

    @GetMapping("/bills/{id}")
    public ApiResponse<BillView> bill(@PathVariable long id) {
        return ApiResponse.ok(billService.getForStaff(id));
    }

    @GetMapping("/bills/{id}/lines")
    public ApiResponse<List<BillLineView>> billLines(@PathVariable long id) {
        return ApiResponse.ok(billService.linesForStaff(id));
    }

    @GetMapping("/bills/{id}/history")
    public ApiResponse<List<BillHistoryView>> billHistory(@PathVariable long id) {
        return ApiResponse.ok(billService.historyForStaff(id));
    }

    @PatchMapping("/bills/{id}/status")
    public ApiResponse<BillView> updateBillStatus(@AuthenticationPrincipal Jwt jwt,
                                                  @PathVariable long id,
                                                  @Valid @RequestBody BillStatusRequest request) {
        Number managerId = jwt.getClaim("managerId");
        boolean owner = "ADMIN".equals(jwt.getClaimAsString("role"));
        return ApiResponse.ok("Đã cập nhật trạng thái đơn hàng",
                billService.updateManagedStatus(id, request.status(), request.note(),
                        managerId.longValue(), owner));
    }

    @PatchMapping("/bills/{id}/payment-status")
    public ApiResponse<BillView> updatePaymentStatus(@AuthenticationPrincipal Jwt jwt,
                                                     @PathVariable long id,
                                                     @Valid @RequestBody PaymentStatusRequest request) {
        Number managerId = jwt.getClaim("managerId");
        return ApiResponse.ok("Đã cập nhật trạng thái thanh toán",
                billService.updateCashPaymentStatus(id, request.paid(), managerId.longValue()));
    }

    @GetMapping("/managers")
    public ApiResponse<List<ManagerView>> managers() {
        return ApiResponse.ok(managerService.list());
    }

    @PostMapping("/managers")
    public ApiResponse<ManagerView> createStaff(
            @Valid @RequestBody ManagerCreateRequest request) {
        return ApiResponse.ok("Đã tạo tài khoản nhân viên",
                managerService.createStaff(request));
    }
}
