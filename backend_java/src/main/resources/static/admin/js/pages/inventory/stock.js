"use strict";

(() => {
  const { params, known, isAdmin, kind, labels, dates, allRows, csv, valuation } =
    window.AdminV8Utils;
  async function skuCost(id) {
    const d = await api("/api/admin/sku-cost/" + id),
      s = d.stock;
    modal(
      "Đối chiếu giá vốn SKU " + s.SKU,
      `<p>Màu: ${esc(s.COLOR_NAME)} · Size: ${esc(s.SIZE_NAME)} · ID biến thể: ${esc(s.ID)}</p><p>Tại kho: <b>${esc(s.PHYSICAL_QUANTITY)}</b> · Giữ: ${esc(s.RESERVED_QUANTITY)} · Sẵn bán: ${esc(s.QUANTITY)}</p><p>Giá vốn hiện tại: ${known(s.AVERAGE_COST)} · Phần giá trị có căn cứ: ${money(s.KNOWN_VALUE)}</p><p class="report-note">Chốt số dư tại thời điểm hiện tại theo chứng từ đã đối chiếu. Không đổi số lượng hoặc giá vốn xuất lịch sử; chỉ ảnh hưởng giá trị tồn hiện tại và các lần xuất sau. Có thể thực hiện với SKU đã bán/xuất. Lịch sử thiếu chứng từ vẫn được đánh dấu trong báo cáo.</p>${table(
        d.receipts,
        [
          ["DOC_CODE", "Phiếu nhập"],
          ["QUANTITY", "SL lô"],
          ["UNIT_COST", "Đơn giá lô", known],
          ["POSTED_AT", "Ghi sổ", date],
        ],
      )}<div class="grid">${input("unitCost", "Giá vốn bình quân đã đối chiếu (VNĐ)", s.AVERAGE_COST ?? "", "number", true, 'min="0" max="999999999999" step="0.00000001"')}${input("evidence", "Chứng từ / mốc đối chiếu", "", "text", true, 'maxlength="500"')}${input("reason", "Lý do chốt số dư", "", "text", true, 'maxlength="500"')}</div><p id="sku-cost-preview"></p>`,
      async (form) =>
        api("/api/admin/sku-cost/" + id, "POST", {
          expectedQuantity: Number(s.PHYSICAL_QUANTITY),
          unitCost: form.unitCost,
          evidence: form.evidence,
          reason: form.reason,
        }),
    );
    $("#fields").oninput = () =>
      ($("#sku-cost-preview").textContent =
        "Giá trị sau đối chiếu: " +
        money(Number($("#fields [name=unitCost]").value) * Number(s.PHYSICAL_QUANTITY)));
  }

  const stock = {
    async render() {
      const d = await api("/api/admin/stock?" + params({ page }));
      return `<div class="metrics">${valuation("Giá trị toàn kho (gồm hàng giữ)", d.globalTotals)}${valuation("Giá trị theo bộ lọc, mọi trang", d.totals)}</div><form id="filter" class="toolbar">${input("q", "Mã / tên / SKU", filter.q || "", "text", false)}${select("productId", "Mã sản phẩm", d.products || [], filter.productId, true)}${select(
        "stockState",
        "Tình trạng",
        [
          { ID: "ZERO", NAME: "Hết sẵn bán" },
          { ID: "LOW", NAME: "Sắp hết (≤5)" },
          { ID: "HELD", NAME: "Đang giữ cho đơn" },
          { ID: "MISSING", NAME: "Thiếu giá vốn" },
        ],
        filter.stockState,
        true,
      )}${input("threshold", "Tồn sẵn bán tối đa", filter.threshold ?? "", "number", false, 'min="0"')}${input("minValue", "Tổng giá trị mã từ (VNĐ)", filter.minValue ?? "", "number", false, 'min="0"')}${input("maxValue", "Đến giá trị mã (VNĐ)", filter.maxValue ?? "", "number", false, 'min="0"')}<button>Lọc</button><button type="button" id="clear-stock-filter">Xóa bộ lọc</button></form><p class="report-note">Giá trị tại kho gồm cả hàng đang giữ. Khoảng giá trị áp dụng theo tổng mã sản phẩm. Mỗi SKU chỉ cộng một lần; không cộng lặp cột tổng theo mã. Giá vốn hiển thị làm tròn VNĐ, giá trị tính với độ chính xác nội bộ.</p>${table(
        d.items,
        [
          ["PRODUCT_CODE", "Mã"],
          ["PRODUCT_NAME", "Sản phẩm"],
          ["SKU", "SKU"],
          ["COLOR_NAME", "Màu"],
          ["SIZE_NAME", "Size"],
          ["PHYSICAL", "Tại kho"],
          ["RESERVED", "Giữ"],
          ["QUANTITY", "Sẵn bán"],
          ["AVERAGE_COST", "Vốn / cái", known],
          ["INVENTORY_VALUE", "Giá trị tại kho", known],
          ["AVAILABLE_VALUE", "Giá trị sẵn bán", known],
          ["KNOWN_VALUE", "Giá trị có căn cứ", money],
          ["PRODUCT_INVENTORY_VALUE", "Tổng mã", known],
        ],
        (r) =>
          button("Lịch sử SKU", "sku-history", r.ID) +
          button("Lập phiếu", "sku-document", r.ID) +
          (isAdmin() ? button("Đối chiếu giá vốn", "sku-cost", r.ID) : ""),
      )}${pager(d)}`;
    },
    async action(a, id) {
      if (a === "sku-history") {
        section = "history";
        filter = { variantId: id };
        page = 1;
        await load();
        return;
      }
      if (a === "sku-document") return window.AdminModules.documents.create(null, null, id);
      if (a === "sku-cost") return skuCost(id);
      return false;
    },
  };
  window.AdminV8.skuCost = skuCost;
  window.AdminV8.stock = stock;
})();
