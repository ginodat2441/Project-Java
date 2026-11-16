"use strict";

(() => {
  const { params, known, isAdmin, kind, labels, dates, allRows, csv, valuation } =
    window.AdminV8Utils;
  const dashboard = {
    async render() {
      const y = filter.year || new Date().getFullYear(),
        d = await api(
          "/api/admin/dashboard?" + params({ year: y, threshold: filter.threshold || 5 }),
        );
      return `<form id="filter" class="toolbar">${input("year", "Năm", y, "number", true, 'min="2000" max="2200"')}${input("threshold", "Ngưỡng sắp hết", filter.threshold || 5, "number", true, 'min="0"')}<button>Xem</button></form><p class="report-note">Kỳ doanh thu: ${esc(y)}. Doanh thu tại ngày hoàn tất, giảm trừ tại ngày nhận hoàn/đảo; phí giao riêng. Giá trị tồn luôn là hiện tại.</p><div class="metrics">${valuation("Giá trị tại kho hiện tại", d.inventory)}<section class="metric"><span>Doanh thu ròng trong kỳ</span><strong>${money(d.totals?.NET_AFTER_RETURN)}</strong><p>Giá vốn: ${known(d.totals?.COGS)}</p><p>Lãi gộp: ${known(d.totals?.GROSS_PROFIT)}</p><p>Lãi gộp có đủ căn cứ: ${money(d.totals?.KNOWN_GROSS_PROFIT)}</p></section></div><div class="toolbar">${button("SKU sắp hết (" + esc(d.alerts?.LOW_STOCK ?? d.lowStock?.length ?? 0) + ")", "alert-low", Number(filter.threshold || 5))}${button("SKU thiếu giá vốn", "alert-cost", "")}${button("Đơn chờ xác nhận (" + esc(d.alerts?.WAIT_CONFIRM ?? 0) + ")", "alert-orders", "") + button("Chờ xuất giao (" + esc(d.alerts?.WAIT_DISPATCH ?? 0) + ")", "alert-dispatch", "") + button("Giao thất bại (" + esc(d.alerts?.FAILED ?? 0) + ")", "alert-failed", "")}${button("Phiếu chờ duyệt (" + esc(d.alerts?.WAIT_DOCUMENT ?? 0) + ")", "alert-documents", "")}</div><h3>Doanh thu theo tháng</h3>${table(
        d.revenue,
        [
          ["MONTH", "Tháng"],
          ["REVENUE", "Doanh thu ròng", money],
        ],
      )}<h3>Đơn theo trạng thái</h3>${table(d.statuses, [
        ["STATUS", "Trạng thái", (v) => esc(statusNames[v])],
        ["TOTAL", "Số đơn"],
      ])}<h3>Sản phẩm bán chạy</h3>${table(d.topProducts, [
        ["PRODUCT_NAME", "Sản phẩm"],
        ["SOLD", "SL ròng"],
        ["REVENUE", "Doanh thu", money],
      ])}`;
    },
    async action(a, id) {
      if (a === "alert-low") {
        section = "stock";
        filter = { threshold: id };
      } else if (a === "alert-cost") {
        section = "stock";
        filter = { stockState: "MISSING" };
      } else if (a === "alert-orders") {
        section = "orders";
        filter = { status: 0 };
      } else if (a === "alert-dispatch") {
        section = "orders";
        filter = { status: 1 };
      } else if (a === "alert-failed") {
        section = "orders";
        filter = { status: 6 };
      } else if (a === "alert-documents") {
        section = "documents";
        filter = { status: "DRAFT" };
      } else return false;
      page = 1;
      await load();
    },
  };
  window.AdminV8.dashboard = dashboard;
})();
