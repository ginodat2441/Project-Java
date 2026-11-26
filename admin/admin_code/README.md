# Clothing Shop – Spring Boot + Oracle + SQL thuần (JDBC)

Xem [V008_HANDOVER.md](../../V008_HANDOVER.md) để biết trạng thái database và hướng dẫn nâng cấp. File chuẩn duy nhất để tạo mới hoặc nâng cấp schema là [`sql/ora_schema.sql`](../../sql/ora_schema.sql); file này giữ dữ liệu hiện có và không nạp dữ liệu mẫu. Với database hiện tại, sao lưu và dừng tiến trình ghi trước khi chạy toàn bộ file bằng SQL Developer (F5) với schema `CLOTHING_SHOP`. API ghi đã yêu cầu đăng nhập/CSRF, API SQL tùy ý luôn bị khóa và cấu hình password phải dùng biến môi trường.

Schema chuẩn đã bao gồm các cập nhật voucher, Viettel Post, fulfillment và checkout hiện tại. Các file migration theo phiên bản được giữ làm lịch sử/phục vụ kiểm thử; không chạy chúng riêng khi đã dùng `sql/ora_schema.sql`.

Java 17 · Spring Boot 3.3 · Maven · Oracle 12c+ (ojdbc11) · `NamedParameterJdbcTemplate` (KHÔNG dùng JPA/Hibernate)

## Chạy dự án
1. Tạo user Oracle (ví dụ `CLOTHING_SHOP`), kết nối bằng user đó và chạy `sql/ora_schema.sql` một lần bằng SQL Developer (F5). Không chạy các file migration riêng sau đó và không nạp dữ liệu mẫu vào database đang dùng.
2. Sửa `application.properties`: url / username / password.
3. Thiết lập `CLOUDINARY_CLOUD_NAME`, `CLOUDINARY_API_KEY` và `CLOUDINARY_API_SECRET` trong môi trường chạy backend admin (lấy tại Cloudinary Dashboard → API Keys). Ví dụ PowerShell:
   ```powershell
   $env:CLOUDINARY_CLOUD_NAME = "<cloud-name>"
   $env:CLOUDINARY_API_KEY = "<api-key>"
   $env:CLOUDINARY_API_SECRET = "<api-secret>"
   ```
   Không đưa API secret vào frontend hoặc commit vào repository.
4. `mvn spring-boot:run`

Ảnh sản phẩm mới được kiểm tra ở backend, tải lên Cloudinary và lưu bằng `secure_url` trong database. Nếu Cloudinary chưa được cấu hình hoặc không khả dụng, thao tác upload sẽ báo lỗi thay vì lưu ảnh cục bộ.

## Cấu trúc (mỗi module: controller → service → repository)
```
com.example.clothingshop
  controller/
  service/
  repository/
  dto/
  model/
  config/
  integration/
  bootstrap/
  util/
  common/
```
**SQL: repository. Business rules and transactions: service. HTTP: controller.** Service lo nghiệp vụ + transaction.

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
