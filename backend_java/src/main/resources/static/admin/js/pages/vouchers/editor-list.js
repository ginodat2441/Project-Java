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
          ["ID", "Đối tượng", (_, r) => {
            const rules = [];
            if (r.NEW_ACCOUNT_DAYS != null) rules.push(`Tài khoản trong ${r.NEW_ACCOUNT_DAYS} ngày`);
            if (Number(r.NO_PREVIOUS_ORDERS) === 1) rules.push("Chưa có đơn");
            if (r.MINIMUM_ORDERS != null) rules.push(`Từ ${r.MINIMUM_ORDERS} đơn`);
            return esc(rules.length ? rules.join(" · ") : "Tất cả khách hàng");
          }],
          ["STARTS_AT", "Từ", date],
          ["ENDS_AT", "Đến", (v) => (v == null ? "Không hết hạn" : date(v))],
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
    const noExpiry = Boolean(id && r.ENDS_AT == null);
    const choice = (name, title, description, checked) => `
      <label class="voucher-choice">
        <input type="checkbox" name="${name}" ${checked ? "checked" : ""}>
        <span><strong>${title}</strong><small>${description}</small></span>
      </label>`;
    const productOptions = products
      .map(
        (p) =>
          `<option value="${p.ID}" ${selectedIds.has(String(p.ID)) ? "selected" : ""}>${esc(p.PRODUCT_CODE)} · ${esc(p.PRODUCT_NAME)}${p.STATUS === 1 ? "" : " (ngừng bán)"}</option>`,
      )
      .join("");
    modal(
      id ? "Sửa voucher" : "Thêm voucher",
      `<div class="voucher-editor">
        <section class="voucher-section">
          <div class="voucher-section-heading">
            <div><h3>Thông tin ưu đãi</h3><p>Cấu hình mã và mức giảm hiển thị cho khách hàng.</p></div>
            <span class="voucher-section-number">01</span>
          </div>
          <div class="voucher-grid">
            ${input("code", "Mã voucher", r.CODE)}
            ${select(
              "type",
              "Loại giảm",
              [
                { ID: "PERCENT", NAME: "Phần trăm" },
                { ID: "FIXED", NAME: "Số tiền" },
              ],
              r.DISCOUNT_TYPE || "PERCENT",
            )}
            ${input("value", "Mức giảm", r.DISCOUNT_VALUE ?? 10, "number", true, 'min="0.01" step="0.01"')}
            ${input("minOrder", "Đơn tối thiểu trên sản phẩm áp dụng", r.MIN_ORDER ?? 0, "number", true, 'min="0"')}
            ${input("maxDiscount", "Giảm tối đa (áp dụng cho giảm %)", r.MAX_DISCOUNT ?? 50000, "number", true, 'min="0"')}
            ${input("usageLimit", "Tổng lượt sử dụng", r.USAGE_LIMIT ?? 100, "number", true, 'min="0"')}
            ${select("status", "Trạng thái", statusOptions, r.STATUS ?? 1)}
          </div>
        </section>

        <section class="voucher-section">
          <div class="voucher-section-heading">
            <div><h3>Thời hạn áp dụng</h3><p>Chọn khoảng hiệu lực hoặc để voucher không hết hạn.</p></div>
            <span class="voucher-section-number">02</span>
          </div>
          ${choice(
            "noExpiry",
            "Không giới hạn thời gian",
            "Voucher bắt đầu có hiệu lực ngay khi tạo và không có ngày kết thúc.",
            noExpiry,
          )}
          <div class="voucher-grid voucher-dates">
            ${input("startsAt", "Có hiệu lực từ", r.STARTS_AT?.slice(0, 16) || "", "datetime-local", !noExpiry)}
            ${input("endsAt", "Hết hạn", r.ENDS_AT?.slice(0, 16) || "", "datetime-local", !noExpiry)}
          </div>
        </section>

        <section class="voucher-section">
          <div class="voucher-section-heading">
            <div><h3>Đối tượng khách hàng</h3><p>Có thể kết hợp nhiều điều kiện; khách cần thỏa tất cả điều kiện đã chọn.</p></div>
            <span class="voucher-section-number">03</span>
          </div>
          <div class="voucher-audience-grid">
            <div class="voucher-audience-card">
              ${choice(
                "recentAccounts",
                "Tài khoản mới",
                "Chỉ áp dụng cho tài khoản tạo trong khoảng thời gian gần đây.",
                r.NEW_ACCOUNT_DAYS != null,
              )}
              <div class="voucher-audience-field">
                ${input("newAccountDays", "Số ngày gần nhất", r.NEW_ACCOUNT_DAYS ?? 7, "number", false, 'min="1" max="36500" step="1"')}
              </div>
            </div>
            <div class="voucher-audience-card">
              ${choice(
                "noPreviousOrders",
                "Chưa từng đặt đơn",
                "Chỉ áp dụng cho khách chưa có đơn đủ điều kiện.",
                Number(r.NO_PREVIOUS_ORDERS) === 1,
              )}
            </div>
            <div class="voucher-audience-card">
              ${choice(
                "minimumOrdersEnabled",
                "Khách hàng thân thiết",
                "Đặt điều kiện số đơn tối thiểu đã hoàn thành hoặc đang xử lý.",
                r.MINIMUM_ORDERS != null,
              )}
              <div class="voucher-audience-field">
                ${input("minimumOrders", "Số đơn tối thiểu", r.MINIMUM_ORDERS ?? 3, "number", false, 'min="1" step="1"')}
              </div>
            </div>
          </div>
          <p class="voucher-hint">Đơn đã hủy hoặc giao thất bại không được tính vào số đơn.</p>
        </section>

        <section class="voucher-section">
          <div class="voucher-section-heading">
            <div><h3>Sản phẩm áp dụng</h3><p>Chọn sản phẩm cụ thể hoặc mở voucher cho toàn bộ sản phẩm.</p></div>
            <span class="voucher-section-number">04</span>
          </div>
          ${choice(
            "allProducts",
            "Áp dụng cho mọi sản phẩm",
            "Khách có thể dùng voucher với tất cả sản phẩm đang bán.",
            allProducts,
          )}
          <label class="voucher-product-label">Danh sách sản phẩm
            <select class="voucher-product-list" name="productIds" multiple size="7" aria-label="Sản phẩm được áp dụng" ${allProducts ? "" : "required"}>${productOptions}</select>
            <small>Giữ Ctrl (Windows) hoặc Command (Mac) để chọn nhiều sản phẩm.</small>
          </label>
        </section>
      </div>`,
      (d) => {
        const all = $("#editor [name=allProducts]").checked;
        const noExpiryEnabled = $("#editor [name=noExpiry]").checked;
        const recentEnabled = $("#editor [name=recentAccounts]").checked;
        const noOrdersEnabled = $("#editor [name=noPreviousOrders]").checked;
        const minimumOrdersEnabled = $("#editor [name=minimumOrdersEnabled]").checked;
        const days = recentEnabled ? Number(d.newAccountDays) : null;
        const minimumOrders = minimumOrdersEnabled ? Number(d.minimumOrders) : null;
        if (recentEnabled && (!Number.isInteger(days) || days < 1 || days > 36500))
          throw Error("Số ngày tạo tài khoản phải từ 1 đến 36500.");
        if (minimumOrdersEnabled && (!Number.isInteger(minimumOrders) || minimumOrders < 1))
          throw Error("Số đơn tối thiểu phải lớn hơn 0.");
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
          startsAt: noExpiryEnabled ? null : d.startsAt,
          endsAt: noExpiryEnabled ? null : d.endsAt,
          status: Number(d.status),
          productIds,
          noExpiry: noExpiryEnabled,
          newAccountDays: days,
          noPreviousOrders: noOrdersEnabled,
          minimumOrders,
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
    const noExpiryToggle = $("#editor [name=noExpiry]"),
      startInput = $("#editor [name=startsAt]"),
      endInput = $("#editor [name=endsAt]");
    const syncExpiry = () => {
      startInput.disabled = endInput.disabled = noExpiryToggle.checked;
      startInput.required = endInput.required = !noExpiryToggle.checked;
    };
    noExpiryToggle.onchange = syncExpiry;
    syncExpiry();
    const targetControls = [
      ["recentAccounts", "newAccountDays"],
      ["minimumOrdersEnabled", "minimumOrders"],
    ];
    for (const [toggleName, fieldName] of targetControls) {
      const toggle = $("#editor [name=" + toggleName + "]"),
        field = $("#editor [name=" + fieldName + "]");
      const syncTarget = () => {
        field.disabled = !toggle.checked;
        field.required = toggle.checked;
      };
      toggle.onchange = syncTarget;
      syncTarget();
    }
  },
};
