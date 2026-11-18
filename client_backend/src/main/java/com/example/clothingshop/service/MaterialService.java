package com.example.clothingshop.service;

import com.example.clothingshop.common.BusinessException;
import com.example.clothingshop.common.NotFoundException;
import com.example.clothingshop.common.PageResult;
import com.example.clothingshop.dto.request.MaterialRequest;
import com.example.clothingshop.entity.Material;
import com.example.clothingshop.repositories.MaterialRepository;
import com.example.clothingshop.view.MaterialView;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MaterialService {

    private final MaterialRepository repo;

    public MaterialService(MaterialRepository repo) { this.repo = repo; }

    public PageResult<MaterialView> search(String keyword, int page, int size) {
        return repo.search(keyword, page, size).map(MaterialView::from);
    }

    public MaterialView get(long id) { return MaterialView.from(getEntity(id)); }

    @Transactional
    public MaterialView create(MaterialRequest r) {
        if (repo.existsByName(r.materialName(), 0)) {
            throw new BusinessException("Chất liệu '" + r.materialName().trim() + "' đã tồn tại");
        }
        return get(repo.insert(r));
    }

    @Transactional
    public MaterialView update(long id, MaterialRequest r) {
        getEntity(id);
        if (repo.existsByName(r.materialName(), id)) {
            throw new BusinessException("Chất liệu '" + r.materialName().trim() + "' đã tồn tại");
        }
        repo.update(id, r);
        return get(id);
    }

    @Transactional
    public void delete(long id) {
        getEntity(id);
        long used = repo.countUsage(id);
        if (used > 0) {
            throw new BusinessException("Không thể xóa: chất liệu đang được dùng bởi " + used + " sản phẩm");
        }
        repo.softDelete(id);
    }

    private Material getEntity(long id) {
        return repo.findById(id).orElseThrow(() -> new NotFoundException("Không tìm thấy chất liệu id=" + id));
    }
}
