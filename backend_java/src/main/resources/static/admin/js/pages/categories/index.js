"use strict";

(() => {
  const { params, known, isAdmin, kind, labels, dates, allRows, csv, valuation } =
    window.AdminV8Utils;
  const categories = {
    async render() {
      const rows = await api("/api/admin/categories?" + params({}));
      lookups = await api("/api/admin/lookups");
      return (
        searchToolbar("", "Danh mục") +
        `<p class="report-note">Ẩn cha sẽ ẩn hiệu lực toàn nhánh trên cửa hàng và khi đặt đơn. Khôi phục cha giữ nguyên trạng thái riêng của các danh mục con.</p>` +
        table(
          rows,
          [
            [
              "CATEGORY_PATH",
              "Cây danh mục",
              (_, r) =>
                `${"　".repeat(Math.max(0, Number(r.DEPTH || 1) - 1))}${esc(r.CATEGORY_NAME)}`,
            ],
            ["STATUS", "Trạng thái riêng", active],
            ["EFFECTIVE_STATUS", "Hiệu lực cửa hàng", active],
          ],
          (r) => button("Sửa / Ẩn / Khôi phục", "edit", r.ID),
        )
      );
    },
  };
  window.AdminV8.categories = categories;
})();
