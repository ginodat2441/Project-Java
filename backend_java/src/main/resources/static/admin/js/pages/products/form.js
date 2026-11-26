function productCodeInput(product) {
  return input(
    "code",
    "Mã sản phẩm (tự tăng)",
    product.PRODUCT_CODE || "",
    "text",
    false,
    'readonly placeholder="Tự cấp khi lưu sản phẩm"',
  );
}

function variantSkuInput(variant) {
  return input(
    "sku",
    "SKU (tự tạo)",
    variant.SKU || "",
    "text",
    false,
    'readonly placeholder="Chọn màu và nhập size"',
  );
}

function refreshVariantSku(row, code) {
  if (row.dataset.id) return;
  const color = lookups.colors.find(
      (c) => String(c.ID) === row.querySelector("[name=colorId]").value,
    ),
    size = row.querySelector("[name=size]").value.trim();
  const part = (s) =>
    s
      .normalize("NFD")
      .replace(/[\u0300-\u036f]/g, "")
      .replace(/đ/g, "d")
      .replace(/Đ/g, "D")
      .toUpperCase()
      .replace(/[^A-Z0-9]+/g, "-")
      .replace(/^-|-$/g, "")
      .slice(0, 12);
  row.querySelector("[name=sku]").value =
    color && size
      ? (code || "SP[tự tăng]") +
        "-" +
        (part(color.NAME) || "C" + color.ID) +
        "-" +
        (part(size) || "SIZE")
      : "";
}

function namedLookup(name, label, rows, id) {
  const current = rows.find((r) => String(r.ID) === String(id));
  return `<label>${label}<input name="${name}" list="${name}-choices" value="${esc(current?.NAME || "")}" placeholder="Nhập mới hoặc chọn từ gợi ý…" maxlength="150" required autocomplete="off"><datalist id="${name}-choices">${rows.map((r) => `<option value="${esc(r.NAME)}"></option>`).join("")}</datalist></label>`;
}

function productPhotoFields() {
  return `<section class="product-photo-section"><div class="product-photo-heading"><div><h3>Ảnh &amp; xem trước</h3><p class="muted">Ảnh được tải lên khi bấm Lưu thay đổi. Chọn ảnh đại diện, màu và thứ tự hiển thị.</p></div><span id="product-photo-count" class="product-photo-count">0/20 ảnh mới</span></div><label id="product-photo-drop" class="product-photo-drop"><input name="productFiles" type="file" multiple accept="image/jpeg,image/png,image/webp" aria-label="Chọn nhiều ảnh sản phẩm"><span class="product-photo-upload-icon" aria-hidden="true">↑</span><span class="product-photo-drop-copy"><strong>Chọn ảnh hoặc kéo thả vào đây</strong><small>PNG, JPG, WEBP · tối đa 5 MB/ảnh · 25 MB/lần lưu</small></span></label><div id="product-photos" class="product-photo-grid"></div></section>`;
}

function bindProductPhotos(product) {
  const container = $("#product-photos"),
    picker = $("#editor [name=productFiles]"),
    pending = [];
  let serial = 0,
    cover = product.THUMBNAIL_URL ? "existing" : null;
  const imageUrl = (u) => (u?.startsWith("assets/") ? "/store/" + u : u);
  function render() {
    const existing = product.THUMBNAIL_URL
      ? `<article class="product-photo-card existing"><div class="product-photo-preview"><img src="${esc(imageUrl(product.THUMBNAIL_URL))}" alt="Ảnh đại diện hiện tại"><span class="product-photo-badge">Đang dùng</span></div><div class="product-photo-card-body"><p class="product-photo-name">Ảnh hiện tại</p><label class="product-photo-cover"><input type="radio" name="draftCover" value="existing" ${cover === "existing" ? "checked" : ""}><span>Ảnh đại diện</span></label></div></article>`
      : "";
    const photos = pending
      .map(
        (p, index) =>
          `<article class="product-photo-card" data-photo="${p.key}"><div class="product-photo-preview"><img src="${esc(p.url)}" alt="${esc(p.file.name)}"><span class="product-photo-badge">${cover === p.key ? "★ Ảnh đại diện" : "Ảnh " + (index + 1)}</span></div><div class="product-photo-card-body"><p class="product-photo-name" title="${esc(p.file.name)}">${esc(p.file.name)}</p><label class="product-photo-cover"><input type="radio" name="draftCover" value="${p.key}" ${cover === p.key ? "checked" : ""}><span>Ảnh đại diện</span></label><div class="product-photo-options">${select("photoColor", "Màu / ảnh chung", lookups.colors, p.colorId, true)}${input("photoOrder", "Thứ tự", p.order, "number", true, 'min="0"')}</div><button type="button" data-photo-remove="${p.key}" class="secondary product-photo-remove">Gỡ ảnh</button></div></article>`,
      )
      .join("");
    container.innerHTML =
      existing +
      photos ||
      '<p class="product-photo-empty">Chưa có ảnh xem trước. Chọn hoặc kéo ảnh vào khu vực phía trên.</p>';
    $("#product-photo-count").textContent = `${pending.length}/20 ảnh mới`;
  }
  const rejectFiles = (text) => {
    $("#form-error").textContent = text;
    picker.value = "";
  };
  const addFiles = (files) => {
    if (!files.length) return;
    if (pending.length + files.length > 20) {
      rejectFiles("Mỗi lần lưu tối đa 20 ảnh.");
      return;
    }
    if (files.some((f) => f.size > 5 * 1024 * 1024 || f.size === 0)) {
      rejectFiles("Mỗi ảnh phải từ 1 byte đến 5 MB.");
      return;
    }
    if (files.some((f) => !/\.(jpe?g|png|webp)$/i.test(f.name))) {
      rejectFiles("Chỉ hỗ trợ ảnh JPEG, PNG hoặc WebP.");
      return;
    }
    if (
      pending.reduce((total, p) => total + p.file.size, 0) +
        files.reduce((total, file) => total + file.size, 0) >
      25 * 1024 * 1024
    ) {
      rejectFiles("Tổng dung lượng ảnh không vượt quá 25 MB mỗi lần lưu.");
      return;
    }
    for (const file of files) {
      const key = String(++serial);
      pending.push({
        key,
        file,
        url: URL.createObjectURL(file),
        colorId: null,
        order: pending.length,
      });
      if (cover == null) cover = key;
    }
    $("#form-error").textContent = "";
    picker.value = "";
    render();
  };
  const dropZone = $("#product-photo-drop");
  picker.onchange = () => addFiles([...picker.files]);
  dropZone.addEventListener("dragover", (e) => {
    e.preventDefault();
    dropZone.classList.add("is-dragging");
  });
  dropZone.addEventListener("dragleave", (e) => {
    if (!dropZone.contains(e.relatedTarget)) dropZone.classList.remove("is-dragging");
  });
  dropZone.addEventListener("drop", (e) => {
    e.preventDefault();
    dropZone.classList.remove("is-dragging");
    addFiles([...e.dataTransfer.files]);
  });
  container.onchange = (e) => {
    if (e.target.name === "draftCover") {
      cover = e.target.value;
      render();
      return;
    }
    const row = e.target.closest("[data-photo]"),
      p = pending.find((p) => p.key === row?.dataset.photo);
    if (p) {
      p.colorId = row.querySelector("[name=photoColor]").value || null;
      p.order = Number(row.querySelector("[name=photoOrder]").value);
    }
  };
  container.onclick = (e) => {
    const b = e.target.closest("[data-photo-remove]");
    if (!b) return;
    const index = pending.findIndex((p) => p.key === b.dataset.photoRemove);
    if (index < 0) return;
    const removed = pending.splice(index, 1)[0];
    URL.revokeObjectURL(removed.url);
    if (cover === removed.key)
      cover = product.THUMBNAIL_URL ? "existing" : (pending[0]?.key ?? null);
    render();
  };
  $("#dialog").addEventListener("close", () => pending.forEach((p) => URL.revokeObjectURL(p.url)), {
    once: true,
  });
  render();
  return {
    pending,
    metadata: () =>
      pending.map((p) => ({
        colorId: p.colorId ? Number(p.colorId) : null,
        order: p.order,
        cover: p.key === cover,
      })),
  };
}
