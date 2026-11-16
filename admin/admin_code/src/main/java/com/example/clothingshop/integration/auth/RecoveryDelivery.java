package com.example.clothingshop.integration.auth;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.*;
import java.util.*;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
public class RecoveryDelivery {
  private final ObjectProvider<JavaMailSender> mail;
  private final Path directory;
  private final String mode, from;

  public RecoveryDelivery(
      ObjectProvider<JavaMailSender> mail,
      @Value("${shop.private-dir:${user.home}/clothing-shop-private}") String root,
      @Value("${shop.reset.delivery:local}") String mode,
      @Value("${shop.reset.from:no-reply@localhost}") String from) {
    this.mail = mail;
    this.directory = Path.of(root).toAbsolutePath().normalize().resolve("password-reset");
    this.mode = mode;
    this.from = from;
  }

  public Path directory() {
    return directory;
  }

  public void deliver(String email, String url) throws IOException {
    String body =
        "Clothing Shop - Khoi phuc mat khau quan tri\n"
            + "Lien ket chi dung mot lan, het han sau 15 phut:\n"
            + url
            + "\nNeu ban khong yeu cau, hay bo qua thu nay.\n";
    if (mode.equals("smtp")) {
      var sender = mail.getIfAvailable();
      if (sender == null) throw new IllegalStateException("SMTP chưa cấu hình");
      var message = new SimpleMailMessage();
      message.setFrom(from);
      message.setTo(email);
      message.setSubject("Clothing Shop - Khoi phuc mat khau");
      message.setText(body);
      sender.send(message);
      return;
    }
    if (!mode.equals("local"))
      throw new IllegalStateException("Phương thức gửi khôi phục không hợp lệ");
    Files.createDirectories(directory);
    Path file = Files.createTempFile(directory, "reset-", ".txt");
    try {
      var acl = Files.getFileAttributeView(file, AclFileAttributeView.class);
      if (acl != null)
        acl.setAcl(
            List.of(
                AclEntry.newBuilder()
                    .setType(AclEntryType.ALLOW)
                    .setPrincipal(Files.getOwner(file))
                    .setPermissions(EnumSet.allOf(AclEntryPermission.class))
                    .build()));
      else Files.setPosixFilePermissions(file, PosixFilePermissions.fromString("rw-------"));
      Files.writeString(file, body);
    } catch (Exception e) {
      Files.deleteIfExists(file);
      throw e;
    }
  }
}
