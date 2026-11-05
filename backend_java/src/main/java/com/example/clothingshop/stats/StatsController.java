package com.example.clothingshop.stats;

import com.example.clothingshop.common.ApiResponse;
import com.example.clothingshop.common.SqlHelper;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Báo cáo trả về List<Map> (không cần tạo record cho mỗi báo cáo).
 * Key của Map là tên cột viết hoa, ví dụ row.get("REVENUE").
 */
@RestController
@RequestMapping("/api/stats")
public class StatsController {

    private final SqlHelper sql;

    public StatsController(SqlHelper sql) { this.sql = sql; }

    /** Doanh thu theo tháng (chỉ đơn hoàn thành STATUS = 3) */
    @GetMapping("/revenue-by-month")
    public ApiResponse<List<Map<String, Object>>> revenueByMonth() {
        String q = """
                SELECT TO_CHAR(CREATED_AT, 'YYYY-MM') AS MONTH,
                       COUNT(*)                       AS TOTAL_BILLS,
                       SUM(TOTAL_AMOUNT)              AS REVENUE
                  FROM BILL
                 WHERE STATUS = 3 AND DELETE_FLAG = 0
                 GROUP BY TO_CHAR(CREATED_AT, 'YYYY-MM')
                 ORDER BY MONTH
                """;
        return ApiResponse.ok(sql.maps(q, Map.of()));
    }

    /** Top sản phẩm bán chạy (GROUP BY + HAVING + FETCH FIRST) */
    @GetMapping("/top-products")
    public ApiResponse<List<Map<String, Object>>> topProducts(@RequestParam(defaultValue = "5") int limit,
                                                              @RequestParam(defaultValue = "1") int minSold) {
        String q = """
                SELECT p.ID, p.PRODUCT_NAME, SUM(bd.QUANTITY) AS SOLD, SUM(bd.TOTAL_PRICE) AS REVENUE
                  FROM BILL_DETAIL bd
                  JOIN BILL b            ON b.ID  = bd.BILL_ID AND b.STATUS = 3
                  JOIN PRODUCT_DETAIL pd ON pd.ID = bd.PRODUCT_DETAIL_ID
                  JOIN PRODUCT p         ON p.ID  = pd.PRODUCT_ID
                 GROUP BY p.ID, p.PRODUCT_NAME
                HAVING SUM(bd.QUANTITY) >= :minSold
                 ORDER BY SOLD DESC
                 FETCH FIRST :limit ROWS ONLY
                """;
        return ApiResponse.ok(sql.maps(q, Map.of("limit", limit, "minSold", minSold)));
    }

    /** Biến thể sắp hết hàng (tồn <= ngưỡng) */
    @GetMapping("/low-stock")
    public ApiResponse<List<Map<String, Object>>> lowStock(@RequestParam(defaultValue = "5") int threshold) {
        String q = """
                SELECT pd.SKU, p.PRODUCT_NAME, c.COLOR_NAME, pd.SIZE_NAME, pd.QUANTITY
                  FROM PRODUCT_DETAIL pd
                  JOIN PRODUCT p ON p.ID = pd.PRODUCT_ID
                  JOIN COLOR c   ON c.ID = pd.COLOR_ID
                 WHERE pd.DELETE_FLAG = 0 AND pd.QUANTITY <= :t
                 ORDER BY pd.QUANTITY, p.PRODUCT_NAME
                """;
        return ApiResponse.ok(sql.maps(q, Map.of("t", threshold)));
    }
}
