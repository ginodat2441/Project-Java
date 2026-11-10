"use strict";
window.AdminPages ??= {};
window.AdminPages.products = {
  async render() {
    let html = "";
    const qs = new URLSearchParams({ page, q: filter.q || "" });
    if (filter.categoryId) qs.set("categoryId", filter.categoryId);
    if (filter.status !== "undefined" && filter.status) qs.set("status", filter.status);
    for (const k of ["from", "to"]) if (filter[k]) qs.set(k, filter[k]);
    const data = await api("/api/admin/products?" + qs);
    const totalsRow = `<tr id="product-totals"><th colspan="7">Tổng cộng</th><td>${data.totals?.INVENTORY_VALUE == null ? "Chưa đủ giá vốn" : money(data.totals.INVENTORY_VALUE)}</td><td>${money(data.totals?.KNOWN_VALUE || 0)}</td><td>${esc(data.totals?.UNKNOWN_COST_SKUS || 0)}</td><td>${money(data.totals?.TOTAL_REVENUE || 0)}</td><td colspan="2"></td></tr>`;
    html =
      searchToolbar(
        select("categoryId", "Danh mục", lookups.categories, filter.categoryId, true) +
          select("status", "Trạng thái", statusOptions, filter.status, true) +
          input("from", "Doanh thu từ ngày", filter.from || "", "date", false) +
          input("to", "Đến ngày", filter.to || "", "date", false),
        "Sản phẩm",
      ) +
      `<p class="report-note">Giá trị tồn kho là số hiện tại (gồm hàng giữ tại kho). Bộ lọc ngày chỉ áp dụng cho doanh thu; hàng hoàn ghi giảm vào ngày xác nhận nhận hoàn. Tổng cộng gồm tất cả mã khớp bộ lọc, qua mọi trang.</p>` +
      table(
        data.items,
        [
          ["ID", "ID"],
          ["PRODUCT_CODE", "Mã"],
          ["PRODUCT_NAME", "Tên sản phẩm"],
          ["CATEGORY_NAME", "Danh mục"],
          ["PHYSICAL", "Tồn tại kho"],
          ["RESERVED", "Giữ"],
          ["STOCK", "Sẵn bán"],
          ["INVENTORY_VALUE", "Giá trị tồn kho", (v) => (v == null ? "Chưa đủ giá vốn" : money(v))],
          ["KNOWN_VALUE", "Giá trị có căn cứ", money],
          ["UNKNOWN_COST_SKUS", "SKU thiếu giá"],
          ["TOTAL_REVENUE", "Tổng doanh thu", (v) => money(v ?? 0)],
          ["STATUS", "Trạng thái", active],
        ],
        (r) =>
          button("Sửa", "edit", r.ID) +
          button("Ảnh", "images", r.ID) +
          button("Doanh thu SKU", "product-revenue", r.ID),
      ).replace("</tbody>", `</tbody><tfoot>${totalsRow}</tfoot>`) +
      (data.items.length
        ? ""
        : `<div class="table-wrap"><table><tbody>${totalsRow}</tbody></table></div>`) +
      pager(data);

    return html;
  },
  async action(a, id) {
    if (a === "product-revenue") return window.AdminModules.reports.action("product-report", id);
    if (a === "images") return imageEditor(id);
    lookups = await api("/api/admin/lookups");
    const p = id ? await api("/api/admin/products/" + id) : { STATUS: 0, variants: [] };
    let photos;
    modal(
      id ? "Sửa sản phẩm" : "Thêm sản phẩm",
      `<div class="grid">${productCodeInput(p)}${input("name", "Tên sản phẩm", p.PRODUCT_NAME)}${namedLookup("brandName", "Th\u01b0\u01a1ng hi\u1ec7u", lookups.brands, p.BRAND_ID)}${namedLookup("materialName", "Ch\u1ea5t li\u1ec7u", lookups.materials, p.MATERIAL_ID)}${categorySearch(p.CATEGORY_ID)}${select(
        "gender",
        "Đối tượng",
        [
          { ID: 0, NAME: "Nữ" },
          { ID: 1, NAME: "Nam" },
          { ID: 2, NAME: "Unisex" },
        ],
        p.TARGET_GENDER ?? 2,
      )}${select("status", "Trạng thái", statusOptions, p.STATUS)}<input type="hidden" name="thumbnailUrl" value="${esc(p.THUMBNAIL_URL)}"><label class="wide">Mô tả<textarea name="description">${esc(p.DESCRIPTION)}</textarea></label></div>${productPhotoFields()}<h3>Biến thể</h3><p class="muted">Mỗi dòng là một màu và size. Thêm dòng để bán nhiều màu/size cho cùng sản phẩm.</p><details class="color-creator"><summary>+ Thêm màu mới</summary><div class="toolbar"><label>Tên màu<input name="newColorName" maxlength="100" placeholder="Ví dụ: Xanh navy, Be, Đỏ đô"></label><label>Mã màu (không bắt buộc)<input name="newColorHex" maxlength="7" placeholder="#1A237E"></label><button type="button" data-action="create-color">Thêm màu</button></div><p id="color-message" role="status"></p></details><div id="variants"></div>${button("+ Thêm biến thể", "add-variant", "")}`,
      async (d) => {
        const variants = [...document.querySelectorAll(".variant")].map((row) => ({
          id: row.dataset.id ? Number(row.dataset.id) : null,
          colorId: Number(row.querySelector("[name=colorId]").value),
          size: row.querySelector("[name=size]").value,
          sku: row.querySelector("[name=sku]").value,
          price: Number(row.querySelector("[name=price]").value),
          quantity: Number(row.dataset.stockQuantity),
          status: Number(row.querySelector("[name=vstatus]").value),
        }));
        const product = {
          code: d.code,
          name: d.name,
          brandName: d.brandName.trim(),
          materialName: d.materialName.trim(),
          categoryId: d.categoryId ? Number(d.categoryId) : null,
          gender: Number(d.gender),
          status: Number(d.status),
          description: d.description,
          thumbnailUrl: d.thumbnailUrl || null,
          variants,
        };
        if (photos.pending.length) {
          const body = new FormData();
          body.append(
            "product",
            new Blob([JSON.stringify(product)], { type: "application/json" }),
            "product.json",
          );
          body.append(
            "images",
            new Blob([JSON.stringify(photos.metadata())], { type: "application/json" }),
            "images.json",
          );
          photos.pending.forEach((p) => body.append("files", p.file));
          await api(
            "/api/admin/products" + (id ? "/" + id : "") + "/with-images",
            id ? "PUT" : "POST",
            body,
          );
        } else
          await api("/api/admin/products" + (id ? "/" + id : ""), id ? "PUT" : "POST", product);
      },
    );
    const addVariant = (v) => {
      const row = document.createElement("div");
      row.className = "variant";
      row.dataset.id = v.ID || "";
      row.dataset.stockQuantity = String(v.QUANTITY ?? 0);
      row.innerHTML =
        select("colorId", "Màu", lookups.colors, v.COLOR_ID, true) +
        input("size", "Size", v.SIZE_NAME || "M") +
        variantSkuInput(v) +
        input("price", "Giá bán", v.PRICE ?? 0, "number", true, 'min="0" step="0.01"') +
        select("vstatus", "Bán", statusOptions, v.STATUS ?? 1) +
        (v.ID ? "" : button("×", "remove-variant", ""));
      row.querySelector("[name=colorId]").required = true;
      row.addEventListener("input", () => refreshVariantSku(row, p.PRODUCT_CODE));
      row.addEventListener("change", () => refreshVariantSku(row, p.PRODUCT_CODE));
      $("#variants").append(row);
      refreshVariantSku(row, p.PRODUCT_CODE);
    };
    photos = bindProductPhotos(p);
    bindCategorySearch();
    p.variants.forEach(addVariant);
    if (!p.variants.length) addVariant({});
    $("#fields").onclick = async (e) => {
      const b = e.target.closest("[data-action]");
      if (b?.dataset.action === "add-variant") addVariant({});
      if (b?.dataset.action === "remove-variant") b.closest(".variant").remove();
      if (b?.dataset.action === "create-color") {
        const name = $("#editor [name=newColorName]"),
          hex = $("#editor [name=newColorHex]"),
          message = $("#color-message");
        if (!name.value.trim()) {
          message.textContent = "Nhập tên màu mới.";
          name.focus();
          return;
        }
        if (hex.value.trim() && !/^#[0-9a-f]{6}$/i.test(hex.value.trim())) {
          message.textContent = "Mã màu có dạng #1A237E hoặc để trống.";
          return;
        }
        b.disabled = true;
        message.textContent = "Đang lưu màu…";
        try {
          const color = await api("/api/admin/colors", "POST", {
            name: name.value.trim(),
            hexCode: hex.value.trim() || null,
          });
          if (!lookups.colors.some((c) => String(c.ID) === String(color.ID)))
            lookups.colors.push(color);
          lookups.colors.sort((a, b) => a.NAME.localeCompare(b.NAME, "vi"));
          const selects = [...document.querySelectorAll("#variants [name=colorId]")],
            target = selects.filter((s) => !s.value).pop();
          selects.forEach((s) => {
            const previous = s.value;
            s.innerHTML = options(lookups.colors, s === target ? color.ID : previous, true);
          });
          document.querySelectorAll("#product-photos [name=photoColor]").forEach((s) => {
            const previous = s.value;
            s.innerHTML = options(lookups.colors, previous, true);
          });
          document
            .querySelectorAll("#variants .variant")
            .forEach((row) => refreshVariantSku(row, p.PRODUCT_CODE));
          name.value = "";
          hex.value = "";
          message.textContent = "Màu " + color.NAME + " đã có trong danh sách chọn.";
        } catch (error) {
          message.textContent = error.message;
        } finally {
          b.disabled = false;
        }
      }
    };
  },
};
