"use strict";

(() => {
  const { params, known, isAdmin, kind, labels, dates, allRows, csv, valuation } =
    window.AdminV8Utils;
  const auditNames = {
    STATUS_0: "Đặt đơn",
    STATUS_1: "Xác nhận đơn",
    STATUS_2: "Xuất giao",
    STATUS_3: "Hoàn tất đơn",
    STATUS_4: "Hủy trước giao",
    STATUS_5: "Yêu cầu trả hàng",
    STATUS_6: "Giao thất bại",
    ACCESS: "Đổi quyền / khóa nhân viên",
    CUSTOMER_STATUS: "Khóa/mở khách hàng",
    COST_SNAPSHOT: "Chốt giá vốn SKU",
    POST_DISPATCH: "Duyệt xuất giao",
    POST_RETURN: "Duyệt nhận hoàn",
    POST_RECEIPT: "Duyệt nhập",
    REFUND_PAID: "Đã hoàn tiền",
    REFUND_PENDING: "Chờ hoàn tiền",
    REFUND_FAILED: "Hoàn tiền thất bại",
    CREATE_REVERSAL: "Lập phiếu đảo",
    SAVE_DRAFT: "Lưu phiếu nháp",
  };

  const audit = {
    async render() {
      const d = await api("/api/admin/audit?" + params({ page }));
      return `<form id="filter" class="toolbar">${input("q", "Người thao tác", filter.q || "", "text", false)}${select(
        "action",
        "Hành động",
        Object.entries(auditNames).map(([ID, NAME]) => ({ ID, NAME })),
        filter.action,
        true,
      )}${input("object", "Đối tượng (BILL, MANAGER…)", filter.object || "", "text", false)}${dates()}<button>Lọc</button></form>${table(
        d.items,
        [
          ["USERNAME", "Người làm"],
          ["ACTION", "Hành động", (v) => esc(auditNames[v] || kind[v?.replace("POST_", "")] || v)],
          ["OBJECT_TYPE", "Đối tượng"],
          ["OBJECT_ID", "ID"],
          [
            "BEFORE_DATA",
            "Trước",
            (v) => `<pre class="audit-data">${esc(v || "Chưa ghi chi tiết ở bản cũ")}</pre>`,
          ],
          ["AFTER_DATA", "Sau", (v) => `<pre class="audit-data">${esc(v || "—")}</pre>`],
          ["REASON", "Lý do"],
          ["CREATED_AT", "Thời gian", date],
        ],
      )}${pager(d)}`;
    },
  };
  window.AdminV8.audit = audit;
})();
