# Clothing Shop – Spring Boot + Oracle + SQL thuần (JDBC)

Java 17 · Spring Boot 3.3 · Maven · Oracle 12c+ (ojdbc11) · `NamedParameterJdbcTemplate` (KHÔNG dùng JPA/Hibernate)

## Chạy dự án
1. Tạo user Oracle (ví dụ `CLOTHING_SHOP`), mở SQL Developer chạy lần lượt:
   - `src/main/resources/db/schema.sql` (bảng, index, trigger)
   - `src/main/resources/db/sample_data.sql` (dữ liệu mẫu)
2. Sửa `application.properties`: url / username / password.
3. `mvn spring-boot:run`

## Cấu trúc (mỗi module: controller → service → repository)
```
com.example.clothingshop
├── common/    SqlHelper (list/one/scalar/maps/update/insert/page), ApiResponse, exception
├── brand/     CRUD mẫu: INSERT / UPDATE / xóa mềm / tìm kiếm + phân trang
├── product/   JOIN nhiều bảng, subquery, tìm kiếm động
├── bill/      Đặt hàng @Transactional (trừ kho nguyên tử, snapshot địa chỉ)
├── stats/     GROUP BY / HAVING / FETCH FIRST
└── dev/       /api/dev/sql: chạy SELECT tùy ý khi học (tắt bằng profile prod)
```
**Quy tắc: SQL chỉ nằm ở lớp `*Repository` (và `stats`).** Service lo nghiệp vụ + transaction.

## Thêm chức năng mới (3 bước)
1. Tạo `record` có field khớp tên cột (`PRODUCT_NAME` → `productName`).
2. Trong Repository viết SQL bằng text block, tham số dạng `:ten`:
   ```java
   sql.list("SELECT ID, COLOR_NAME FROM COLOR WHERE DELETE_FLAG = 0 AND LOWER(COLOR_NAME) LIKE :k",
            Map.of("k", "%đen%"), Color.class);
   ```
3. Service gọi repository, Controller trả `ApiResponse`.

## Lưu ý Oracle
- `Map.of()` không cho giá trị null → dùng `HashMap` khi tham số có thể null.
- Phân trang: `OFFSET n ROWS FETCH NEXT m ROWS ONLY` (12c+), SQL phải có `ORDER BY`.
- Luôn thêm `DELETE_FLAG = 0` khi SELECT (xóa mềm).
- SQL + tham số được log ra console (xem application.properties).

## Thử API
```
GET   /api/brands?keyword=rou&page=1&size=5
POST  /api/brands   {"brandName":"Zara","country":"Tây Ban Nha"}
GET   /api/products?keyword=áo&brandId=1
GET   /api/products/1/details
POST  /api/bills    {"customerId":1,"addressId":1,"paymentMethod":"COD","shippingFee":30000,
                     "items":[{"productDetailId":1,"quantity":2}]}
PATCH /api/bills/1/status?status=1&managerId=1
GET   /api/stats/revenue-by-month
POST  /api/dev/sql  {"sql":"SELECT * FROM BRAND"}
```
Bài tập SQL mẫu: `src/main/resources/sql/practice_queries.sql`
