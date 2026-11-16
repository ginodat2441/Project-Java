"use strict";

(() => {
  const { params, known, isAdmin, kind, labels, dates, allRows, csv, valuation } =
    window.AdminV8Utils;
  const inventoryReport = {
    async render() {
      const d = await api("/api/admin/inventory/summary?" + params({ page }));
      return `<form id="filter" class="toolbar">${dates()}${input("q", "SKU / tên / mã sản phẩm", filter.q || "", "text", false)}<button>Xem nhập xuất tồn</button>${button("CSV mọi trang", "export-balance", "")}</form><p class="report-note">Sẵn bán = tại kho − giữ. Giữ/giải phóng giữ không đổi tại kho. Xuất giao giảm tại kho và giữ cùng lúc. Số tại kho đầu/cuối trống nếu phạm vi chứa lịch sử cũ chỉ ghi sẵn bán; không diễn giải số cũ thành tồn vật lý đã đối soát.</p>${table(
        d.items,
        [
          ["SKU", "SKU"],
          ["PRODUCT_NAME", "Sản phẩm"],
          ["PHYSICAL_OPENING", "Kho đầu"],
          ["RESERVED_OPENING", "Giữ đầu"],
          ["OPENING", "Bán đầu"],
          ["RECEIPT", "Nhập"],
          ["RESERVED", "Giữ mới"],
          ["RELEASED", "Giải giữ"],
          ["DISPATCH", "Xuất giao"],
          ["ISSUE", "Xuất khác"],
          ["RETURNED", "Hoàn nhập"],
          ["ADJUSTMENT", "Kiểm kê/đảo"],
          ["PHYSICAL_CLOSING", "Kho cuối"],
          ["RESERVED_CLOSING", "Giữ cuối"],
          ["CLOSING", "Bán cuối"],
          ["LEGACY_ROWS", "Dòng cũ cần đối chiếu"],
        ],
        (r) => button("Lịch sử SKU", "balance-history", r.ID),
      )}<p>Tổng mọi trang: sẵn bán đầu ${esc(d.totals?.OPENING ?? 0)} · cuối ${esc(d.totals?.CLOSING ?? 0)} · xuất giao ${esc(d.totals?.DISPATCH ?? 0)} · giữ mới ${esc(d.totals?.RESERVED ?? 0)} · giải giữ ${esc(d.totals?.RELEASED ?? 0)}.</p>${pager(d)}`;
    },
    async action(a, id) {
      if (a === "balance-history") {
        section = "history";
        filter = { ...filter, variantId: id };
        page = 1;
        await load();
        return;
      }
      if (a === "export-balance") {
        const p = params({ detail: "balance" });
        p.delete("page");
        const a = document.createElement("a");
        a.href = "/api/admin/inventory/export?" + p;
        a.download = "nhap-xuat-ton.csv";
        a.click();
        return;
      }
      return false;
    },
  };
  window.AdminV8.inventoryReport = inventoryReport;
})();
