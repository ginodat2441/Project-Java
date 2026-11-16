package com.example.clothingshop.service.product;

import static com.example.clothingshop.dto.admin.AdminRequests.*;
import static com.example.clothingshop.repository.admin.AdminRepository.number;

import com.example.clothingshop.common.BusinessException;
import com.example.clothingshop.repository.admin.AdminRepository;
import com.example.clothingshop.repository.product.ProductOperationsRepository;
import com.example.clothingshop.service.admin.AdminService;
import java.io.IOException;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ProductFormService {
  private final ProductOperationsRepository queries;
  private final AdminService admin;
  private final ImageService images;
  private final AdminRepository db;

  public ProductFormService(
      AdminService admin,
      ImageService images,
      AdminRepository db,
      ProductOperationsRepository queries) {
    this.queries = queries;
    this.admin = admin;
    this.images = images;
    this.db = db;
  }

  @Transactional(rollbackFor = Exception.class)
  public long save(
      Long id, Product r, List<MultipartFile> files, List<ImageInput> metadata, long actor)
      throws IOException {
    if (files.isEmpty() || files.size() > 20 || files.size() != metadata.size())
      throw new BusinessException("Danh sách ảnh không hợp lệ (tối đa 20 ảnh)");
    if (metadata.stream().filter(ImageInput::cover).count() > 1)
      throw new BusinessException("Chỉ chọn một ảnh đại diện");
    if (id == null && r.status() == 1 && metadata.stream().noneMatch(ImageInput::cover))
      throw new BusinessException("Chọn ảnh đại diện trước khi bật bán");
    var draft =
        new Product(
            r.code(),
            r.name(),
            r.brandId(),
            r.materialId(),
            r.categoryId(),
            r.gender(),
            r.description(),
            r.thumbnailUrl(),
            0,
            r.variants(),
            r.brandName(),
            r.materialName());
    long product = admin.product(id, draft, actor);
    for (int i = 0; i < files.size(); i++) {
      var m = metadata.get(i);
      if (m.order() < 0) throw new BusinessException("Thứ tự ảnh không được âm");
      long image = images.upload(product, m.colorId(), files.get(i), actor);
      images.edit(product, image, m.colorId(), m.order(), m.cover(), actor);
    }
    var saved = queries.findProductDetail("i", product);
    if (r.status() == 1 && (saved.get("THUMBNAIL_URL") == null || number(saved, "SELLING") == 0))
      throw new BusinessException("Sản phẩm đang bán cần ảnh đại diện và biến thể đang bán");
    queries.updateProduct3("s", r.status(), "i", product);
    return product;
  }
}
