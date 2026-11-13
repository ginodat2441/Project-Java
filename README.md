# Clothing Shop – Spring Boot + Oracle + SQL thuần (JDBC)

Java 17 · Spring Boot 3.3 · Maven · Oracle 12c+ (ojdbc11) · `NamedParameterJdbcTemplate` (KHÔNG dùng JPA/Hibernate)

## Chạy dự án
1. Tạo user Oracle (ví dụ `CLOTHING_SHOP`), mở SQL Developer chạy lần lượt:
   - `src/main/resources/db/schema.sql` (bảng, index, trigger)
   - `src/main/resources/db/sample_data.sql` (dữ liệu mẫu)
   Đây là bộ schema/dữ liệu mẫu của ứng dụng. Nếu nhập toàn bộ database từ export
   `clothing_shop_export_20261108.sql`, dùng hai file tách bên dưới thay cho cả hai file trên.
2. Với `clothing_shop_export_20261108.sql`, schema owner là `CLOTHING_SHOP`; backend mặc định kết nối bằng user này.
   Có thể ghi đè user bằng biến môi trường `DB_USERNAME`. Đặt mật khẩu đúng của user bằng
   `SPRING_DATASOURCE_PASSWORD` (không dùng mật khẩu của user `WEBSHOP` nếu khác).
   Nếu dùng schema khác, bảo đảm user kết nối có quyền trên các bảng và migration phù hợp.
3. Cấu hình secret JWT (tối thiểu 32 ký tự), sau đó chạy `mvn spring-boot:run`.
   PowerShell:
   ```
   $bytes = New-Object byte[] 32
   [Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($bytes)
   $env:JWT_SECRET = [Convert]::ToBase64String($bytes)
   ```
4. Đặt `ADMIN_BOOTSTRAP_TOKEN` thành một chuỗi bí mật ngẫu nhiên để khởi tạo chủ cửa hàng lần đầu.
   Có thể tạo bằng:
   ```
   $bytes = New-Object byte[] 32
   [Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($bytes)
   $env:ADMIN_BOOTSTRAP_TOKEN = [Convert]::ToBase64String($bytes)
   ```
5. Phí giao hàng mặc định là 30.000; có thể cấu hình qua biến môi trường `ORDER_SHIPPING_FEE`.

### Nhập export Oracle
Trong SQL Developer, kết nối bằng user `CLOTHING_SHOP` tới dịch vụ `FREEPDB1`, rồi chạy toàn bộ
từng file bằng **F5** theo thứ tự:
1. `clothing_shop_schema_20261108.sql` — tạo bảng, khóa, index, sequence và trigger. Có thể chạy
   lại; các đối tượng đã tồn tại và cột đã NOT NULL được bỏ qua.
2. `clothing_shop_data_20261108.sql` — nạp toàn bộ dữ liệu theo thứ tự khóa ngoại, bỏ qua các khóa
   đã có và đồng bộ IDENTITY sau khi nạp.

Không chạy trực tiếp `clothing_shop_export_20261108.sql`; file export gốc sắp xếp dữ liệu theo tên
bảng nên bảng con như `ADDRESS` có thể được chèn trước bảng cha `CUSTOMER`.

## Chạy giao diện tiếng Việt
1. Chạy backend ở `http://localhost:8080` sau khi Oracle và schema đã sẵn sàng.
2. Mở thư mục `giao dien` bằng VS Code và chạy `index.html` qua Live Server (không mở trực tiếp bằng `file://`).
   Giao diện mặc định gọi API tại `http://localhost:8080/api`.
3. Nếu dùng địa chỉ khác, khai báo `window.CLOTHING_SHOP_API_BASE` trước `assets/js/storefront.js`.
   CORS cho phép các origin loopback (`localhost` và `127.0.0.1`) cùng domain ngrok đang dùng; có thể đổi
   danh sách bằng biến `CORS_ALLOWED_ORIGINS`, phân tách bằng dấu phẩy. Không mở CORS cho mọi origin khi triển khai thật.

Giao diện gọi API cho danh sách/tìm kiếm và chi tiết sản phẩm, đăng ký/đăng nhập, hồ sơ và đơn hàng,
địa chỉ giao hàng, giỏ hàng và thanh toán khi nhận hàng (COD) hoặc trực tuyến qua SePay. Token đăng nhập được giữ trong phiên
trình duyệt hiện tại. Các trang thông tin/chính sách hiển thị tiếng Việt; cửa hàng cần rà soát và bổ sung
thông tin pháp lý, giao nhận và liên hệ thực tế trước khi vận hành chính thức.

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
                           {"addressId":1,"paymentMethod":"COD hoặc SEPAY","note":"Giao giờ hành chính",
                           "items":[{"productDetailId":1,"quantity":2}]}
POST  /api/carts/customer/{customerId}/checkout/sepay Authorization: ******
                           {"addressId":1,"paymentMethod":"SEPAY","note":"Giao giờ hành chính"}
POST  /api/bills/{id}/sepay-payment Authorization: ******  Chuẩn bị lại thanh toán QR
POST  /api/payment  Webhook ngân hàng SePay (cũng hỗ trợ /api/payments/sepay/ipn)
GET   /api/bills         Danh sách đơn của khách đang đăng nhập
GET   /api/bills/1/lines Chi tiết sản phẩm (màu, size, SKU, giá tại thời điểm mua)
GET   /api/bills/1/history
PATCH /api/bills/1/cancel Chỉ hủy đơn đang chờ xác nhận; tồn kho giữ chỗ được giải phóng
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
Khi đặt hàng, số lượng được giữ chỗ bằng cách giảm `PRODUCT_DETAIL.QUANTITY` và tăng
`RESERVED_QUANTITY`; `PHYSICAL_QUANTITY` không đổi. `QUANTITY` là số còn bán trên website.
Đơn mới hỗ trợ `COD` và `SEPAY`. Checkout tạo QR VietQR với tài khoản nhận tiền, tổng tiền và mã hóa đơn.
Khách quét QR bằng ứng dụng ngân hàng; khi SePay báo giao dịch tiền vào, backend kiểm tra
`Authorization: Apikey <API_KEY>`, loại giao dịch, mã hóa đơn và số tiền rồi cập nhật trạng thái thanh toán.
API key được đọc từ `app.sepay.secret-key` trong `application.properties` (có thể đặt qua biến môi trường
`SEPAY_SECRET_KEY`); chỉ lưu key thật ở cấu hình máy chạy, không commit lên Git. Cấu hình định dạng webhook là JSON.

Trong trang Webhooks của SePay, đăng ký URL `https://broaden-grating-stoneware.ngrok-free.dev/api/payment`,
chọn sự kiện **Có tiền vào**, định dạng JSON, xác thực **API Key**, và đặt key khớp với `app.sepay.secret-key`.
Proxy ngrok chuyển `/api` về backend cổng 8080. Endpoint cũng có bí danh
`/api/payments/sepay/ipn`. Mã QR dùng `bank=Vietcombank`, tài khoản `SBSEPAYGL9TEMZRXDAK`, `template=compact`,
`amount` là tổng tiền đơn hàng và `des` là mã đơn. Trang chi tiết đơn hiển thị QR và nút kiểm tra lại trạng thái.
Không dùng việc quay lại trang đơn hàng làm bằng chứng đã thanh toán; chỉ webhook xác thực và đối chiếu thành công
mới cập nhật `BILL.PAYMENT_STATUS` và `PAYMENT_HISTORY`.
Tham khảo hướng dẫn chính thức về [webhook](https://docs.sepay.vn/tich-hop-webhooks.html) và
[QR VietQR động](https://docs.sepay.vn/tao-qr-code-vietqr-dong.html).

Với database đang chạy, kết nối schema `CLOTHING_SHOP` và chạy một lần
`src/main/resources/db/migration/V5__enable_sepay.sql`. Migration giữ constraint `ENABLE NOVALIDATE` để
giữ dữ liệu thanh toán lịch sử; constraint không còn cho phép `ZALOPAY` và bổ sung `SEPAY`.
`MOMO`/`VNPAY` còn trong constraint để tương thích các bản ghi mẫu/lịch sử; API mới chỉ nhận `COD` và `SEPAY`.
Schema database mới chỉ bật `COD`, `BANK`, `SEPAY`.
Với database mới tạo từ `src/main/resources/db/schema.sql`, SePay đã được bật sẵn. Các migration V2/V3
được giữ lại để phản ánh lịch sử cập nhật; không chạy lại chúng sau V5. Khi đăng ký, backend tự tạo hoặc
kích hoạt role `CUSTOMER` nếu bị thiếu; V4 có thể chạy thủ công để chuẩn bị role trước. Với cơ sở dữ liệu
cũ khác, sửa owner trong migration cho đúng schema.
Tạo chủ cửa hàng lần đầu bằng bootstrap API ở trên, sau đó đăng nhập qua `MANAGER`;
`paid:true` xác nhận đã thu tiền mặt (chỉ khi đơn đang giao/hoàn thành), `paid:false` đánh dấu chưa thanh toán.
Mỗi lần đổi trạng thái thanh toán được ghi vào `PAYMENT_HISTORY`. Đặt `ADMIN_BOOTSTRAP_TOKEN` rồi gọi bootstrap đúng một lần;
endpoint chỉ cho phép tạo tài khoản chủ cửa hàng khi cơ sở dữ liệu chưa có chủ cửa hàng. Chủ cửa hàng có thể tạo role `STAFF` qua
`POST /api/admin/managers`. Chủ cửa hàng có toàn quyền trên đơn hàng, nhân viên chỉ xem đơn và duyệt trạng thái
`0 (chờ xác nhận) -> 1 (đã xác nhận)`. Chỉ chủ cửa hàng được sửa trạng thái thanh toán.

Bài tập SQL mẫu: `src/main/resources/sql/practice_queries.sql`
