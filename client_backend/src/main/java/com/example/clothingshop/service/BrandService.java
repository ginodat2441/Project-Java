package com.example.clothingshop.service;

import com.example.clothingshop.entity.Brand;
import com.example.clothingshop.dto.request.BrandRequest;
import com.example.clothingshop.common.NotFoundException;
import com.example.clothingshop.common.PageResult;
import com.example.clothingshop.repositories.BrandRepository;
import com.example.clothingshop.view.BrandView;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BrandService {

    private final BrandRepository repo;

    public BrandService(BrandRepository repo) { this.repo = repo; }

    // ---------- repo trả ENTITY (Brand) -> service đổi sang VIEW (BrandView) ----------

    public PageResult<BrandView> search(String keyword, int page, int size) {
        return repo.search(keyword, page, size).map(BrandView::from);
    }

    public BrandView get(long id) { return BrandView.from(getEntity(id)); }

    @Transactional
    public BrandView create(BrandRequest r) {
        return get(repo.insert(r));
    }

    @Transactional
    public BrandView update(long id, BrandRequest r) {
        if (repo.update(id, r) == 0) throw new NotFoundException("Không tìm thấy thương hiệu id=" + id);
        return get(id);
    }

    @Transactional
    public void delete(long id) {
        if (repo.softDelete(id) == 0) throw new NotFoundException("Không tìm thấy thương hiệu id=" + id);
    }

    private Brand getEntity(long id) {
        return repo.findById(id).orElseThrow(() -> new NotFoundException("Không tìm thấy thương hiệu id=" + id));
    }
}
