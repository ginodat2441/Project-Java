# Chức năng khách hàng

## COD và chọn sản phẩm trong giỏ

Đơn mới chỉ nhận COD. Phương thức thanh toán và các khoản tiền là dữ liệu do máy chủ quyết định; không có trường để khách nhập lại đơn giá/tổng tiền. Khách được sửa người nhận/địa chỉ trước khi đặt. Mỗi dòng giỏ có checkbox và có Chọn tất cả; tạm tính chỉ cộng các dòng được chọn. Trạng thái chọn lưu trong phiên trình duyệt theo email khách, còn sản phẩm/số lượng/giá vẫn đọc từ Oracle. Checkout kiểm tra lại các SKU được chọn thuộc giỏ của khách và số lượng chưa đổi; sau khi tạo đơn chỉ bỏ những dòng đã mua, giữ phần còn lại trong giỏ.

Đơn COD mới có trạng thái chưa thu tiền. Admin không được đổi phương thức hay trạng thái thu tiền trong form giao hàng; API cũng từ chối sửa các trường này. Xác nhận giao thành công đồng thời xác nhận đã thu COD, cập nhật trạng thái và ghi PAYMENT_HISTORY trong cùng giao dịch. Không ghi nhận thu tiền hai lần khi gửi lại chuyển trạng thái. Đơn đã kết thúc khóa sửa vận chuyển; đổi trả/hoàn tiền tiếp tục đi theo chứng từ nghiệp vụ hiện có. Dữ liệu phương thức thanh toán của đơn cũ được giữ nguyên, không tự chuyển đổi sang COD.

`SelectedCodOracleTest` kiểm tra đơn một phần trên Oracle thật, rollback toàn bộ sau kiểm thử, chỉ mô phỏng báo giá nhà vận chuyển. Mặc định không chạy; cần bật rõ `RUN_COD_ORACLE_TEST=true`. Không dùng phí kiểm thử làm cấu hình giao hàng thực tế.

Chạy từ backend Java, mở `/store/index.html`. Không dùng Live Server cho các API tài khoản/giỏ hàng. Trang gốc vẫn giữ HTML, ID, các section và `assets/css/main.css` của nhóm; nội dung tĩnh đã được Việt hóa. `assets/js/client-store.js` điền dữ liệu Java vào các vùng sản phẩm, giỏ và tài khoản có sẵn. `client-store.css` chỉ bổ sung trạng thái và bộ chọn.

- Header đọc `/api/customer/me`; đăng xuất là POST có CSRF. Session khách tách với admin.
- Đăng ký/đăng nhập dùng `client-auth.js`. Đăng ký lưu Oracle rồi yêu cầu đăng nhập riêng. Đăng nhập về trang chủ.
- Form đăng nhập cũng nhận tên tài khoản ADMIN/STAFF: gửi tới `/login` của Spring Security và chuyển `/admin` khi xác thực thành công. Khách vẫn dùng email và API customer. Không nhận vai trò do trình duyệt gửi; đăng ký công khai chỉ tạo khách hàng.
- Sản phẩm, giá, biến thể, tìm kiếm và bộ lọc đọc `/api/catalog`. Phải chọn màu và size có thật trước khi thêm giỏ.
- Giỏ lưu `CART`/`CART_DETAIL`, API `/api/customer/cart`. Máy chủ lấy khách từ session, không nhận customer ID của trình duyệt. Không chuyển giỏ localStorage cũ sang tài khoản mới. Thêm giỏ không giữ/trừ kho; tồn được kiểm tra lại khi đặt đơn.
- Hồ sơ, địa chỉ và đơn hàng chỉ lấy theo khách đang đăng nhập. Tài khoản mới có giỏ/đơn/sổ địa chỉ trống. Địa chỉ gắn vào hóa đơn là snapshot, không sửa qua sổ địa chỉ.
- Checkout sử dụng giỏ Oracle, báo giá vận chuyển và các service đơn hàng sẵn có. Nếu chưa cấu hình Viettel Post, không bịa phí hay báo đặt hàng thành công. Các chức năng mẫu chưa có API như ví, đánh giá, yêu thích không hiển thị dữ liệu giả.
- Link cũ `/shop/` chuyển về trang native; sửa lỗi viết nhầm `ctategory.html`.

Các lớp thêm nằm trong `controller/store`, `service/store`, `repository/store`, `dto/store`; không sửa service quản trị, schema, xử lý kho hay cấu hình bảo mật. Sau khi sửa Java, dừng server cũ trước khi build/run lại bằng `shop.cmd`.

Kiểm thử: `CustomerAuthTest`, `CustomerCartServiceTest`, `CustomerAccountServiceTest`, `tools/client-auth.test.cjs`, `tools/client-store.test.cjs`. Đã kiểm tra trình duyệt và Oracle thật với hai tài khoản thử riêng: giỏ được giữ sau reload, tài khoản thứ hai không thấy giỏ/đơn của tài khoản thứ nhất.

Sau khi import export có ID chỉ định, cần đồng bộ identity với `MAX(ID)` trước khi tạo dữ liệu mới; nếu không Oracle có thể báo ORA-00001 dù khách/giỏ là mới. Trên máy này đã đồng bộ bộ đếm các bảng dùng trong luồng khách, không xóa hay thay thế bản ghi. Không chạy lại schema/sample data trên database đã có dữ liệu.
