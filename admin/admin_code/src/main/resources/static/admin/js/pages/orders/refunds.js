"use strict";

(() => {
  const { params, known, isAdmin, kind, labels, dates, allRows, csv, valuation } =
    window.AdminV8Utils;
  async function refundPanel(id) {
    const rows = await api("/api/admin/orders/" + id + "/refunds");
    return `<h3>Tiền thực tế hoàn cho khách</h3><p>Ghi nhận tiền riêng với nhận hàng hoàn. Khoản chi tiền không giảm doanh thu thêm lần nữa.</p>${table(
      rows,
      [
        ["DOC_CODE", "Phiếu hoàn"],
        ["AMOUNT", "Số tiền", money],
        ["STATUS", "Trạng thái"],
        ["REFERENCE_CODE", "Tham chiếu"],
        ["USERNAME", "Người làm"],
        ["PAID_AT", "Đã chi lúc", date],
      ],
      (r) =>
        r.STATUS === "PENDING" && isAdmin()
          ? button("Cập nhật khoản chờ", "edit-refund", r.ID)
          : "",
    )}${isAdmin() ? button("Ghi nhận hoàn tiền", "record-refund", id) : ""}`;
  }

  async function refundForm(bill, refundId = null) {
    const detail = await api("/api/admin/orders/" + bill),
      rows = await api("/api/admin/orders/" + bill + "/refunds"),
      old = rows.find((r) => Number(r.ID) === Number(refundId)) || {},
      docs = detail.inventoryDocuments.filter(
        (d) => (d.DOC_TYPE === "RETURN" || (d.DOC_TYPE === "RELEASE" && Number(detail.order.STATUS)===4)) && d.STATUS === "POSTED",
      );
    modal(
      "Hoàn tiền · " + detail.order.BILL_CODE,
      `<p>Đơn hủy trước xuất giao: hoàn tối đa tổng tiền đã thu theo phiếu giải phóng giữ. Đơn trả hàng: hoàn theo giá trị hàng đã nhận và kiểm tra. Tổng khoản đã chi và đang chờ không vượt mức được hoàn.</p><div class="grid">${select(
        "documentId",
        "Phiếu nhận hoàn / giải phóng giữ",
        docs.map((d) => ({ ID: d.ID, NAME: d.DOC_CODE })),
        old.RETURN_DOCUMENT_ID,
        true,
      )}${input("amount", "Số tiền (VNĐ)", old.AMOUNT || "", "number", true, 'min="1" step="1"')}${select(
        "status",
        "Trạng thái",
        [
          { ID: "PENDING", NAME: "Chờ chi" },
          { ID: "PAID", NAME: "Đã chi tiền" },
          { ID: "FAILED", NAME: "Chi thất bại" },
        ],
        old.STATUS || "PENDING",
      )}${input("reference", "Mã tham chiếu duy nhất", old.REFERENCE_CODE || "", "text", true, 'maxlength="150"')}${input("reason", "Lý do", old.REASON || "", "text", true, 'maxlength="500"')}</div>`,
      (data) =>
        api(
          "/api/admin/orders/" + bill + "/refunds" + (refundId ? "/" + refundId : ""),
          refundId ? "PUT" : "POST",
          {
            documentId: Number(data.documentId),
            amount: Number(data.amount),
            status: data.status,
            reference: data.reference,
            reason: data.reason,
          },
        ),
    );
    $("#fields [name=documentId]").required = true;
  }
  window.AdminV8.refundPanel = refundPanel;
  window.AdminV8.refundForm = refundForm;
})();
