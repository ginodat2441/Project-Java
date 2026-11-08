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
    private final com.example.clothingshop.admin.ReportService reports;

    public StatsController(SqlHelper sql, com.example.clothingshop.admin.ReportService reports) { this.sql = sql; this.reports=reports; }

    /** Doanh thu theo tháng (chỉ đơn hoàn thành STATUS = 3) */
    @SuppressWarnings("unchecked")
    @GetMapping("/revenue-by-month")
    public ApiResponse<List<Map<String,Object>>> revenueByMonth() {
        var result=reports.report("MONTH",java.time.LocalDate.now().getYear(),null,null,null,null,null,"",1);
        var rows=((List<Map<String,Object>>)result.get("series")).stream().map(row->{
            Map<String,Object> mapped=new java.util.LinkedHashMap<>();mapped.put("MONTH",row.get("PERIOD"));mapped.put("TOTAL_BILLS",row.get("ORDERS"));mapped.put("REVENUE",row.get("NET_AFTER_RETURN"));return mapped;
        }).toList();
        return ApiResponse.ok(rows);
    }

    /** Top sản phẩm bán chạy (GROUP BY + HAVING + FETCH FIRST) */
    @GetMapping("/top-products")
    public ApiResponse<List<Map<String,Object>>> topProducts(@RequestParam(defaultValue="5") int limit,@RequestParam(defaultValue="1") int minSold) {
        return ApiResponse.ok(reports.topProducts(java.time.LocalDate.now().getYear(),limit,minSold));
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
