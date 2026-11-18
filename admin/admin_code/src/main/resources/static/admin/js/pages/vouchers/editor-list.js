"use strict";
window.AdminPages ??= {};
window.AdminPages.vouchers = {
  async render() {
    let html = "";
    const rows = await api("/api/admin/vouchers");
    window.vouchers = rows;
    html =
      `<div class="toolbar">${button("+ Thêm voucher", "create", "")}</div>` +
      table(
        rows,
        [
          ["CODE", "Mã"],
          ["DISCOUNT_TYPE", "Loại"],
          ["DISCOUNT_VALUE", "Mức giảm"],
          ["MIN_ORDER", "Đơn tối thiểu", money],
          ["STARTS_AT", "Từ", date],
          ["ENDS_AT", "Đến", date],
          ["USED_COUNT", "Đã dùng"],
          ["USAGE_LIMIT", "Giới hạn"],
          ["STATUS", "Trạng thái", active],
        ],
        (r) => button("Sửa / Bật tắt", "edit", r.ID),
      );

    return html;
  },
  async action(a, id) {
    const r = window.vouchers.find((v) => String(v.ID) === String(id)) || {};
    const [products, selectedProducts] = await Promise.all([
      api("/api/admin/vouchers/products"),
      id ? api("/api/admin/vouchers/" + id + "/products") : Promise.resolve([]),
    ]);
    const selectedIds = new Set(selectedProducts.map((p) => String(p.ID)));
    const allProducts = id ? selectedIds.size === 0 : false;
    modal(
      id ? "Sửa voucher" : "Thêm voucher",
      `<div class="grid">${input("code", "Mã voucher", r.CODE)}${select(
        "type",
        "Loại giảm",
        [
          { ID: "PERCENT", NAME: "Phần trăm" },
          { ID: "FIXED", NAME: "Số tiền" },
        ],
        r.DISCOUNT_TYPE || "PERCENT",
      )}${input("value", "Mức giảm", r.DISCOUNT_VALUE ?? 10, "number", true, 'min="0.01" step="0.01"')}${input("minOrder", "Đơn tối thiểu trên sản phẩm áp dụng", r.MIN_ORDER ?? 0, "number", true, 'min="0"')}${input("maxDiscount", "Giảm tối đa (bắt buộc cho %)", r.MAX_DISCOUNT ?? 50000, "number", true, 'min="0"')}${input("usageLimit", "Giới hạn lượt", r.USAGE_LIMIT ?? 100, "number", true, 'min="0"')}${input("startsAt", "Có hiệu lực từ", r.STARTS_AT?.slice(0, 16) || "", "datetime-local")}${input("endsAt", "Hết hạn", r.ENDS_AT?.slice(0, 16) || "", "datetime-local")}${select("status", "Trạng thái", statusOptions, r.STATUS ?? 1)}<label class="wide">Sản phẩm được áp dụng<select name="productIds" multiple size="8" ${allProducts ? "" : "required"}>${products.map((p) => `<option value="${p.ID}" ${selectedIds.has(String(p.ID)) ? "selected" : ""}>${esc(p.PRODUCT_CODE)} · ${esc(p.PRODUCT_NAME)}${p.STATUS === 1 ? "" : " (ngừng bán)"}</option>`).join("")}</select></label><label class="wide"><input type="checkbox" name="allProducts" ${allProducts ? "checked" : ""}> Áp dụng cho mọi sản phẩm</label></div>`,
      (d) => {
        const all = $("#editor [name=allProducts]").checked;
        const productIds = all
          ? []
          : [...$("#editor [name=productIds]").selectedOptions].map((option) => Number(option.value));
        if (!all && !productIds.length) throw Error("Chọn ít nhất một sản phẩm hoặc áp dụng cho mọi sản phẩm.");
        return api("/api/admin/vouchers" + (id ? "/" + id : ""), id ? "PUT" : "POST", {
          code: d.code,
          type: d.type,
          value: Number(d.value),
          minOrder: Number(d.minOrder),
          maxDiscount: Number(d.maxDiscount),
          usageLimit: Number(d.usageLimit),
          startsAt: d.startsAt,
          endsAt: d.endsAt,
          status: Number(d.status),
          productIds,
        });
      },
    );
    const productSelect = $("#editor [name=productIds]"),
      allProductsToggle = $("#editor [name=allProducts]");
    allProductsToggle.onchange = () => {
      productSelect.disabled = allProductsToggle.checked;
      productSelect.required = !allProductsToggle.checked;
    };
    productSelect.disabled = allProductsToggle.checked;
  },
};
