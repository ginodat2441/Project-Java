# Reverse proxy cho ngrok

Proxy dùng Python 3 và thư viện có sẵn, không cần cài package:

- `http://localhost:3000/api` và mọi đường dẫn bắt đầu bằng `/api/` được chuyển tiếp tới backend `http://127.0.0.1:8080`.
- Các đường dẫn khác được chuyển tiếp tới Live Server tại `http://127.0.0.1:5500`, không được đọc từ thư mục giao diện trực tiếp.
- Nếu Live Server đang phục vụ từ thư mục gốc của dự án, mở URL ngrok với đường dẫn `/giao%20dien/` (ví dụ `https://<domain-ngrok>/giao%20dien/`). Nếu Live Server đang phục vụ trực tiếp thư mục `giao dien`, mở URL ngrok ở đường dẫn `/`.
- Giao diện gọi API cùng origin khi truy cập qua ngrok hoặc cổng 3000. Khi chạy bằng Live Server ở `localhost:5500`, giao diện gọi backend `localhost:8080`.

## Chạy

1. Chạy Spring Boot backend tại cổng `8080`.
2. Chạy `giao dien/index.html` bằng Live Server trên cổng `5500` (không mở trực tiếp bằng `file://`).
3. Nhấp đúp `start_proxy.bat`. Proxy sẽ lắng nghe tại cổng `3000`; nếu tìm thấy ngrok trong `PATH`, file này cũng mở `ngrok http 3000`.
4. Nếu ngrok chưa được cài hoặc chưa có trong `PATH`, chạy `ngrok http 3000` trong terminal riêng sau khi cấu hình ngrok.
5. Mở URL HTTPS do ngrok cung cấp, thêm `/giao%20dien/` nếu Live Server phục vụ từ thư mục gốc của dự án.

Có thể đổi địa chỉ frontend, backend hoặc cổng proxy bằng biến môi trường `FRONTEND_HOST`, `FRONTEND_PORT`,
`BACKEND_HOST`, `BACKEND_PORT`, `PROXY_HOST`, `PROXY_PORT`.
