package com.example.clothingshop.common;

import org.springframework.jdbc.core.DataClassRowMapper;
import org.springframework.jdbc.core.SingleColumnRowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Component;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Bọc NamedParameterJdbcTemplate: bạn chỉ viết SQL thuần + tham số :ten,
 * kết quả tự map vào record (cột MIN_PRICE -> field minPrice).
 */
@Component
public class SqlHelper {

    private final NamedParameterJdbcTemplate jdbc;

    public SqlHelper(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** SELECT nhiều dòng -> List<record> */
    public <T> List<T> list(String sql, Map<String, ?> params, Class<T> type) {
        return jdbc.query(sql, params, new DataClassRowMapper<>(type));
    }

    /** SELECT 1 dòng -> Optional<record> */
    public <T> Optional<T> one(String sql, Map<String, ?> params, Class<T> type) {
        return list(sql, params, type).stream().findFirst();
    }

    /** SELECT 1 giá trị (COUNT, SUM, MAX, giá...) */
    public <T> Optional<T> scalar(String sql, Map<String, ?> params, Class<T> type) {
        return jdbc.query(sql, params, new SingleColumnRowMapper<>(type)).stream().findFirst();
    }

    /** SELECT -> List<Map> (key = tên cột VIẾT HOA), tiện cho báo cáo */
    public List<Map<String, Object>> maps(String sql, Map<String, ?> params) {
        return jdbc.queryForList(sql, params);
    }

    /** INSERT/UPDATE/DELETE -> số dòng bị ảnh hưởng */
    public int update(String sql, Map<String, ?> params) {
        return jdbc.update(sql, params);
    }

    /** INSERT và lấy ID tự tăng (IDENTITY) vừa sinh ra */
    public long insert(String sql, Map<String, ?> params) {
        KeyHolder kh = new GeneratedKeyHolder();
        jdbc.update(sql, new MapSqlParameterSource(params), kh, new String[]{"ID"});
        Number key = kh.getKey();
        if (key == null) throw new IllegalStateException("Không lấy được ID vừa insert");
        return key.longValue();
    }

    /**
     * Phân trang Oracle 12c: OFFSET ... FETCH NEXT ...
     * selectSql PHẢI có ORDER BY ở cuối. Tổng dòng = SELECT COUNT(*) FROM (selectSql).
     */
    public <T> PageResult<T> page(String selectSql, Map<String, Object> params,
                                  int page, int size, Class<T> type) {
        int p = Math.max(page, 1);
        int s = Math.min(Math.max(size, 1), 100);

        Long total = jdbc.queryForObject("SELECT COUNT(*) FROM (" + selectSql + ")", params, Long.class);
        Map<String, Object> pagedParams = new HashMap<>(params);
        pagedParams.put("_offset", (p - 1) * s);
        pagedParams.put("_size", s);

        List<T> items = list(selectSql + " OFFSET :_offset ROWS FETCH NEXT :_size ROWS ONLY",
                pagedParams, type);
        long t = total == null ? 0 : total;
        return new PageResult<>(items, p, s, t, (int) Math.ceil((double) t / s));
    }
}
