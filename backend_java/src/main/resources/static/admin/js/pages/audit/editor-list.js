"use strict";
window.AdminPages ??= {};
window.AdminPages.audit = {
  async render() {
    let html = "";
    const data = await api(
      "/api/admin/" + (section === "history" ? "inventory-history" : "audit") + "?page=" + page,
    );
    html =
      table(
        data.items,
        section === "history"
          ? [
              ["DOC_CODE", "Phiếu"],
              ["MOVEMENT_TYPE", "Nghiệp vụ", (v) => esc(movementName(v))],
              ["BEFORE_QUANTITY", "Tồn trước"],
              ["AFTER_QUANTITY", "Tồn sau"],
              ["SKU", "SKU"],
              ["QUANTITY_CHANGE", "Thay đổi"],
              ["REASON", "Lý do"],
              ["USERNAME", "Người thao tác"],
              ["BILL_ID", "Đơn hàng"],
              ["CREATED_AT", "Thời điểm", date],
            ]
          : [
              ["USERNAME", "Người thao tác"],
              ["ACTION", "Hành động"],
              ["OBJECT_TYPE", "Đối tượng"],
              ["OBJECT_ID", "ID"],
              ["CREATED_AT", "Thời điểm", date],
            ],
      ) + pager(data);

    return html;
  },
};
