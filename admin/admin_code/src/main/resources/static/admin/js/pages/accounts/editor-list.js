"use strict";
window.AdminPages ??= {};
window.AdminPages.accounts = {
  async render() {
    let html = "";
    html = `<div class="tabs">${button("Khách hàng", "customers", "")}${button("Nhân viên quản trị", "managers", "")}</div>`;
    if (tab === "customers") {
      const data = await api(
        "/api/admin/customers?" + new URLSearchParams({ page, q: filter.q || "" }),
      );
      html +=
        searchToolbar() +
        table(
          data.items,
          [
            ["FULL_NAME", "Khách hàng"],
            ["EMAIL", "Email"],
            ["PHONE", "Điện thoại"],
            ["STATUS", "Trạng thái", active],
          ],
          (r) =>
            button("Chi tiết", "detail", r.ID) +
            button(r.STATUS ? "Khóa" : "Mở khóa", r.STATUS ? "lock" : "unlock", r.ID),
        ) +
        pager(data);
    } else {
      const rows = await api("/api/admin/managers?" + new URLSearchParams(filter));
      window.managers = rows;
      html +=
        searchToolbar(
          select(
            "role",
            "Vai trò",
            [
              { ID: "ADMIN", NAME: "Quản trị" },
              { ID: "STAFF", NAME: "Nhân viên" },
            ],
            filter.role,
            true,
          ) + select("status", "Trạng thái", statusOptions, filter.status, true),
          "Nhân viên",
        ) +
        table(
          rows,
          [
            ["USERNAME", "Tài khoản"],
            ["FULL_NAME", "Họ tên"],
            ["EMAIL", "Email"],
            ["ROLE_CODE", "Vai trò"],
            ["STATUS", "Trạng thái", active],
          ],
          (r) => button("Quyền / Khóa", "edit", r.ID),
        );
    }

    return html;
  },
  async action(a, id) {
    if (a === "detail") {
      const d = await api("/api/admin/customers/" + id);
      modal(
        "Chi tiết khách hàng",
        `<h3>${esc(d.customer.FULL_NAME)}</h3><p>${esc(d.customer.EMAIL)} · ${esc(d.customer.PHONE)}</p><h3>Lịch sử đơn hàng</h3>` +
          table(d.orders, [
            ["BILL_CODE", "Mã đơn"],
            ["TOTAL_AMOUNT", "Tổng", money],
            ["STATUS", "Trạng thái", (v) => esc(statusNames[v])],
            ["CREATED_AT", "Ngày", date],
          ]),
      );
      return;
    }
    const roles = await api("/api/admin/roles"),
      r = window.managers?.find((m) => m.ID === id) || {};
    modal(
      id ? "Quyền truy cập nhân viên" : "Tạo nhân viên",
      `<div class="grid">${id ? "" : input("username", "Tài khoản") + input("password", "Mật khẩu (12–72 ký tự)", "", "password", true, 'minlength="12" maxlength="72" autocomplete="new-password"') + input("fullName", "Họ tên") + input("email", "Email", "", "email")}${select("roleId", "Vai trò", roles, r.MANAGER_ROLE_ID)}${id ? select("status", "Trạng thái", statusOptions, r.STATUS) : ""}</div>`,
      (d) =>
        api("/api/admin/managers" + (id ? "/" + id + "/access" : ""), id ? "PUT" : "POST", {
          ...d,
          roleId: Number(d.roleId),
          status: Number(d.status ?? 1),
        }),
    );
  },
};
