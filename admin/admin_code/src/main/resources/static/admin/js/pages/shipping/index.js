"use strict";

(() => {
  function statusText(status) {
    if (!status.configured) return "Chưa hoàn tất cấu hình";
    if (!status.lastSuccessfulQuote) return "Đã cấu hình, chờ kiểm tra báo giá";
    const time = new Intl.DateTimeFormat("vi-VN", {
      dateStyle: "short", timeStyle: "medium", timeZone: "Asia/Ho_Chi_Minh",
    }).format(new Date(status.lastSuccessfulQuote));
    return "Đã nhận báo giá Viettel Post lúc " + time;
  }

  function addressPicker(name, label, optionsList, disabled = false) {
    const id = "shipping-" + name;
    return `<div class="category-search" data-address-picker="${name}">
      <label for="${id}">${label}</label>
      <div class="category-input">
        <input id="${id}" type="search" autocomplete="off" required role="combobox"
          aria-autocomplete="list" aria-expanded="false" aria-controls="${id}-results"
          placeholder="${disabled ? "Chọn tỉnh/thành phố trước" : "Gõ tên tỉnh/thành phố…"}" ${disabled ? "disabled" : ""}>
        <button type="button" aria-label="Mở danh sách ${label}" aria-controls="${id}-results" ${disabled ? "disabled" : ""}>⌄</button>
      </div>
      <select name="${name}" hidden>${options(optionsList, "", true)}</select>
      <div id="${id}-results" class="category-results" role="listbox" aria-label="${label}" hidden></div>
    </div>`;
  }

  function bindAddressPicker(form, name, onChange) {
    const root = form.querySelector('[data-address-picker="' + name + '"]');
    const field = root.querySelector("input");
    const toggle = root.querySelector("button");
    const results = root.querySelector("[role=listbox]");
    const select = form.elements[name];
    let entries = Array.from(select.options).filter((option) => option.value)
      .map((option) => ({ value: option.value, label: option.text }));
    let matches = [];
    let cursor = -1;

    function normalize(text) {
      return text.toLowerCase().normalize("NFD").replace(/\p{M}/gu, "").replace(/đ/g, "d").trim();
    }

    function close() {
      results.hidden = true;
      field.setAttribute("aria-expanded", "false");
      field.removeAttribute("aria-activedescendant");
      cursor = -1;
    }

    function open(all = false) {
      if (field.disabled) return;
      const query = all || select.value ? "" : normalize(field.value);
      const tokens = query.split(/\s+/);
      matches = entries.filter(({ label }) => tokens.every((token) => normalize(label).includes(token)));
      results.innerHTML = matches.length ? matches.map(({ label }, index) =>
        `<button type="button" role="option" aria-selected="false" id="${field.id}-option-${index}" data-option="${index}">${esc(label)}</button>`
      ).join("") : '<p role="status">Không tìm thấy kết quả phù hợp.</p>';
      results.hidden = false;
      field.setAttribute("aria-expanded", "true");
      field.removeAttribute("aria-activedescendant");
      cursor = -1;
    }

    function choose(index) {
      const entry = matches[index];
      if (!entry) return;
      const previous = select.value;
      select.value = entry.value;
      field.value = entry.label;
      field.setCustomValidity("");
      close();
      if (previous !== entry.value) onChange();
    }

    field.oninput = () => {
      const previous = select.value;
      select.value = "";
      field.setCustomValidity(field.value ? "Chọn một giá trị trong danh sách gợi ý." : "");
      if (previous) onChange();
      open();
    };
    field.onfocus = () => open();
    field.onblur = close;
    toggle.onmousedown = (event) => event.preventDefault();
    toggle.onclick = () => {
      if (!results.hidden) return close();
      field.focus();
      open(true);
    };
    results.onmousedown = (event) => event.preventDefault();
    results.onclick = (event) => {
      const option = event.target.closest("[data-option]");
      if (option) choose(Number(option.dataset.option));
    };
    field.onkeydown = (event) => {
      if (event.key === "Escape") {
        close();
      } else if (["ArrowDown", "ArrowUp"].includes(event.key)) {
        event.preventDefault();
        if (results.hidden) open();
        if (!matches.length) return;
        cursor = event.key === "ArrowDown" ? (cursor + 1) % matches.length
          : (cursor <= 0 ? matches.length - 1 : cursor - 1);
        const buttons = results.querySelectorAll("[role=option]");
        buttons.forEach((button, index) => button.setAttribute("aria-selected", String(index === cursor)));
        field.setAttribute("aria-activedescendant", buttons[cursor].id);
        buttons[cursor].scrollIntoView?.({ block: "nearest" });
      } else if (event.key === "Enter" && !results.hidden) {
        event.preventDefault();
        choose(cursor < 0 ? 0 : cursor);
      }
    };
    return {
      reset(list, placeholder) {
        entries = list;
        select.replaceChildren(new Option("", ""), ...list.map(({ label, value }) => new Option(label, value)));
        field.value = "";
        field.placeholder = placeholder;
        field.setCustomValidity("");
        field.disabled = toggle.disabled = !list.length;
        close();
      },
    };
  }

  window.AdminV8.shipping = {
    async render() {
      const status = await api("/api/admin/shipping/status");
      const provinces = status.configured ? await api("/api/shipping/provinces") : [];
      return `<section class="card">
        <h2>Viettel Post</h2>
        <p>Trạng thái: <b data-shipping-status>${esc(statusText(status))}</b></p>
        ${table([
          { name: "Token API Viettel Post", ready: status.tokenConfigured },
          { name: "Địa chỉ kho gửi", ready: status.originConfigured },
          { name: "Khối lượng và kích thước kiện", ready: status.parcelConfigured },
        ], [["name", "Thông tin"], ["ready", "Trạng thái", (value) => value ? "Đã nhập" : "Còn thiếu"]])}
        <p>Môi trường: ${esc(status.environment === "production" ? "Chính thức" : "Thử nghiệm")}</p>
        ${status.configured ? `
          <h3>Kiểm tra báo giá</h3>
          <p>Tính thử cho một sản phẩm trị giá 100.000 ₫, không thu hộ và không tạo vận đơn.</p>
          <form data-shipping-test>
            <div class="grid">
              ${addressPicker("province", "Tỉnh/Thành phố", provinces.map(({ code, name }) => ({ ID: code, NAME: name })))}
              ${addressPicker("ward", "Phường/Xã", [], true)}
              ${input("address", "Địa chỉ", "", "text", true, 'maxlength="300"')}
            </div>
            <p data-ward-message aria-live="polite"></p><button type="button" data-retry-wards hidden>Tải lại Phường/Xã</button><p><button type="submit" disabled>Kiểm tra báo giá</button></p>
          </form>
          <div data-shipping-result aria-live="polite"></div>` :
          `<h3>Cấu hình trên máy chủ</h3>
          <p>Chạy trong PowerShell tại thư mục dự án rồi khởi động lại Admin.</p>
          <pre>powershell -NoProfile -ExecutionPolicy Bypass -File .\\tools\\configure-viettel.ps1 -WebsiteToken</pre>
          <p>Dán token sao chép từ Viettel Post vào ô nhập ẩn của script.</p>`}
        <p class="report-note">${esc(status.weightPolicy)}</p>
      </section>`;
    },

    mount(container) {
      const form = container.querySelector("[data-shipping-test]");
      if (!form) return;
      const button = form.querySelector("[type=submit]");
      const output = container.querySelector("[data-shipping-result]");
      const status = container.querySelector("[data-shipping-status]");
      const province = form.elements.province;
      const ward = form.elements.ward;
      const retry = form.querySelector("[data-retry-wards]");
      const message = form.querySelector("[data-ward-message]");
      bindAddressPicker(form, "province", loadWards);
      const wardPicker = bindAddressPicker(form, "ward", () => { button.disabled = !ward.value; });
      let sequence = 0;
      async function loadWards() {
        const current = ++sequence;
        wardPicker.reset([], province.value ? "Đang tải Phường/Xã…" : "Chọn tỉnh/thành phố trước");
        button.disabled = true;
        retry.hidden = true;
        output.textContent = "";
        message.textContent = "";
        if (!province.value) return;
        try {
          const names = await api("/api/admin/shipping/wards?province=" + encodeURIComponent(province.value));
          if (current !== sequence) return;
          wardPicker.reset(names.map((name) => ({ value: name, label: name })),
            names.length ? "Gõ tên phường/xã…" : "Chưa có danh sách Phường/Xã");
          if (!names.length) {
            message.textContent = "Viettel Post chưa trả danh sách Phường/Xã cho tỉnh/thành phố này.";
            retry.hidden = false;
          }
        } catch (error) {
          if (current !== sequence) return;
          wardPicker.reset([], "Không tải được Phường/Xã");
          message.textContent = error.message;
          retry.hidden = false;
        }
      }

      retry.onclick = loadWards;
      if (province.value) loadWards();

      form.onsubmit = async (event) => {
        event.preventDefault();
        if (!ward.value || ward.disabled) return;
        const current = sequence;
        button.disabled = true;
        output.textContent = "Đang lấy báo giá từ Viettel Post…";
        try {
          const result = await api("/api/admin/shipping/test-quote", "POST", Object.fromEntries(new FormData(form)));
          if (current !== sequence) return;
          status.textContent = statusText(result.status);
          output.innerHTML = table(result.services, [
            ["serviceName", "Dịch vụ"],
            ["fee", "Cước vận chuyển", money],
            ["deliveryTime", "Thời gian dự kiến"],
          ]);
        } catch (error) {
          if (current === sequence) output.textContent = error.message || "Không lấy được báo giá Viettel Post.";
        } finally {
          if (current === sequence) button.disabled = !ward.value;
        }
      };
    },
  };
})();
