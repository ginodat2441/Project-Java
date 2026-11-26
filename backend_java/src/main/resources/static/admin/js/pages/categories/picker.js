function categoryChoices() {
  return lookups.categories.map((c) => {
    const names = [c.NAME],
      seen = new Set([String(c.ID)]);
    let parent = c.PARENT_ID;
    while (parent != null && !seen.has(String(parent))) {
      seen.add(String(parent));
      const p = lookups.categories.find((x) => String(x.ID) === String(parent));
      if (!p) break;
      names.unshift(p.NAME);
      parent = p.PARENT_ID;
    }
    return {
      id: String(c.ID),
      label: names.join(" → ") + (Number(c.STATUS) === 0 ? " (đã ẩn)" : ""),
    };
  });
}

function categorySearch(value) {
  const selected = categoryChoices().find((c) => c.id === String(value));
  return `<div class="category-search"><label for="product-category-search">Danh mục</label><div class="category-input"><input id="product-category-search" type="search" autocomplete="off" placeholder="Gõ tên để tìm danh mục…" role="combobox" aria-autocomplete="list" aria-expanded="false" aria-controls="product-category-results" value="${esc(selected?.label || "")}"><button type="button" id="category-toggle" aria-label="Mở danh sách danh mục" aria-controls="product-category-results">⌄</button></div><input type="hidden" name="categoryId" value="${esc(selected?.id || "")}"><div id="product-category-results" class="category-results" role="listbox" aria-label="Kết quả danh mục" hidden></div></div>`;
}

function bindCategorySearch() {
  const field = $("#product-category-search"),
    results = $("#product-category-results"),
    id = $("#editor [name=categoryId]");
  let choices = categoryChoices(),
    matches = [],
    cursor = -1;
  const normalize = (s) =>
    s
      .normalize("NFD")
      .replace(/[\u0300-\u036f]/g, "")
      .replace(/đ/g, "d")
      .replace(/Đ/g, "D")
      .toLowerCase();
  const close = () => {
    results.hidden = true;
    field.setAttribute("aria-expanded", "false");
    field.removeAttribute("aria-activedescendant");
    cursor = -1;
  };
  const choose = (index) => {
    const c = matches[index];
    if (!c) return;
    field.value = c.id ? c.label : "";
    id.value = c.id;
    field.setCustomValidity("");
    field.focus();
    close();
  };
  const search = (all = false) => {
    const q = all ? "" : normalize(field.value.trim());
    matches = q
      ? choices.filter((c) => normalize(c.label).includes(q)).slice(0, 20)
      : [{ id: "", label: "Chưa chọn" }, ...choices];
    cursor = -1;
    results.innerHTML = matches.length
      ? matches
          .map(
            (c, i) =>
              `<button type="button" role="option" aria-selected="false" id="category-result-${i}" data-result="${i}">${esc(c.label)}</button>`,
          )
          .join("")
      : '<p role="status">Không tìm thấy danh mục phù hợp.</p>';
    results.hidden = false;
    field.setAttribute("aria-expanded", "true");
  };
  field.oninput = () => {
    id.value = "";
    field.setCustomValidity(
      field.value.trim()
        ? "Chọn một danh mục trong kết quả tìm kiếm, hoặc xóa nội dung để bỏ chọn."
        : "",
    );
    search();
  };
  field.onfocus = () => search();
  const toggle = $("#category-toggle");
  toggle.onmousedown = (e) => e.preventDefault();
  toggle.onclick = () => {
    if (results.hidden) {
      field.focus();
      search(true);
    } else close();
  };
  field.onkeydown = (e) => {
    if (e.key === "Escape") {
      close();
      return;
    }
    if ((e.key === "ArrowDown" || e.key === "ArrowUp") && matches.length) {
      e.preventDefault();
      if (results.hidden) search();
      cursor =
        (cursor + (e.key === "ArrowDown" ? 1 : matches.length - 1) + matches.length) %
        matches.length;
      results
        .querySelectorAll("[role=option]")
        .forEach((b, i) => b.setAttribute("aria-selected", String(i === cursor)));
      field.setAttribute("aria-activedescendant", "category-result-" + cursor);
    } else if (e.key === "Enter" && !results.hidden) {
      e.preventDefault();
      choose(cursor < 0 ? 0 : cursor);
    }
  };
  results.onmousedown = (e) => e.preventDefault();
  results.onclick = (e) => {
    const b = e.target.closest("[data-result]");
    if (b) choose(Number(b.dataset.result));
  };
  field.onblur = close;
  const wrapper = field.closest(".category-search");
  wrapper.insertAdjacentHTML(
    "afterend",
    `<div class="category-create"><button type="button" id="category-add" aria-expanded="false" aria-controls="category-create-panel">+ Thêm danh mục</button><div id="category-create-panel" hidden><label for="category-new-name">Tên danh mục mới</label><input id="category-new-name" maxlength="150" autocomplete="off"><label for="category-new-parent">Danh mục cha (không bắt buộc)</label><select id="category-new-parent"></select><p id="category-create-error" class="error" role="alert"></p><button type="button" id="category-create-save">Tạo và chọn</button> <button type="button" id="category-create-cancel">Hủy</button></div></div>`,
  );
  const group = document.createElement("div");
  group.className = "category-field";
  wrapper.before(group);
  group.append(wrapper, wrapper.nextElementSibling);
  const panel = $("#category-create-panel"),
    add = $("#category-add"),
    name = $("#category-new-name"),
    parent = $("#category-new-parent"),
    error = $("#category-create-error"),
    save = $("#category-create-save");
  const hide = () => {
    panel.hidden = true;
    add.setAttribute("aria-expanded", "false");
  };
  add.onclick = () => {
    if (!panel.hidden) {
      hide();
      return;
    }
    close();
    name.value = id.value ? "" : field.value.trim();
    parent.innerHTML =
      '<option value="">Không có danh mục cha</option>' +
      choices.map((c) => `<option value="${esc(c.id)}">${esc(c.label)}</option>`).join("");
    error.textContent = "";
    panel.hidden = false;
    add.setAttribute("aria-expanded", "true");
    name.focus();
  };
  $("#category-create-cancel").onclick = hide;
  const create = async () => {
    if (save.disabled) return;
    const categoryName = name.value.trim();
    if (!categoryName) {
      error.textContent = "Vui lòng nhập tên danh mục.";
      name.focus();
      return;
    }
    save.disabled = true;
    add.disabled = true;
    $("#category-create-cancel").disabled = true;
    const productSave = $("#editor footer button");
    productSave.disabled = true;
    error.textContent = "";
    try {
      const parentId = parent.value ? Number(parent.value) : null;
      const newId = await api("/api/admin/categories", "POST", {
        name: categoryName,
        parentId,
        status: 1,
      });
      lookups.categories.push({ ID: newId, NAME: categoryName, PARENT_ID: parentId, STATUS: 1 });
      choices = categoryChoices();
      id.value = String(newId);
      field.value = choices.find((c) => c.id === String(newId)).label;
      field.setCustomValidity("");
      hide();
      field.focus();
      close();
    } catch (e) {
      error.textContent = e.message;
    } finally {
      save.disabled = false;
      add.disabled = false;
      $("#category-create-cancel").disabled = false;
      productSave.disabled = false;
    }
  };
  save.onclick = create;
  [name, parent].forEach(
    (control) =>
      (control.onkeydown = (e) => {
        if (e.key === "Enter") {
          e.preventDefault();
          create();
        }
      }),
  );
}
