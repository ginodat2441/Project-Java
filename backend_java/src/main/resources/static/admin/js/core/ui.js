function notice(s, error = false) {
  $("#notice").textContent = s;
  $("#notice").className = error ? "error" : "success";
}

function table(rows, cols, actions) {
  if (!rows.length) return '<div class="empty">Chưa có dữ liệu phù hợp.</div>';
  return `<div class="table-wrap"><table><thead><tr>${cols.map((c) => `<th>${esc(c[1])}</th>`).join("")}${actions ? "<th>Thao tác</th>" : ""}</tr></thead><tbody>${rows.map((r) => `<tr>${cols.map((c) => `<td>${c[2] ? c[2](r[c[0]], r) : esc(r[c[0]])}</td>`).join("")}${actions ? `<td>${actions(r)}</td>` : ""}</tr>`).join("")}</tbody></table></div>`;
}

function button(label, action, id, extra = "") {
  return `<button type="button" data-action="${action}" data-id="${esc(id)}" ${extra}>${label}</button>`;
}

function pager(result) {
  return `<div class="pager">${button("← Trước", "prev", "", page <= 1 ? "disabled" : "")}<span>Trang ${page} · ${result.total} bản ghi</span>${button("Sau →", "next", "", page * result.size >= result.total ? "disabled" : "")}</div>`;
}

function options(rows, selected, empty = true) {
  return (
    (empty ? '<option value="">Chưa chọn</option>' : "") +
    rows
      .map(
        (r) =>
          `<option value="${r.ID}" ${String(r.ID) === String(selected) ? "selected" : ""}>${esc(r.NAME || r.ROLE_NAME)}</option>`,
      )
      .join("")
  );
}

function input(name, label, value = "", type = "text", required = true, extra = "") {
  return `<label>${label}<input name="${name}" type="${type}" value="${esc(value)}" ${required ? "required" : ""} ${extra}></label>`;
}

function select(name, label, rows, value, empty = false) {
  return `<label>${label}<select name="${name}" ${empty ? "" : "required"}>${options(rows, value, empty)}</select></label>`;
}

function formData(form) {
  return Object.fromEntries(new FormData(form));
}

function modal(title, html, save) {
  $("#dialog-title").textContent = title;
  $("#fields").onclick = null;
  $("#fields").oninput = null;
  $("#fields").innerHTML = html;
  $("#form-error").textContent = "";
  submitEditor = typeof save === "function" ? save : null;
  $("#editor footer").hidden = !submitEditor;
  $("#editor footer button").disabled = !submitEditor;
  $("#dialog").showModal();
}

function searchToolbar(extra = "", create = "") {
  return `<form id="filter" class="toolbar"><input name="q" value="${esc(filter.q || "")}" placeholder="Tìm kiếm tên hoặc mã…" aria-label="Tìm kiếm">${extra}<button>Tìm kiếm</button>${create ? button("+ " + create, "create", "") : ""}</form>`;
}
