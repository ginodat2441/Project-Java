function orderCustomerRequestPanel(rows, order) {
  if (!rows?.length) return '';
  const names={PENDING:'Chờ duyệt',APPROVED:'Đã chấp nhận',REJECTED:'Đã từ chối'};
  const pendingCancel=rows.some(r=>r.REQUEST_TYPE==='CANCEL'&&r.STATUS==='PENDING');
  const paidSepayCancel=pendingCancel&&order.PAYMENT_METHOD==='SEPAY'&&Number(order.PAYMENT_STATUS)===1;
  const urgentAlert=pendingCancel?`<div class="order-customer-request-alert" role="alert" aria-live="assertive"><span>KHÁCH ĐANG YÊU CẦU HỦY ĐƠN</span><strong>${paidSepayCancel?'ĐÃ THANH TOÁN SEPAY · CẦN XỬ LÝ HOÀN TIỀN NẾU DUYỆT HỦY':'CẦN DUYỆT YÊU CẦU CỦA KHÁCH'}</strong><p>${paidSepayCancel?'Đơn đã thu tiền qua SePay. Việc duyệt hủy không tự chuyển tiền hoàn; cần đối soát và ghi nhận hoàn tiền thủ công.':'Chưa xuất giao: hãy xem lý do bên dưới rồi chấp nhận hoặc từ chối yêu cầu.'}</p></div>`:'';
  return `${urgentAlert}<section class="order-customer-requests"><h3>Yêu cầu hủy / trả hàng của khách</h3>${rows.map(r=>`<article class="order-request-review"><div class="order-request-heading"><strong>${r.REQUEST_TYPE==='CANCEL'?'Hủy đơn trước vận chuyển':'Trả hàng / hoàn tiền'}</strong><span class="order-request-label" data-state="${esc(r.STATUS)}">${esc(names[r.STATUS])}</span></div><p class="order-request-reason">${esc(r.REASON)}</p><small>Gửi lúc ${date(r.CREATED_AT)}</small>${r.RESPONSE_NOTE?`<p><b>Phản hồi:</b> ${esc(r.RESPONSE_NOTE)}</p>`:''}${r.STATUS==='PENDING'&&window.AdminV8Utils?.isAdmin()?`<label>Phản hồi gửi cho khách<textarea name="request-note-${Number(r.ID)}" minlength="10" maxlength="500" rows="3" placeholder="Nêu kết quả kiểm tra; nếu duyệt trả hàng, hướng dẫn cách gửi trả"></textarea></label><p class="order-explainer">Duyệt hủy sẽ hủy đơn và giải phóng giữ hàng. Duyệt trả chỉ chấp nhận yêu cầu; tiếp nhận hàng và hoàn tiền được xác nhận riêng.</p><div class="toolbar">${button('Chấp nhận yêu cầu','approve-customer-request',r.ID)}${button('Từ chối và phản hồi','reject-customer-request',r.ID)}</div>`:''}</article>`).join('')}</section>`;
}

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
  const refundAlert =
    Number(r.STATUS) === 4 && Number(r.PAYMENT_STATUS) === 1
      ? '<section class="refund-critical-alert" role="alert" aria-live="assertive"><span class="refund-critical-alert__label">MỨC ĐỘ KHẨN CẤP</span><div><strong>Đơn đã hủy nhưng chưa hoàn tiền</strong><p>Đơn này đã thu tiền. Cần đối soát và xử lý hoàn tiền thủ công ngay.</p></div></section>'
      : "";
  const customerRequestPanel=orderCustomerRequestPanel(d.customerRequests,r);
  const transitions = { 0: [1, 4], 1: [4], 2: [3, 6], 3: [5], 4: [], 5: [], 6: [] };
  modal(
    "Đơn " + r.BILL_CODE,
    `${refundAlert}${customerRequestPanel}<div class="grid"><section><h3>${esc(r.RECEIVER_NAME)}</h3><p>${esc(r.RECEIVER_PHONE)}<br>${esc(r.SHIPPING_ADDRESS)}</p><p>Trạng thái: <b>${esc(statusNames[r.STATUS])}</b></p><p>Ghi chú: ${esc(r.NOTE)}</p></section><section><p>Tiền hàng: ${money(r.SUB_TOTAL)}<br>Voucher: ${esc(r.VOUCHER_CODE || "—")}<br>Giảm giá: ${money(r.DISCOUNT_AMOUNT)}<br>Phí giao: ${money(r.SHIPPING_FEE)}<br>${r.SHIPPING_SERVICE_NAME ? `Dịch vụ: ${esc(r.SHIPPING_SERVICE_NAME)} (${esc(r.SHIPPING_SERVICE)})<br>Báo giá lúc: ${date(r.SHIPPING_QUOTED_AT)}<br>` : ""}<b>Phải trả: ${money(r.TOTAL_AMOUNT)}</b></p></section></div><h3>Sản phẩm trong đơn</h3>${table(
      d.lines.map((x) => ({
        NAME: x.productName,
        COLOR: x.colorName,
        SIZE: x.sizeName,
        QTY: x.quantity,
        PRICE: x.unitPrice,
        TOTAL: x.totalPrice,
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
        { ID: "SEPAY", NAME: "SePay" },
        { ID: "BANK", NAME: "Chuyển khoản cũ" },
      ],
      r.PAYMENT_METHOD,
    )}${select(
      "paymentStatus",
      "Thanh toán",
      [
        { ID: 0, NAME: "Chưa nhận tiền" },
        { ID: 1, NAME: "Đã nhận tiền" },
        { ID: 2, NAME: "Đã hoàn tiền" },
      ],
      r.PAYMENT_STATUS,
    )}${input("carrier", "Đơn vị vận chuyển", r.CARRIER, "text", false)}${input("trackingCode", "Mã theo dõi", r.TRACKING_CODE, "text", false)}</div><div class="toolbar">${button("Lưu giao hàng / thanh toán", "save-shipping", id)}</div>${Number(r.STATUS) === 1 ? `<div class="toolbar">${button("Lập phiếu xuất giao (ADMIN duyệt)", "prepare-dispatch", id)}</div>` : ""}<h3>Chuyển trạng thái</h3><div class="toolbar">${select(
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
  // Group only order detail content; other administration screens keep their layout.
  const fields = $("#fields");
  const children = [...fields.children];
  const overview = fields.querySelector(".grid");
  overview.classList.add("order-overview");
  let panel = null;
  for (const node of children.slice(children.indexOf(overview) + 1)) {
    if (node.tagName === "H3") {
      panel = document.createElement("section");panel.className="order-detail-section";
      fields.insertBefore(panel,node);panel.append(node);
    } else if (panel) panel.append(node);
  }
  const progress=document.createElement("div");progress.className="order-progress";
  progress.innerHTML = [0,1,2,3].map((state)=>`<span class="${Number(r.STATUS)<=3&&Number(r.STATUS)>=state?'is-reached':''}">${esc(statusNames[state])}</span>`).join("");
  fields.prepend(progress);
  if (Number(r.STATUS)>=4) progress.innerHTML=AdminOrderUI.state(r.STATUS);
  ["paymentMethod","paymentStatus"].forEach(name=>fields.querySelector("[name="+name+"]").disabled=true);
  const frozen=Number(r.STATUS)>=3;
  ["carrier","trackingCode"].forEach(name=>fields.querySelector("[name="+name+"]").disabled=frozen);
  $("[data-action=save-shipping]").hidden=frozen;
  $("[data-action=save-shipping]").textContent="Lưu thông tin vận chuyển";
  if (!(transitions[r.STATUS] || []).length) {
    $("[name=nextStatus]").closest('.toolbar').hidden=true;
  }
  const help=document.createElement('p');help.className='order-explainer';
  help.textContent=r.PAYMENT_METHOD==='COD'?'COD: xác nhận hoàn thành đồng nghĩa đã giao hàng và thu đủ tiền.':'Chuyển khoản: trạng thái thanh toán lấy từ giao dịch được xác nhận, không sửa bằng form giao hàng.';
  fields.querySelector("[name=paymentMethod]").closest('.grid').before(help);
  $("#fields").onclick = async (e) => {
    const b = e.target.closest("[data-action]");
    if (!b) return;
    b.disabled = true;
    try {
      if (b.dataset.action === 'approve-customer-request' || b.dataset.action === 'reject-customer-request') {
        const note=fields.querySelector('[name="request-note-'+Number(b.dataset.id)+'"]').value.trim();
        if(note.length<10)throw Error('Nhập phản hồi từ 10 đến 500 ký tự trước khi duyệt.');
        const approved=b.dataset.action==='approve-customer-request';
        if(!confirm(approved?'Xác nhận chấp nhận yêu cầu này? Với hủy đơn, hàng giữ sẽ được giải phóng.':'Xác nhận từ chối yêu cầu và gửi phản hồi cho khách?')){b.disabled=false;return;}
        await api('/api/admin/order-requests/'+Number(b.dataset.id),'PUT',{decision:approved?'APPROVED':'REJECTED',note});
        $('#dialog').close();await load();await orderEditor(id);return;
      }
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
          carrier: fields.querySelector("[name=carrier]").value,
          trackingCode: fields.querySelector("[name=trackingCode]").value,
        });
      else {
        if ($("[name=nextStatus]").value === "") throw Error("Chọn trạng thái mới.");
        if (Number($("[name=nextStatus]").value)===3 && r.PAYMENT_METHOD==='COD'
            && !confirm('Xác nhận khách đã nhận hàng và đã thu đủ tiền COD?')) {b.disabled=false;return;}
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
