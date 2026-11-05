package com.example.clothingshop.brand;

import com.example.clothingshop.common.NotFoundException;
import com.example.clothingshop.common.PageResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BrandService {

    private final BrandRepository repo;

    public BrandService(BrandRepository repo) { this.repo = repo; }

    public PageResult<Brand> search(String keyword, int page, int size) {
        return repo.search(keyword, page, size);
    }

    public Brand get(long id) {
        return repo.findById(id).orElseThrow(() -> new NotFoundException("Không tìm thấy thương hiệu id=" + id));
    }

    @Transactional
    public Brand create(BrandRequest r) {
        return get(repo.insert(r));
    }

    @Transactional
    public Brand update(long id, BrandRequest r) {
        if (repo.update(id, r) == 0) throw new NotFoundException("Không tìm thấy thương hiệu id=" + id);
        return get(id);
    }

    @Transactional
    public void delete(long id) {
        if (repo.softDelete(id) == 0) throw new NotFoundException("Không tìm thấy thương hiệu id=" + id);
    }
}
