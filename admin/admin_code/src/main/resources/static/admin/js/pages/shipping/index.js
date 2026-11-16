"use strict";

(() => {
  const { params, known, isAdmin, kind, labels, dates, allRows, csv, valuation } =
    window.AdminV8Utils;
  const shipping = {
    async render() {
      const d = await api("/api/admin/shipping/status");
      return `<section class="card"><h2>Viettel Post</h2><p>Trạng thái: <b>${d.configured ? "Đã nhập cấu hình — cần kiểm tra báo giá thật" : "Chưa hoàn tất cấu hình"}</b></p><p>${d.configured ? "Cấu hình đã có. Cần xác minh một báo giá bằng tài khoản Viettel Post thực tế." : "Cần bổ sung các mục còn thiếu bên dưới để lấy phí giao hàng thực tế."}</p>${table(
        [
          { NAME: "Token API Viettel Post", READY: d.tokenConfigured },
          { NAME: "Địa chỉ kho gửi", READY: d.originConfigured },
          { NAME: "Khối lượng và kích thước kiện", READY: d.parcelConfigured },
        ],
        [
          ["NAME", "Thông tin"],
          ["READY", "Trạng thái", (v) => (v ? "Đã nhập" : "Còn thiếu")],
        ],
      )}<p>Môi trường: ${esc(d.environment === "production" ? "Chính thức" : d.environment === "development" ? "Thử nghiệm" : d.environment)}</p><h3>Cấu hình trên máy chủ</h3><p>Chạy trong PowerShell tại thư mục dự án:</p><pre>powershell -NoProfile -ExecutionPolicy Bypass -File .\\tools\\configure-viettel.ps1</pre><p>Nhập token ở dạng ẩn ngay trên máy, không gửi qua chat. Cần tài khoản Viettel Post được cấp API và địa chỉ kho thật.</p><p class="report-note">Chức năng hiện chỉ lấy báo giá, chưa tạo vận đơn hoặc đặt lịch lấy hàng. Thông báo “Còn thiếu” là trạng thái cấu hình, không phải lỗi hệ thống.</p><p>${esc(d.weightPolicy)}</p></section>`;
    },
  };
  window.AdminV8.shipping = shipping;
})();
