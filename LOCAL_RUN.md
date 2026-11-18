# Chạy trên máy cá nhân

Chạy các lệnh tại thư mục gốc dự án. Oracle phải đang hoạt động.

Cấu hình riêng nằm trong `.vscode/oracle-local.env` (Git bỏ qua):
`SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`.
Client cần `JWT_SECRET` ngẫu nhiên tối thiểu 32 ký tự. Không đưa file này lên Git.
Script nạp cấu hình riêng cho cả hai backend, ghi đè giá trị mặc định trong source.

Sau khi cập nhật code, build từng backend:

```powershell
.\shop.cmd -Mode Build -Service Client
.\shop.cmd -Mode Build -Service Admin
```

Mở hai Terminal riêng và chạy:

```powershell
.\shop.cmd -Mode Run -Service Client
```

```powershell
.\shop.cmd -Mode Run -Service Admin
```

Client API: `http://localhost:8080/api/products`.
Admin: `http://localhost:8888/login`.
Giao diện client: chuột phải `client/index.html` → **Open with Live Server**.
Không dùng đường dẫn `/store/index.html` của cấu trúc cũ.

Khi khởi động lại máy, chỉ cần chạy hai lệnh Run và mở Live Server.
Dừng bằng Ctrl+C tại Terminal tương ứng. Không chạy lại SQL export để khởi động web.
Nếu JAVA_HOME chưa được cấu hình, script thử tìm JDK ở vị trí Oracle SQL Developer trên máy;
máy khác cần đặt JAVA_HOME trỏ đến thư mục JDK 17 hoặc mới hơn, có `bin/javac.exe`.
