function orderReturnPanel(order, id) {
  const labels = {
    NONE: "Chưa nhận hoàn",
    REQUESTED: "Đang yêu cầu trả",
    PARTIAL: "Đã nhận hoàn một phần",
    FULL: "Đã nhận hoàn toàn bộ",
  };
  return `<p class="report-note">Đặt đơn chỉ giữ hàng. ADMIN duyệt xuất giao mới giảm tồn tại kho. Hủy trước xuất chỉ giải phóng giữ. Hàng khách trả phải lập phiếu nhận hoàn theo đơn gốc và kiểm tra tình trạng trước khi nhập lại hàng bán.</p><p><b>Hàng hoàn:</b> ${esc(labels[order.RETURN_STATUS] || order.RETURN_STATUS || "Chưa nhận")} · Giá trị hàng hoàn: ${money(order.RETURN_VALUE)}</p><div class="toolbar">${[3, 5, 6].includes(Number(order.STATUS)) && order.RETURN_STATUS !== "FULL" ? button("Lập phiếu nhận hàng hoàn", "receive-return", id) : ""}</div>`;
}

function orderInventoryPanel(data) {
  return (
    "<h3>Chứng từ xuất / hoàn theo đơn</h3>" +
    table(
      data.inventoryDocuments || [],
      [
        ["DOC_CODE", "Phiếu"],
        ["DOC_TYPE", "Nghiệp vụ", (v) => esc(movementName(v))],
        ["STATUS", "Trạng thái"],
        ["POSTED_AT", "Ghi tồn", date],
      ],
      (r) => button("Xem phiếu", "view-inventory", r.ID),
    )
  );
}

async function orderEditor(id) {
  const d = await api("/api/admin/orders/" + id),
    r = d.order,
    refundHtml = window.AdminV8 ? await window.AdminV8.refundPanel(id) : "";
  const transitions = { 0: [1, 4], 1: [4], 2: [3, 6], 3: [5], 4: [], 5: [], 6: [] };
  modal(
    "Đơn " + r.BILL_CODE,
    `<div class="grid"><section><h3>${esc(r.RECEIVER_NAME)}</h3><p>${esc(r.RECEIVER_PHONE)}<br>${esc(r.SHIPPING_ADDRESS)}</p><p>Trạng thái: <b>${esc(statusNames[r.STATUS])}</b></p><p>Ghi chú: ${esc(r.NOTE)}</p></section><section><p>Tiền hàng: ${money(r.SUB_TOTAL)}<br>Voucher: ${esc(r.VOUCHER_CODE || "—")}<br>Giảm giá: ${money(r.DISCOUNT_AMOUNT)}<br>Phí giao: ${money(r.SHIPPING_FEE)}<br>${r.SHIPPING_SERVICE_NAME ? `Dịch vụ: ${esc(r.SHIPPING_SERVICE_NAME)} (${esc(r.SHIPPING_SERVICE)})<br>Báo giá lúc: ${date(r.SHIPPING_QUOTED_AT)}<br>` : ""}<b>Phải trả: ${money(r.TOTAL_AMOUNT)}</b></p></section></div>${table(
      d.lines.map((x) => ({
        NAME: x.productName,
        COLOR: x.colorName,
        SIZE: x.sizeName,
        QTY: x.quantity,
        PRICE: x.unitPrice,
        TOTAL: x.totalAmount ?? x.totalPrice,
      })),
      [
        ["NAME", "Sản phẩm"],
        ["COLOR", "Màu"],
        ["SIZE", "Size"],
        ["QTY", "SL"],
        ["PRICE", "Đơn giá", money],
        ["TOTAL", "Thành tiền", money],
      ],
    )}<h3>Thanh toán và giao hàng</h3><div class="grid">${select(
      "paymentMethod",
      "Phương thức",
      [
        { ID: "COD", NAME: "COD" },
        { ID: "BANK", NAME: "Chuyển khoản" },
      ],
      r.PAYMENT_METHOD,
    )}${select(
      "paymentStatus",
      "Thanh toán",
      [
        { ID: 0, NAME: "Chưa nhận tiền" },
        { ID: 1, NAME: "Đã nhận tiền" },
      ],
      r.PAYMENT_STATUS,
    )}${input("carrier", "Đơn vị vận chuyển", r.CARRIER, "text", false)}${input("trackingCode", "Mã theo dõi", r.TRACKING_CODE, "text", false)}</div><div class="toolbar">${button("Lưu thông tin giao hàng", "save-shipping", id)}</div>${Number(r.STATUS) === 1 ? `<div class="toolbar">${button("Lập phiếu xuất giao (ADMIN duyệt)", "prepare-dispatch", id)}</div>` : ""}<h3>Chuyển trạng thái</h3><div class="toolbar">${select(
      "nextStatus",
      "Trạng thái mới",
      (transitions[r.STATUS] || []).map((ID) => ({ ID, NAME: statusNames[ID] })),
      null,
      true,
    )}${input("note", "Ghi chú xử lý", "", "text", false)}${button("Chuyển trạng thái", "transition", id)}</div>${orderReturnPanel(r, id)}${orderInventoryPanel(d)}${refundHtml}<h3>Lịch sử</h3>${table(
      d.history,
      [
        ["NEW_STATUS", "Trạng thái", (v) => esc(statusNames[v])],
        ["NOTE", "Ghi chú"],
        ["USERNAME", "Người xử lý"],
        ["CREATED_AT", "Thời điểm", date],
      ],
    )}`,
  );
  // Payment terms and collection state belong to the order workflow, not this editor.
  for (const name of ["paymentMethod", "paymentStatus"]) {
    const control = $("[name=" + name + "]");
    if (control) { control.disabled = true; control.title = "Thông tin thanh toán của đơn không được sửa tại đây"; }
  }
  if (Number(r.STATUS) >= 3) {
    for (const name of ["carrier", "trackingCode"]) $("[name=" + name + "]").disabled = true;
    const save = $("[data-action=save-shipping]"); if (save) save.hidden = true;
  }
  $("#fields").onclick = async (e) => {
    const b = e.target.closest("[data-action]");
    if (!b) return;
    b.disabled = true;
    try {
      if (b.dataset.action === "record-refund" || b.dataset.action === "edit-refund") {
        await window.AdminV8.refundForm(
          id,
          b.dataset.action === "edit-refund" ? Number(b.dataset.id) : null,
        );
        return;
      }
      if (b.dataset.action === "prepare-dispatch") {
        const doc = await api("/api/admin/orders/" + id + "/dispatch", "POST");
        await window.AdminModules.documents.detail(Number(doc));
        return;
      }
      if (b.dataset.action === "view-inventory") {
        $("#dialog").close();
        await window.AdminModules.documents.detail(Number(b.dataset.id));
        return;
      }
      if (b.dataset.action === "receive-return") {
        $("#dialog").close();
        await window.AdminModules.documents.receive(id);
        return;
      }
      if (b.dataset.action === "save-shipping")
        await api(`/api/admin/orders/${id}/shipping`, "PUT", {
          paymentMethod: r.PAYMENT_METHOD,
          paymentStatus: Number(r.PAYMENT_STATUS),
          carrier: $("[name=carrier]").value,
          trackingCode: $("[name=trackingCode]").value,
        });
      else {
        if ($("[name=nextStatus]").value === "") throw Error("Chọn trạng thái mới.");
        if (Number($("[name=nextStatus]").value) === 3 && r.PAYMENT_METHOD === "COD" && !confirm("Xác nhận giao thành công và đã thu đủ tiền COD theo đơn?")) { b.disabled = false; return; }
        await api(`/api/admin/orders/${id}/status`, "PUT", {
          status: Number($("[name=nextStatus]").value),
          note: $("[name=note]").value,
        });
      }
      $("#dialog").close();
      await load();
      await orderEditor(id);
    } catch (e) {
      $("#form-error").textContent = e.message;
      b.disabled = false;
    }
  };
}
