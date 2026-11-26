"use strict";

(() => {
  const { params, known, isAdmin, kind, labels, dates, allRows, csv, valuation } =
    window.AdminV8Utils;
  const effective = {
    UPCOMING: "Chưa bắt đầu",
    ACTIVE: "Đang áp dụng",
    EXPIRED: "Hết hạn",
    EXHAUSTED: "Hết lượt",
    DISABLED: "Đã tắt",
  };
  const audience = (row) => {
    const rules = [];
    if (row.NEW_ACCOUNT_DAYS != null)
      rules.push(`Tài khoản trong ${row.NEW_ACCOUNT_DAYS} ngày`);
    if (Number(row.NO_PREVIOUS_ORDERS) === 1) rules.push("Chưa có đơn");
    if (row.MINIMUM_ORDERS != null) rules.push(`Từ ${row.MINIMUM_ORDERS} đơn`);
    return rules.length ? rules.join(" · ") : "Tất cả khách hàng";
  };

  const vouchers = {
    async render() {
      const d = await api("/api/admin/vouchers?" + params({}));
      window.vouchers = d;
      return (
        searchToolbar(
          select(
            "effective",
            "Hiệu lực",
            Object.entries(effective).map(([ID, NAME]) => ({ ID, NAME })),
            filter.effective,
            true,
          ),
          "Voucher",
        ) +
        `<p class="report-note">Mã hết hiệu lực ở đúng thời điểm kết thúc. Lượt đã dùng không trả lại khi hủy đơn (quy tắc hiện hành).</p>` +
        table(
          d,
          [
            ["CODE", "Mã"],
            ["EFFECTIVE_STATUS", "Hiệu lực", (v) => esc(effective[v])],
            ["DISCOUNT_VALUE", "Mức giảm"],
            ["MIN_ORDER", "Đơn tối thiểu", money],
            ["MAX_DISCOUNT", "Trần giảm", money],
            ["USED_COUNT", "Đã dùng"],
            ["REMAINING", "Còn lại"],
            ["PRODUCT_COUNT", "Sản phẩm áp dụng", (v) => (Number(v) ? v + " sản phẩm" : "Tất cả")],
            ["ID", "Đối tượng", (_, r) => esc(audience(r))],
            ["STARTS_AT", "Bắt đầu", date],
            ["ENDS_AT", "Kết thúc", (v) => (v == null ? "Không hết hạn" : date(v))],
          ],
          (r) => button("Sửa", "edit", r.ID) + button("Đơn đã dùng", "voucher-orders", r.ID),
        )
      );
    },
    async action(a, id) {
      if (a !== "voucher-orders") return false;
      const rows = await api("/api/admin/vouchers/" + id + "/orders");
      modal(
        "Đơn dùng voucher",
        table(
          rows,
          [
            ["BILL_CODE", "Đơn"],
            ["STATUS", "Trạng thái", (v) => esc(statusNames[v])],
            ["DISCOUNT_AMOUNT", "Giảm", money],
            ["CREATED_AT", "Ngày", date],
          ],
          (r) => button("Mở đơn", "trace-order", r.ID),
        ),
      );
      $("#fields").onclick = (e) => {
        const b = e.target.closest("[data-action=trace-order]");
        if (b) orderEditor(Number(b.dataset.id));
      };
    },
  };
  window.AdminV8.vouchers = vouchers;
})();
