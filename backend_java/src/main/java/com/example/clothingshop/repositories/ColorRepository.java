package com.example.clothingshop.repositories;

import com.example.clothingshop.common.PageResult;
import com.example.clothingshop.common.SqlHelper;
import com.example.clothingshop.dto.request.ColorRequest;
import com.example.clothingshop.entity.Color;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Repository
public class ColorRepository {

    private final SqlHelper sql;

    public ColorRepository(SqlHelper sql) { this.sql = sql; }

    private static final String SELECT_COLS =
            "SELECT ID, COLOR_NAME, HEX_CODE, CREATED_AT, UPDATED_AT FROM COLOR ";

    public PageResult<Color> search(String keyword, int page, int size) {
        StringBuilder q = new StringBuilder(SELECT_COLS).append("WHERE DELETE_FLAG = 0 ");
        Map<String, Object> p = new HashMap<>();
        if (keyword != null && !keyword.isBlank()) {
            q.append("AND LOWER(COLOR_NAME) LIKE '%' || LOWER(:keyword) || '%' ");
            p.put("keyword", keyword.trim());
        }
        q.append("ORDER BY ID DESC");
        return sql.page(q.toString(), p, page, size, Color.class);
    }

    public Optional<Color> findById(long id) {
        return sql.one(SELECT_COLS + "WHERE ID = :id AND DELETE_FLAG = 0", Map.of("id", id), Color.class);
    }

    /** Trùng tên (không phân biệt hoa thường) với màu khác đang dùng? excludeId = 0 khi tạo mới. */
    public boolean existsByName(String name, long excludeId) {
        String q = "SELECT COUNT(*) FROM COLOR WHERE DELETE_FLAG = 0 AND ID <> :excludeId "
                 + "AND LOWER(COLOR_NAME) = LOWER(:name)";
        return sql.scalar(q, Map.of("name", name.trim(), "excludeId", excludeId), Long.class).orElse(0L) > 0;
    }

    /** Số biến thể sản phẩm đang dùng màu này. */
    public long countUsage(long id) {
        return sql.scalar("SELECT COUNT(*) FROM PRODUCT_DETAIL WHERE COLOR_ID = :id AND DELETE_FLAG = 0",
                Map.of("id", id), Long.class).orElse(0L);
    }

    public long insert(ColorRequest r) {
        String q = "INSERT INTO COLOR (COLOR_NAME, HEX_CODE) VALUES (:name, :hex)";
        return sql.insert(q, params(r));
    }

    public int update(long id, ColorRequest r) {
        String q = "UPDATE COLOR SET COLOR_NAME = :name, HEX_CODE = :hex WHERE ID = :id AND DELETE_FLAG = 0";
        Map<String, Object> p = params(r);
        p.put("id", id);
        return sql.update(q, p);
    }

    /** Xóa mềm */
    public int softDelete(long id) {
        return sql.update("UPDATE COLOR SET DELETE_FLAG = 1 WHERE ID = :id AND DELETE_FLAG = 0", Map.of("id", id));
    }

    private Map<String, Object> params(ColorRequest r) {
        Map<String, Object> p = new HashMap<>();   // HashMap vì hexCode có thể null
        p.put("name", r.colorName().trim());
        p.put("hex", r.hexCode() == null ? null : r.hexCode().toUpperCase());
        return p;
    }
}
