"use strict";
window.AdminV8 = {};
(() => {
  const params = (extra) => {
    const p = new URLSearchParams();
    for (const [k, v] of Object.entries({ ...filter, ...extra }))
      if (v !== "" && v != null) p.set(k, v);
    return p;
  };

  const known = (v) => (v == null ? "Chưa đủ dữ liệu" : money(v));

  const isAdmin = () => me?.roles.some((r) => r.authority === "ROLE_ADMIN");

  const kind = {
    RESERVE: "Giữ hàng",
    RELEASE: "Giải phóng giữ",
    DISPATCH: "Xuất giao thực tế",
    RECEIPT: "Nhập kho",
    ISSUE: "Xuất khác",
    COUNT: "Kiểm kê tại kho",
    RETURN: "Nhận hoàn",
    REVERSAL: "Đảo phiếu",
    SALE: "Giữ/xuất cũ",
    CANCEL: "Hủy cũ",
  };

  const labels = Object.entries(kind).map(([ID, NAME]) => ({ ID, NAME }));

  const dates = () =>
    input("from", "Từ ngày", filter.from || "", "date", false) +
    input("to", "Đến ngày", filter.to || "", "date", false);

  async function allRows(path) {
    let out = [],
      p = 1;
    for (;;) {
      const d = await api(path + (path.includes("?") ? "&" : "?") + "page=" + p);
      out.push(...d.items);
      if (out.length >= d.total || !d.items.length) return out;
      p++;
    }
  }

  function csv(rows, name) {
    if (!rows.length) {
      notice("Không có dữ liệu xuất.");
      return;
    }
    const keys = Object.keys(rows[0]),
      cell = (v) =>
        '"' +
        String(v ?? "")
          .replace(/^[=+@-]/, "'$&")
          .replaceAll('"', '""') +
        '"';
    const content =
        "\uFEFF" +
        [keys.map(cell).join(","), ...rows.map((r) => keys.map((k) => cell(r[k])).join(","))].join(
          "\r\n",
        ),
      a = document.createElement("a"),
      url = URL.createObjectURL(new Blob([content], { type: "text/csv;charset=utf-8" }));
    a.href = url;
    a.download = name + ".csv";
    a.click();
    URL.revokeObjectURL(url);
  }

  function valuation(title, t = {}) {
    return `<section class="metric"><span>${esc(title)}</span><strong>${known(t.INVENTORY_VALUE)}</strong><p>Phần giá trị có căn cứ: ${money(t.KNOWN_VALUE || 0)}</p><small>${esc(t.UNKNOWN_COST_SKUS || 0)} SKU thiếu giá vốn · ${esc(t.PHYSICAL ?? "—")} tại kho · ${esc(t.RESERVED ?? "—")} giữ · ${esc(t.QUANTITY ?? "—")} sẵn bán</small></section>`;
  }
  window.AdminV8Utils = { params, known, isAdmin, kind, labels, dates, allRows, csv, valuation };
})();
Object.assign(window.AdminV8, window.AdminV8Utils);
