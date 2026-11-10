package com.example.clothingshop.bootstrap.admin;

import com.example.clothingshop.repository.admin.AdminRepository;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import org.springframework.boot.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

@Component
public class FirstAdmin implements ApplicationRunner {
  private final AdminRepository db;
  private final PasswordEncoder encoder;
  private final TransactionTemplate tx;

  public FirstAdmin(AdminRepository db, PasswordEncoder encoder, TransactionTemplate tx) {
    this.db = db;
    this.encoder = encoder;
    this.tx = tx;
  }

  interface Input {
    String line(String prompt);

    char[] password(String prompt);

    void message(String message);
  }

  @Override
  public void run(ApplicationArguments args) {
    if (!args.containsOption("create-admin")) return;
    var console = System.console();
    if (console == null)
      throw new IllegalStateException(
          "Run .\\shop.cmd -Mode CreateAdmin in an interactive terminal.");
    runInteractive(
        new Input() {
          public String line(String prompt) {
            return console.readLine("%s", prompt);
          }

          public char[] password(String prompt) {
            return console.readPassword("%s", prompt);
          }

          public void message(String message) {
            console.printf("%s%n", message);
          }
        });
  }

  // ASCII messages remain readable in legacy Windows console encodings.
  void runInteractive(Input input) {
    if (adminExists()) {
      input.message("Da co admin. Khong the khoi tao lai.");
      return;
    }
    String username;
    while (true) {
      String value = input.line("Admin username (3-100: A-Z, a-z, 0-9, _, ., -): ");
      if (value == null) {
        cancelled(input);
        return;
      }
      username = value.strip();
      if (username.matches("[A-Za-z0-9_.-]{3,100}")) break;
      input.message("Ten dang nhap khong hop le: dung 3-100 ky tu chu, so, _, . hoac -.");
    }
    String email;
    while (true) {
      String value = input.line("Email: ");
      if (value == null) {
        cancelled(input);
        return;
      }
      email = value.strip();
      if (email.length() <= 255 && email.matches("[^@\\s]+@[^@\\s]+\\.[^@\\s]+")) break;
      input.message("Email khong hop le. Vi du: ten@example.com. Vui long nhap lai.");
    }
    while (true) {
      char[] password = input.password("Password (12-72 characters; max 72 UTF-8 bytes): ");
      if (password == null) {
        cancelled(input);
        return;
      }
      try {
        if (password.length < 12 || password.length > 72) {
          input.message("Mat khau phai co tu 12 den 72 ky tu. Vui long nhap lai.");
          continue;
        }
        if (new String(password).getBytes(StandardCharsets.UTF_8).length > 72) {
          input.message("Mat khau vuot 72 byte UTF-8 (BCrypt). Hay giam ky tu co dau/emoji.");
          continue;
        }
        char[] confirmation = input.password("Confirm password: ");
        if (confirmation == null) {
          cancelled(input);
          return;
        }
        boolean matches;
        try {
          matches = Arrays.equals(password, confirmation);
        } finally {
          Arrays.fill(confirmation, '\0');
        }
        if (!matches) {
          input.message("Hai mat khau khong khop. Vui long nhap lai.");
          continue;
        }
        String savedUsername = username, savedEmail = email;
        try {
          tx.executeWithoutResult(
              s -> {
                db.update("LOCK TABLE MANAGER IN EXCLUSIVE MODE");
                if (adminExists())
                  throw new IllegalStateException("Da co admin. Khong the khoi tao lai.");
                long role =
                    AdminRepository.number(
                        db.one(
                            "SELECT ID FROM MANAGER_ROLE WHERE ROLE_CODE='ADMIN' AND"
                                + " DELETE_FLAG=0"),
                        "ID");
                long id =
                    db.insert(
                        "INSERT INTO"
                            + " MANAGER(MANAGER_ROLE_ID,USERNAME,PASSWORD_HASH,FULL_NAME,EMAIL)"
                            + " VALUES(:r,:u,:p,:u,:e)",
                        "r",
                        role,
                        "u",
                        savedUsername,
                        "p",
                        encoder.encode(new String(password)),
                        "e",
                        savedEmail);
                db.audit(id, "BOOTSTRAP", "MANAGER", id);
              });
          input.message("Da tao admin thanh cong. Dang nhap tai http://localhost:8080/login");
        } catch (IllegalStateException e) {
          input.message(e.getMessage());
        }
        return;
      } finally {
        Arrays.fill(password, '\0');
      }
    }
  }

  private boolean adminExists() {
    return AdminRepository.number(
            db.one(
                "SELECT COUNT(*) N FROM MANAGER m JOIN MANAGER_ROLE r ON r.ID=m.MANAGER_ROLE_ID"
                    + " WHERE r.ROLE_CODE='ADMIN'"),
            "N")
        > 0;
  }

  private void cancelled(Input input) {
    input.message("Da huy. Chua tao tai khoan.");
  }
}
