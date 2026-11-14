package com.example.clothingshop.service;

import com.example.clothingshop.common.BusinessException;
import com.example.clothingshop.dto.request.ManagerCreateRequest;
import com.example.clothingshop.dto.request.OwnerBootstrapRequest;
import com.example.clothingshop.repositories.ManagerRepository;
import com.example.clothingshop.view.ManagerView;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import java.util.Locale;

import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.FORBIDDEN;

@Service
public class ManagerService {

    private final ManagerRepository managers;
    private final PasswordEncoder passwordEncoder;
    private final String bootstrapToken;

    public ManagerService(ManagerRepository managers, PasswordEncoder passwordEncoder,
                          @Value("${app.security.admin-bootstrap-token:}") String bootstrapToken) {
        this.managers = managers;
        this.passwordEncoder = passwordEncoder;
        this.bootstrapToken = bootstrapToken;
    }

    @Transactional
    public ManagerView bootstrapOwner(OwnerBootstrapRequest request) {
        if (bootstrapToken.isBlank() || !MessageDigest.isEqual(
                bootstrapToken.getBytes(StandardCharsets.UTF_8),
                request.bootstrapToken().getBytes(StandardCharsets.UTF_8))) {
            throw new ResponseStatusException(FORBIDDEN, "Mã khởi tạo chủ cửa hàng không hợp lệ");
        }

        long adminRoleId = managers.lockAdminRole();
        long ownerCount = managers.countOwners();
        if (ownerCount != 0) {
            throw new ResponseStatusException(CONFLICT,
                    "Đã tồn tại tài khoản chủ cửa hàng chưa xóa trong bảng MANAGER");
        }
        ManagerCreateRequest owner = new ManagerCreateRequest(request.username(), request.email(),
                request.password(), request.fullName(), null);
        return create(owner, adminRoleId, "ADMIN");
    }

    @Transactional
    public ManagerView createStaff(ManagerCreateRequest request) {
        return create(request, managers.roleId("STAFF"), "STAFF");
    }

    public List<ManagerView> list() {
        return managers.findAll();
    }

    private ManagerView create(ManagerCreateRequest request, long roleId, String roleCode) {
        String username = request.username().trim();
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        ManagerRepository.ManagerAccountLookup lookup = managers.lookupAccount(username, email);
        if (lookup.usernameCount() != null && lookup.usernameCount() > 0) {
            throw new BusinessException("Tên đăng nhập quản lý đã được sử dụng");
        }
        if (lookup.emailCount() != null && lookup.emailCount() > 0) {
            throw new BusinessException("Email quản lý đã được sử dụng");
        }

        long id = managers.insert(roleId, username, email, passwordEncoder.encode(request.password()),
                request.fullName().trim(),
                request.phone() == null || request.phone().isBlank() ? null : request.phone().trim());
        return managers.findViewById(id)
                .filter(manager -> manager.roleCode().equals(roleCode))
                .orElseThrow(() -> new IllegalStateException("Không tải lại được tài khoản quản lý vừa tạo"));
    }
}
