package com.example.clothingshop.service;

import com.example.clothingshop.common.BusinessException;
import com.example.clothingshop.common.NotFoundException;
import com.example.clothingshop.common.PageResult;
import com.example.clothingshop.dto.request.ColorRequest;
import com.example.clothingshop.entity.Color;
import com.example.clothingshop.repositories.ColorRepository;
import com.example.clothingshop.view.ColorView;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ColorService {

    private final ColorRepository repo;

    public ColorService(ColorRepository repo) { this.repo = repo; }

    public PageResult<ColorView> search(String keyword, int page, int size) {
        return repo.search(keyword, page, size).map(ColorView::from);
    }

    public ColorView get(long id) { return ColorView.from(getEntity(id)); }

    @Transactional
    public ColorView create(ColorRequest r) {
        if (repo.existsByName(r.colorName(), 0)) {
            throw new BusinessException("Màu '" + r.colorName().trim() + "' đã tồn tại");
        }
        return get(repo.insert(r));
    }

    @Transactional
    public ColorView update(long id, ColorRequest r) {
        getEntity(id);
        if (repo.existsByName(r.colorName(), id)) {
            throw new BusinessException("Màu '" + r.colorName().trim() + "' đã tồn tại");
        }
        repo.update(id, r);
        return get(id);
    }

    @Transactional
    public void delete(long id) {
        getEntity(id);
        long used = repo.countUsage(id);
        if (used > 0) {
            throw new BusinessException("Không thể xóa: màu đang được dùng bởi " + used + " biến thể sản phẩm");
        }
        repo.softDelete(id);
    }

    private Color getEntity(long id) {
        return repo.findById(id).orElseThrow(() -> new NotFoundException("Không tìm thấy màu id=" + id));
    }
}
