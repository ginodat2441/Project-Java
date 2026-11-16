package com.example.clothingshop.bootstrap.product;

import com.example.clothingshop.repository.admin.AdminRepository;
import com.example.clothingshop.service.product.ImageService;
import java.io.IOException;
import java.nio.file.*;
import java.time.*;
import org.springframework.boot.*;
import org.springframework.stereotype.Component;

/** Explicit maintenance command: only server-generated, unreferenced files older than 24h. */
@Component
public class ImageCleanup implements ApplicationRunner {
  private final ImageService images;
  private final AdminRepository db;

  public ImageCleanup(ImageService images, AdminRepository db) {
    this.images = images;
    this.db = db;
  }

  @Override
  public void run(ApplicationArguments args) throws IOException {
    if (!args.containsOption("cleanup-images") || !Files.isDirectory(images.uploadRoot())) return;
    int deleted = 0;
    try (var paths = Files.list(images.uploadRoot())) {
      for (var path : paths.toList()) {
        String name = path.getFileName().toString();
        if (!name.matches("[0-9a-f-]{36}\\.(jpg|png|webp)")
            || !Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)
            || Files.getLastModifiedTime(path)
                .toInstant()
                .isAfter(Instant.now().minus(Duration.ofHours(24)))) continue;
        String url = "/uploads/" + name;
        long refs =
            AdminRepository.number(
                db.one(
                    "SELECT (SELECT COUNT(*) FROM PRODUCT_IMAGE WHERE IMAGE_URL=:u)+(SELECT"
                        + " COUNT(*) FROM PRODUCT WHERE THUMBNAIL_URL=:u)+(SELECT COUNT(*) FROM"
                        + " PRODUCT_DETAIL WHERE IMAGE_URL=:u) N FROM DUAL",
                    "u",
                    url),
                "N");
        if (refs == 0 && Files.deleteIfExists(path)) deleted++;
      }
    }
    System.out.println("Unreferenced images removed: " + deleted);
  }
}
