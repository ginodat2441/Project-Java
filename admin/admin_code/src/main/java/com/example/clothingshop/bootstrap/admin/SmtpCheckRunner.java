package com.example.clothingshop.bootstrap.admin;

import jakarta.mail.AuthenticationFailedException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Component;

/** Authenticates the SMTP connection without sending an email or printing credentials. */
@Component
public class SmtpCheckRunner implements ApplicationRunner {
  private final ObjectProvider<JavaMailSender> mail;

  public SmtpCheckRunner(ObjectProvider<JavaMailSender> mail) {
    this.mail = mail;
  }

  record Result(boolean success, String code) {}

  Result check() {
    var candidate = mail.getIfAvailable();
    if (!(candidate instanceof JavaMailSenderImpl sender)
        || sender.getHost() == null
        || sender.getHost().isBlank()
        || sender.getUsername() == null
        || sender.getUsername().isBlank()
        || sender.getPassword() == null
        || sender.getPassword().isBlank()) return new Result(false, "SMTP_CHECK_NOT_CONFIGURED");
    try {
      sender.testConnection();
      return new Result(true, "SMTP_CHECK_OK");
    } catch (AuthenticationFailedException e) {
      return new Result(false, "SMTP_CHECK_AUTH_FAILED");
    } catch (Exception e) {
      return new Result(false, "SMTP_CHECK_CONNECTION_FAILED");
    }
  }

  @Override
  public void run(ApplicationArguments args) {
    if (!args.containsOption("smtp-check")) return;
    Result result = check();
    System.out.println(result.code());
    if (!result.success()) throw new IllegalStateException(result.code());
  }
}
