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
  return `<section class="product-photo-section"><h3>Hình ảnh sản phẩm</h3><p class="muted">Chọn nhiều ảnh, ảnh đại diện và màu ngay tại đây. Ảnh được tải lên khi bấm Lưu thay đổi. JPEG, PNG, WebP · tối đa 5 MB/ảnh, 25 MB/lần lưu.</p>${input("productFiles", "Thêm ảnh", "", "file", false, 'multiple accept="image/jpeg,image/png,image/webp"')}<div id="product-photos"></div></section><p class="muted">Biến thể mới bắt đầu tồn 0. Nhập hàng bằng phiếu kho được ADMIN xác nhận.</p>`;
}

function bindProductPhotos(product) {
  const container = $("#product-photos"),
    picker = $("#editor [name=productFiles]"),
    pending = [];
  let serial = 0,
    cover = product.THUMBNAIL_URL ? "existing" : null;
  const imageUrl = (u) => (u?.startsWith("assets/") ? "/store/" + u : u);
  function render() {
    container.innerHTML =
      (product.THUMBNAIL_URL
        ? `<label class="draft-photo existing"><input type="radio" name="draftCover" value="existing" ${cover === "existing" ? "checked" : ""}><img src="${esc(imageUrl(product.THUMBNAIL_URL))}" alt="Ảnh đại diện hiện tại"><span>Giữ ảnh đại diện hiện tại</span></label>`
        : "") +
      pending
        .map(
          (p) =>
            `<div class="draft-photo" data-photo="${p.key}"><img src="${esc(p.url)}" alt="${esc(p.file.name)}"><span>${esc(p.file.name)}</span><label>Ảnh đại diện<input type="radio" name="draftCover" value="${p.key}" ${cover === p.key ? "checked" : ""}></label>${select("photoColor", "Màu (hoặc ảnh chung)", lookups.colors, p.colorId, true)}${input("photoOrder", "Thứ tự", p.order, "number", true, 'min="0"')}<button type="button" data-photo-remove="${p.key}" class="secondary">Gỡ ảnh</button></div>`,
        )
        .join("");
  }
  picker.onchange = () => {
    const files = [...picker.files];
    if (pending.length + files.length > 20) {
      $("#form-error").textContent = "Mỗi lần lưu tối đa 20 ảnh.";
      return;
    }
    if (files.some((f) => f.size > 5 * 1024 * 1024 || f.size === 0)) {
      $("#form-error").textContent = "Mỗi ảnh phải từ 1 byte đến 5 MB.";
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
    picker.value = "";
    $("#form-error").textContent = "";
    render();
  };
  container.onchange = (e) => {
    if (e.target.name === "draftCover") {
      cover = e.target.value;
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
