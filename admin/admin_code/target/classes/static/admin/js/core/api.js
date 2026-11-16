async function api(path, method = "GET", body) {
  const options = { method, headers: {} };
  if (method !== "GET") {
    if (!csrf) csrf = await fetch("/api/csrf").then((r) => r.json());
    options.headers[csrf.headerName] = csrf.token;
    if (body instanceof FormData) options.body = body;
    else if (body !== undefined) {
      options.headers["Content-Type"] = "application/json";
      options.body = JSON.stringify(body);
    }
  }
  const r = await fetch(path, options);
  if (r.status === 401) {
    location.href = "/login";
    throw Error("Phiên đã hết hạn");
  }
  let data = await r.text();
  try {
    data = data ? JSON.parse(data) : null;
  } catch {}
  if (!r.ok)
    throw Error(
      data?.message ||
        (r.status === 403
          ? "Bạn không có quyền hoặc phiên đã thay đổi. Tải lại trang."
          : "Không thể thực hiện thao tác (" + r.status + ")."),
    );
  return data;
}
