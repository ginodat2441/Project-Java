package com.example.clothingshop.service;

import com.example.clothingshop.common.NotFoundException;
import com.example.clothingshop.common.PageResult;
import com.example.clothingshop.repositories.ProductRepository;
import com.example.clothingshop.view.ProductDetailView;
import com.example.clothingshop.view.ProductImageView;
import com.example.clothingshop.view.ProductView;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProductService {

    private final ProductRepository repo;

    public ProductService(ProductRepository repo) { this.repo = repo; }

    public PageResult<ProductView> search(String keyword, Long brandId, Integer gender, int page, int size) {
        return search(keyword, brandId, gender, null, "newest", page, size);
    }

    public PageResult<ProductView> search(String keyword, Long brandId, Integer gender, Long categoryId,
                                         String sort, int page, int size) {
        if (!List.of("newest", "price_asc", "price_desc").contains(sort))
            throw new com.example.clothingshop.common.BusinessException("Thứ tự sắp xếp không hợp lệ");
        return repo.searchViews(keyword, brandId, gender, categoryId, sort, page, size);
    }

    public List<com.example.clothingshop.view.CategoryView> categories() { return repo.categories(); }

    public ProductView get(long id) {
        return repo.findViewById(id)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy sản phẩm id=" + id));
    }

    public List<ProductDetailView> details(long productId) {
        get(productId);                              // 404 nếu sản phẩm không tồn tại
        return repo.findDetailViews(productId);
    }

    public List<ProductImageView> images(long productId) {
        get(productId);
        return repo.findImageViews(productId);
    }
}
