package com.example.clothingshop.service;

import com.example.clothingshop.common.BusinessException;
import com.example.clothingshop.entity.UserAccount;
import com.example.clothingshop.repositories.UserRepository;
import com.example.clothingshop.repositories.ManagerRepository;
import com.example.clothingshop.entity.ManagerAccount;
import com.example.clothingshop.dto.request.LoginRequest;
import com.example.clothingshop.dto.request.RegisterRequest;
import com.example.clothingshop.view.AuthView;
import com.example.clothingshop.view.AuthenticatedUserView;
import com.example.clothingshop.view.AdminAuthView;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Locale;

import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@Service
public class AuthenticationService {

    private static final long TOKEN_LIFETIME_SECONDS = 43_200;

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final JwtEncoder jwtEncoder;
    private final ManagerRepository managers;

    public AuthenticationService(UserRepository users, PasswordEncoder passwordEncoder,
                                 JwtEncoder jwtEncoder, ManagerRepository managers) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.jwtEncoder = jwtEncoder;
        this.managers = managers;
    }

    @Transactional
    public AuthView register(RegisterRequest request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        String username = request.username().trim();
        if (users.emailExists(email)) {
            throw new BusinessException("Email đã được sử dụng");
        }
        if (users.usernameExists(username)) {
            throw new BusinessException("Tên đăng nhập đã được sử dụng");
        }

        long userId = users.insertUser(users.customerRoleId(), username, email,
                passwordEncoder.encode(request.password()));
        long customerId = users.insertCustomer(userId, request.fullName().trim(),
                request.phone() == null || request.phone().isBlank() ? null : request.phone().trim());
        UserAccount user = users.findByCustomerId(customerId)
                .orElseThrow(() -> new IllegalStateException("Không tải lại được tài khoản vừa tạo"));
        return issueToken(user);
    }

    public AuthView login(LoginRequest request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        UserAccount user = users.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(UNAUTHORIZED, "Email hoặc mật khẩu không chính xác"));
        if (user.status() == null || user.status() != 1
                || !passwordEncoder.matches(request.password(), user.passwordHash())) {
            throw new ResponseStatusException(UNAUTHORIZED, "Email hoặc mật khẩu không chính xác");
        }
        return issueToken(user);
    }

    public AdminAuthView loginAdmin(LoginRequest request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        ManagerAccount manager = managers.findActiveByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(UNAUTHORIZED, "Email hoặc mật khẩu không chính xác"));
        if (!passwordEncoder.matches(request.password(), manager.passwordHash())) {
            throw new ResponseStatusException(UNAUTHORIZED, "Email hoặc mật khẩu không chính xác");
        }

        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("clothing-shop")
                .issuedAt(now)
                .expiresAt(now.plus(TOKEN_LIFETIME_SECONDS, ChronoUnit.SECONDS))
                .subject(manager.email())
                .claim("managerId", manager.id())
                .claim("role", manager.roleCode())
                .build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
        return new AdminAuthView("Bearer", token, TOKEN_LIFETIME_SECONDS, manager.id(),
                manager.username(), manager.fullName(), manager.email(), manager.roleCode());
    }

    public AuthenticatedUserView currentUser(long customerId) {
        return users.findByCustomerId(customerId).map(this::toView)
                .orElseThrow(() -> new ResponseStatusException(UNAUTHORIZED, "Tài khoản không còn hoạt động"));
    }

    private AuthView issueToken(UserAccount user) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("clothing-shop")
                .issuedAt(now)
                .expiresAt(now.plus(TOKEN_LIFETIME_SECONDS, ChronoUnit.SECONDS))
                .subject(user.email())
                .claim("customerId", user.customerId())
                .claim("userId", user.userId())
                .claim("role", "CUSTOMER")
                .build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
        return new AuthView("Bearer", token, TOKEN_LIFETIME_SECONDS, toView(user));
    }

    private AuthenticatedUserView toView(UserAccount user) {
        return new AuthenticatedUserView(user.userId(), user.customerId(), user.username(),
                user.email(), user.fullName(), user.phone());
    }
}
