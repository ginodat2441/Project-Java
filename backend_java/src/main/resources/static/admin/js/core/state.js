"use strict";

const $ = (s) => document.querySelector(s),
  esc = (s) =>
    String(s ?? "").replace(
      /[&<>"']/g,
      (c) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" })[c],
    );

const titles = {
  shipping: "Vận chuyển Viettel Post",
  dashboard: "Tổng quan",
  reports: "Thống kê doanh thu",
  accounts: "Tài khoản",
  categories: "Danh mục",
  products: "Sản phẩm",
  vouchers: "Voucher",
  orders: "Đơn hàng",
  stock: "Tồn kho",
  documents: "Phiếu nhập / xuất / hoàn",
  inventoryReport: "Báo cáo nhập xuất tồn",
  history: "Lịch sử tồn kho",
  audit: "Nhật ký quản trị",
};

const statusNames = [
  "Chờ xác nhận",
  "Đã xác nhận",
  "Đang giao",
  "Hoàn thành",
  "Đã hủy",
  "Trả hàng",
  "Giao thất bại",
];

let csrf,
  lookups,
  me,
  section = "dashboard",
  page = 1,
  tab = "customers",
  filter = {},
  submitEditor;

const money = (n) => Number(n || 0).toLocaleString("vi-VN", { maximumFractionDigits: 0 }) + " ₫";

const date = (s) => (s ? String(s).replace("T", " ").slice(0, 19) : "—");

const active = (n) => `<span class="pill">${Number(n) === 1 ? "Hoạt động" : "Đã ẩn / khóa"}</span>`;

function movementName(value) {
  return (
    window.AdminV8?.kind[value] ||
    {
      RECEIPT: "Nhập kho",
      ISSUE: "Xuất kho",
      SALE: "Giữ / xuất bán",
      CANCEL: "Hủy đơn hoàn giữ",
      RETURN: "Nhận hoàn",
      COUNT: "Kiểm kê",
      REVERSAL: "Đảo phiếu",
    }[value] ||
    "Dữ liệu cũ"
  );
}

const statusOptions = [
  { ID: 1, NAME: "Hoạt động / đang bán" },
  { ID: 0, NAME: "Ẩn / ngừng bán" },
];
