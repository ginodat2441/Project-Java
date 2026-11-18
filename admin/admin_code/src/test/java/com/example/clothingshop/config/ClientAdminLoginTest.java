package com.example.clothingshop.config;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.example.clothingshop.controller.auth.AdminSessionController;
import com.example.clothingshop.repository.admin.AdminRepository;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(classes=ClientAdminLoginTest.App.class)
@AutoConfigureMockMvc
class ClientAdminLoginTest {
  @SpringBootConfiguration
  @EnableAutoConfiguration(exclude=DataSourceAutoConfiguration.class)
  @Import({SecurityConfig.class, AdminSessionController.class})
  static class App {
    @Bean AdminRepository db() { return mock(AdminRepository.class); }
  }
  @Autowired MockMvc mvc;
  @Autowired AdminRepository db;
  @Autowired PasswordEncoder encoder;
  @BeforeEach void setup() {
    reset(db);
    when(db.rows(anyString(), any(Object[].class))).thenReturn(List.of(Map.of(
      "USERNAME","testadmin", "PASSWORD_HASH",encoder.encode("ValidPassword123!"), "STATUS",1, "ROLE_CODE","ADMIN")));
    when(db.one(anyString(), any(Object[].class))).thenReturn(Map.of("AUTH_VERSION",1L));
  }
  @Test void emailLoginCreatesRealSessionFromClientOrigin() throws Exception {
    var csrf=mvc.perform(get("/api/csrf").header("Origin","http://127.0.0.1:5500"))
      .andExpect(status().isOk()).andExpect(header().string("Access-Control-Allow-Credentials","true"))
      .andReturn().getResponse();
    var json=new com.fasterxml.jackson.databind.ObjectMapper().readTree(csrf.getContentAsString());
    var result=mvc.perform(post("/login").header("Origin","http://127.0.0.1:5500")
      .header("Accept","application/json").header(json.get("headerName").asText(),json.get("token").asText())
      .cookie(csrf.getCookies()).param("username","admin@example.test").param("password","ValidPassword123!"))
      .andExpect(status().isOk()).andExpect(jsonPath("$.redirect").value("/admin")).andReturn();
    var session=(org.springframework.mock.web.MockHttpSession)result.getRequest().getSession(false);
    Assertions.assertEquals(1L,session.getAttribute("manager-access-version"));
    mvc.perform(get("/api/admin/me").session(session)).andExpect(status().isOk())
      .andExpect(jsonPath("$.username").value("testadmin"));
    var freshCsrf=mvc.perform(get("/api/csrf").session(session)).andReturn().getResponse();
    var freshJson=new com.fasterxml.jackson.databind.ObjectMapper().readTree(freshCsrf.getContentAsString());
    mvc.perform(post("/logout").session(session).cookie(freshCsrf.getCookies())
      .header("Accept","application/json")
      .header(freshJson.get("headerName").asText(),freshJson.get("token").asText()))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.redirect").value("http://127.0.0.1:5500/client/index.html"))
      .andExpect(cookie().maxAge("JSESSIONID",0));
    Assertions.assertTrue(session.isInvalid());
    mvc.perform(get("/api/admin/me")).andExpect(status().isUnauthorized());
  }
  @Test void loginWithoutCsrfIsRejected() throws Exception {
    mvc.perform(post("/login").header("Accept","application/json").param("username","testadmin").param("password","ValidPassword123!"))
      .andExpect(status().isForbidden());
  }
  @Test void untrustedOriginCannotReadCsrf() throws Exception {
    mvc.perform(get("/api/csrf").header("Origin","https://untrusted.example")).andExpect(status().isForbidden());
  }
  @Test void emailLookupKeepsCanonicalUsernameAndRejectsLockedAccount() {
    var users=new SecurityConfig().managers(db);
    Assertions.assertEquals("testadmin",users.loadUserByUsername("admin@example.test").getUsername());
    when(db.rows(anyString(),any(Object[].class))).thenReturn(List.of(Map.of(
      "USERNAME","testadmin","PASSWORD_HASH","unused","STATUS",0,"ROLE_CODE","ADMIN")));
    Assertions.assertFalse(users.loadUserByUsername("admin@example.test").isEnabled());
  }
}
