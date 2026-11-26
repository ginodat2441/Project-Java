# Bản hiện tại: V008

Xem [báo cáo bàn giao và hướng dẫn migration/khôi phục](V008_HANDOVER.md) và [mô hình kho hiện tại](backend_java/INVENTORY_COST.md). V008 đã thay việc đặt đơn trừ kho bằng giữ hàng riêng, ADMIN duyệt xuất giao và giá vốn theo SKU. Các ghi chép nghiệm thu V001–V007 bên dưới được giữ làm lịch sử, không dùng phần mô hình kho cũ để vận hành V008.

---

# Quản trị Clothing Shop

## Cấu hình Gmail SMTP bằng lệnh

Trong PowerShell tại thư mục dự án, chạy ` .\shop.cmd -Mode ConfigureSmtp `.
Lệnh hỏi địa chỉ Gmail và **mật khẩu ứng dụng Google 16 ký tự**, nhập ở dạng ẩn; không dùng mật khẩu đăng nhập Gmail, không gửi mật khẩu qua chat. Tạo tại https://myaccount.google.com/apppasswords sau khi bật xác minh hai bước. Cấu hình dùng `smtp.gmail.com:587`, STARTTLS bắt buộc và kiểm tra danh tính chứng chỉ máy chủ.

Lệnh kiểm tra đăng nhập SMTP **không gửi email**, chỉ lưu khi xác thực thành công. Cấu hình lưu trong `.vscode/oracle-local.env` ngoài Git, giữ nguyên các biến Oracle và đặt quyền tệp cho người dùng hiện tại/SYSTEM. Nếu kiểm tra thất bại thì không ghi SMTP mới. Sau khi thành công, lệnh tự khởi động lại backend JAR đang chạy để nạp cấu hình; nếu backend chạy từ IDE thì tự restart trong IDE. Cuối cùng mở `/forgot-password` và gửi yêu cầu đến tài khoản quản trị của bạn để kiểm tra thư đến.

Có thể kiểm tra lại kết nối bằng `.\shop.cmd -Mode TestSmtp`. Máy hiện chưa được cung cấp mật khẩu ứng dụng nên SMTP thật chỉ bật sau khi người dùng hoàn thành lệnh cấu hình. Đã kiểm tra cổng Gmail 587 có thể kết nối, kiểm thử xác thực thành công/thất bại bằng mock và kiểm thử ghi cấu hình/ACL bằng tệp giả; chưa xác thực tài khoản Gmail thật.

## Thống kê, phiếu kho và khôi phục mật khẩu (bản cập nhật 08/11/2026)

- **Thống kê doanh thu:** chọn từng ngày/tháng/quý/năm, lọc năm, tháng, quý hoặc khoảng ngày; lọc tên/mã sản phẩm. Có bảng đủ các kỳ kể cả kỳ bằng 0, biểu đồ, CSV và chi tiết doanh thu theo sản phẩm/màu/size. Ngày hoàn thành đơn là ngày ghi doanh thu; ngày xác nhận phiếu hoàn là ngày ghi giá trị hàng hoàn. Doanh thu ròng = tiền hàng − giảm giá − hàng hoàn, không gồm phí giao. Giảm giá được phân bổ về dòng hàng, xử lý phần làm tròn để hoàn đủ không dư/thiếu. Đây là giá trị doanh thu/hàng hoàn, không tự chuyển tiền hoàn qua ngân hàng.
- **Phiếu nhập / xuất / hoàn:** STAFF hoặc ADMIN lập/sửa/hủy phiếu nháp; chỉ ADMIN xác nhận ghi tồn hoặc lập phiếu đảo. Phiếu đã xác nhận không sửa/xóa, duyệt lặp bị từ chối. Có lý do, người lập/duyệt, thời gian, tồn trước/sau từng biến thể. Sửa phiếu nháp của người khác chỉ dành cho ADMIN.
- **Tồn mới bằng 0:** không cho nhập tồn ngay trong form sản phẩm hoặc gọi API điều chỉnh trực tiếp. Nhập hàng bằng phiếu RECEIPT; xuất khác bằng ISSUE, không xuất vượt tồn. COUNT kiểm kê số sẵn bán thực tế (loại trừ hàng đã giữ cho đơn); nếu tồn đã đổi từ lúc lập phiếu thì phải sửa/đếm lại trước khi duyệt. REVERSAL đảo đúng biến động của phiếu RECEIPT/ISSUE/COUNT; không được đảo lặp.
- **Bán / hủy:** hệ thống tự tạo chứng từ SALE khi giữ/trừ hàng cho đơn; CANCEL khi hủy trước giao/thanh toán thất bại và hoàn giữ tồn đúng một lần. Trang tồn tách tồn sẵn bán, số đang giữ cho đơn chưa giao và số còn tại kho. Báo cáo nhập xuất tồn ghi rõ đầu kỳ, nhập, xuất khác, xuất/giữ bán, hủy đơn, hoàn nhập, kiểm kê/đảo và cuối kỳ. Số dư cũ giữ nguyên; không bịa chứng từ quá khứ.
- **Nhận hàng hoàn:** mở đơn hoàn thành hoặc đơn đang yêu cầu trả → Lập phiếu nhận hàng hoàn. Chọn đúng biến thể, số lượng và tình trạng; bỏ “Nhập lại hàng bán” với hàng lỗi. ADMIN duyệt mới ghi nhận hàng hoàn. Không cho nhận vượt số đã bán chưa hoàn; hỗ trợ hoàn một phần/toàn bộ và chặn hai phiếu đồng thời hoàn vượt. Hàng lỗi vẫn tính giá trị hàng hoàn nhưng không tăng tồn bán. Không cho đảo phiếu nhận hoàn bằng chức năng đảo phiếu thủ công; phải kiểm tra kỹ trước khi duyệt.
- **Quên mật khẩu quản trị:** `/forgot-password`; liên kết dùng một lần, hết hạn 15 phút, lưu SHA-256 trong Oracle, không trả token qua API/log. Đổi mật khẩu thu hồi các phiên cũ và không tự mở khóa tài khoản. Nút Hiện/Ẩn có trên đăng nhập, đặt lại mật khẩu và các ô mật khẩu quản trị mới.

Máy hiện tại dùng **thư khôi phục cục bộ**: sau khi gửi yêu cầu, mở tệp `reset-*.txt` mới nhất trong `%USERPROFILE%\clothing-shop-private\password-reset`, rồi mở liên kết trong tệp. Thư mục ở ngoài Git, tệp chỉ cấp quyền cho chủ sở hữu. Liên kết chứa token trong fragment để tránh đưa token vào access log. Không gửi liên kết/mật khẩu qua chat. Chưa cấu hình SMTP nên chưa gửi email thật.

Muốn gửi email thật, đặt các biến dưới đây trong môi trường cục bộ hoặc `.vscode/oracle-local.env` (tệp đã bị bỏ qua bởi Git), rồi khởi động lại. Không ghi mật khẩu SMTP vào mã nguồn:

```text
SHOP_RESET_DELIVERY=smtp
SPRING_MAIL_HOST=<máy chủ SMTP>
SPRING_MAIL_PORT=587
SPRING_MAIL_USERNAME=<tài khoản gửi>
SPRING_MAIL_PASSWORD=<mật khẩu ứng dụng SMTP>
SHOP_RESET_FROM=<địa chỉ gửi>
SHOP_SMTP_AUTH=true
SHOP_SMTP_STARTTLS=true
SHOP_SMTP_SSL=false
SHOP_RESET_BASE_URL=http://localhost:8080
```

Đã kiểm thử Oracle thật: phiếu nháp không đổi tồn; STAFF không được duyệt; duyệt lặp/xuất vượt tồn bị từ chối; sửa/hủy/đảo phiếu; kiểm kê phát hiện số tồn đã đổi; hoàn một phần, hàng lỗi không nhập bán, hai phiếu hoàn đồng thời; báo cáo ngày/tháng/quý/năm và cân đối tồn; token hết hạn/dùng lại/tài khoản khóa bị từ chối, đổi mật khẩu thu hồi phiên. Chín kiểm thử DOM kiểm tra các form, bộ lọc báo cáo, duyệt phiếu có bước xác nhận và nút hiện mật khẩu. SMTP thật chưa kiểm thử do chưa có cấu hình; gửi thư cục bộ đã kiểm thử. Kiểm tra hiển thị bằng Chrome thật vẫn chưa thực hiện vì quyền điều khiển Chrome trước đó bị từ chối.

Mã sản phẩm mới tự tăng theo sequence Oracle, dạng SP004, SP005…; mã thật được cấp khi lưu. SKU biến thể mới tự sinh từ mã sản phẩm + tên màu không dấu + size, ví dụ SP004-DEN-M. Hai ô này chỉ đọc; SKU xem trước đổi khi chọn màu/nhập size. Backend bỏ qua mã/SKU do trình duyệt gửi và giữ mã/SKU cũ khi sửa để không thay đổi lịch sử. Khi có tên màu/size trùng dạng chuẩn hóa, backend thêm hậu tố số để SKU duy nhất. Sequence có thể nhảy số sau rollback/kiểm thử, không tái sử dụng số đã cấp.

Form sản phẩm hiện cho nhập thương hiệu/chất liệu trực tiếp (có gợi ý dữ liệu cũ), không chọn sẵn khi thêm mới. Tên mới được tạo cùng giao dịch lưu sản phẩm; tên đã có được dùng lại. Có thể chọn nhiều ảnh, xem trước, gán màu/thứ tự và chọn ảnh đại diện ngay trong form thêm/sửa. Khi lưu kèm ảnh, backend lưu toàn bộ sản phẩm, biến thể, ảnh và thương hiệu/chất liệu mới trong cùng transaction; ảnh lỗi sẽ rollback dữ liệu và dọn file vừa tạo. Không cần lưu nháp rồi mở hộp ảnh riêng.

## Chạy trên máy hiện tại

Từ thư mục `Project-Java`, dùng terminal tương tác:

Trước khi build lại, dừng backend đang chạy để Windows không khóa tệp JAR. Bản hiện tại đã build và đang chạy; có thể bỏ qua Build/Run và chỉ chạy CreateAdmin ở terminal thứ hai.

```bat
./shop.cmd -Mode Build
./shop.cmd -Mode CreateAdmin
./shop.cmd -Mode Run
```

`CreateAdmin` hỏi username, email và mật khẩu 12–72 ký tự trực tiếp trong terminal; mật khẩu không hiện trên màn hình, không truyền qua command line và không ghi vào Git. Chỉ khởi tạo khi chưa tồn tại tài khoản vai trò ADMIN, kể cả admin đã khóa. Nếu backend đang chạy, vẫn có thể chạy lệnh khởi tạo ở terminal thứ hai vì lệnh này không mở cổng HTTP. Không có mật khẩu mặc định.

Script đọc thông tin Oracle từ `.vscode/oracle-local.env`, chỉ nạp các biến datasource/upload được cho phép. Không sửa tệp này. Ưu tiên `JAVA_HOME`; trên máy hiện tại tự tìm được Java 17.0.19 trong SQL Developer. Ưu tiên `mvn` trong PATH; nếu chưa có thì dùng Maven 3.9.9 đã tải tại `.local/apache-maven-3.9.9`. Thư mục `.local` không vào Git. Máy khác cần JDK 17 và Maven trong PATH hoặc đặt `JAVA_HOME`.

Các địa chỉ, cùng một backend/cổng nên không cần chạy server frontend riêng:

- Đăng nhập quản trị: http://localhost:8080/login
- Quản trị: http://localhost:8080/admin
- Giao diện bán hàng gốc: http://localhost:8080/store/index.html
- Danh sách sản phẩm từ Oracle: http://localhost:8080/shop/index.html
- Tài khoản khách hàng: http://localhost:8080/shop/index.html?view=account
- Giỏ hàng thật: http://localhost:8080/store/cart.html?live=1
- Thanh toán thật: http://localhost:8080/store/checkout.html?live=1

Giao diện mẫu gốc vẫn giữ bố cục/nội dung. Nút **Mua hàng · Sản phẩm thực tế** mở luồng lấy dữ liệu Oracle. Chi tiết sản phẩm có `?live=1&id=...` và các trang giỏ hàng/thanh toán thật dùng phần nội dung động, giữ header/footer của mẫu. Phiên khách hàng riêng với phiên quản trị; đăng ký tài khoản khách để thử mua hàng. Tài khoản `khach1` trong dữ liệu mẫu cũ dùng hash giả nên không thể dùng để đăng nhập thật.

Nếu chạy từ VS Code, tiếp tục dùng cấu hình **Clothing Shop (Oracle)** đã có. Nếu chạy JAR thủ công, đặt thư mục làm việc là `backend_java` hoặc đặt `shop.storefront` đến thư mục `giao dien`.

## Migration và dữ liệu

Đã áp dụng trên Oracle `FREEPDB1`, schema `CLOTHING_SHOP` ngày 07/11/2026:

| Migration | Nội dung | Giờ Việt Nam |
|---|---|---|
| V001__admin.sql | CATEGORY, PRODUCT_IMAGE, VOUCHER, INVENTORY_HISTORY, ADMIN_AUDIT, SHOP_MIGRATION; liên kết danh mục; thông tin vận chuyển/voucher/giữ tồn; UNIQUE màu–size | 22:30 |
| V002__session_revocation.sql | AUTH_VERSION cho MANAGER và USERS để thu hồi phiên | 22:45 |
| V003__automatic_product_codes.sql | Sequence mã sản phẩm, bắt đầu sau mã SP lớn nhất; không sửa mã/SKU cũ | 08/11/2026 13:18 |
| V004__inventory_reports_recovery.sql | Phiếu kho/dòng phiếu, liên kết lịch sử, ngày hoàn thành và hàng hoàn, token khôi phục mật khẩu | 08/11/2026 |

Xem trạng thái bằng `SELECT VERSION, APPLIED_AT FROM SHOP_MIGRATION ORDER BY VERSION`.

Không chạy lại `schema.sql`, `sample_data.sql` hay migration đã áp dụng. Migration Oracle có DDL tự commit; phải sao lưu và áp dụng một lần theo thứ tự. Nếu triển khai sang database khác, đối chiếu cấu trúc và kiểm tra trùng biến thể trước:

```sql
SELECT PRODUCT_ID, COLOR_ID, SIZE_NAME, COUNT(*)
FROM PRODUCT_DETAIL
GROUP BY PRODUCT_ID, COLOR_ID, SIZE_NAME
HAVING COUNT(*) > 1;
```

Nếu có trùng, xử lý dữ liệu theo nghiệp vụ trước; migration sẽ dừng thay vì xóa dữ liệu. Nếu DDL gặp lỗi giữa chừng, kiểm tra object đã tạo trước khi tiếp tục, không chạy lại toàn bộ mù quáng. Bản ghi SHOP_MIGRATION chỉ được thêm sau khi migration hoàn tất.

Hai sản phẩm và ba biến thể cũ được giữ nguyên; sau nghiệm thu vẫn có 0 BILL và 0 MANAGER. Dữ liệu kiểm thử có tiền tố ngẫu nhiên và đã được dọn, không xóa dữ liệu shop. Sản phẩm cũ chưa có ảnh vẫn được giữ nguyên; khi lưu bằng form quản trị, muốn bật bán phải chọn ảnh đại diện và có biến thể đang bán. Danh mục của sản phẩm cũ có thể để trống.

## Chức năng

- ADMIN: quản lý khách hàng, nhân viên/vai trò, danh mục, sản phẩm/biến thể/ảnh, voucher, đơn hàng, tồn kho, báo cáo và nhật ký. STAFF: danh mục, sản phẩm, voucher, đơn, tồn, báo cáo; không được quản lý tài khoản hoặc đọc nhật ký quản trị.
- BCrypt cost 12; CSRF cho mọi thao tác ghi; đăng xuất bằng POST; phiên hết hạn sau 30 phút không hoạt động; giới hạn 5 lần đăng nhập thất bại trong 10 phút theo IP. Khóa/thay quyền tăng phiên bản quyền; phiên cũ mất hiệu lực cả khi sau đó mở khóa. Kiểm tra vai trò/trạng thái ở backend trên mỗi request quản trị. Không tự khóa/tự hạ quyền, không vô hiệu hóa admin hoạt động cuối cùng.
- API ghi cũ được bảo vệ; API hóa đơn chứa thông tin cá nhân chỉ dành cho quản trị. `/api/dev/**` luôn bị từ chối, không thể đọc SQL tùy ý. API tài khoản dùng danh sách cột an toàn, không trả hash. Lỗi database không trả SQL hoặc nội dung Oracle chi tiết. Tắt log tham số JDBC để tránh ghi mật khẩu/hash.
- Khách hàng có tìm kiếm tên/email/điện thoại, chi tiết và lịch sử đơn; khóa/mở khóa bằng trạng thái, giữ liên kết cũ. Tạo nhân viên, đổi ADMIN/STAFF và khóa/mở khóa.
- Danh mục cha/con, ẩn/khôi phục; chặn tự tham chiếu và vòng lặp. Form sản phẩm chọn danh mục, thương hiệu, chất liệu. Tìm kiếm, lọc danh mục/trạng thái và phân trang sản phẩm.
- Lưu sản phẩm và các biến thể cùng transaction. Bắt buộc ít nhất một biến thể; SKU chuẩn hóa chữ hoa và duy nhất; màu–size duy nhất trong sản phẩm; không cho giá/tồn âm. Giữ biến thể cũ, ngừng bán thay vì xóa. Không đổi màu–size của biến thể đã tạo để bảo toàn lịch sử đơn.
- Tồn chỉ thay đổi qua phiếu kho được duyệt hoặc chứng từ bán/hủy tự động, có lý do và lịch sử. Sản phẩm mới tồn 0; không sửa tồn trực tiếp trong form sản phẩm. Giá, quyền, tài khoản, voucher, trạng thái đơn và ảnh có nhật ký trong transaction.
- Upload nhiều ảnh có preview, thứ tự, ảnh đại diện, màu và gỡ ảnh. Kiểm tra chữ ký và giải mã JPEG/PNG/WebP; tối đa 5 MB/ảnh, 40 megapixel; tên UUID do server tạo. Ảnh theo màu dùng chung giữa các size; fallback ảnh chung/đại diện.
- Voucher phần trăm/cố định, ngày hiệu lực theo giờ địa phương của Oracle, đơn tối thiểu, trần giảm phần trăm, giới hạn lượt, bật/tắt. Oracle khóa/cập nhật có điều kiện, tính giảm và ghi lượt cùng transaction đặt đơn. Lượt đã dùng không được trả lại khi hủy, tránh tái sử dụng ngoài giới hạn.
- Đơn có tìm kiếm, lọc trạng thái/ngày, phân trang; chi tiết khách, địa chỉ snapshot, dòng hàng/màu/size, giá tại lúc mua, voucher, giảm, phí giao và tổng. Ghi nhận COD hoặc chuyển khoản thủ công, trạng thái đã nhận tiền, đơn vị vận chuyển/mã theo dõi. Không tích hợp cổng thanh toán bên ngoài.
- Tổng quan: doanh thu ròng theo tháng ghi nhận trong năm hiện tại, đơn theo trạng thái, bán chạy sau hàng hoàn, cảnh báo tồn. Báo cáo chi tiết có ngày/tháng/quý/năm. Bảng rỗng, đang tải và thông báo lỗi/thành công đã có.

## Quy tắc đơn hàng và tồn kho

Giỏ lưu ID `PRODUCT_DETAIL`. Checkout lấy khách từ phiên, không nhận customer ID tùy ý. Backend đọc giá Oracle, kiểm tra trạng thái khách/sản phẩm/biến thể; số lượng phải là số nguyên dương. Phí giao cố định 30.000 ₫ do shop quy định; bỏ qua phí/tổng/giá từ trình duyệt. Tiền giảm tính lại từ voucher trên server.

Khi đặt đơn, khóa biến thể theo thứ tự ID và trừ tồn bằng UPDATE có điều kiện. Cùng transaction ghi đơn, dòng hàng, lịch sử, chuyển động tồn và lượt voucher; lỗi ở bước nào rollback toàn bộ. Thứ tự khóa nhất quán giúp giảm deadlock khi có nhiều dòng hàng.

Các chuyển trạng thái hợp lệ:

```text
Chờ xác nhận (0) → Đã xác nhận (1) → Đang giao (2) → Hoàn thành (3) → Trả hàng (5)
          0 hoặc 1 → Đã hủy (4)
```

Hủy hoặc ghi nhận thanh toán thất bại trước khi giao dùng cùng luồng hủy. Khóa dòng BILL và đổi STOCK_RESERVED từ 1 sang 0, rồi hoàn đúng các biến thể một lần. Yêu cầu hủy lặp bị từ chối. Đơn cũ trước migration không được tự đánh dấu đã giữ tồn, tránh bịa lịch sử/hoàn tồn sai.

Hoàn thành COD tự ghi nhận đã thanh toán; chuyển khoản phải ghi nhận nhận tiền trước khi hoàn thành. Trạng thái yêu cầu trả không tự tăng tồn: nhận hàng bằng phiếu RETURN gắn đơn gốc và ADMIN xác nhận. Không tự chuyển tiền hoàn hoặc nhập bán lại hàng chưa kiểm tra. Đơn đã kết thúc không sửa thông tin giao/thu tiền; không đổi đã thu tiền về chưa thu tiền.

## Ảnh và bảo mật cấu hình

Mặc định ảnh lưu tại `%USERPROFILE%/clothing-shop-uploads`, ngoài mã nguồn. Có thể đặt `SHOP_UPLOAD_DIR` trong cấu hình môi trường cục bộ. Oracle chỉ lưu URL/thứ tự/màu. Không dùng tên file khách cung cấp; URL file chỉ nhận tên UUID và đuôi ảnh hợp lệ.

Gỡ ảnh chỉ xóa liên kết Oracle. Chạy `./shop.cmd -Mode CleanImages` để dọn file server tạo đã quá 24 giờ và không còn được tham chiếu từ PRODUCT_IMAGE, PRODUCT hoặc PRODUCT_DETAIL; file còn được sản phẩm khác dùng sẽ được giữ. Không tự xóa ảnh cũ bên ngoài thư mục upload.

## Kết quả nghiệm thu

Đã chạy Maven và ứng dụng trên Java 17.0.19, Spring Boot 3.3.5, Oracle thật. Bộ `OracleAcceptanceTest` khởi động HTTP server thật trên cổng ngẫu nhiên, gọi HTTP qua cookie/CSRF và đối chiếu SQL, sau đó dọn chính các fixture của nó. Không mock database.

Đã qua: truy cập chưa đăng nhập; đăng nhập đúng/sai; CSRF; STAFF không được quản lý tài khoản; khóa mất quyền phiên; không tự khóa admin; API không lộ hash; khóa/mở khách và buộc đăng nhập lại; vòng lặp danh mục; tạo ba biến thể; trùng SKU và màu–size; rollback sản phẩm lỗi; upload PNG và từ chối ảnh giả; chọn ảnh đại diện/gán màu/gỡ cover khi đang bán bị từ chối; giá/phí do server tính; voucher hợp lệ/hết hạn/chưa hiệu lực/chưa đạt tối thiểu/hết lượt; hai đơn cạnh tranh tồn; hai đơn cạnh tranh lượt voucher; trạng thái đúng/sai; hủy lặp không hoàn tồn hai lần; tồn biến thể khác giữ nguyên; điều chỉnh âm vượt tồn bị từ chối; checkout lấy đúng customer từ phiên; đối chiếu số đơn dashboard với SQL.

Chạy lại:

```bat
./shop.cmd -Mode Test
```

Mặc định `mvn test` bỏ qua bộ Oracle để tránh tự sửa database nếu chưa chủ động bật `RUN_ORACLE_TESTS=true`. Script Test bật biến này và dùng upload test trong `.local`. Kết quả nghiệm thu được ghi tại `.local/java17-test.log`; build tại `.local/build.log`.

Hai test DOM dùng mã JavaScript thật đã qua: form admin gửi ba biến thể; gallery đổi ảnh theo màu, giữ ảnh khi đổi size, fallback ảnh chung và giỏ lưu ID biến thể. Cài `jsdom@26.1.0` bằng `npm install --prefix .local/dom-tests jsdom@26.1.0 --no-audit --no-fund`, rồi `node --test tools/frontend.test.cjs`. Không cần Node để chạy cửa hàng.

HTTP trên backend cổng 8080 trả 200 cho login, storefront gốc, shop thật, catalog và CSRF. **Chưa xác minh bố cục và thao tác bằng Chrome thật:** bộ phê duyệt tự động từ chối điều khiển Google Chrome. Test DOM không thay thế xác nhận hiển thị trên trình duyệt. Cần tự mở URL, tạo admin bằng lệnh trên rồi thử các trang; không cần gửi mật khẩu qua chat.

## Giữ công việc có sẵn

Giữ nhánh `anh-hieu`, không commit/push. Giữ thay đổi ban đầu của `pom.xml` (ojdbc11 23.7.0.25.01), `SqlHelper.java` (không sửa thêm) và cấu hình FREEPDB1/mật khẩu từ biến môi trường trong `application.properties`. Các phần bảo mật được thêm vào cấu hình hiện có. Không sửa `.vscode/oracle-local.env` hay `.vscode/launch.json`; `.vscode`, upload và `.local` được bỏ qua bởi Git. Giao diện mẫu chỉ thêm liên kết và nạp script cho luồng dữ liệu thật.
