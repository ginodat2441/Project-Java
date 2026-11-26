# Bàn giao Clothing Shop — tồn vật lý và giá vốn SKU (V008)

Ngày triển khai: 08/11/2026. Bản chính: <http://localhost:8080/admin>. Bản thử dùng schema độc lập: <http://localhost:8081/admin>.

## Phạm vi và trạng thái

Đã sửa Java/Spring Boot, Oracle, API, quản trị và kiểm thử; đã áp dụng V008 lên dữ liệu DEMO sau khi chạy thử, đối chiếu, thử rollback và sao lưu. Không nạp lại DEMO, không reset sequence, không đổi ID/SKU, không đổi tài khoản/mật khẩu `hieu`, không thay cấu hình SMTP. Không reset/commit các thay đổi Git có sẵn. Không tìm thấy AGENTS.md trong workspace hoặc các thư mục cha đã kiểm tra.

Bản đầu: Java 17, Spring Boot 3.3.5, artifact `clothing-shop-0.0.1-SNAPSHOT.jar`, Git nền `c4663fd`. SHA-256 JAR trước nâng: `CDB23F24D246CDC1E9336A48755FE832EE2A7A488F018C50636670D965814251`. SHA-256 JAR V008 đang chạy: `CE4A7FFB578A8B0DE42A58A167A28805A000E15AEDF7BB377296AE2C906CEB19`. Bản JAR cũ được giữ để khôi phục; phiên bản artifact vẫn giữ nguyên, thay đổi nghiệp vụ được nhận diện bằng migration V008.

| Nhóm | Kết quả hiện tại | Xác minh |
|---|---|---|
| Tồn và giá vốn | Ba số dư riêng theo ID biến thể; bình quân trên hàng tại kho từng SKU; hàng giữ vẫn nằm trong giá trị tại kho; giá trị sẵn bán có nhãn riêng; theo dõi phần giá trị có căn cứ và SKU thiếu giá | Oracle acceptance, ràng buộc số dư DB, kiểm thử đồng thời, API, DOM |
| Migration và lịch sử | Snapshot trước nâng, DDL kiểm tra tồn tại, marker DML, tái dựng chứng từ theo SKU; giá vốn chung cũ không được gán cho SKU; chốt số dư hiện tại có chứng từ/lý do/người làm | Chạy trên schema sao chép, thử rollback, chặn rollback sau ghi mới, chạy lại migration, đối chiếu ID/SKU/số dư |
| Đơn và phiếu | Đặt chỉ giữ; hủy trước xuất giải giữ; nhân viên lập nháp xuất giao, ADMIN duyệt; chốt giá vốn lúc xuất; giao thất bại không tự hoàn kho; nhận hoàn/đảo hoàn/hoàn tiền riêng | Duyệt trùng, xuất/hoàn đồng thời, rollback đặt lỗi, quyền STAFF/ADMIN, hoàn vượt, đảo không an toàn |
| Tổng quan/thống kê/sản phẩm | Dùng cùng sự kiện doanh thu; giá lịch sử, chiết khấu và phí giao phân bổ VNĐ; COGS/lãi gộp/phần chưa xác định; tìm SKU, chi tiết màu/size; CSV mọi trang | Đối chiếu ba màn hình, tổng SKU/sản phẩm/kỳ, test ba dòng chia chiết khấu 1đ, CSV API và DOM |
| Tồn và lịch sử | Lọc hết/sắp hết/đang giữ/thiếu giá; lịch sử mở từ SKU, truy ngược phiếu/đơn; báo cáo giữ/giải giữ/xuất thực tế; CSV theo bộ lọc | API, kiểm tra tổng mọi trang, DOM, đánh dấu lịch sử cũ |
| Tài khoản/danh mục/voucher/nhật ký | Lọc nhân viên; thu hồi phiên; bảo vệ admin; quên mật khẩu; cây danh mục và phạm vi ẩn; hiệu lực/lượt voucher; nhật ký trước/sau có nhãn nghiệp vụ | Oracle/HTTP quyền, khóa phiên, khôi phục mật khẩu một lần, vòng lặp/ẩn cha, tranh lượt cuối, DOM |
| Giao diện thực tế | Có bản thử và bản chính; đã kiểm tra mã giao diện bằng DOM | **Chưa xác minh trực quan trong trình duyệt**, xem giới hạn cuối báo cáo |

## Số liệu đối chiếu DEMO

| Chỉ tiêu | Trước V008 | Sau V008 |
|---|---:|---:|
| Sản phẩm / SKU | 16 / 96 | 16 / 96 |
| Sẵn bán | 8.034 | 8.034 |
| Giữ cho đơn chưa xuất | 96 (suy từ đơn 0/1 đã giữ) | 96 (cột riêng) |
| Tại kho | 8.130 (suy diễn) | 8.130 (cột riêng) |
| Giá trị sẵn bán cũ | 1.171.790.000đ | Được tách riêng khỏi giá trị tại kho |
| Giá trị tại kho, gồm hàng giữ | Chưa có giá trị độc lập | **1.185.758.000đ** |
| SKU thiếu giá sau tái dựng DEMO | Không có chỉ tiêu riêng | 0 |
| Dòng lệch `sẵn bán = tại kho − giữ` | Chưa kiểm tra bằng constraint | 0 |
| ID/SKU bị đổi bởi migration | — | 0 |

Đối chiếu năm 2026 trên API chạy thật:

- Tổng quan = Thống kê = Sản phẩm = tổng các SKU = **44.283.154đ** doanh thu ròng.
- Giá vốn hàng bán: **19.115.000đ**; lãi gộp: **25.168.154đ**.
- Phí giao phân bổ theo SKU cộng lại đúng **1.260.000đ**, hiển thị riêng với doanh thu hàng.
- Các tổng giá trị theo bộ lọc tính trên toàn bộ kết quả, không chỉ trang đang xem. Cột tổng mã không được cộng lặp theo số biến thể.

## Các ca nghiệm thu số học

| Fixture | Kết quả kiểm thử |
|---|---|
| SP108-XANH-REU-XL: tại kho 90, giữ 5, sẵn bán 85, vốn 107.000đ | Giá trị tại kho **9.630.000đ**, giá trị sẵn bán **9.095.000đ**. Đã xác minh trực tiếp trên bản chính với **ID biến thể 280**; có thêm fixture Oracle và test DOM |
| NK000216: tồn cũ 20 thiếu giá + nhập 100 × 100.000đ | Tại kho **120**, phần lô mới có căn cứ **10.000.000đ**, giá trị toàn SKU **chưa đủ dữ liệu**, không lấy 100.000đ áp cho 20 hàng cũ |
| NK000216: tồn cũ 20 × 80.000đ + nhập 100 × 100.000đ | Giá trị **11.600.000đ**, bình quân nội bộ **96.666,66666667đ**, hiển thị **96.667đ/cái** |
| Hai màu/size cùng mã, nhập 107.000đ và 80.000đ | Giá vốn độc lập, không dùng chung bình quân mã |
| Xuất 5 tại 107.000đ, sau đó nhập 10 tại 200.000đ | Dòng xuất vẫn giữ 107.000đ; giá bình quân tồn mới thay đổi theo SKU |
| Giữ → hủy / giữ → xuất | Hủy không đổi tại kho; xuất giảm tại kho và giữ cùng lúc một lần; không trừ sẵn bán thêm lần hai |
| Hai lượt xuất/hoàn đồng thời | Chỉ một lượt hợp lệ được ghi; lượt trùng/vượt bị từ chối |
| Kiểm kê có hàng giữ | So sánh tại kho, giữ và phiên bản số dư; không cho tại kho thấp hơn giữ; đổi rồi trở lại cùng số vẫn bị phát hiện bằng phiên bản |
| Tăng kiểm kê thiếu giá / giá vốn 0 | Thiếu giá vẫn lưu số lượng và giá trị biết; giá 0 có chứng từ là số biết, không phải NULL; tồn 0 có giá trị 0 |
| Đảo nhận hoàn | Ghi phiếu có liên kết nguồn, đảo doanh thu tại ngày duyệt đảo; chặn nếu đã có giao dịch sau hoặc khoản hoàn tiền đang chờ/đã chi |
| Giao thất bại và nhận lại | Không tự cộng kho khi đổi trạng thái; chỉ chứng từ nhận hàng mới nhập lại; không giảm doanh thu chưa từng được ghi nhận |
| Ba dòng chia giảm giá 1đ, hoàn từng phần | Phần dư thuộc dòng cuối; tổng chi tiết và tổng đơn/kỳ khớp, không giảm giá hai lần |

### SP001-XAM-M trong bản sao lưu cũ

Đọc riêng các trường cần đối chiếu từ bản sao lưu trước DEMO cho thấy ID biến thể **61**, SKU `SP001-XAM-M`, `COLOR_ID=1` (tên lưu là **Đen**), size lưu **XL**. Chưa có chứng từ chứng minh SKU hay màu/size bên nào là đúng. Không đổi ID/SKU/màu/size chỉ dựa trên tên SKU; dữ liệu cũ này không được phục hồi đè lên DEMO. Không đưa dữ liệu xác thực trong bản sao lưu vào log.

## Quy tắc vận hành

- `PRODUCT_DETAIL.QUANTITY` tiếp tục là **sẵn bán** để tương thích API cửa hàng. Hai cột mới `PHYSICAL_QUANTITY`, `RESERVED_QUANTITY` lưu tại kho và giữ. DB chặn số dư âm, giữ vượt tại kho và sai công thức.
- `RESERVE`/`RELEASE` chỉ đổi phần giữ. `DISPATCH` là xuất giao thực tế: phiếu nháp không đổi số dư; ADMIN duyệt mới ghi kho, người duyệt và giá vốn. Tại chi tiết đơn bấm **Lập phiếu xuất giao** rồi mở phiếu để duyệt.
- `AVERAGE_COST` trên PRODUCT_DETAIL mới là giá vốn đang sử dụng. Cột PRODUCT.AVERAGE_COST cũ chỉ là dấu vết lịch sử. API bổ sung giá vốn chung V007 đã ngừng ghi.
- Giá vốn nội bộ dùng BigDecimal/NUMBER, 8 số lẻ; giá tiền VNĐ và giá vốn của dòng bán/hoàn ghi nhận làm tròn HALF_UP đến đồng. Phân bổ chiết khấu, phí giao và hoàn từng phần dùng phần dư để bảo toàn tổng.
- Kho thiếu giá vẫn lưu đầy đủ đơn giá/số lượng lô nhập và phần giá trị đã biết. Nếu xuất từ tồn trộn phần không biết, phần giá trị biết được phân bổ theo tỷ lệ; không chứng nhận đó là giá trị đầy đủ. Chỉ báo thiếu giá vẫn còn.
- **Đối chiếu giá vốn** theo ID SKU cho phép ADMIN chốt tại mốc hiện tại, kể cả đã bán/xuất. Form xem trước số dư, chứng từ và phạm vi ảnh hưởng; yêu cầu lý do/căn cứ và số lượng dự kiến. Không đổi số lượng hay giá vốn bán lịch sử; chỉ tác động số dư hiện tại và các lần xuất sau. Muốn sửa giá vốn quá khứ cần đối chiếu chứng từ và quy trình tái dựng riêng, không tự suy ra từ giá bán.
- Doanh thu tại ngày hoàn tất; hàng hoàn giảm ở ngày duyệt nhận; đảo nhận hoàn ghi bù ở ngày duyệt đảo. Phí giao riêng. Hàng hoàn lỗi không tăng hàng bán và giá vốn hàng lỗi được báo riêng.
- Nhận hàng hoàn, giá trị giảm trừ doanh thu và **tiền thực tế đã hoàn** là ba việc riêng. ADMIN ghi khoản PENDING/PAID/FAILED, số tiền, tham chiếu, thời gian và người làm; không cộng khoản này vào giảm trừ doanh thu lần hai. Tổng đang chờ/đã chi không vượt phiếu hoàn, mã tham chiếu không trùng. Nhân viên được xem nhưng không ghi tiền.
- Lịch sử `AVAILABLE_LEGACY` không chứng minh được chính xác thời điểm tại kho/giữ của dữ liệu cũ. Báo cáo không bịa số vật lý đầu/cuối cho phạm vi đó; dòng mới `PHYSICAL_V8` có số dư và giá trị trước/sau khi đủ giá.
- Ẩn danh mục cha làm cả nhánh không bán được trên catalog/checkout; khôi phục cha không tự bật danh mục con đã tự ẩn. Voucher hết hiệu lực tại đúng giờ kết thúc; lượt dùng không trả lại khi hủy, giữ quy tắc cũ và nêu trên màn hình.

## Migration, chạy lại và khôi phục

### Đã thực hiện

1. Sao chép schema đang chạy sang `CLOTHING_SHOP_STAGE`; không thay đổi schema nguồn khi thử.
2. Chạy `V008__sku_physical_inventory.sql`, tái dựng bằng `tools/OracleCostRebuild.java apply`, chạy kiểm thử.
3. Chạy rollback trên schema thử, rồi áp dụng lại. `VerifyV008Migration.java` kiểm tra thêm: rollback phải từ chối nếu có thay đổi được ghi nhật ký sau nâng; xóa riêng fixture kiểm tra rồi rollback và retry giữ nguyên 96 ID/SKU/sẵn bán.
4. Dừng tiến trình ứng dụng chính do tác vụ quản lý. Xuất Oracle Data Pump schema CLOTHING_SHOP, kiểm tra log `successfully completed`, chép bản sao riêng tư và đối chiếu hash.
5. Áp dụng V008, tái dựng chi phí trên schema chính, build và khởi động lại. Không chạy schema.sql/sample_data.sql, không nạp lại DEMO.

### Bản sao lưu

- `.local/backups/pre-v008-oracle.dmp`: bản xuất Oracle đầy đủ **2.596.864 byte**, kèm `pre-v008-oracle.log` xác nhận hoàn thành.
- `.local/backups/pre-v008-application.jar`: artifact trước nâng.
- Các bảng `V008_BACKUP_PD`, `BILL`, `BD`, `DOC`, `LINE`, `HISTORY`, `PRODUCT`, `AUDIT` (đều có tiền tố `V008_BACKUP_`) giữ số liệu trước DDL/DML.
- Thư mục `.local/backups` giới hạn quyền người dùng/SYSTEM, nằm ngoài Git. Không chia sẻ dump hoặc env vì chứa tài khoản/dữ liệu riêng tư.

### Cách chạy trên schema đã chuẩn bị

Trước mỗi migration: sao lưu, dừng mọi nguồn ghi và đối chiếu các đơn chưa xuất/đã giao với cách biểu diễn số dư cũ. Với schema khác chưa có V001–V007, cần DBA chuẩn bị theo thứ tự trước. Không áp dụng V008 lên một database không cùng mô hình cũ.

Nạp các biến `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD` từ tệp riêng tư tương ứng. `.local/stage.env` dành cho thử; `.vscode/oracle-local.env` dành cho bản chính. Không đưa mật khẩu vào tham số lệnh/log.

```powershell
# Ví dụ trên schema thử đã chuẩn bị, tại thư mục gốc dự án:
$java = Join-Path $env:USERPROFILE 'Downloads/sqldeveloper-26.2.0.186.2220-x64/sqldeveloper/jdk/jre/bin/java.exe'
$ojdbc = Join-Path $env:USERPROFILE '.m2/repository/com/oracle/database/jdbc/ojdbc11/23.7.0.25.01/ojdbc11-23.7.0.25.01.jar'
foreach ($line in Get-Content .local/stage.env) {
    if ($line -match '^([^=]+)=(.*)$') {
        [Environment]::SetEnvironmentVariable($Matches[1], $Matches[2], 'Process')
    }
}
& $java --class-path $ojdbc tools/OracleMigration.java backend_java/src/main/resources/db/migrations/V008__preflight.sql
& $java --class-path $ojdbc tools/OracleMigration.java backend_java/src/main/resources/db/migrations/V008__sku_physical_inventory.sql
& $java --class-path $ojdbc tools/OracleCostRebuild.java apply
# Chỉ trên *_STAGE, có thể chạy thử toàn bộ rollback/retry:
& $java --class-path $ojdbc tools/VerifyV008Migration.java
# Lệnh verification có rollback số giá, nên tái dựng lại sau đó:
& $java --class-path $ojdbc tools/OracleCostRebuild.java apply
```

Preflight từ chối số dư âm, đơn cũ chưa xuất nhưng thiếu giữ hàng, tham chiếu SKU đã xóa và số tại kho suy ra vượt giới hạn. Cần đối chiếu những trường hợp này trước khi chuyển dữ liệu.

DDL kiểm tra sự tồn tại từng cột/bảng; snapshot đầu tiên không bị ghi đè khi retry. DML chuyển số dư có marker V008, tái dựng có marker V008_COST/V008_COST_ROUND. Chạy lại không cộng giữ lần nữa hoặc lấy bình quân mã gán cho SKU. Nếu bị dừng giữa DDL, giữ nguyên snapshot và chạy lại khi vẫn dừng ghi; đối chiếu log/marker, không xóa marker để ép chạy trên dữ liệu đang hoạt động.

### Khôi phục

- **Chưa có ghi mới sau nâng:** dừng ứng dụng, chạy `V008__rollback_before_new_writes.sql` với JDBC runner rồi khởi động JAR cũ. Script từ chối khi có lịch sử vật lý mới hoặc nhật ký thay đổi mới. Đường này đã chạy thử trên schema riêng.
- **Đã có ghi mới:** không chạy rollback nhanh. Cần DBA chọn mốc phục hồi từ Data Pump, kiểm tra giao dịch phát sinh và nhập thử sang schema khác trước. Phục hồi hoàn chỉnh bằng Data Pump chưa được chạy đè lên schema chính; dump đã xuất thành công. Không làm mất giao dịch mới để trở lại bản cũ.
- Sau rollback, nếu lại có ghi bằng bản cũ, cần tạo bộ sao lưu/snapshot mới có kiểm soát trước lần nâng tiếp theo. Không dùng snapshot cũ như bản sao của dữ liệu mới. Trigger có thể ghi lại UPDATED_AT lúc phục hồi; bản snapshot giữ timestamp gốc.

## Kiểm thử và log

```powershell
.\shop.cmd -Mode Test
node --test tools/frontend.test.cjs tools/workflow.test.cjs
.\shop.cmd -Mode Build
```

- `Mode Test` hiện nạp `.local/stage.env`, bắt buộc schema thử; không chạy destructive seeder hoặc bộ Oracle V1 trên dữ liệu đang dùng.
- Java: **19 test chạy thành công**, không lỗi; 2 mục opt-in không chạy là seeder và bộ acceptance mô hình cũ. Gồm 8 ca tích hợp Oracle V008, 4 ca tính giá vốn và 7 ca admin đầu tiên/SMTP checker. Kiểm tra bổ sung chốt giá vốn khi tồn bằng 0 xác nhận cả số lưu và nhật ký đều bằng 0. Schema thử có **17 trigger hợp lệ và 43 khóa ngoại**, khớp schema chính.
- DOM: **19 test thành công**; dùng mã giao diện thật để kiểm tra form, ảnh, lọc, số liệu mẫu, phân quyền điều khiển và đường dẫn chứng từ. Không thay thế nghiệm thu trực quan.
- Log: `.local/final-java-tests.log`, `.local/frontend-v8-tests.log`, `.local/final-build.log`, `.local/migration-roundtrip-test.log`, `.local/live-migration-v008.log`, `.local/live-cost-rebuild.log`, `.local/live-v008-reconciliation.json`.
- API bản thử: Tổng quan, Sản phẩm, Báo cáo, Tồn kho, Đơn, Danh mục, Voucher, Nhân viên, Nhật ký, Lịch sử, NXT và Phiếu đều đã trả 200. Kiểm tra cuối trên hai cổng có **24 yêu cầu thành công**, ghi tại `.local/final-http-checks.json`. API CSV có content-type CSV UTF-8. Kiểm tra STAFF đọc hoàn tiền nhưng không ghi, không xem audit/đối chiếu giá vốn; thu hồi phiên sau khóa và reset mật khẩu.
- Khôi phục mật khẩu dùng delivery **local trên schema thử**, liên kết một lần và hết hiệu lực khi đổi phiên bản xác thực. Không gửi email thật/không thay SMTP của bạn.

## Nhóm file thay đổi

- Kho/giá vốn: `InventoryService.java`, `StockCostService.java`, `ProductMetricsService.java`, `InventoryReportService.java`, `InventoryController.java`, `SkuCostController.java`, các API opening-cost V007 ngừng ghi.
- Đơn/tiền: `BillService.java`, `BillRepository.java`, `RefundService.java`, `WorkflowController.java`, `AdminRequests.java`.
- Dùng chung báo cáo/lọc: `SaleAccounting.java`, `ReportService.java`, `AdminSearchService.java`, `ExportController.java`, `Csv.java`, `AdminController.java`.
- Quyền/danh mục/nhật ký: `SecurityConfig.java`, `AdminService.java`, `AdminRepository.java`, `CategoryVisibility.java`, `ProductRepository.java`, `StoreController.java`.
- Giao diện: `static/admin/index.html`, `admin-assets/admin.js`, `operations.js`, `workflow.js`, `admin.css`.
- DB/công cụ: migration V008 và rollback, `tools/OracleMigration.java`, `OracleCostRebuild.java`, `VerifyV008Migration.java`, `tools/shop.ps1`.
- Kiểm thử/tài liệu: `InventoryV8AcceptanceTest.java`, `DemoDataSeedTest.java` tương thích mô hình mới, `tools/workflow.test.cjs`, các test hiện có, tài liệu kho và báo cáo này.

## Giới hạn và bước xác minh còn lại

1. **Chưa kiểm tra trực quan trên Brave:** Computer Use bị cơ chế kiểm duyệt tự động dừng vì không xác định chắc chắn URL hiện tại. Không tiếp tục qua công cụ điều khiển trình duyệt khác. Cần bạn mở bản chính hoặc bản thử để xem bố cục ở độ phân giải thực tế; DOM/API đã kiểm tra như trên.
2. Dữ liệu cũ thiếu chứng từ không tự có giá vốn chính xác; ADMIN phải đối chiếu hóa đơn hoặc chốt số dư có căn cứ. Chốt hiện tại không tự sửa lãi gộp quá khứ. Trường hợp SP001-XAM-M cũ cần chứng từ để biết màu/size đúng.
3. Lịch sử tồn vật lý trước V008 không được khẳng định theo thời gian khi nguồn chỉ ghi sẵn bán; báo cáo có cờ/ô trống để chỉ rõ phạm vi.
4. SMTP gửi email thật và khôi phục Data Pump đè database chính không được thực hiện trong nghiệm thu này. Các luồng tiền là ghi nhận thủ công, không chuyển tiền qua ngân hàng.
