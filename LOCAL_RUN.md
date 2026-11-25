# Chạy trên máy cá nhân

Chạy các lệnh tại thư mục gốc dự án. Oracle phải đang hoạt động.

Cấu hình riêng nằm trong `.vscode/oracle-local.env` (Git bỏ qua):
`SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`.
Đặt `JWT_SECRET` ngẫu nhiên tối thiểu 32 byte để giữ hiệu lực phiên khi khởi động lại. Khi thiếu biến này, Client dùng khóa ngẫu nhiên trong bộ nhớ và yêu cầu đăng nhập lại sau khi khởi động. Không đưa file cấu hình lên Git.
Script nạp cấu hình riêng cho cả hai backend, ghi đè giá trị mặc định trong source.

Sau khi cập nhật code, build cả hai backend:

```powershell
.\shop.cmd -Mode Build -Service Both
```

Chạy cả hai trong cùng một Terminal:

```powershell
.\shop.cmd -Mode Run -Service Both
```

- Client: `http://localhost:8081/`
- Client API: `http://localhost:8081/api/products`
- Admin: `http://localhost:8888/login`

Để chạy riêng, dùng `-Service Client` hoặc `-Service Admin`.
Sau khi khởi động lại máy, chạy lại lệnh Run. Nhấn Ctrl+C tại Terminal để dừng các dịch vụ đã khởi động từ lệnh đó.
Không chạy lại SQL export để khởi động web. Cơ sở dữ liệu hiện có cần áp dụng đến V014 để checkout dùng mã chống gửi trùng.
Nếu JAVA_HOME chưa được cấu hình, script thử tìm JDK ở vị trí Oracle SQL Developer trên máy;
máy khác cần đặt JAVA_HOME trỏ đến thư mục JDK 17 hoặc mới hơn, có `bin/javac.exe`.

## Thanh toán SePay

Đặt `SEPAY_SECRET_KEY`, `SEPAY_BANK` và `SEPAY_ACCOUNT_NUMBER` trong `.vscode/oracle-local.env` theo cấu hình tài khoản thụ hưởng của cửa hàng. Máy chủ Client tạo URL QR từ thông tin này. Khi cấu hình chưa đủ, khách vẫn có thể đặt COD. Webhook SePay phải cùng tài khoản và bí mật xác thực đã cấu hình.
## Ảnh sản phẩm

Đặt `CLOUDINARY_CLOUD_NAME`, `CLOUDINARY_API_KEY`, `CLOUDINARY_API_SECRET` trong cấu hình riêng của Admin để bật tải ảnh Cloudinary. Khi thiếu cấu hình, Admin trả lỗi rõ ràng và giao dịch cập nhật sản phẩm được rollback.