# Cấu trúc dự án

## Backend

Mã Java nằm trong `backend_java/src/main/java/com/example/clothingshop/`:

```text
controller/     HTTP endpoint, nhận request, trả response
service/        nghiệp vụ, kiểm tra dữ liệu, transaction
repository/     câu SQL, truy vấn và ghi dữ liệu Oracle
dto/            request/response, không chứa nghiệp vụ
model/          mô hình dữ liệu
config/         Spring Security, cấu hình web
integration/    kết nối Viettel Post, gửi thư khôi phục mật khẩu
bootstrap/      tạo admin đầu tiên, kiểm tra SMTP, dọn ảnh
util/           tiện ích theo nghiệp vụ
common/         exception, response, tiện ích JDBC dùng chung
```

Trong mỗi lớp thư mục tiếp tục chia theo nghiệp vụ: `account`, `auth`,
`product`, `order`, `inventory`, `report`, `shipping`, `promotion`, `store`…

Ví dụ quản lý ảnh:

```text
controller/product/ImageController.java
service/product/ImageService.java
repository/product/ProductOperationsRepository.java
```

Controller gọi service; service xử lý nghiệp vụ và gọi repository. Không viết
SQL trong controller hoặc service. Repository dùng JDBC và tham số bind,
không ghép giá trị người dùng trực tiếp vào SQL. Transaction đặt ở service.
Các repository `*OperationsRepository` nhóm thao tác SQL theo nghiệp vụ;
`AdminRepository` cung cấp cơ chế JDBC và nhật ký dùng chung.
`AdminService` là lớp chuyển tiếp tương thích; nghiệp vụ thực tế nằm trong
`ManagerService`, `CustomerService`, `CategoryService`, `ColorService`,
`ProductCommandService`, `VoucherService` và `InventoryMovementService`.

Các truy vấn báo cáo riêng nằm ở `repository/report/ReportRepository`,
`repository/product/ProductMetricsRepository` và
`repository/inventory/InventoryReportRepository`.

Test Java đặt ở `backend_java/src/test/java/`, theo package tương ứng;
test nghiệm thu xuyên chức năng đặt ở package gốc.

## Giao diện

Mã phục vụ trực tiếp nằm trong `backend_java/src/main/resources/static/`:

```text
admin/
  index.html                  khung trang quản trị
  css/admin.css               kiểu dáng quản trị
  js/
    app.js                    khởi tạo, gắn sự kiện chung
    core/                     trạng thái, API, UI chung, điều hướng
    pages/
      accounts/               khách hàng, nhân viên, phân quyền
      products/               danh sách, form sản phẩm, quản lý ảnh
      categories/             danh sách, bộ chọn danh mục
      orders/                 danh sách, chi tiết đơn, hoàn tiền
      inventory/              tồn kho, chứng từ nhập/xuất, lịch sử
      reports/                doanh thu, báo cáo nhập xuất tồn
      dashboard/              tổng quan
      vouchers/               voucher
      shipping/               trạng thái tích hợp Viettel Post
      audit/                  nhật ký quản trị
auth/js/                      đăng nhập, quên/đặt lại mật khẩu
shared/js/                    hiện/ẩn mật khẩu dùng chung
store/js/                     mua hàng, địa chỉ, báo giá vận chuyển
store/css/                    kiểu dáng cửa hàng
shop/index.html               trang cửa hàng
```

Các file trong `pages/` phụ trách màn hình tương ứng. `core/router.js` gọi
module của trang; không chứa toàn bộ danh sách sản phẩm, tài khoản và đơn hàng.
`form.js`, `images.js`, `detail.js` tách phần form, ảnh và chi tiết khỏi danh sách.
`editor-list.js` chứa danh sách và thao tác của đúng một chức năng.

HTML tải JavaScript bằng các thẻ `script defer` có thứ tự rõ ràng.
`admin/js/manifest.json` ghi nhóm file để bộ kiểm tra giao diện nạp đúng mã nguồn.
Các module vẫn dùng JavaScript thuần; không cần npm để chạy ứng dụng.

`static/store/assets/` là tài nguyên cửa hàng hiện có. `giao dien/` giữ nguồn
thiết kế cửa hàng ban đầu; giao diện quản trị không nằm chung trong đó.

## Chạy và kiểm tra

Tại thư mục gốc trong PowerShell:

```powershell
.\shop.cmd -Mode Build
.\shop.cmd -Mode Run
```

Mở `http://localhost:8080/login`.

Kiểm tra Java với schema nghiệm thu riêng đã cấu hình trong `.local/stage.env`:

```powershell
.\shop.cmd -Mode Test
```

Kiểm tra giao diện khi đã có Node và jsdom trong `.local/dom-tests`:

```powershell
node --test tools/frontend.test.cjs tools/workflow.test.cjs tools/shipping.test.cjs
```

Cấu hình bí mật tiếp tục nằm trong file môi trường cục bộ bị Git bỏ qua.
SQL schema và migration vẫn nằm trong `backend_java/src/main/resources/db/`.
Việc chia thư mục không yêu cầu tạo lại database hoặc xóa dữ liệu.
