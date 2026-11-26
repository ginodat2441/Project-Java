"use strict";
window.AdminPages ??= {};
window.AdminPages.dashboard = {
  async render() {
    let html = "";
    const d = await api("/api/admin/dashboard?threshold=" + Number(filter.threshold || 5));
    html = `<form id="filter" class="toolbar">${input("threshold", "Ngưỡng sắp hết hàng", filter.threshold || 5, "number", true, 'min="0"')}<button>Cập nhật</button></form><div class="grid"><section class="card"><h2>Doanh thu theo tháng</h2><p class="muted">Doanh thu ròng theo tháng ghi nhận trong năm hiện tại; trừ hàng hoàn, không gồm phí giao.</p>${table(
      d.revenue,
      [
        ["MONTH", "Tháng"],
        ["REVENUE", "Doanh thu", money],
      ],
    )}</section><section class="card"><h2>Đơn theo trạng thái</h2>${table(d.statuses, [
      ["STATUS", "Trạng thái", (v) => esc(statusNames[v])],
      ["TOTAL", "Số đơn"],
    ])}</section><section class="card"><h2>Sản phẩm bán chạy</h2>${table(d.topProducts, [
      ["PRODUCT_NAME", "Sản phẩm"],
      ["SOLD", "SL ròng"],
      ["RETURNED", "SL hoàn"],
      ["REVENUE", "Doanh thu ròng", money],
    ])}</section><section class="card"><h2>Biến thể sắp hết hàng</h2>${table(d.lowStock, [
      ["SKU", "SKU"],
      ["PRODUCT_NAME", "Sản phẩm"],
      ["SIZE_NAME", "Size"],
      ["QUANTITY", "Tồn"],
    ])}</section></div>`;

    return html;
  },
};
