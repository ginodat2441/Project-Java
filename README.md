# Clothing Shop – Spring Boot + Oracle + SQL thuần (JDBC)

Java 17 · Spring Boot 3.3 · Maven · Oracle 12c+ (ojdbc11) · `NamedParameterJdbcTemplate` (KHÔNG dùng JPA/Hibernate)

## Chạy dự án
1. Tạo user Oracle (ví dụ `CLOTHING_SHOP`), mở SQL Developer chạy lần lượt:
   - `src/main/resources/db/schema.sql` (bảng, index, trigger)
   - `src/main/resources/db/sample_data.sql` (dữ liệu mẫu)
2. Sửa `application.properties`: url / username / password.
3. Cấu hình secret JWT (tối thiểu 32 ký tự), sau đó chạy `mvn spring-boot:run`.
   PowerShell:
   ```
   $bytes = New-Object byte[] 32
   [Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($bytes)
   $env:JWT_SECRET = [Convert]::ToBase64String($bytes)
   ```
   Đặt thêm `ADMIN_BOOTSTRAP_TOKEN` thành một chuỗi bí mật ngẫu nhiên để khởi tạo chủ cửa hàng lần đầu.
   Có thể tạo bằng:
   ```
   $bytes = New-Object byte[] 32
   [Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($bytes)
   $env:ADMIN_BOOTSTRAP_TOKEN = [Convert]::ToBase64String($bytes)
   ```
4. Phí giao hàng mặc định là 30.000; có thể cấu hình qua biến môi trường `ORDER_SHIPPING_FEE`.

## Cấu trúc (mỗi module: controller → service → repository)
```
com.example.clothingshop
├── common/    SqlHelper (list/one/scalar/maps/update/insert/page), ApiResponse, exception
├── brand/     CRUD mẫu: INSERT / UPDATE / xóa mềm / tìm kiếm + phân trang
├── product/   JOIN nhiều bảng, subquery, tìm kiếm động
├── address/   CRUD địa chỉ giao hàng theo tài khoản đăng nhập
├── auth/      Đăng ký, đăng nhập, JWT Bearer và hồ sơ khách hàng
├── bill/      Đặt hàng, chi tiết, lịch sử và hủy đơn @Transactional
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
- Câu SQL được log; bind values không được ghi để tránh lộ dữ liệu tài khoản.

## Thử API
```
GET   /api/brands?keyword=rou&page=1&size=5
POST  /api/brands   {"brandName":"Zara","country":"Tây Ban Nha"}
GET   /api/products?keyword=áo&brandId=1
GET   /api/products/1/details
POST  /api/auth/register {"username":"linh","email":"linh@example.com","password":"MatKhau123",
                          "fullName":"Nguyễn Minh Linh","phone":"0901234567"}
POST  /api/auth/login    {"email":"linh@example.com","password":"MatKhau123"}
GET   /api/auth/me       Authorization: Bearer <accessToken>
GET   /api/addresses     Authorization: Bearer <accessToken>
POST  /api/addresses     Authorization: Bearer <accessToken>
                          {"receiverName":"Nguyễn Minh Linh","receiverPhone":"0901234567",
                           "province":"Hà Nội","ward":"Phường Dịch Vọng",
                           "detailAddress":"12 Trần Thái Tông","isDefault":true}
POST  /api/bills         Authorization: Bearer <accessToken>
                          {"addressId":1,"paymentMethod":"COD","note":"Giao giờ hành chính",
                           "items":[{"productDetailId":1,"quantity":2}]}
GET   /api/bills         Danh sách đơn của khách đang đăng nhập
GET   /api/bills/1/lines Chi tiết sản phẩm (màu, size, SKU, giá tại thời điểm mua)
GET   /api/bills/1/history
PATCH /api/bills/1/cancel Chỉ hủy đơn đang chờ xác nhận; tồn kho được hoàn lại
POST  /api/admin/auth/login {"email":"admin@example.com","password":"..."}
POST  /api/admin/auth/bootstrap {"bootstrapToken":"<ADMIN_BOOTSTRAP_TOKEN>","username":"owner",
                                  "email":"owner@example.com","password":"...","fullName":"Chủ cửa hàng"}
POST  /api/admin/managers (Bearer chủ cửa hàng)
                            {"username":"staff01","email":"staff01@example.com",
                             "password":"...","fullName":"Nhân viên bán hàng"}
GET   /api/admin/bills (Bearer chủ cửa hàng hoặc nhân viên)
PATCH /api/admin/bills/1/status (Bearer chủ cửa hàng hoặc nhân viên) {"status":1,"note":"Đã xác nhận đơn"}
PATCH /api/admin/bills/1/payment-status Authorization: Bearer <adminAccessToken>
                             {"paid":true}
GET   /api/stats/revenue-by-month
POST  /api/dev/sql  {"sql":"SELECT * FROM BRAND"}
```
Các API `/api/addresses`, `/api/bills` và `/api/carts` yêu cầu `Authorization: Bearer <accessToken>`.
Khách chỉ truy cập được địa chỉ, giỏ hàng và đơn hàng của chính mình. Trạng thái đơn: 0 chờ xác nhận,
1 đã xác nhận, 2 đang giao, 3 hoàn thành, 4 đã hủy, 5 trả hàng. Tài khoản mẫu trong `sample_data.sql`
dùng mật khẩu hash giả để minh họa, không đăng nhập được; hãy đăng ký tài khoản mới để thử.
Chỉ chấp nhận `COD` cho đơn mới. Tạo chủ cửa hàng lần đầu bằng bootstrap API ở trên, sau đó đăng nhập qua `MANAGER`;
`paid:true` xác nhận đã thu tiền mặt (chỉ khi đơn đang giao/hoàn thành), `paid:false` đánh dấu chưa thanh toán.
Mỗi lần đổi trạng thái thanh toán được ghi vào `PAYMENT_HISTORY`. Đặt `ADMIN_BOOTSTRAP_TOKEN` rồi gọi bootstrap đúng một lần;
endpoint chỉ cho phép tạo tài khoản chủ cửa hàng khi cơ sở dữ liệu chưa có chủ cửa hàng. Chủ cửa hàng có thể tạo role `STAFF` qua
`POST /api/admin/managers`. Chủ cửa hàng có toàn quyền trên đơn hàng, nhân viên chỉ xem đơn và duyệt trạng thái
`0 (chờ xác nhận) -> 1 (đã xác nhận)`. Chỉ chủ cửa hàng được sửa trạng thái thanh toán.

Bài tập SQL mẫu: `src/main/resources/sql/practice_queries.sql`
