package com.example.clothingshop.service.product;

import static com.example.clothingshop.dto.admin.AdminRequests.*;
import static com.example.clothingshop.repository.admin.AdminRepository.number;

import com.example.clothingshop.common.*;
import com.example.clothingshop.repository.admin.AdminOperationsRepository;
import com.example.clothingshop.repository.admin.AdminRepository;
import java.util.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductCommandService {
  private static String skuPart(String value) {
    String part =
        java.text.Normalizer.normalize(value, java.text.Normalizer.Form.NFD)
            .replaceAll("\\p{M}+", "")
            .replace('đ', 'd')
            .replace('Đ', 'D')
            .toUpperCase(Locale.ROOT)
            .replaceAll("[^A-Z0-9]+", "-")
            .replaceAll("^-|-$", "");
    return part.length() > 12 ? part.substring(0, 12) : part;
  }

  private final AdminRepository db;
  private final AdminOperationsRepository queries;
  private final PasswordEncoder encoder;

  public ProductCommandService(
      AdminRepository db, AdminOperationsRepository queries, PasswordEncoder encoder) {
    this.db = db;
    this.queries = queries;
    this.encoder = encoder;
  }

  public Map<String, Object> product(long id) {
    var product = queries.findProduct("i", id);
    product.put("variants", queries.listProductDetail("i", id));
    product.put("images", queries.listProductImage("i", id));
    return product;
  }

  @Transactional
  public long product(Long id, Product r, long actor) {
    // Consistent lookup lock order also applies when a product creates both values.
    long brandId = lookup("BRAND", r.brandId(), r.brandName(), actor);
    long materialId = lookup("MATERIAL", r.materialId(), r.materialName(), actor);
    String code =
        id == null
            ? nextProductCode()
            : queries.findProduct2("i", id).get("PRODUCT_CODE").toString();
    var pairs = new HashSet<String>();
    var ids = new HashSet<Long>();
    boolean selling = false;
    Map<Long, String> colorNames = new HashMap<>();
    for (var v : r.variants()) {
      if (v.id() == null && v.quantity() != 0)
        throw new BusinessException(
            "Biến thể mới bắt đầu tồn 0. Lập phiếu nhập kho sau khi tạo sản phẩm.");
      if (!pairs.add(v.colorId() + ":" + v.size().trim().toUpperCase(Locale.ROOT)))
        throw new BusinessException("Trùng cặp màu–size");
      if (v.id() != null && !ids.add(v.id())) throw new BusinessException("Biến thể bị lặp");
      selling |= v.status() == 1;
      colorNames.put(
          v.colorId(), queries.findColor2("i", v.colorId()).get("COLOR_NAME").toString());
    }
    String cover = r.thumbnailUrl();
    if (cover != null
        && !cover.isBlank()
        && !(cover.startsWith("/uploads/")
            || cover.startsWith("/store/assets/img/")
            || cover.startsWith("assets/img/")))
      throw new BusinessException("Chọn ảnh đã tải lên hoặc ảnh cửa hàng");
    if (cover != null
        && cover.startsWith("/uploads/")
        && queries.listProductImage2("u", cover, "i", id).isEmpty())
      throw new BusinessException("Ảnh không thuộc sản phẩm");
    if (r.status() == 1 && (!selling || cover == null || cover.isBlank()))
      throw new BusinessException(
          "Sản phẩm đang bán cần ảnh đại diện và ít nhất một biến thể đang bán");
    if (r.categoryId() != null) queries.findCategory2("i", r.categoryId());
    Object[] params = {
      "c",
      code,
      "n",
      r.name().trim(),
      "b",
      brandId,
      "m",
      materialId,
      "cat",
      r.categoryId(),
      "g",
      r.gender(),
      "d",
      r.description(),
      "t",
      cover,
      "s",
      r.status(),
      "i",
      id
    };
    if (id == null) id = queries.insertProduct(params);
    else queries.updateProduct(params);
    var existing = queries.listProductDetail2("i", id);
    for (var e : existing)
      if (!ids.contains(number(e, "ID")))
        throw new BusinessException(
            "Giữ biến thể cũ trong form; dùng trạng thái ngừng bán thay vì xóa");
    for (var v : r.variants()) {
      String sku =
          v.id() == null ? nextSku(code, colorNames.get(v.colorId()), v.colorId(), v.size()) : null;
      Object[] vp = {
        "p",
        id,
        "c",
        v.colorId(),
        "s",
        v.size().trim().toUpperCase(Locale.ROOT),
        "sku",
        sku,
        "price",
        v.price().setScale(0, java.math.RoundingMode.HALF_UP),
        "q",
        v.quantity(),
        "st",
        v.status(),
        "i",
        v.id()
      };
      if (v.id() == null) {
        queries.insertProductDetail(vp);
      } else {
        var old = queries.findProductDetail("i", v.id(), "p", id);
        if (number(old, "COLOR_ID") != v.colorId()
            || !old.get("SIZE_NAME").toString().equalsIgnoreCase(v.size().trim()))
          throw new BusinessException(
              "Không đổi màu–size của biến thể cũ; tạo biến thể mới để giữ lịch sử");
        if (number(old, "QUANTITY") != v.quantity())
          throw new BusinessException("Điều chỉnh tồn qua trang Tồn kho và nhập lý do");
        queries.updateProductDetail(vp);
      }
    }
    db.audit(actor, "SAVE_PRICE_VARIANTS", "PRODUCT", id);
    return id;
  }

  private String nextProductCode() {
    for (int i = 0; i < 1000; i++) {
      String value =
          new java.math.BigDecimal(queries.findDual().get("N").toString())
              .toBigIntegerExact()
              .toString();
      String code = "SP" + "0".repeat(Math.max(0, 3 - value.length())) + value;
      if (queries.listProduct("c", code).isEmpty()) return code;
    }
    throw new BusinessException("Không cấp được mã sản phẩm. Kiểm tra sequence mã sản phẩm.");
  }

  private String nextSku(String code, String color, long colorId, String size) {
    String c = skuPart(color), s = skuPart(size);
    String base = code + "-" + (c.isBlank() ? "C" + colorId : c) + "-" + (s.isBlank() ? "SIZE" : s);
    for (int i = 1; i < 10000; i++) {
      String suffix = i == 1 ? "" : "-" + i;
      String sku = base.substring(0, Math.min(base.length(), 80 - suffix.length())) + suffix;
      if (queries.listProductDetail3("s", sku).isEmpty()) return sku;
    }
    throw new BusinessException("Không thể tạo SKU duy nhất");
  }

  private long lookup(String kind, Long id, String name, long actor) {
    String column =
        switch (kind) {
          case "BRAND" -> "BRAND_NAME";
          case "MATERIAL" -> "MATERIAL_NAME";
          default -> throw new IllegalArgumentException("Lookup không hợp lệ");
        };
    if (id != null) return number(queries.lookupById(kind, id), "ID");
    if (name == null || name.isBlank())
      throw new BusinessException("Nhập " + (kind.equals("BRAND") ? "thương hiệu" : "chất liệu"));
    String normalized = name.strip().replaceAll("\\s+", " ");
    queries.lockLookup(kind);
    var existing = queries.lookupByName(kind, column, normalized);
    if (!existing.isEmpty()) return number(existing.get(0), "ID");
    long created = queries.insertLookup(kind, column, normalized);
    db.audit(actor, "CREATE", kind, created);
    return created;
  }
}
