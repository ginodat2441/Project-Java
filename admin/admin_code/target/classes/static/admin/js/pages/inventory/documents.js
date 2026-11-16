"use strict";

(() => {
  const { types, states, admin, choose, today, year, query } = window.AdminModulesUtils;
  async function renderDocuments() {
    const data = await api("/api/admin/inventory/documents?" + query({ page }));
    return `<p class="report-note">Nháp không làm đổi tồn. ADMIN xác nhận phiếu mới ghi vào tồn và lịch sử. Phiếu đã xác nhận không sửa/xóa; dùng phiếu đảo cho nhập/xuất/kiểm kê bị sai. Đặt đơn chỉ giữ hàng; ADMIN duyệt phiếu xuất giao mới giảm tại kho.</p><form id="filter" class="toolbar">${input("q", "Mã phiếu / mã đơn / người nhận", filter.q || "", "text", false)}${choose(
      "type",
      "Loại phiếu",
      Object.entries(types).map(([ID, NAME]) => ({ ID, NAME })),
      filter.type,
    )}${choose(
      "status",
      "Trạng thái",
      Object.entries(states).map(([ID, NAME]) => ({ ID, NAME })),
      filter.status,
    )}<button>Lọc phiếu</button>${button("Xuất theo đơn hàng", "order-issue", "")}${button("+ Lập phiếu khác", "create", "")}</form>${table(
      data.items,
      [
        ["DOC_CODE", "Mã phiếu"],
        ["DOC_TYPE", "Loại", (v) => esc(types[v])],
        ["STATUS", "Trạng thái", (v) => esc(states[v])],
        ["PURCHASE_TOTAL", "Giá trị nhập", (v) => (v == null ? "—" : money(v))],
        ["REASON", "Lý do"],
        [
          "BILL_CODE",
          "Đơn hàng",
          (v, r) => (r.BILL_ID ? button(esc(v || r.BILL_ID), "open-linked-order", r.BILL_ID) : "—"),
        ],
        ["RECEIVER_NAME", "Người nhận"],
        ["CREATED_BY", "Người lập"],
        ["CREATED_AT", "Ngày lập", date],
        ["POSTED_AT", "Ngày ghi tồn", date],
      ],
      (r) => button("Chi tiết", "detail", r.ID),
    )}${pager(data)}`;
  }

  async function createDocument(id = null, billId = null, variantId = null) {
    let record = id ? await api("/api/admin/inventory/documents/" + id) : null;
    const r = record?.document || { DOC_TYPE: billId ? "RETURN" : "RECEIPT", BILL_ID: billId };
    let choices = await api("/api/admin/inventory/variants");
    const oldLines = record?.lines || [];
    for (const l of oldLines)
      if (!choices.some((c) => c.ID === l.PRODUCT_DETAIL_ID))
        choices.push({
          ID: l.PRODUCT_DETAIL_ID,
          SKU: l.SKU,
          PRODUCT_NAME: l.PRODUCT_NAME,
          QUANTITY: l.EXPECTED_QUANTITY,
        });
    let initial = oldLines;
    if (billId && !id) {
      const order = await api("/api/admin/orders/" + billId);
      initial = order.lines.map((l) => ({
        PRODUCT_DETAIL_ID: l.productDetailId,
        QUANTITY: 1,
        RESTOCK: 1,
      }));
      for (const l of order.lines)
        if (!choices.some((c) => c.ID === l.productDetailId))
          choices.push({
            ID: l.productDetailId,
            SKU: l.sku,
            PRODUCT_NAME: l.productName,
            QUANTITY: "—",
          });
    }
    modal(
      id ? "Sửa phiếu nháp" : "Lập phiếu kho",
      `<div class="grid">${choose(
        "type",
        "Loại nghiệp vụ",
        Object.entries(types)
          .filter(([k]) => ["RECEIPT", "ISSUE", "COUNT", "RETURN"].includes(k))
          .map(([ID, NAME]) => ({ ID, NAME })),
        r.DOC_TYPE,
        false,
      )}${input("billId", "ID đơn gốc (bắt buộc khi nhận hoàn)", r.BILL_ID || "", "number", false, 'min="1"')}<label class="wide">Lý do / thông tin giao nhận<textarea name="reason" maxlength="500" required>${esc(r.REASON || "")}</textarea></label></div><p class="muted">Nhập/xuất/hoàn: số lượng cần xử lý. Kiểm kê: toàn bộ hàng tại kho, gồm phần giữ. Tăng kiểm kê cần đơn giá có căn cứ hoặc sẽ ghi thiếu giá. Tìm SKU hoặc tên rồi chọn biến thể. Hàng hoàn bị lỗi có thể bỏ “Nhập lại hàng bán”.</p><p>${button("Xuất hàng cho đơn bán", "order-issue", "")}</p><div id="document-lines"></div><p id="receipt-total" aria-live="polite"></p>${button("+ Thêm dòng", "add-document-line", "")}`,
      async (data) => {
        const lines = [...document.querySelectorAll(".document-line")].map((row) => ({
          variantId: Number(row.querySelector("[name=variantId]").value),
          quantity: Number(row.querySelector("[name=lineQuantity]").value),
          restock: row.querySelector("[name=restock]").checked,
          conditionNote: row.querySelector("[name=conditionNote]").value,
          unitCost:
            ["RECEIPT", "COUNT"].includes(data.type) &&
            row.querySelector("[name=unitCost]").value !== ""
              ? Number(row.querySelector("[name=unitCost]").value)
              : null,
        }));
        await api("/api/admin/inventory/documents" + (id ? "/" + id : ""), id ? "PUT" : "POST", {
          type: data.type,
          reason: data.reason,
          billId: data.type === "RETURN" ? Number(data.billId) : null,
          lines,
        });
      },
    );
    function rowOptions(rows, value) {
      return options(
        rows.map((c) => ({ ID: c.ID, NAME: `${c.SKU} · ${c.PRODUCT_NAME} · tồn ${c.QUANTITY}` })),
        value,
        true,
      );
    }
    function addLine(l = {}) {
      const row = document.createElement("div");
      row.className = "document-line";
      row.innerHTML = `<label>Tìm biến thể<input class="variant-query" placeholder="SKU hoặc tên sản phẩm"><button type="button" data-doc-action="find">Tìm</button></label><label>Biến thể<select name="variantId" required>${rowOptions(choices, l.PRODUCT_DETAIL_ID || variantId)}</select></label>${input("lineQuantity", "Số lượng", l.QUANTITY ?? 1, "number", true, 'min="0" max="100000000"')}<label class="receipt-cost">Đơn giá nhập (VNĐ)<input name="unitCost" type="number" min="0" max="999999999999.99" step="0.01" value="${esc(l.UNIT_COST ?? "")}"></label><p class="receipt-cost">Thành tiền: <output class="line-cost"></output></p><label class="restock-label"><input type="checkbox" name="restock" ${l.RESTOCK !== 0 ? "checked" : ""}>Nhập lại hàng bán</label><button type="button" data-doc-action="remove" class="secondary">Gỡ dòng</button><label class="wide">Tình trạng hàng / ghi chú<input name="conditionNote" maxlength="500" value="${esc(l.CONDITION_NOTE)}"></label>`;
      $("#document-lines").append(row);
      updateType();
    }
    function updateType() {
      const type = $("#editor [name=type]").value;
      $("#editor [name=billId]").required = type === "RETURN";
      $("#editor [name=billId]").disabled = type !== "RETURN";
      $("#editor [name=billId]").closest("label").hidden = type !== "RETURN";
      document.querySelectorAll(".document-line").forEach((row) => {
        row
          .querySelectorAll(".receipt-cost")
          .forEach((el) => (el.hidden = !["RECEIPT", "COUNT"].includes(type)));
        row.querySelector("[name=unitCost]").required = type === "RECEIPT";
        row.querySelector("[name=unitCost]").disabled = !["RECEIPT", "COUNT"].includes(type);
        row.querySelector(".restock-label").hidden = type !== "RETURN";
        row.querySelector("[name=lineQuantity]").min = type === "COUNT" ? 0 : 1;
      });
      updateTotals();
    }
    function updateTotals() {
      let total = 0;
      document.querySelectorAll(".document-line").forEach((row) => {
        const value =
          (Math.round(Number(row.querySelector("[name=unitCost]").value || 0) * 100) *
            Number(row.querySelector("[name=lineQuantity]").value || 0)) /
          100;
        row.querySelector(".line-cost").textContent = money(value);
        total += value;
      });
      $("#receipt-total").hidden = $("#editor [name=type]").value !== "RECEIPT";
      $("#receipt-total").textContent = "Tổng giá trị nhập: " + money(total);
    }
    $("#document-lines").oninput = updateTotals;
    $("#editor [name=type]").onchange = updateType;
    $("#fields").onclick = async (e) => {
      const button = e.target.closest("button");
      if (!button) return;
      if (button.dataset.action === "order-issue") {
        await orderIssuePicker();
        return;
      }
      if (button.dataset.action === "add-document-line") {
        addLine();
        return;
      }
      const row = button.closest(".document-line");
      if (!row) return;
      if (button.dataset.docAction === "remove") {
        row.remove();
        updateTotals();
        return;
      }
      if (button.dataset.docAction === "find") {
        button.disabled = true;
        try {
          const result = await api(
            "/api/admin/inventory/variants?q=" +
              encodeURIComponent(row.querySelector(".variant-query").value),
          );
          row.querySelector("[name=variantId]").innerHTML = rowOptions(result, null);
          if (!result.length) $("#form-error").textContent = "Không có biến thể phù hợp.";
        } catch (e) {
          $("#form-error").textContent = e.message;
        } finally {
          button.disabled = false;
        }
      }
    };
    if (initial.length) initial.forEach(addLine);
    else addLine();
    updateType();
  }

  async function detailDocument(id) {
    const data = await api("/api/admin/inventory/documents/" + id),
      r = data.document;
    modal(
      "Phiếu " + r.DOC_CODE,
      `<div class="grid"><section><p><b>${esc(types[r.DOC_TYPE])}</b> · ${esc(states[r.STATUS])}</p><p>${esc(r.REASON)}</p><p>Đơn gốc: ${r.BILL_ID ? button(esc(r.BILL_CODE || r.BILL_ID), "open-linked-order", r.BILL_ID) : "—"}${r.RECEIVER_NAME ? " · " + esc(r.RECEIVER_NAME) : ""}${r.ORDER_STATUS != null ? " · " + esc(statusNames[r.ORDER_STATUS]) : ""} · Phiếu gốc: ${esc(r.SOURCE_ID || "—")}</p></section><section><p>Lập: ${esc(r.CREATOR || "Hệ thống / khách")} · ${date(r.CREATED_AT)}</p><p>Xác nhận: ${esc(r.APPROVER || "—")} · ${date(r.POSTED_AT)}</p><p>Lý do hủy: ${esc(r.CANCEL_REASON || "—")}</p></section></div>${r.DOC_TYPE === "RECEIPT" ? `<p><b>Tổng giá trị nhập: ${data.lines.some((l) => l.UNIT_COST == null) ? "Chưa đủ đơn giá" : money(data.lines.reduce((sum, l) => sum + Number(l.LINE_COST || 0), 0))}</b></p>` : ""}${table(
        data.lines,
        [
          ["SKU", "SKU"],
          ["PRODUCT_NAME", "Sản phẩm"],
          ["COLOR_NAME", "Màu"],
          ["SIZE_NAME", "Size"],
          ["UNIT_COST", "Đơn giá nhập", (v) => (v == null ? "—" : money(v))],
          ["LINE_COST", "Thành tiền nhập", (v) => (v == null ? "—" : money(v))],
          ["QUANTITY", "SL xử lý"],
          ["PHYSICAL_BEFORE", "Tại kho trước"],
          ["PHYSICAL_AFTER", "Tại kho sau"],
          ["RESERVED_BEFORE", "Giữ trước"],
          ["RESERVED_AFTER", "Giữ sau"],
          ["COST_UNIT", "Vốn áp dụng", (v) => (v == null ? "Thiếu giá" : money(v))],
          ["COST_TOTAL", "Giá vốn dòng", (v) => (v == null ? "Thiếu giá" : money(v))],
          ["BEFORE_QUANTITY", "Sẵn bán trước"],
          ["STOCK_DELTA", "Tăng / giảm"],
          ["AFTER_QUANTITY", "Tồn sau"],
          [
            "RESTOCK",
            "Nhập lại hàng hoàn",
            (v) => (r.DOC_TYPE === "RETURN" ? (v === 1 ? "Có" : "Không") : "—"),
          ],
          ["CONDITION_NOTE", "Tình trạng"],
          ["RETURN_VALUE", "Giá trị hàng hoàn", (v) => money(v)],
        ],
      )}<div class="toolbar">${r.STATUS === "DRAFT" && !["REVERSAL", "DISPATCH"].includes(r.DOC_TYPE) ? button("Sửa phiếu nháp", "edit-document", id) : ""}${r.STATUS === "DRAFT" && admin() ? button("Xác nhận và ghi tồn", "approve-document", id) : ""}${r.STATUS === "DRAFT" ? button("Hủy phiếu nháp", "cancel-document", id) : ""}${r.STATUS === "POSTED" && admin() && ["RECEIPT", "ISSUE", "COUNT", "RETURN"].includes(r.DOC_TYPE) ? button("Lập phiếu đảo", "reverse-document", id) : ""}</div><div id="document-confirm"></div>`,
    );
    $("#fields").onclick = async (e) => {
      const b = e.target.closest("[data-action]");
      if (!b) return;
      const action = b.dataset.action;
      if (action === "open-linked-order") {
        await orderEditor(Number(b.dataset.id));
        return;
      }
      if (action === "edit-document") {
        $("#dialog").close();
        await createDocument(id);
        return;
      }
      if (!["approve-document", "cancel-document", "reverse-document"].includes(action)) return;
      const note = action !== "approve-document";
      $("#document-confirm").innerHTML =
        `<p>${note ? "Nhập lý do để tiếp tục." : "Xác nhận phiếu sẽ ghi thay đổi tồn và không cho sửa nội dung. Bạn đã kiểm tra số lượng và hàng thực tế?"}</p>${note ? input("operationReason", "Lý do", "", "text", true) : ""}<button type="button" id="confirm-document-action">${note ? "Xác nhận thao tác" : "Xác nhận ghi tồn"}</button>`;
      $("#confirm-document-action").onclick = async () => {
        const button = $("#confirm-document-action");
        button.disabled = true;
        try {
          const reason = note ? $("#editor [name=operationReason]").value.trim() : "";
          if (note && !reason) throw Error("Nhập lý do.");
          const suffix =
            action === "approve-document"
              ? "approve"
              : action === "cancel-document"
                ? "cancel"
                : "reverse";
          const result = await api(
            `/api/admin/inventory/documents/${id}/${suffix}`,
            "POST",
            note ? { reason } : undefined,
          );
          $("#dialog").close();
          notice("Đã xử lý phiếu thành công.");
          await load();
          await detailDocument(suffix === "reverse" ? Number(result) : id);
        } catch (e) {
          $("#form-error").textContent = e.message;
          button.disabled = false;
        }
      };
    };
  }

  async function orderIssuePicker() {
    modal(
      "Xuất theo đơn hàng",
      `<p>Chọn đơn để xem phiếu xuất bán đã liên kết. Sản phẩm và số lượng lấy từ đơn; hệ thống đã giữ/trừ tồn khi đặt hàng nên không lập phiếu trừ thêm.</p><label>Tìm mã đơn / người nhận / điện thoại<input id="issue-order-query" type="search" autocomplete="off"></label><button type="button" id="issue-order-search">Tìm đơn</button><div id="issue-order-results"></div>`,
    );
    const search = async () => {
      const b = $("#issue-order-search");
      b.disabled = true;
      try {
        const rows = await api(
          "/api/admin/inventory/order-issues?q=" +
            encodeURIComponent($("#issue-order-query").value),
        );
        $("#issue-order-results").innerHTML = table(
          rows,
          [
            ["BILL_CODE", "Mã đơn"],
            ["RECEIVER_NAME", "Người nhận"],
            ["ORDER_STATUS", "Trạng thái đơn", (v) => esc(statusNames[v])],
            ["DOC_CODE", "Phiếu xuất"],
          ],
          (r) => button("Xem phiếu xuất", "choose-order-issue", r.DOCUMENT_ID),
        );
      } catch (e) {
        $("#form-error").textContent = e.message;
      } finally {
        b.disabled = false;
      }
    };
    $("#issue-order-search").onclick = search;
    $("#issue-order-query").onkeydown = (e) => {
      if (e.key === "Enter") {
        e.preventDefault();
        search();
      }
    };
    $("#fields").onclick = async (e) => {
      const b = e.target.closest("[data-action=choose-order-issue]");
      if (b)
        try {
          await detailDocument(Number(b.dataset.id));
        } catch (error) {
          $("#form-error").textContent = error.message;
        }
    };
    await search();
  }

  async function documentAction(action, id) {
    if (action === "create") return createDocument();
    if (action === "detail") return detailDocument(id);
    if (action === "order-issue") return orderIssuePicker();
    if (action === "open-linked-order") return orderEditor(id);
  }
  Object.assign(window.AdminModules, {
    documents: {
      render: renderDocuments,
      action: documentAction,
      detail: detailDocument,
      create: createDocument,
      receive: (bill) => createDocument(null, bill),
    },
  });
})();
