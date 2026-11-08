async function imageEditor(id) {
  const p = await api("/api/admin/products/" + id);
  modal(
    "Ảnh · " + p.PRODUCT_NAME,
    `<p>JPEG, PNG, WebP · tối đa 5 MB/ảnh. Chọn màu để ảnh dùng chung cho các size cùng màu.</p><div class="toolbar">${input("files", "Tải nhiều ảnh", "", "file", false, 'multiple accept="image/jpeg,image/png,image/webp"')}${select("uploadColor", "Màu", lookups.colors, null, true)}${button("Tải lên", "upload", "")}</div><div id="previews"></div><div id="image-list">${p.images.map((r) => `<div class="image-row" data-image="${r.ID}"><img src="${esc(r.IMAGE_URL.startsWith("assets/") ? "/store/" + r.IMAGE_URL : r.IMAGE_URL)}" alt="Ảnh sản phẩm">${select("color", "Màu", lookups.colors, r.COLOR_ID, true)}${input("order", "Thứ tự", r.SORT_ORDER, "number")}${button(r.IMAGE_URL === p.THUMBNAIL_URL ? "★ Đại diện" : "Chọn đại diện", "cover", r.ID)}${button("Lưu màu / thứ tự", "save-image", r.ID)}${button("Gỡ", "remove-image", r.ID)}</div>`).join("") || "<p>Chưa có ảnh.</p>"}</div>`,
  );
  $("#fields input[type=file]").onchange = (e) => {
    const dest = $("#previews");
    dest.replaceChildren();
    for (const f of e.target.files) {
      const img = document.createElement("img"),
        url = URL.createObjectURL(f);
      img.src = url;
      img.width = 100;
      img.onload = () => URL.revokeObjectURL(url);
      dest.append(img);
    }
  };
  $("#fields").onclick = async (e) => {
    const b = e.target.closest("[data-action]");
    if (!b) return;
    b.disabled = true;
    try {
      if (b.dataset.action === "upload") {
        const files = $("#fields input[type=file]").files;
        if (!files.length) throw Error("Chọn ít nhất một ảnh.");
        for (const file of files) {
          if (file.size > 5 * 1024 * 1024) throw Error("Ảnh vượt quá 5 MB.");
          const data = new FormData();
          data.append("file", file);
          const color = $("[name=uploadColor]").value;
          await api(
            `/api/admin/products/${id}/images` + (color ? "?colorId=" + color : ""),
            "POST",
            data,
          );
        }
      } else {
        const row = b.closest(".image-row"),
          color = row.querySelector("[name=color]").value;
        if (b.dataset.action === "remove-image")
          await api(`/api/admin/products/${id}/images/${b.dataset.id}`, "DELETE");
        else
          await api(
            `/api/admin/products/${id}/images/${b.dataset.id}?` +
              new URLSearchParams({
                order: row.querySelector("[name=order]").value,
                cover: b.dataset.action === "cover",
                ...(color ? { colorId: color } : {}),
              }),
            "PUT",
          );
      }
      $("#dialog").close();
      await imageEditor(id);
    } catch (e) {
      $("#form-error").textContent = e.message;
      b.disabled = false;
    }
  };
}
