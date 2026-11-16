async function load() {
  $("#title").textContent = titles[section];
  document
    .querySelectorAll("#nav button")
    .forEach((b) => b.classList.toggle("active", b.dataset.section === section));
  $("#content").innerHTML = '<p class="empty">Đang tải dữ liệu…</p>';
  try {
    let html = "";
    if (window.AdminV8?.[section]?.render) html = await window.AdminV8[section].render();
    else if (window.AdminModules?.[section]?.render)
      html = await window.AdminModules[section].render();
    else if (window.AdminPages?.[section]?.render) html = await window.AdminPages[section].render();
    $("#content").innerHTML = html;
    if (section === "stock")
      $("#clear-stock-filter").onclick = () => {
        filter = {};
        page = 1;
        load();
      };
    if (section === "dashboard")
      $("#content").insertAdjacentHTML(
        "afterbegin",
        `<div class="toolbar">${button("Thống kê theo ngày / tháng / quý / năm", "open-reports", "")}</div>`,
      );
  } catch (e) {
    $("#content").innerHTML =
      '<div class="card">Không tải được dữ liệu. Hãy kiểm tra kết nối và thử lại.</div>';
    notice(e.message, true);
  }
}

async function action(a, id) {
  if (a === "prev" || a === "next") {
    page += a === "next" ? 1 : -1;
    await load();
    return;
  }
  if (a === "customers" || a === "managers") {
    tab = a;
    page = 1;
    await load();
    return;
  }
  if (a === "open-reports") {
    section = "reports";
    page = 1;
    filter = {};
    await load();
    return;
  }
  if (window.AdminV8?.[section]?.action && (await window.AdminV8[section].action(a, id)) !== false)
    return;
  if (window.AdminModules?.[section]) return window.AdminModules[section].action(a, id);
  if (section === "stock" && a === "edit")
    return window.AdminModules.documents.create(null, null, id);
  if (section === "stock" && a === "opening-cost") {
    const data = await api("/api/admin/opening-cost/" + id),
      p = data.product,
      missing = data.receipts.filter((l) => l.UNIT_COST == null);
    modal(
      "Bổ sung giá vốn · " + p.PRODUCT_CODE,
      `<p>${esc(p.PRODUCT_NAME)} · Tổng tồn hiện tại: <b>${esc(data.quantity)}</b></p><p>Giá nhập mới đã được ghi nhận. Nhập giá vốn của tồn cũ và những phiếu cũ còn thiếu để tính đủ bình quân cho toàn mã. Số lượng tồn không thay đổi.</p><div class="grid">${data.openingQuantity > 0 ? input("openingUnitCost", "Đơn giá vốn của " + data.openingQuantity + " sản phẩm tồn đầu chưa có chứng từ", "", "number", true, 'min="0" max="999999999999.99" step="0.01"') : ""}${missing.map((l) => `<label>Phiếu ${esc(l.DOC_CODE)} · ${esc(l.SKU)} · SL ${esc(l.QUANTITY)}<input type="number" data-cost-line="${esc(l.ID)}" min="0" max="999999999999.99" step="0.01" required></label>`).join("")}${input("reason", "Căn cứ / lý do bổ sung", "", "text", true, 'maxlength="500"')}</div>${table(
        data.receipts,
        [
          ["DOC_CODE", "Phiếu nhập"],
          ["SKU", "SKU"],
          ["QUANTITY", "Số lượng"],
          ["UNIT_COST", "Đơn giá đã ghi", (v) => (v == null ? "Cần bổ sung" : money(v))],
        ],
      )}<p id="opening-cost-preview" aria-live="polite"></p><p class="muted">ADMIN xác nhận theo chứng từ thực tế. Thao tác được ghi nhật ký và không sửa giá nhập đã có.</p>`,
      async (d) => {
        await api("/api/admin/opening-cost/" + id, "POST", {
          openingUnitCost: data.openingQuantity > 0 ? Number(d.openingUnitCost) : null,
          reason: d.reason,
          receipts: missing.map((l) => ({
            lineId: l.ID,
            unitCost: Number(document.querySelector(`[data-cost-line="${l.ID}"]`).value),
          })),
        });
      },
    );
    const calculate = () => {
      const inputs = [...document.querySelectorAll("#fields input[type=number]")],
        ready = inputs.every((el) => el.value !== "" && el.checkValidity());
      if (!ready) {
        $("#opening-cost-preview").textContent =
          "Nhập đủ đơn giá còn thiếu để xem giá trị tồn và giá vốn bình quân.";
        return;
      }
      const receiptTotal = data.receipts.reduce(
          (sum, l) =>
            sum +
            l.QUANTITY *
              Number(l.UNIT_COST ?? document.querySelector(`[data-cost-line="${l.ID}"]`).value),
          0,
        ),
        total =
          receiptTotal +
          data.openingQuantity * Number($("#fields [name=openingUnitCost]")?.value || 0);
      $("#opening-cost-preview").textContent =
        "Giá trị tồn sau bổ sung: " +
        money(total) +
        " · Giá vốn bình quân: " +
        money(total / data.quantity);
    };
    $("#fields").oninput = calculate;
    calculate();
    return;
  }
  if (a === "lock" || a === "unlock") {
    await api(`/api/admin/customers/${id}/status?status=${a === "lock" ? 0 : 1}`, "PUT");
    notice("Đã cập nhật tài khoản.");
    await load();
    return;
  }
  if (window.AdminPages?.[section]?.action) return window.AdminPages[section].action(a, id);
}
