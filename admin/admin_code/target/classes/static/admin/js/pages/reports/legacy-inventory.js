"use strict";

(() => {
  const { types, states, admin, choose, today, year, query } = window.AdminModulesUtils;
  async function renderInventoryReport() {
    const data = await api("/api/admin/inventory/summary?" + query({ page }));
    return `<form id="filter" class="toolbar">${input("from", "Từ ngày", filter.from || today().slice(0, 8) + "01", "date")}${input("to", "Đến ngày", filter.to || today(), "date")}${input("q", "SKU / tên sản phẩm", filter.q || "", "text", false)}<button>Xem nhập xuất tồn</button></form><p class="report-note">Báo cáo theo tồn sẵn bán; xuất bán gồm lượng giữ cho đơn đã đặt. Tồn cuối = tồn đầu + nhập − xuất thủ công − xuất bán + hủy đơn + hoàn nhập lại + điều chỉnh/đảo phiếu. Hàng hoàn không nhập bán lại được ghi trên phiếu nhưng không tăng tồn bán. Số dư cũ được giữ nguyên; báo cáo dựa trên lịch sử hệ thống đã ghi.</p>${table(
      data.items,
      [
        ["SKU", "SKU"],
        ["PRODUCT_NAME", "Sản phẩm"],
        ["COLOR_NAME", "Màu"],
        ["SIZE_NAME", "Size"],
        ["OPENING", "Đầu kỳ"],
        ["RECEIPT", "Nhập"],
        ["ISSUE", "Xuất khác"],
        ["SALE", "Xuất bán"],
        ["CANCELLED", "Hủy đơn"],
        ["RETURNED", "Hoàn nhập"],
        ["ADJUSTMENT", "Kiểm kê / đảo"],
        ["CLOSING", "Cuối kỳ"],
      ],
    )}${pager(data)}`;
  }
  Object.assign(window.AdminModules, {
    inventoryReport: { render: renderInventoryReport, action: async () => {} },
  });
})();
