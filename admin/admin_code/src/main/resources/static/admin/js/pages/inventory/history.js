"use strict";

(() => {
  const { params, known, isAdmin, kind, labels, dates, allRows, csv, valuation } =
    window.AdminV8Utils;
  const history = {
    async render() {
      const d = await api("/api/admin/inventory-history?" + params({ page }));
      return `<form id="filter" class="toolbar">${input("q", "SKU / sản phẩm", filter.q || "", "text", false)}${filter.variantId ? `<input type="hidden" name="variantId" value="${esc(filter.variantId)}">` : ""}${dates()}${select("type", "Nghiệp vụ", labels, filter.type, true)}${input("document", "Mã phiếu", filter.document || "", "text", false)}${input("bill", "Mã đơn", filter.bill || "", "text", false)}<button>Lọc lịch sử</button>${button("CSV mọi trang", "export-history", "")}</form><p class="report-note">Dòng AVAILABLE_LEGACY chỉ có lịch sử sẵn bán. Dòng PHYSICAL_V8 đối chiếu được tại kho, giữ và sẵn bán. Giá trị trống nghĩa là thiếu chứng từ giá vốn.</p>${table(
        d.items,
        [
          ["SKU", "SKU"],
          ["MOVEMENT_TYPE", "Nghiệp vụ", (v) => esc(kind[v] || v)],
          ["PHYSICAL_BEFORE", "Kho trước"],
          ["PHYSICAL_AFTER", "Kho sau"],
          ["RESERVED_BEFORE", "Giữ trước"],
          ["RESERVED_AFTER", "Giữ sau"],
          ["BEFORE_QUANTITY", "Bán trước"],
          ["AFTER_QUANTITY", "Bán sau"],
          ["VALUE_BEFORE", "Giá trị trước", known],
          ["VALUE_AFTER", "Giá trị sau", known],
          [
            "DOC_CODE",
            "Phiếu",
            (v, r) => (v ? button(esc(v), "trace-document", r.DOCUMENT_ID) : "—"),
          ],
          ["BILL_CODE", "Đơn", (v, r) => (v ? button(esc(v), "trace-order", r.BILL_ID) : "—")],
          ["USERNAME", "Người làm"],
          ["REASON", "Lý do"],
          ["CREATED_AT", "Thời gian", date],
        ],
      )}${pager(d)}`;
    },
    async action(a, id) {
      if (a === "trace-document") return window.AdminModules.documents.detail(id);
      if (a === "trace-order") return orderEditor(id);
      if (a === "export-history") {
        const p = params({ detail: "history" });
        p.delete("page");
        const a = document.createElement("a");
        a.href = "/api/admin/inventory/export?" + p;
        a.download = "lich-su-ton-kho.csv";
        a.click();
        return;
      }
      return false;
    },
  };
  window.AdminV8.history = history;
})();
