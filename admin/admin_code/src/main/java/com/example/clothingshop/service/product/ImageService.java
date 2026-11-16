package com.example.clothingshop.service.product;

import static com.example.clothingshop.repository.admin.AdminRepository.number;

import com.example.clothingshop.common.BusinessException;
import com.example.clothingshop.repository.admin.AdminRepository;
import com.example.clothingshop.repository.product.ProductOperationsRepository;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ImageService {
  private final ProductOperationsRepository queries;
  final AdminRepository db;
  final Path root;

  public ImageService(
      AdminRepository db,
      @Value("${shop.upload-dir}") String root,
      ProductOperationsRepository queries) {
    this.queries = queries;
    this.db = db;
    this.root = Path.of(root).toAbsolutePath().normalize();
  }

  public Path uploadRoot() {
    return root;
  }

  @Transactional
  public long upload(long product, Long color, MultipartFile file, long actor) throws IOException {
    queries.findProduct("i", product);
    if (color != null) queries.findColor("i", color);
    if (file.isEmpty() || file.getSize() > 5 * 1024 * 1024)
      throw new BusinessException("Mỗi ảnh phải từ 1 byte đến 5 MB");
    byte[] bytes = file.getBytes();
    String ext = extension(bytes);
    try (var stream =
        javax.imageio.ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
      var readers = javax.imageio.ImageIO.getImageReaders(stream);
      if (!readers.hasNext()) throw new BusinessException("Không thể đọc nội dung ảnh");
      var reader = readers.next();
      try {
        reader.setInput(stream);
        int width = reader.getWidth(0), height = reader.getHeight(0);
        if (width > 12000 || height > 12000 || (long) width * height > 40000000)
          throw new BusinessException("Ảnh quá lớn, tối đa 40 megapixel");
        if (reader.read(0) == null) throw new BusinessException("Ảnh không hợp lệ");
      } finally {
        reader.dispose();
      }
    } catch (javax.imageio.IIOException e) {
      throw new BusinessException("Nội dung ảnh bị lỗi");
    }
    Files.createDirectories(root);
    String name = UUID.randomUUID() + "." + ext;
    Path path = root.resolve(name);
    Files.write(path, bytes, StandardOpenOption.CREATE_NEW);
    org.springframework.transaction.support.TransactionSynchronizationManager
        .registerSynchronization(
            new org.springframework.transaction.support.TransactionSynchronization() {
              @Override
              public void afterCompletion(int status) {
                if (status != STATUS_COMMITTED)
                  try {
                    Files.deleteIfExists(path);
                  } catch (IOException ignored) {
                  }
              }
            });
    long id = queries.insertProductImage("p", product, "c", color, "u", "/uploads/" + name);
    db.audit(actor, "UPLOAD", "PRODUCT_IMAGE", id);
    return id;
  }

  static String extension(byte[] b) {
    if (b.length >= 3 && (b[0] & 255) == 255 && (b[1] & 255) == 216 && (b[2] & 255) == 255)
      return "jpg";
    if (b.length >= 8
        && Arrays.equals(Arrays.copyOf(b, 8), new byte[] {(byte) 137, 80, 78, 71, 13, 10, 26, 10}))
      return "png";
    if (b.length >= 20
        && new String(b, 0, 4, java.nio.charset.StandardCharsets.US_ASCII).equals("RIFF")
        && new String(b, 8, 4, java.nio.charset.StandardCharsets.US_ASCII).equals("WEBP")
        && Set.of("VP8 ", "VP8L", "VP8X")
            .contains(new String(b, 12, 4, java.nio.charset.StandardCharsets.US_ASCII))) {
      long size =
          ((long) b[4] & 255)
              | (((long) b[5] & 255) << 8)
              | (((long) b[6] & 255) << 16)
              | (((long) b[7] & 255) << 24);
      if (size + 8 == b.length) return "webp";
    }
    throw new BusinessException("Chỉ chấp nhận nội dung JPEG, PNG hoặc WebP hợp lệ");
  }

  @Transactional
  public void edit(long product, long id, Long color, int order, boolean cover, long actor) {
    queries.findProduct2("p", product);
    var image = queries.findProductImage("i", id, "p", product);
    if (color != null) queries.findColor("i", color);
    if (cover) {
      queries.updateProductImage("p", product);
      queries.updateProduct("u", image.get("IMAGE_URL"), "p", product);
    }
    queries.updateProductImage2("c", color, "o", order, "cover", cover ? 1 : 0, "i", id);
    db.audit(actor, "EDIT", "PRODUCT_IMAGE", id);
  }

  @Transactional
  public void remove(long product, long id, long actor) {
    var p = queries.findProduct3("p", product);
    var image = queries.findProductImage("i", id, "p", product);
    String url = image.get("IMAGE_URL").toString();
    if (url.equals(p.get("THUMBNAIL_URL"))) {
      if (number(p, "STATUS") == 1)
        throw new BusinessException("Chọn ảnh đại diện khác hoặc ẩn sản phẩm trước khi gỡ");
      queries.updateProduct2("p", product);
    }
    queries.deleteProductImage("i", id);
    db.audit(actor, "REMOVE", "PRODUCT_IMAGE", id);
    // Retain physical files for safe recovery; cleanup only unreferenced server-generated files.
  }

  public Path path(String name) {
    if (!name.matches("[0-9a-f-]{36}\\.(jpg|png|webp)"))
      throw new BusinessException("Tên ảnh không hợp lệ");
    Path path = root.resolve(name).normalize();
    if (!path.startsWith(root)) throw new BusinessException("Đường dẫn không hợp lệ");
    return path;
  }
}
