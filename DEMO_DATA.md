# Dữ liệu demo cửa hàng

**Cập nhật V008:** bộ dữ liệu dưới đây được giữ nguyên ID/SKU/đơn khi nâng mô hình kho. Đặt đơn mới chỉ giữ hàng; nhân viên lập phiếu xuất giao và ADMIN duyệt mới giảm tại kho. Giá vốn và giá trị tồn hiện tính riêng từng SKU, gồm hàng đang giữ. Xem [báo cáo V008](V008_HANDOVER.md). Nếu tạo lại DEMO bằng seeder mới, sẽ có thêm chứng từ giữ/xuất tách riêng so với số chứng từ của bộ DEMO ban đầu mô tả bên dưới.

Đã thay dữ liệu nghiệp vụ cũ bằng dữ liệu giả ngày 08/11/2026. Tài khoản quản trị `hieu` và mật khẩu hiện tại được giữ nguyên. Toàn bộ dữ liệu cũ được sao lưu trong `.local/backups/before-demo-*.json`, thư mục riêng tư, không đưa vào Git.

## Tài khoản dùng thử

| Tài khoản | Vai trò | Mật khẩu |
|---|---|---|
| `hieu` | Quản trị viên | Mật khẩu hiện tại của bạn |
| `demo_staff` | Nhân viên | `DemoShop@2026!` |
| `demo_locked` | Nhân viên đã khóa | `DemoShop@2026!` |
| `demo_khach01` … `demo_khach23` | Khách hàng | `DemoShop@2026!` |
| `demo_khach24` | Khách hàng đã khóa | `DemoShop@2026!` |

Email có đuôi `example.test` là địa chỉ giả, không nhận thư thực tế. Cấu hình SMTP hiện có được giữ nguyên. Chức năng quên mật khẩu của quản trị viên dùng email thật của tài khoản `hieu`.

## Dữ liệu để xem

- **Sản phẩm:** 16 mã, 96 biến thể, 6 màu, 32 ảnh lấy từ tài nguyên cửa hàng có sẵn. Có nhiều thương hiệu/chất liệu/danh mục, sản phẩm đang bán, sắp hết hàng và bản nháp. Mã sản phẩm/SKU vẫn dùng bộ sinh tự động, không đặt lại sequence.
- **Khách hàng:** 24 hồ sơ, địa chỉ, một tài khoản bị khóa, một giỏ hàng mẫu.
- **Đơn hàng:** `DEMO-HD-001` đến `DEMO-HD-120`; 12 chờ xác nhận, 12 đã xác nhận, 12 đang giao, 48 hoàn tất, 12 hủy và 24 ở nhóm trả hàng. Nhóm trả hàng có yêu cầu chờ nhận, hoàn một phần và hoàn toàn bộ.
- **Kho:** 203 chứng từ gồm 29 phiếu nhập đã xác nhận, 1 nhập nháp, 1 nhập đã hủy, 2 xuất, 1 kiểm kê, 1 đảo phiếu, 36 phiếu nhận hoàn, cùng chứng từ bán/hủy đơn tự động. Các lần nhập khác đơn giá để xem giá vốn bình quân; hàng hoàn có cả nhập lại bán và hàng lỗi không nhập lại.
- **Voucher:** `DEMO10` giảm 10%, `DEMO50K` giảm 50.000đ; thêm mã hết hạn, chưa mở và đã tắt để thử thông báo.
- **Thanh toán:** có COD và chuyển khoản, thông tin vận chuyển/mã theo dõi, lịch sử thanh toán và lịch sử xử lý đơn.

## Các màn hình nên thử

1. **Sản phẩm / Tồn kho:** xem tổng giá trị, chọn từng mã, lọc khoảng tiền và hàng tồn dưới 5. Mũ lưỡi trai có tồn thấp; túi canvas là bản nháp chưa nhập.
2. **Đơn hàng:** tìm mã `DEMO-HD-...`, lọc từng trạng thái, mở chi tiết xem giá thực bán, voucher, lịch sử và chứng từ kho. Có thể xử lý tiếp các đơn chờ và đang giao.
3. **Phiếu nhập/xuất/hoàn:** lọc nháp để thử xác nhận; lọc phiếu đã hủy/đảo; xem chi phí nhập và biến động trước/sau.
4. **Thống kê:** chọn năm 2025 hoặc 2026; thử tháng, quý, cả năm và khoảng ngày. Từ 01/11/2026 đến 08/11/2026 có đơn theo từng ngày. Lọc một sản phẩm để đối chiếu doanh thu sau giảm giá và hàng hoàn.
5. **Báo cáo nhập xuất tồn:** chọn từ 01/01/2025 đến 08/11/2026 hoặc tháng 11/2026 để xem số đầu/cuối kỳ, bán, hủy và hoàn.
6. **Tài khoản / Nhật ký:** kiểm tra vai trò nhân viên, tài khoản khóa và các thao tác demo được ghi lại.

Các phiếu và đơn được tạo bằng service nghiệp vụ trong một giao dịch. Ngày chứng từ được đặt giả để thử thống kê lịch sử; không có giao dịch thanh toán hay email thực tế nào được gửi.

## Tạo lại bộ demo khi cần

Thao tác này xóa dữ liệu nghiệp vụ đang có và tạo lại bộ demo; không dùng trên dữ liệu thật. Seeder chỉ chạy khi bật rõ `RUN_DEMO_SEED=REPLACE_WITH_FAKE_DATA`, và luôn xuất bản sao lưu trước khi xóa. File thực hiện: `backend_java/src/test/java/com/example/clothingshop/DemoDataSeedTest.java`. Chạy test/build thông thường không kích hoạt seeder.
