"use strict";
window.AdminPages ??= {};
window.AdminPages.orders = {
  async render() {
    let html = "";
    const qs = new URLSearchParams({ page, q: filter.q || "" });
    for (const k of ["status", "from", "to", "paymentMethod", "paymentStatus", "returnStatus"])
      if (filter[k] !== undefined && filter[k] !== "") qs.set(k, filter[k]);
    const data = await api("/api/admin/orders?" + qs);
    html =
      searchToolbar(
        select(
          "status",
          "Trạng thái",
          statusNames.map((NAME, ID) => ({ ID, NAME })),
          filter.status,
          true,
        ) +
          select(
            "paymentMethod",
            "Phương thức",
            [
              { ID: "COD", NAME: "COD" },
              { ID: "BANK", NAME: "Chuyển khoản" },
            ],
            filter.paymentMethod,
            true,
          ) +
          select(
            "paymentStatus",
            "Thanh toán",
            [
              { ID: 0, NAME: "Chưa thu" },
              { ID: 1, NAME: "Đã thu" },
              { ID: 2, NAME: "Đã hoàn" },
            ],
            filter.paymentStatus,
            true,
          ) +
          select(
            "returnStatus",
            "Hàng hoàn",
            ["NONE", "REQUESTED", "PARTIAL", "FULL"].map((ID) => ({ ID, NAME: ID })),
            filter.returnStatus,
            true,
          ) +
          input("from", "Từ ngày", filter.from || "", "date", false) +
          input("to", "Đến ngày", filter.to || "", "date", false),
      ) +
      table(
        data.items,
        [
          ["BILL_CODE", "Mã đơn"],
          ["RECEIVER_NAME", "Người nhận"],
          ["TOTAL_AMOUNT", "Tổng tiền đơn", money],
          ["STATUS", "Trạng thái", (v) => esc(statusNames[v])],
          ["PAYMENT_STATUS", "Trạng thái thanh toán", (v) => (v === 1 ? "Đã nhận" : "Chưa nhận")],
          ["CREATED_AT", "Ngày đặt", date],
        ],
        (r) => button("Chi tiết", "detail", r.ID),
      ) +
      pager(data);

    return html;
  },
  async action(a, id) {
    return orderEditor(id);
  },
};
