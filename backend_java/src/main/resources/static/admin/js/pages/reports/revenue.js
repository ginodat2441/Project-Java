"use strict";

(() => {
  const { types, states, admin, choose, today, year, query } = window.AdminModulesUtils;
  let lastReport;

  function metrics(t) {
    return `<div class="metrics">${[
      ["Doanh thu hàng sau giảm", money(t.NET)],
      ["Giá trị hàng hoàn", money(t.RETURN_VALUE)],
      ["Doanh thu ròng", money(t.NET_AFTER_RETURN)],
      ["Số lượng đã bán", t.SOLD],
      ["Số lượng nhận hoàn", t.RETURNED],
      ["Đơn hoàn thành", t.ORDERS],
      ["Phí giao hàng", money(t.SHIPPING)],
      ["Giá vốn hàng bán", t.COGS == null ? "Chưa đủ giá" : money(t.COGS)],
      ["Lãi gộp", t.GROSS_PROFIT == null ? "Chưa xác định đủ" : money(t.GROSS_PROFIT)],
      ["Lãi gộp có căn cứ", money(t.KNOWN_GROSS_PROFIT)],
      ["SKU thiếu giá vốn", t.UNKNOWN_COST_SKUS ?? 0],
      ["Chi phí hàng hoàn lỗi (riêng)", money(t.DAMAGED_COST)],
    ]
      .map(
        ([label, value]) =>
          `<div class="metric"><span class="muted">${label}</span><strong>${esc(value)}</strong></div>`,
      )
      .join("")}</div>`;
  }

  const reportColumns = [
    ["PERIOD", "Kỳ"],
    ["ORDERS", "Đơn"],
    ["SOLD", "SL bán"],
    ["RETURNED", "SL hoàn"],
    ["GROSS", "Tiền hàng", (v) => money(v)],
    ["DISCOUNT", "Giảm giá", (v) => money(v)],
    ["NET", "Sau giảm", (v) => money(v)],
    ["RETURN_VALUE", "Hàng hoàn", (v) => money(v)],
    ["NET_AFTER_RETURN", "Doanh thu ròng", (v) => money(v)],
    ["SHIPPING", "Phí giao", (v) => money(v)],
  ];

  const productColumns = [
    ["PRODUCT_CODE", "Mã"],
    ["PRODUCT_NAME", "Sản phẩm"],
    ["SOLD", "SL bán"],
    ["RETURNED", "SL hoàn"],
    ["GROSS", "Tiền hàng", (v) => money(v)],
    ["DISCOUNT", "Giảm giá", (v) => money(v)],
    ["RETURN_VALUE", "Hàng hoàn", (v) => money(v)],
    ["NET_AFTER_RETURN", "Doanh thu ròng", (v) => money(v)],
  ];

  for (const cols of [reportColumns, productColumns])
    cols.push(
      ["COGS", "Giá vốn", (v) => (v == null ? "Thiếu giá" : money(v))],
      ["GROSS_PROFIT", "Lãi gộp", (v) => (v == null ? "Chưa đủ" : money(v))],
      ["UNKNOWN_COST_SKUS", "SKU thiếu giá"],
      ["DAMAGED_COST", "Chi phí hàng lỗi", (v) => money(v)],
    );

  async function renderReports() {
    const params = query({ page, year: filter.year || year(), period: filter.period || "MONTH" });
    lastReport = await api("/api/admin/reports?" + params);
    const d = lastReport;
    const periodRows = [
      { ID: "DAY", NAME: "Từng ngày" },
      { ID: "MONTH", NAME: "Từng tháng" },
      { ID: "QUARTER", NAME: "Từng quý" },
      { ID: "YEAR", NAME: "Từng năm" },
    ];
    const chart = d.series.slice(-32),
      max = Math.max(1, ...chart.map((r) => Math.abs(Number(r.NET_AFTER_RETURN))));
    return `<form id="filter" class="toolbar">${choose("period", "Chia kỳ", periodRows, filter.period || "MONTH", false)}${input("year", "Năm", filter.year || year(), "number", true, 'min="2000" max="2200"')}${choose(
      "month",
      "Tháng (tùy chọn)",
      Array.from({ length: 12 }, (_, i) => ({ ID: i + 1, NAME: "Tháng " + (i + 1) })),
      filter.month,
    )}${choose(
      "quarter",
      "Quý (tùy chọn)",
      [1, 2, 3, 4].map((ID) => ({ ID, NAME: "Quý " + ID })),
      filter.quarter,
    )}${input("from", "Hoặc từ ngày", filter.from || "", "date", false)}${input("to", "Đến ngày", filter.to || "", "date", false)}${input("q", "Tên / mã sản phẩm / SKU", filter.q || "", "text", false)}<button>Xem thống kê</button></form><div class="report-note">Từ <b>${esc(d.from)}</b> đến <b>${esc(d.to)}</b>. Doanh thu tính tại ngày hoàn thành đơn; hàng hoàn tính tại ngày phiếu nhận hoàn được xác nhận. Doanh thu ròng = tiền hàng − giảm giá − giá trị hàng hoàn; phí giao hiển thị riêng. Khoảng ngày được ưu tiên hơn bộ lọc năm/tháng/quý.</div>${metrics(d.totals)}<section class="card"><h2>Doanh thu ròng theo kỳ</h2>${chart.map((r) => `<div class="chart-row"><span>${esc(r.PERIOD)}</span><div class="chart-bar ${Number(r.NET_AFTER_RETURN) < 0 ? "negative" : ""}" style="width:${Math.max(0, (Math.abs(Number(r.NET_AFTER_RETURN)) / max) * 100)}%"></div><span>${money(r.NET_AFTER_RETURN)}</span></div>`).join("")}<p class="muted">Biểu đồ tối đa 32 kỳ cuối. Bảng bên dưới gồm đầy đủ các kỳ, kể cả kỳ không có doanh thu.</p></section><section class="card"><h2>Chi tiết ngày / tháng / quý / năm</h2><div class="report-series">${table(d.series, reportColumns)}</div><div class="toolbar">${button("Tải CSV theo kỳ", "export-report", "")}</div></section><section class="card"><h2>Doanh thu từng sản phẩm</h2><div class="toolbar">${button("CSV sản phẩm mọi trang", "export-products", "")}${button("CSV chi tiết SKU", "export-skus", "")}</div>${table(d.products.items, productColumns, (r) => button("Màu / size", "product-report", r.ID))}${pager(d.products)}</section>`;
  }

  async function reportAction(action, id) {
    if (action === "export-products" || action === "export-skus") {
      const p = query({
        year: filter.year || year(),
        period: filter.period || "MONTH",
        detail: action === "export-products" ? "product" : "sku",
      });
      p.delete("page");
      const a = document.createElement("a");
      a.href = "/api/admin/reports/export?" + p;
      a.download = "doanh-thu.csv";
      a.click();
      return;
    }
    if (action === "export-report") {
      const p = query({
        year: filter.year || year(),
        period: filter.period || "MONTH",
        detail: "period",
      });
      p.delete("page");
      const a = document.createElement("a");
      a.href = "/api/admin/reports/export?" + p;
      a.download = "doanh-thu-theo-ky.csv";
      a.click();
      return;
    }
    if (action === "product-report") {
      const d = await api(
        "/api/admin/reports?" +
          query({
            year: filter.year || year(),
            period: filter.period || "MONTH",
            productId: id,
            page: 1,
          }),
      );
      modal(
        "Doanh thu theo màu / size",
        `<p>${esc(d.from)} → ${esc(d.to)}</p>${metrics(d.totals)}${table(d.variants, [
          ["SKU", "SKU"],
          ["COLOR_NAME", "Màu"],
          ["SIZE_NAME", "Size"],
          ["SOLD", "SL bán"],
          ["RETURNED", "SL hoàn"],
          ["NET", "Sau giảm", (v) => money(v)],
          ["RETURN_VALUE", "Hàng hoàn", (v) => money(v)],
          ["NET_AFTER_RETURN", "Doanh thu ròng", (v) => money(v)],
          ["COGS", "Giá vốn", (v) => (v == null ? "Thiếu giá" : money(v))],
          ["GROSS_PROFIT", "Lãi gộp", (v) => (v == null ? "Chưa đủ" : money(v))],
        ])}`,
      );
    }
  }
  Object.assign(window.AdminModules, { reports: { render: renderReports, action: reportAction } });
})();
