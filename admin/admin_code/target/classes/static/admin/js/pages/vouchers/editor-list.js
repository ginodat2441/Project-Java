"use strict";
window.AdminPages ??= {};
window.AdminPages.vouchers = {
  async render() {
    let html = "";
    const rows = await api("/api/admin/vouchers");
    window.vouchers = rows;
    html =
      `<div class="toolbar">${button("+ Thêm voucher", "create", "")}</div>` +
      table(
        rows,
        [
          ["CODE", "Mã"],
          ["DISCOUNT_TYPE", "Loại"],
          ["DISCOUNT_VALUE", "Mức giảm"],
          ["MIN_ORDER", "Đơn tối thiểu", money],
          ["STARTS_AT", "Từ", date],
          ["ENDS_AT", "Đến", date],
          ["USED_COUNT", "Đã dùng"],
          ["USAGE_LIMIT", "Giới hạn"],
          ["STATUS", "Trạng thái", active],
        ],
        (r) => button("Sửa / Bật tắt", "edit", r.ID),
      );

    return html;
  },
  async action(a, id) {
    const r = window.vouchers.find((v) => v.ID === id) || {};
    modal(
      id ? "Sửa voucher" : "Thêm voucher",
      `<div class="grid">${input("code", "Mã voucher", r.CODE)}${select(
        "type",
        "Loại giảm",
        [
          { ID: "PERCENT", NAME: "Phần trăm" },
          { ID: "FIXED", NAME: "Số tiền" },
        ],
        r.DISCOUNT_TYPE || "PERCENT",
      )}${input("value", "Mức giảm", r.DISCOUNT_VALUE ?? 10, "number", true, 'min="0.01" step="0.01"')}${input("minOrder", "Đơn tối thiểu", r.MIN_ORDER ?? 0, "number", true, 'min="0"')}${input("maxDiscount", "Giảm tối đa (bắt buộc cho %)", r.MAX_DISCOUNT ?? 50000, "number", true, 'min="0"')}${input("usageLimit", "Giới hạn lượt", r.USAGE_LIMIT ?? 100, "number", true, 'min="0"')}${input("startsAt", "Có hiệu lực từ", r.STARTS_AT?.slice(0, 16) || "", "datetime-local")}${input("endsAt", "Hết hạn", r.ENDS_AT?.slice(0, 16) || "", "datetime-local")}${select("status", "Trạng thái", statusOptions, r.STATUS ?? 1)}</div>`,
      (d) =>
        api("/api/admin/vouchers" + (id ? "/" + id : ""), id ? "PUT" : "POST", {
          ...d,
          value: Number(d.value),
          minOrder: Number(d.minOrder),
          maxDiscount: Number(d.maxDiscount),
          usageLimit: Number(d.usageLimit),
          status: Number(d.status),
        }),
    );
  },
};
