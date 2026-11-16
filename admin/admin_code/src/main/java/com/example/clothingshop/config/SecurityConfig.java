package com.example.clothingshop.config;

import com.example.clothingshop.repository.admin.AdminRepository;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.context.annotation.*;
import org.springframework.security.authentication.*;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.*;
import org.springframework.web.filter.OncePerRequestFilter;

@Configuration
public class SecurityConfig {
  @Bean
  PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder(12);
  }

  @Bean
  UserDetailsService managers(AdminRepository db) {
    return username -> {
      var found =
          db.rows(
              "SELECT m.ID,m.USERNAME,m.PASSWORD_HASH,m.STATUS,r.ROLE_CODE FROM MANAGER m JOIN"
                  + " MANAGER_ROLE r ON r.ID=m.MANAGER_ROLE_ID WHERE m.USERNAME=:u AND"
                  + " m.DELETE_FLAG=0 AND r.DELETE_FLAG=0",
              "u",
              username);
      if (found.isEmpty()) throw new UsernameNotFoundException("Sai thông tin đăng nhập");
      var m = found.get(0);
      String role = m.get("ROLE_CODE").toString();
      if (!Set.of("ADMIN", "STAFF").contains(role))
        throw new UsernameNotFoundException("Quyền không hợp lệ");
      return User.withUsername(m.get("USERNAME").toString())
          .password(m.get("PASSWORD_HASH").toString())
          .roles(role)
          .disabled(AdminRepository.number(m, "STATUS") != 1)
          .build();
    };
  }

  @Bean
  @org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication
  SecurityFilterChain security(HttpSecurity http, UserDetailsService users, AdminRepository db)
      throws Exception {
    var attempts = new ConcurrentHashMap<String, Attempt>();
    http.authorizeHttpRequests(
            a ->
                a.requestMatchers("/api/dev/**")
                    .denyAll()
                    .requestMatchers(
                        "/login",
                        "/login.html",
                        "/forgot-password",
                        "/forgot-password.html",
                        "/reset-password",
                        "/reset-password.html",
                        "/api/auth/**",
                        "/auth/js/**",
                        "/shared/**",
                        "/admin/css/**",
                        "/admin/js/**",
                        "/api/csrf",
                        "/error")
                    .permitAll()
                    .requestMatchers(
                        "/api/admin/inventory/documents/*/approve",
                        "/api/admin/inventory/documents/*/reverse")
                    .hasRole("ADMIN")
                    .requestMatchers("/api/admin/opening-cost/**", "/api/admin/sku-cost/**")
                    .hasRole("ADMIN")
                    .requestMatchers("/api/admin/shipping/**")
                    .hasRole("ADMIN")
                    .requestMatchers(
                        org.springframework.http.HttpMethod.POST,
                        "/api/admin/orders/*/refunds",
                        "/api/admin/orders/*/refunds/**")
                    .hasRole("ADMIN")
                    .requestMatchers(
                        org.springframework.http.HttpMethod.PUT, "/api/admin/orders/*/refunds/**")
                    .hasRole("ADMIN")
                    .requestMatchers(
                        "/api/admin/managers/**",
                        "/api/admin/roles",
                        "/api/admin/customers/**",
                        "/api/admin/audit")
                    .hasRole("ADMIN")
                    .requestMatchers(
                        "/admin", "/admin/**", "/api/admin/**", "/api/stats/**", "/api/bills/**")
                    .hasAnyRole("ADMIN", "STAFF")
                    .requestMatchers(
                        org.springframework.http.HttpMethod.GET,
                        "/api/products/**",
                        "/api/brands/**",
                        "/api/catalog/**",
                        "/api/shipping/**",
                        "/uploads/**",
                        "/shop/**",
                        "/store/**",
                        "/")
                    .permitAll()
                    .requestMatchers("/api/checkout", "/api/customer/**", "/api/catalog/quote")
                    .permitAll()
                    .anyRequest()
                    .hasAnyRole("ADMIN", "STAFF"))
        .csrf(
            c ->
                c.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                    .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler()))
        .formLogin(
            f ->
                f.loginPage("/login")
                    .loginProcessingUrl("/login")
                    .defaultSuccessUrl("/admin", true)
                    .successHandler(
                        (req, res, auth) -> {
                          attempts.remove(req.getRemoteAddr());
                          req.getSession()
                              .setAttribute(
                                  "manager-access-version",
                                  AdminRepository.number(
                                      db.one(
                                          "SELECT AUTH_VERSION FROM MANAGER WHERE USERNAME=:u",
                                          "u",
                                          auth.getName()),
                                      "AUTH_VERSION"));
                          db.update(
                              "UPDATE MANAGER SET LAST_LOGIN_AT=SYSTIMESTAMP WHERE USERNAME=:u",
                              "u",
                              auth.getName());
                          res.sendRedirect("/admin");
                        })
                    .failureHandler(
                        (req, res, e) -> {
                          attempts.compute(
                              req.getRemoteAddr(),
                              (k, v) ->
                                  v == null || v.until < System.currentTimeMillis()
                                      ? new Attempt(1, System.currentTimeMillis() + 600000)
                                      : new Attempt(v.count + 1, v.until));
                          res.sendRedirect("/login?error");
                        })
                    .permitAll())
        .logout(
            l ->
                l.logoutSuccessUrl("/login?logout")
                    .invalidateHttpSession(true)
                    .deleteCookies("JSESSIONID"))
        .exceptionHandling(
            e ->
                e.authenticationEntryPoint(
                    (req, res, x) -> {
                      if (req.getRequestURI().startsWith("/api/")) res.sendError(401);
                      else res.sendRedirect("/login");
                    }))
        .headers(
            h ->
                h.contentSecurityPolicy(
                    c ->
                        c.policyDirectives(
                            "default-src 'self'; img-src 'self' data: blob: https:; style-src"
                                + " 'self' 'unsafe-inline'; script-src 'self'; font-src 'self'"
                                + " data:; object-src 'none'; base-uri 'self'; frame-ancestors"
                                + " 'none'")));
    http.addFilterBefore(
        new OncePerRequestFilter() {
          @Override
          protected void doFilterInternal(
              HttpServletRequest req, HttpServletResponse res, FilterChain chain)
              throws ServletException, IOException {
            var attempt = attempts.get(req.getRemoteAddr());
            if (req.getMethod().equals("POST")
                && req.getServletPath().equals("/login")
                && attempt != null
                && attempt.until > System.currentTimeMillis()
                && attempt.count >= 5) {
              long seconds = Math.max(1, (attempt.until - System.currentTimeMillis() + 999) / 1000);
              res.setHeader("Retry-After", Long.toString(seconds));
              res.sendRedirect("/login?lockedUntil=" + attempt.until);
              return;
            }
            if (attempts.size() > 10000)
              attempts.entrySet().removeIf(e -> e.getValue().until < System.currentTimeMillis());
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && !auth.getName().equals("anonymousUser")) {
              try {
                var fresh = users.loadUserByUsername(auth.getName());
                Long version =
                    AdminRepository.number(
                        db.one(
                            "SELECT AUTH_VERSION FROM MANAGER WHERE USERNAME=:u",
                            "u",
                            auth.getName()),
                        "AUTH_VERSION");
                Object sessionVersion =
                    req.getSession(false) == null
                        ? null
                        : req.getSession(false).getAttribute("manager-access-version");
                if (!fresh.isEnabled()
                    || !version.equals(sessionVersion)
                    || !new HashSet<>(fresh.getAuthorities())
                        .equals(new HashSet<>(auth.getAuthorities())))
                  throw new DisabledException("Quyền đã thay đổi");
              } catch (org.springframework.security.core.AuthenticationException ex) {
                SecurityContextHolder.clearContext();
                if (req.getSession(false) != null) req.getSession(false).invalidate();
                if (req.getRequestURI().startsWith("/api/")) res.sendError(401);
                else res.sendRedirect("/login");
                return;
              }
            }
            chain.doFilter(req, res);
          }
        },
        UsernamePasswordAuthenticationFilter.class);
    return http.build();
  }

  private record Attempt(int count, long until) {}
}
