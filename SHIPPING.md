# Viettel Post

## Tình trạng tích hợp

Client chọn địa chỉ từ danh mục Viettel Post. Client tách module vẫn tính phí giao cố định theo `app.order.shipping-fee`. Admin lấy báo giá theo đơn thật trước khi chọn dịch vụ vận chuyển. Module cửa hàng tích hợp dùng báo giá trực tiếp ở bước checkout.

Danh mục và báo giá đã được kiểm tra với API thật. Tạo vận đơn được kiểm thử bằng phản hồi mô phỏng và cần vận đơn thật do Admin xác nhận để kiểm tra trên tài khoản của shop. Để nhận hành trình tự động, cấu hình URL HTTPS của webhook trong hệ thống Partner. [Tài liệu tích hợp Viettel Post](https://partner2.viettelpost.vn/document/detail-instructions)

## Cấu hình tại máy

Đăng ký hoặc sử dụng tài khoản Viettel Post thật được cấp API. Shop tự nhập thông tin nhận diện/tài khoản khi Viettel yêu cầu. Không đưa mật khẩu hay token vào chat.

Tại thư mục dự án, PowerShell:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\configure-viettel.ps1
```

Lệnh hỏi môi trường, địa chỉ kho thật, token ở dạng ẩn, gram mỗi sản phẩm, bao bì và kích thước kiện. Không mặc định địa chỉ/khối lượng giả. Các thông số cần được cân/đo và xác nhận phù hợp hàng bán; cách hiện tại là khối lượng theo mỗi sản phẩm + bao bì, chưa có trọng lượng riêng từng SKU hay thuật toán chia kiện. Đơn vượt 30kg bị từ chối để shop tư vấn chia kiện. Với hàng có trọng lượng khác nhau đáng kể cần mở rộng trọng lượng theo SKU trước khi nhận đơn thật.

Cấu hình lưu tại `.vscode/viettel-local.env`, ngoài Git, ACL người dùng hiện tại và SYSTEM. File này không sửa cấu hình Oracle/SMTP. `shop.cmd` nạp hai file khi khởi chạy. Lệnh chỉ lưu cấu hình, không khẳng định token hợp lệ. Dừng backend đang chạy rồi chạy:

```powershell
.\shop.cmd -Mode Run
```

ADMIN xem **Vận chuyển Viettel Post** để biết token/kho/đóng gói đã cấu hình hay còn thiếu. Giao diện chỉ hiển thị cờ trạng thái, không đọc token hoặc địa chỉ kho. Nhân viên không có quyền API cấu hình này.

## Checkout

- Mở `/shop/index.html?view=checkout` hoặc `/store/checkout.html?live=1` với giỏ hàng và đăng nhập khách.
- Chọn một trong 34 tỉnh/thành, nhập phường/xã và số nhà/đường. Danh sách tỉnh theo [Quyết định 19/2025/QĐ-TTg trên Báo Chính phủ](https://baochinhphu.vn/bang-danh-muc-va-ma-so-cua-34-tinh-thanh-moi-3321-don-vi-hanh-chinh-cap-xa-moi-102250704153652947.htm); đây là danh sách địa phương, **không phải bảng phí tự đặt**. Phường/xã hiện nhập tên, chưa có danh mục phường từ Viettel.
- Địa chỉ nhận, thông số kiện và tiền hàng/thu hộ được gửi server-to-server để hỏi giá. Không gửi họ tên/điện thoại của khách trong request tính cước.
- Chọn dịch vụ theo báo giá trả về. Đổi địa chỉ, số lượng, voucher hoặc phương thức thanh toán sẽ bỏ cước cũ; phải tính lại. Phí không có/lỗi hãng/thiếu cấu hình không được đổi thành 0 hay 30.000đ giả.
- Khi đặt, server gọi lại hãng và đối chiếu dịch vụ, mức phí khách đã xem. Cước đổi thì báo khách tính lại, không âm thầm tính khác. Giá/voucher đổi thì rollback toàn đơn. Không nhận `shippingFee` do trình duyệt tự tính làm giá chuẩn.
- Phí giao snapshot tại BILL.SHIPPING_FEE; thêm SHIPPING_SERVICE, SHIPPING_SERVICE_NAME, SHIPPING_QUOTED_AT. Đơn cũ giữ nguyên số phí đã chốt. Phí giao không đưa vào doanh thu hàng.
- API `/api/bills` cũ dành cho quản trị/test vẫn giữ chính sách phí 30.000đ cho tương thích fixture và nghiệp vụ cũ; **checkout khách luôn đi qua báo giá Viettel mới**, không dùng API cũ này để xác nhận cước thật. Chưa có tạo vận đơn hãng, tracking hay webhook tự động.
- Khoản COD gửi hỏi cước là tiền hàng sau giảm (shop trả phí vận chuyển cho hãng). Trước production cần đối chiếu đúng loại hợp đồng, phụ phí và cách thu cước/COD với Viettel; không tự đồng nhất phí shop thu với hóa đơn hãng ngoài phản hồi báo giá.

## DB và kiểm thử

Migration cộng thêm `V009__carrier_quote.sql` có kiểm tra từng cột để chạy lại an toàn. Không đổi số dư, giá vốn, đơn cũ hoặc SKU. Đã chạy/retry trên CLOTHING_SHOP_STAGE trước khi áp dụng bản chính. Muốn quay lại ứng dụng cũ có thể giữ các cột bổ sung; không cần xóa dữ liệu để rollback giao diện. Không bỏ cột khi đã có đơn mới chứa snapshot.

Kiểm thử:

```powershell
.\shop.cmd -Mode Test
node --test tools/frontend.test.cjs tools/workflow.test.cjs tools/shipping.test.cjs
```

`ShippingQuoteServiceTest` dùng HTTP server localhost: kiểm tra header riêng tư, payload gram/COD/địa chỉ, phản hồi dịch vụ, token hết quyền, dữ liệu sai/âm, không cấu hình và giới hạn kiện. `ShippingCheckoutTest` xác nhận cước đổi/dịch vụ giả không tạo địa chỉ/đơn và snapshot đúng dịch vụ. `shipping.test.cjs` chạy JavaScript checkout thật với phản hồi giả để kiểm tra vô hiệu cước cũ và payload đặt đơn.

**Chưa xác minh:** xác thực tài khoản hãng thật, báo giá thật hai tỉnh khác nhau, đối chiếu response chính thức theo hợp đồng được cấp, độ chính xác đóng gói thực tế, UI trực quan và danh mục phường/xã từ hãng. Cần token + kho + thông số thật và quyền API để hoàn thành các mục này. Không gửi thông tin giả ra ngoài để vượt qua bước còn thiếu.

## Đồng bộ hành trình vào đơn hàng

Áp dụng `V011__viettel_order_tracking.sql` trên Oracle. Trong Admin, mở chi tiết đơn và gắn **Mã vận đơn Viettel Post** đã có. Mỗi mã chỉ gắn với một đơn. Trạng thái hãng và thời điểm sự kiện được hiển thị trong chi tiết đơn. Webhook mã 501 tự hoàn thành đơn đang giao đủ điều kiện. COD vẫn cần Admin đối chiếu và xác nhận đã nhận tiền. Hàng hoàn được xử lý theo quy trình kho.

Đặt một bí mật riêng tối thiểu 32 ký tự trong `SHOP_VIETTEL_WEBHOOK_SECRET` tại `.vscode/viettel-local.env` hoặc cấu hình môi trường của Admin. Trong Partner Viettel Post, cấu hình webhook bằng URL HTTPS của Admin, đường dẫn `/api/shipping/viettel-webhook`, và đặt **Secret parameter** trùng với bí mật này. Dùng chức năng kiểm tra kết nối của Partner trước khi bật thật. Dịch vụ local chỉ lắng nghe loopback, nên Viettel Post cần một URL HTTPS có thể truy cập để đẩy sự kiện.
## Tạo vận đơn từ đơn khách đặt

Áp dụng `V012__viettel_dispatch.sql`. Thêm `SHOP_SHIPPING_SENDER_NAME` và `SHOP_SHIPPING_SENDER_PHONE` vào `.vscode/viettel-local.env` bằng thông tin người gửi đã đăng ký với hãng, rồi khởi động lại Admin. Khách đặt đơn như bình thường. Admin xác nhận đơn, duyệt phiếu xuất giao, rồi mở chi tiết đơn ở trạng thái **Đang giao**. Bấm **Lấy cước theo đơn**, chọn **Dịch vụ Viettel Post** và bấm **Tạo vận đơn Viettel Post**. Shop thanh toán cước cho Viettel Post. Với COD chưa thu, hãng được yêu cầu thu tổng tiền của đơn trên shop.

Nếu hãng không trả kết quả, tra mã đơn của shop trên Viettel Post trước. Khi đã có mã vận đơn, dùng **Gắn mã vận đơn** để liên kết lại. Nếu hãng chưa tạo vận đơn, bấm Gửi lại Viettel Post. Hệ thống gửi cùng mã đơn bán với CHECK_UNIQUE và chỉ cho một lần gửi đồng thời.
## Liên kết đơn bán, phiếu xuất và vận đơn

Chạy lần lượt V011, V012 và V013 nếu chưa áp dụng. V013 bổ sung kho xuất, liên kết chứng từ của đơn giao, trạng thái gửi và lịch sử Viettel Post. Cấu hình `SHOP_INVENTORY_WAREHOUSE_NAME` nếu cần đổi nhãn kho chính của dự án. Khi Admin xác nhận đơn, hệ thống tạo phiếu xuất và đơn giao nháp cùng giao dịch. Duyệt phiếu xuất ghi tồn. Chọn dịch vụ và xác nhận gửi để tạo vận đơn thật. Mã do Viettel Post cấp cùng lịch sử webhook được hiển thị tại phần Vận chuyển của đơn.