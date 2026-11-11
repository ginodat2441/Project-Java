package com.example.clothingshop.repositories;

import com.example.clothingshop.common.PageResult;
import com.example.clothingshop.common.SqlHelper;
import com.example.clothingshop.dto.request.MaterialRequest;
import com.example.clothingshop.entity.Material;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Repository
public class MaterialRepository {

    private final SqlHelper sql;

    public MaterialRepository(SqlHelper sql) { this.sql = sql; }

    private static final String SELECT_COLS =
            "SELECT ID, MATERIAL_NAME, DESCRIPTION, CREATED_AT, UPDATED_AT FROM MATERIAL ";

    public PageResult<Material> search(String keyword, int page, int size) {
        StringBuilder q = new StringBuilder(SELECT_COLS).append("WHERE DELETE_FLAG = 0 ");
        Map<String, Object> p = new HashMap<>();
        if (keyword != null && !keyword.isBlank()) {
            q.append("AND LOWER(MATERIAL_NAME) LIKE '%' || LOWER(:keyword) || '%' ");
            p.put("keyword", keyword.trim());
        }
        q.append("ORDER BY ID DESC");
        return sql.page(q.toString(), p, page, size, Material.class);
    }

    public Optional<Material> findById(long id) {
        return sql.one(SELECT_COLS + "WHERE ID = :id AND DELETE_FLAG = 0", Map.of("id", id), Material.class);
    }

    public boolean existsByName(String name, long excludeId) {
        String q = "SELECT COUNT(*) FROM MATERIAL WHERE DELETE_FLAG = 0 AND ID <> :excludeId "
                 + "AND LOWER(MATERIAL_NAME) = LOWER(:name)";
        return sql.scalar(q, Map.of("name", name.trim(), "excludeId", excludeId), Long.class).orElse(0L) > 0;
    }

    /** Số sản phẩm đang dùng chất liệu này. */
    public long countUsage(long id) {
        return sql.scalar("SELECT COUNT(*) FROM PRODUCT WHERE MATERIAL_ID = :id AND DELETE_FLAG = 0",
                Map.of("id", id), Long.class).orElse(0L);
    }

    public long insert(MaterialRequest r) {
        String q = "INSERT INTO MATERIAL (MATERIAL_NAME, DESCRIPTION) VALUES (:name, :desc)";
        return sql.insert(q, params(r));
    }

    public int update(long id, MaterialRequest r) {
        String q = "UPDATE MATERIAL SET MATERIAL_NAME = :name, DESCRIPTION = :desc WHERE ID = :id AND DELETE_FLAG = 0";
        Map<String, Object> p = params(r);
        p.put("id", id);
        return sql.update(q, p);
    }

    /** Xóa mềm */
    public int softDelete(long id) {
        return sql.update("UPDATE MATERIAL SET DELETE_FLAG = 1 WHERE ID = :id AND DELETE_FLAG = 0", Map.of("id", id));
    }

    private Map<String, Object> params(MaterialRequest r) {
        Map<String, Object> p = new HashMap<>();
        p.put("name", r.materialName().trim());
        p.put("desc", r.description());
        return p;
    }
}
