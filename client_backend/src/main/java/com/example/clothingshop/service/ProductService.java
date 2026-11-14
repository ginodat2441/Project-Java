package com.example.clothingshop.service;

import com.example.clothingshop.common.NotFoundException;
import com.example.clothingshop.common.PageResult;
import com.example.clothingshop.repositories.ProductRepository;
import com.example.clothingshop.view.ProductDetailView;
import com.example.clothingshop.view.ProductView;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProductService {

    private final ProductRepository repo;

    public ProductService(ProductRepository repo) { this.repo = repo; }

    public PageResult<ProductView> search(String keyword, Long brandId, Integer gender, int page, int size) {
        return repo.searchViews(keyword, brandId, gender, page, size);
    }

    public ProductView get(long id) {
        return repo.findViewById(id)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy sản phẩm id=" + id));
    }

    public List<ProductDetailView> details(long productId) {
        get(productId);                              // 404 nếu sản phẩm không tồn tại
        return repo.findDetailViews(productId);
    }
}
