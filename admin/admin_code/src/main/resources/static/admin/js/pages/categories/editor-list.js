"use strict";
window.AdminPages ??= {};
window.AdminPages.categories = {
  async render() {
    let html = "";
    const rows = await api("/api/admin/categories");
    lookups = await api("/api/admin/lookups");
    html =
      `<div class="toolbar">${button("+ Thêm danh mục", "create", "")}</div>` +
      table(
        rows,
        [
          ["ID", "ID"],
          ["CATEGORY_NAME", "Danh mục"],
          [
            "PARENT_ID",
            "Danh mục cha",
            (v) => esc(rows.find((r) => r.ID === v)?.CATEGORY_NAME || "—"),
          ],
          ["STATUS", "Trạng thái", active],
        ],
        (r) => button("Sửa / Ẩn / Khôi phục", "edit", r.ID),
      );

    return html;
  },
  async action(a, id) {
    const rows = await api("/api/admin/categories"),
      r = rows.find((r) => r.ID === id) || {},
      impact = id ? await api("/api/admin/categories/" + id + "/impact") : null;
    modal(
      id ? "Sửa danh mục" : "Thêm danh mục",
      `${impact ? `<p class="report-note">Ẩn nhánh ảnh hưởng ${esc(impact.CHILDREN)} danh mục con và ${esc(impact.PRODUCTS)} sản phẩm trên cửa hàng.</p>` : ""}<div class="grid">${input("name", "Tên danh mục", r.CATEGORY_NAME)}${select(
        "parentId",
        "Danh mục cha",
        rows.filter((x) => x.ID !== id).map((x) => ({ ID: x.ID, NAME: x.CATEGORY_NAME })),
        r.PARENT_ID,
        true,
      )}${select("status", "Trạng thái", statusOptions, r.STATUS ?? 1)}</div>`,
      (d) =>
        api("/api/admin/categories" + (id ? "/" + id : ""), id ? "PUT" : "POST", {
          ...d,
          parentId: d.parentId ? Number(d.parentId) : null,
          status: Number(d.status),
        }),
    );
  },
};
