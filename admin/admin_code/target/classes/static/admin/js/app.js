$("#editor").onsubmit = async (e) => {
  e.preventDefault();
  if (typeof submitEditor !== "function" || !e.target.reportValidity()) return;
  const b = $("#editor footer button");
  b.disabled = true;
  try {
    await submitEditor(formData(e.target));
    $("#dialog").close();
    notice("Đã lưu thành công.");
    await load();
  } catch (e) {
    $("#form-error").textContent = e.message;
  } finally {
    b.disabled = typeof submitEditor !== "function";
  }
};

$("#close").onclick = () => $("#dialog").close();

$("#logout").onclick = async () => {
  try {
    await api("/logout", "POST");
    location.href = "/login";
  } catch (e) {
    notice(e.message, true);
  }
};

$("#nav").onclick = (e) => {
  const b = e.target.closest("[data-section]");
  if (b) {
    section = b.dataset.section;
    page = 1;
    filter = {};
    load();
  }
};

$("#content").addEventListener("submit", (e) => {
  if (e.target.id === "filter") {
    e.preventDefault();
    filter = formData(e.target);
    page = 1;
    load();
  }
});

$("#content").onclick = async (e) => {
  const b = e.target.closest("[data-action]");
  if (!b) return;
  try {
    await action(b.dataset.action, Number(b.dataset.id));
  } catch (e) {
    notice(e.message, true);
  }
};

(async () => {
  try {
    csrf = await api("/api/csrf");
    me = await api("/api/admin/me");
    lookups = await api("/api/admin/lookups");
    const admin = me.roles.some((r) => r.authority === "ROLE_ADMIN");
    $("#user").textContent = me.username + (admin ? " · Quản trị viên" : " · Nhân viên");
    $("#nav").innerHTML = Object.entries(titles)
      .filter(([k]) => admin || !["accounts", "audit", "shipping"].includes(k))
      .map(([k, v]) => `<button data-section="${k}">${v}</button>`)
      .join("");
    await load();
  } catch (e) {
    notice(e.message, true);
  }
})();
