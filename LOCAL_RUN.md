# Chạy trên máy cá nhân

Chạy các lệnh tại thư mục gốc dự án. Oracle phải đang hoạt động.

Cấu hình riêng nằm trong `.vscode/oracle-local.env` (Git bỏ qua):
`SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`.
Client cần `JWT_SECRET` ngẫu nhiên tối thiểu 32 ký tự. Không đưa file này lên Git.
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
Không chạy lại SQL export để khởi động web.
Nếu JAVA_HOME chưa được cấu hình, script thử tìm JDK ở vị trí Oracle SQL Developer trên máy;
máy khác cần đặt JAVA_HOME trỏ đến thư mục JDK 17 hoặc mới hơn, có `bin/javac.exe`.
