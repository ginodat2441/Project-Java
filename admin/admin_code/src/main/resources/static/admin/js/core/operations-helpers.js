"use strict";
window.AdminModules = {};
(() => {
  const types = {
    RESERVE: "Giữ hàng",
    RELEASE: "Giải phóng giữ",
    DISPATCH: "Xuất giao thực tế",
    RECEIPT: "Nhập kho",
    ISSUE: "Xuất khác (ngoài đơn bán)",
    COUNT: "Kiểm kê",
    RETURN: "Nhận hàng hoàn",
    REVERSAL: "Đảo phiếu",
    SALE: "Giữ/xuất cũ (lịch sử sẵn bán)",
    CANCEL: "Hoàn giữ tồn do hủy đơn",
  };

  const states = { DRAFT: "Nháp · chưa đổi tồn", POSTED: "Đã xác nhận", CANCELLED: "Đã hủy" };

  const admin = () => me?.roles.some((r) => r.authority === "ROLE_ADMIN");

  const choose = (name, label, rows, value, blank = true) =>
    select(name, label, rows, value, blank);

  const today = () => new Date().toLocaleDateString("sv-SE");

  const year = () => new Date().getFullYear();

  const query = (extra) => {
    const p = new URLSearchParams();
    for (const [k, v] of Object.entries({ ...filter, ...extra }))
      if (v !== "" && v != null) p.set(k, v);
    return p;
  };
  window.AdminModulesUtils = { types, states, admin, choose, today, year, query };
})();
