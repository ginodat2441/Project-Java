"use strict";
window.AdminPages ??= {};
window.AdminPages.stock = {
  async render() {
    let html = "";
    const qs = new URLSearchParams({ page });
    for (const k of ["threshold", "q", "productId", "minValue", "maxValue"])
      if (filter[k] !== undefined && filter[k] !== "") qs.set(k, filter[k]);
    const data = await api("/api/admin/stock?" + qs);
    const value = (v) => (v == null ? "Chưa đủ giá vốn" : money(v)),
      summary = (title, total) =>
        `<div class="metric"><span>${title}</span><strong>${value(total?.INVENTORY_VALUE)}</strong>${Number(total?.UNKNOWN_COST_PRODUCTS || 0) ? `<small>${esc(total.UNKNOWN_COST_PRODUCTS)} mã thiếu giá vốn</small>` : ""}</div>`;
    const totalRow = `<tr id="stock-totals"><th colspan="9">Tổng cộng theo bộ lọc (tất cả trang)</th><td>${value(data.totals?.INVENTORY_VALUE)}</td><td colspan="2"></td></tr>`;
    html =
      `<div class="metrics">${summary("Giá trị tồn kho tất cả sản phẩm", data.globalTotals)}${summary("Giá trị tồn kho theo bộ lọc", data.totals)}</div><p class="report-note">Giá trị tồn hiện tại = tồn sẵn bán × giá vốn bình quân. Khoảng giá trị lọc theo tổng của mã sản phẩm, gồm mọi màu/size. Giá trị từng dòng là của biến thể; không cộng lặp cột tổng theo mã.</p><form id="filter" class="toolbar">${input("q", "Mã / tên sản phẩm / SKU", filter.q || "", "text", false)}${select("productId", "Sản phẩm", data.products || [], filter.productId, true)}${input("threshold", "Tồn tối đa", filter.threshold ?? "", "number", false, 'min="0"')}${input("minValue", "Giá trị sản phẩm từ (VNĐ)", filter.minValue ?? "", "number", false, 'min="0" step="0.01"')}${input("maxValue", "Đến giá trị (VNĐ)", filter.maxValue ?? "", "number", false, 'min="0" step="0.01"')}<button>Lọc</button><button type="button" id="clear-stock-filter" class="secondary">Xóa bộ lọc</button></form>` +
      table(
        data.items,
        [
          ["PRODUCT_CODE", "Mã sản phẩm"],
          ["PRODUCT_NAME", "Sản phẩm"],
          ["COLOR_NAME", "Màu"],
          ["SIZE_NAME", "Size"],
          ["SKU", "SKU"],
          ["QUANTITY", "Tồn sẵn bán"],
          ["RESERVED", "Giữ cho đơn"],
          ["PHYSICAL", "Còn tại kho"],
          ["AVERAGE_COST", "Giá vốn bình quân", value],
          ["INVENTORY_VALUE", "Giá trị tồn biến thể", value],
          ["PRODUCT_INVENTORY_VALUE", "Tổng giá trị theo mã", value],
        ],
        (r) =>
          button("Lập phiếu kho", "edit", r.ID) +
          (r.AVERAGE_COST == null &&
          Number(r.QUANTITY) > 0 &&
          me.roles.some((role) => role.authority === "ROLE_ADMIN")
            ? button("Bổ sung giá vốn", "opening-cost", r.PRODUCT_ID)
            : ""),
      ).replace("</tbody>", `</tbody><tfoot>${totalRow}</tfoot>`) +
      (data.items.length ? "" : `<table><tbody>${totalRow}</tbody></table>`) +
      pager(data);

    return html;
  },
  async action(a, id) {
    modal(
      "Điều chỉnh tồn kho",
      `<p>Nhập số dương để nhập hàng; số âm để giảm/kiểm kê. Mọi điều chỉnh được lưu lịch sử.</p>${input("delta", "Số lượng thay đổi", "", "number")}${input("reason", "Lý do")}`,
      (d) => api("/api/admin/stock/" + id, "PUT", { delta: Number(d.delta), reason: d.reason }),
    );
  },
};
