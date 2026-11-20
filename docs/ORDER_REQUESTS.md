# Yêu cầu hủy / trả hàng của khách

- Khách đăng nhập gửi yêu cầu hủy đơn ở trạng thái 0 hoặc 1, chưa xuất giao. Gửi yêu cầu không hủy đơn hoặc giải phóng giữ hàng ngay.
- Khi có yêu cầu hủy chờ duyệt, hệ thống chặn chuyển trạng thái và cả lập/duyệt phiếu xuất giao. Chỉ ADMIN duyệt hoặc từ chối, kèm phản hồi 10–500 ký tự.
- Duyệt hủy chuyển đơn sang 4 và giải phóng giữ hàng qua nghiệp vụ kho hiện có. Đơn đã thu tiền vẫn giữ trạng thái đã thanh toán cho đến khi ADMIN ghi nhận thực tế hoàn tiền; không tự gọi chuyển tiền.
- Khách có thể yêu cầu trả hàng trong 15 ngày tính từ `BILL.COMPLETED_AT`, khi đơn đã giao thành công. Thời hạn được kiểm tra lúc gửi; yêu cầu đã gửi đúng hạn có thể được duyệt sau hạn.
- Duyệt trả chỉ ghi `RETURN_STATUS=REQUESTED`. Cửa hàng phải tiếp nhận/kiểm tra và duyệt phiếu RETURN theo đơn gốc để ghi nhận hàng hoàn, lựa chọn có nhập lại hàng bán hay không. Phiếu hoàn đang chờ duyệt yêu cầu khách chưa được ghi sổ.
- Hoàn tiền đơn trả căn cứ giá trị hàng nhận hoàn; phí giao không tự hoàn. Hoàn tiền đơn hủy trước xuất căn cứ phiếu RELEASE, tối đa tổng tiền đơn đã thu. Tổng khoản PAID và PENDING không được vượt hạn mức, mã tham chiếu không trùng.
- Khách xem yêu cầu, phản hồi, lịch sử và khoản hoàn ở đơn hàng. Dùng nút làm mới hoặc tải lại để lấy tiến độ mới.

## Database dùng chung

Schema dùng một file duy nhất: chạy `sql/ora_schema.sql` trước khi khởi động hai backend trên máy mới. File tạo bảng `BILL_CUSTOMER_REQUEST` và các index cần thiết; không thay đổi dữ liệu đơn cũ.

API client: GET/POST `/api/order-requests` (JWT, chỉ dữ liệu khách sở hữu). API admin: GET `/api/admin/order-requests`, PUT `/api/admin/order-requests/{id}` (ADMIN, CSRF). Đường PATCH `/api/bills/{id}/cancel` cũ không còn hủy trực tiếp; ứng dụng hướng dẫn gửi yêu cầu.

Kiểm thử Oracle mới được bật bằng `RUN_ORDER_REQUESTS_ORACLE_TEST=true`; các test dùng transaction rollback, không lưu đơn/yêu cầu thử nghiệm. Không tự chạy lại script seed dữ liệu.
