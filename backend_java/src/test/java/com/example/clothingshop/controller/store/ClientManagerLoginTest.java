package com.example.clothingshop.controller.store;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.example.clothingshop.repository.admin.AdminRepository;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class ClientManagerLoginTest {
  @Autowired MockMvc mvc;
  @MockBean AdminRepository db;

  void manager(int status) {
    when(db.rows(anyString(), any(Object[].class))).thenReturn(List.of(Map.of(
        "ID", 7L, "USERNAME", "TestAdmin", "PASSWORD_HASH", new BCryptPasswordEncoder(12).encode("AdminTest@12345"),
        "STATUS", status, "ROLE_CODE", "ADMIN")));
    when(db.one(anyString(), any(Object[].class))).thenReturn(Map.of("AUTH_VERSION", 3L));
  }

  @Test void existingSecurityLoginGrantsAdminAndClearsCustomerIdentity() throws Exception {
    manager(1);
    var session = new MockHttpSession();
    session.setAttribute("customer", 42L);
    session.setAttribute("customer-access-version", 0L);
    mvc.perform(post("/login").with(csrf().asHeader()).session(session)
        .param("username", "TestAdmin").param("password", "AdminTest@12345"))
        .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/admin"));
    var context = (SecurityContext) session.getAttribute("SPRING_SECURITY_CONTEXT");
    assertNotNull(context);
    assertTrue(context.getAuthentication().getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")));
    assertEquals(3L, session.getAttribute("manager-access-version"));
    assertNull(session.getAttribute("customer"));
  }

  @Test void wrongPasswordNeverGrantsAdmin() throws Exception {
    manager(1);
    var session = new MockHttpSession();
    mvc.perform(post("/login").with(csrf().asHeader()).session(session)
        .param("username", "TestAdmin").param("password", "wrong"))
        .andExpect(redirectedUrl("/login?error"));
    assertNull(session.getAttribute("SPRING_SECURITY_CONTEXT"));
  }

  @Test void disabledManagerCannotLogin() throws Exception {
    manager(0);
    mvc.perform(post("/login").with(csrf().asHeader())
        .param("username", "TestAdmin").param("password", "AdminTest@12345"))
        .andExpect(redirectedUrl("/login?error"));
  }

  @Test void csrfStillRequired() throws Exception {
    mvc.perform(post("/login").param("username", "TestAdmin").param("password", "AdminTest@12345"))
        .andExpect(status().isForbidden());
    verifyNoInteractions(db);
  }
}
