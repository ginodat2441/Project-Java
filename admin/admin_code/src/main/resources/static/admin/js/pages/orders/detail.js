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
        ["STATUS", "Trạng thái", (value) => ({ DRAFT: "Nháp", POSTED: "Đã ghi tồn", CANCELLED: "Đã hủy" })[value] || value],
        ["WAREHOUSE_NAME", "Kho xuất", (value) => esc(value || "—")],
        ["POSTED_AT", "Ghi tồn", date],
      ],
      (r) => button("Xem phiếu", "view-inventory", r.ID),
    )
  );
}

async function orderEditor(id) {
  const d = await api("/api/admin/orders/" + id),
    r = d.order,
    refundHtml = window.AdminV8 ? await window.AdminV8.refundPanel(id, (d.inventoryDocuments || []).some((doc) => doc.DOC_TYPE === "RETURN" && doc.STATUS === "POSTED")) : "";
  const refundAlert =
    Number(r.STATUS) === 4 && Number(r.PAYMENT_STATUS) === 1
      ? '<section class="refund-critical-alert" role="alert" aria-live="assertive"><span class="refund-critical-alert__label">MỨC ĐỘ KHẨN CẤP</span><div><strong>Đơn đã hủy nhưng chưa hoàn tiền</strong><p>Đơn này đã thu tiền. Cần đối soát và xử lý hoàn tiền thủ công ngay.</p></div></section>'
      : "";
  const customerRequestPanel=orderCustomerRequestPanel(d.customerRequests,r);
  let tracking, trackingError, fulfillment;
  try {
    [tracking, fulfillment] = await Promise.all([
      api(`/api/admin/orders/${id}/viettel-tracking`),
      api(`/api/admin/orders/${id}/viettel-fulfillment`),
    ]);
  } catch (error) {
    trackingError = error.message;
  }
  const state = Number(r.STATUS);
  const isAdmin = typeof me !== "undefined" && me?.roles?.some((role) => role.authority === "ROLE_ADMIN");
  const hasTracking = Boolean(tracking?.TRACKING_CODE);
  const hasOtherTracking = Boolean(r.TRACKING_CODE && !/^viettel[ _]?post$/i.test(r.CARRIER || ""));
  const postedDispatch = (d.inventoryDocuments || []).some((doc) => doc.DOC_TYPE === "DISPATCH" && doc.STATUS === "POSTED");
  const canDispatch = !trackingError && !hasTracking && !r.TRACKING_CODE && state === 2 && postedDispatch && isAdmin;
  const paid = Number(r.PAYMENT_STATUS);
  const shipment = fulfillment?.delivery || {};
  const retry = shipment.SEND_STATE === "RETRY" || shipment.SEND_STATE === "SENDING";
  const shipmentState = {
    DRAFT: shipment.DOCUMENT_STATUS === "POSTED" ? "Sẵn sàng gửi" : "Nháp, chờ xuất hàng",
    SENDING: "Đang gửi hoặc chờ đối chiếu",
    RETRY: "Cần đối chiếu trước khi gửi lại",
    LINKED: "Đã có mã vận đơn",
    CANCELLED: "Đã hủy",
  }[shipment.SEND_STATE];
  const shipmentSummary = shipmentState ? `<p>Đơn giao hàng: <b>${esc(shipmentState)}</b>
    ${shipment.WEIGHT_GRAMS == null ? "" : " · " + esc(shipment.WEIGHT_GRAMS) + " g"}
    ${shipment.SERVICE_CODE ? " · Dịch vụ " + esc(shipment.SERVICE_CODE) : ""}
    ${shipment.ATTEMPTS > 0 ? " · Đã gửi " + esc(shipment.ATTEMPTS) + " lần" : ""}
    ${shipment.DOCUMENT_ID ? " · " + button("Phiếu " + esc(shipment.DOC_CODE), "view-inventory", shipment.DOCUMENT_ID) : ""}
    </p>${shipment.LAST_ERROR ? "<p>" + esc(shipment.LAST_ERROR) + "</p>" : ""}` : "";
  const paymentReady = r.PAYMENT_METHOD === "COD" || paid === 1;
  const dispatchDraft = (d.inventoryDocuments || []).find((doc) =>
    doc.DOC_TYPE === "DISPATCH" && doc.STATUS === "DRAFT");
  const workflow = {
    0: [paymentReady ? "Kiểm tra đơn và xác nhận để chuyển sang chuẩn bị hàng." : "Chờ xác nhận thanh toán.",
        paymentReady ? button("Xác nhận đơn", "confirm-order", id) : ""],
    1: [dispatchDraft ? "Mở phiếu xuất để kiểm tra và ghi tồn trước khi giao." : "Lập phiếu xuất giao từ sản phẩm trong đơn.",
        dispatchDraft ? button("Mở phiếu xuất giao", "view-inventory", dispatchDraft.ID)
          : paymentReady ? button("Lập phiếu xuất giao", "prepare-dispatch", id) : ""],
    2: [canDispatch ? "Lấy cước và chọn dịch vụ để tạo vận đơn." : hasTracking
          ? "Theo dõi vận đơn và xác nhận khi đã giao xong." : "Kiểm tra thông tin vận chuyển trước khi hoàn thành.",
        canDispatch ? button("Đến bước vận chuyển", "show-shipping", id)
          : paymentReady && paid !== 2 ? button("Xác nhận hoàn thành", "complete-order", id) : ""],
    3: [r.PAYMENT_METHOD === "COD" && paid === 0 ? "Đơn đã giao. Đối chiếu khoản thu hộ trước khi ghi nhận đã nhận tiền." : "Đơn đã hoàn thành.",
        r.PAYMENT_METHOD === "COD" && paid === 0 && isAdmin ? button("Xác nhận đã nhận tiền COD", "confirm-cod-received", id) : ""],
    4: ["Đơn đã hủy.", ""],
    5: ["Đơn đang xử lý trả hàng.", ""],
    6: ["Đơn giao thất bại. Lập phiếu nhận hoàn khi hàng về kho.", ""],
  }[state];
  const viettelPanel = trackingError
    ? `<h3>Vận chuyển</h3><p role="status">Không tải được trạng thái Viettel Post. ${esc(trackingError)}</p>`
    : `<h3>Vận chuyển</h3>${shipmentSummary}${canDispatch ? `<div class="toolbar">${button("Lấy cước theo đơn", "quote-viettel-dispatch", id)}</div><div class="grid"><label>Dịch vụ Viettel Post<select name="viettelService" disabled><option value="">Lấy cước để chọn dịch vụ</option></select></label></div><div class="toolbar">${button(retry ? "Gửi lại Viettel Post" : "Tạo vận đơn Viettel Post", "create-viettel-dispatch", id)}</div><p data-viettel-feedback aria-live="polite"></p>` : ""}${hasTracking ? `<p>Mã vận đơn Viettel Post: <b>${esc(tracking.TRACKING_CODE)}</b> ${button("Tải lại trạng thái", "refresh-viettel-tracking", id)}</p>` : !hasOtherTracking ? `<details><summary>Gắn mã vận đơn Viettel Post đã có</summary><div class="grid">${input(
        "viettelTracking", "Mã vận đơn Viettel Post", tracking.TRACKING_CODE ||
          (/^viettel[ _]?post$/i.test(r.CARRIER || "") ? r.TRACKING_CODE : ""),
        "text", false, 'maxlength="50" pattern="[A-Za-z0-9-]{1,50}"'
      )}</div><div class="toolbar">${button("Gắn mã vận đơn", "link-viettel-tracking", id)}</div></details>` : ""}
      <p>Trạng thái hãng: <b>${esc(tracking.STATUS_NAME ||
        (tracking.STATUS_CODE != null ? "Mã trạng thái " + tracking.STATUS_CODE :
          tracking.TRACKING_CODE ? "Đang chờ cập nhật" : hasOtherTracking ? "Đang dùng hãng vận chuyển khác" : "Chưa gắn mã vận đơn"))}</b>
        ${tracking.EVENT_AT ? " · " + esc(tracking.EVENT_AT) : ""}</p>
        ${Number(r.STATUS) === 2 && tracking.STATUS_CODE === 501
          ? "<p class='report-note'>Viettel Post đã báo giao thành công. Kiểm tra điều kiện hoàn thành của đơn.</p>" : ""}
      ${fulfillment?.history?.length ? `<details><summary>Hành trình Viettel Post</summary>${table(fulfillment.history,
        [["EVENT_AT", "Thời điểm"], ["STATUS_NAME", "Trạng thái hãng"], ["STATUS_CODE", "Mã"]])}</details>` : ""}`;
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
    )}<h3>Thanh toán và hãng khác</h3><div class="grid">${select(
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
    )}${input("carrier", "Đơn vị vận chuyển", r.CARRIER, "text", false)}${input("trackingCode", "Mã theo dõi", r.TRACKING_CODE, "text", false)}</div><div class="toolbar">${button("Lưu giao hàng / thanh toán", "save-shipping", id)}</div>${viettelPanel}<h3>Chuyển trạng thái</h3><div class="toolbar">${select(
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
  if (workflow) {
    const step = document.createElement("section");
    step.className = "order-detail-section order-next-step";
    step.innerHTML = `<h3>Bước tiếp theo</h3><p>${esc(workflow[0])}</p>${workflow[1]}`;
    progress.after(step);
  }

  for (const section of fields.querySelectorAll(".order-detail-section")) {
    const heading = section.querySelector("h3");
    if (!heading || !["Thanh toán và hãng khác", "Chuyển trạng thái",
        "Chứng từ xuất / hoàn theo đơn", "Tiền thực tế hoàn cho khách", "Lịch sử"].includes(heading.textContent)) continue;
    const details = document.createElement("details");
    details.className = section.className;
    const summary = document.createElement("summary");
    summary.textContent = heading.textContent;
    heading.remove();
    details.append(summary, ...section.childNodes);
    section.replaceWith(details);
  }
  const shippingSection = Array.from(fields.querySelectorAll(".order-detail-section"))
    .find((section) => section.querySelector("h3")?.textContent === "Vận chuyển");
  const nextStep = fields.querySelector(".order-next-step");
  if (state === 2 && shippingSection && nextStep) nextStep.after(shippingSection);
  const createButton = fields.querySelector("[data-action=create-viettel-dispatch]");
  const serviceField = fields.querySelector("[name=viettelService]");
  if (createButton) {
    createButton.disabled = true;
    serviceField.onchange = () => { createButton.disabled = !serviceField.value; };
  }
  if (Number(r.STATUS)>=4) progress.innerHTML=AdminOrderUI.state(r.STATUS);
  ["paymentMethod","paymentStatus"].forEach(name=>fields.querySelector("[name="+name+"]").disabled=true);
  const frozen = state >= 3 || hasTracking;
  ["carrier","trackingCode"].forEach(name=>fields.querySelector("[name="+name+"]").disabled=frozen);
  $("[data-action=save-shipping]").hidden=frozen;
  $("[data-action=save-shipping]").textContent="Lưu thông tin hãng khác";
  if (!(transitions[r.STATUS] || []).length) {
    $("[name=nextStatus]").closest('.toolbar').hidden=true;
  }
  const help=document.createElement('p');help.className='order-explainer';
  help.textContent=r.PAYMENT_METHOD==='COD'?'COD: xác nhận hoàn thành đồng nghĩa đã giao hàng và thu đủ tiền.':'Chuyển khoản: trạng thái thanh toán lấy từ giao dịch được xác nhận, không sửa bằng form giao hàng.';
  fields.querySelector("[name=paymentMethod]").closest('.grid').before(help);
  let busy = false;
  $("#fields").onclick = async (e) => {
    const b = e.target.closest("[data-action]");
    if (!b || busy) return;
    if (b.dataset.action === "show-shipping") {
      fields.querySelector("[data-action=quote-viettel-dispatch]").focus();
      return;
    }
    busy = true;
    $("#form-error").textContent = "";
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
        await window.AdminModules.documents.detail(Number(doc), id);
        return;
      }
      if (b.dataset.action === "view-inventory") {
        $("#dialog").close();
        await window.AdminModules.documents.detail(Number(b.dataset.id), id);
        return;
      }
      if (b.dataset.action === "receive-return") {
        $("#dialog").close();
        await window.AdminModules.documents.receive(id);
        return;
      }
      if (b.dataset.action === "quote-viettel-dispatch") {
        const field = fields.querySelector("[name=viettelService]");
        const feedback = fields.querySelector("[data-viettel-feedback]");
        field.replaceChildren(new Option("Chọn dịch vụ", ""));
        field.disabled = true;
        createButton.disabled = true;
        feedback.textContent = "Đang lấy cước Viettel Post…";
        try {
          const offers = await api(`/api/admin/shipping/orders/${id}/quote`, "POST");
          for (const offer of offers) {
            const eta = offer.deliveryTime ? " · " + offer.deliveryTime : "";
            field.add(new Option(offer.serviceName + " · " + money(offer.fee) + eta, offer.serviceCode));
          }
          field.disabled = !offers.length;
          if (retry && shipment.SERVICE_CODE && offers.some((offer) => offer.serviceCode === shipment.SERVICE_CODE)) {
            field.value = shipment.SERVICE_CODE;
            createButton.disabled = false;
          }
          feedback.textContent = offers.length ? "Chọn dịch vụ để tạo vận đơn." : "Không có dịch vụ phù hợp.";
          if (offers.length) field.focus();
        } catch (error) {
          feedback.textContent = error.message;
        } finally {
          b.disabled = false;
        }
        return;
      }
      if (b.dataset.action === "confirm-cod-received") {
        if (!confirm("Đã đối chiếu và nhận đủ tiền COD của đơn này?")) {
          b.disabled = false;
          return;
        }
        await api(`/api/admin/shipping/orders/${id}/cash-received`, "POST");
        $("#dialog").close();
        await load();
        await orderEditor(id);
        return;
      }
      if (b.dataset.action === "create-viettel-dispatch") {
        const serviceCode = fields.querySelector("[name=viettelService]").value.trim().toUpperCase();
        if (!serviceCode) throw Error("Dịch vụ Viettel Post là trường bắt buộc.");
        const message = retry
          ? "Đã kiểm tra Viettel Post và chưa thấy vận đơn? Hệ thống sẽ gửi lại với cùng mã đơn."
          : "Tạo vận đơn thật trên Viettel Post cho đơn " + r.BILL_CODE + "?";
        if (!confirm(message)) {
          b.disabled = false;
          return;
        }
        await api(`/api/admin/shipping/orders/${id}/create`, "POST", { serviceCode });
        $("#dialog").close();
        await orderEditor(id);
        return;
      }
      if (b.dataset.action === "link-viettel-tracking" || b.dataset.action === "refresh-viettel-tracking") {
        if (b.dataset.action === "link-viettel-tracking") {
          const field = fields.querySelector("[name=viettelTracking]");
          if (!field.value.trim()) {
            field.focus();
            throw Error("Mã vận đơn Viettel Post là trường bắt buộc.");
          }
          if (!field.reportValidity()) {
            b.disabled = false;
            return;
          }
          await api(`/api/admin/orders/${id}/viettel-tracking`, "PUT", { trackingCode: field.value.trim() });
        }
        $("#dialog").close();
        await orderEditor(id);
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
        const nextStatus = b.dataset.action === "confirm-order" ? 1 :
          b.dataset.action === "complete-order" ? 3 : $("[name=nextStatus]").value;
        if (nextStatus === "") throw Error("Trạng thái mới là trường bắt buộc.");
        if (Number(nextStatus)===3 && r.PAYMENT_METHOD==='COD'
            && !confirm('Xác nhận khách đã nhận hàng và đã thu đủ tiền COD?')) {b.disabled=false;return;}
        await api(`/api/admin/orders/${id}/status`, "PUT", {
          status: Number(nextStatus),
          note: $("[name=note]").value,
        });
      }
      $("#dialog").close();
      await load();
      await orderEditor(id);
    } catch (e) {
      $("#form-error").textContent = e.message;
      b.disabled = false;
    } finally { busy = false; }
  };
}
