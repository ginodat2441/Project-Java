"use strict";
(async () => {
  const query = new URLSearchParams(location.search),
    legacy = !location.pathname.startsWith("/shop/");
  const readCart = () => {
    try {
      return JSON.parse(localStorage.getItem("clothing-cart") || "[]");
    } catch {
      return [];
    }
  };
  const filename = location.pathname.split("/").pop();
  if (
    legacy &&
    !query.has("live") &&
    !(readCart().length && ["cart.html", "checkout.html"].includes(filename))
  )
    return;
  const main = legacy ? document.querySelector("main") : document.querySelector(".live-shop");
  if (!main) return;
  if (legacy) {
    main.replaceChildren();
    main.classList.add("live-shop");
    const link = document.createElement("link");
    link.rel = "stylesheet";
    link.href = "/store/css/store.css";
    document.head.append(link);
  }
  main.innerHTML = '<div id="shop-content"><p>Đang tải…</p></div>';
  const root = document.querySelector("#shop-content"),
    esc = (s) =>
      String(s ?? "").replace(
        /[&<>"']/g,
        (c) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" })[c],
      ),
    money = (n) => Number(n || 0).toLocaleString("vi-VN") + " ₫";
  let csrf;
  try {
    csrf = await fetch("/api/csrf").then((r) => r.json());
  } catch {
    root.textContent = "Không thể kết nối máy chủ. Hãy tải lại trang.";
    return;
  }
  async function api(url, method = "GET", data) {
    const r = await fetch(url, {
      method,
      headers:
        method === "GET"
          ? {}
          : { "Content-Type": "application/json", [csrf.headerName]: csrf.token },
      ...(data ? { body: JSON.stringify(data) } : {}),
    });
    const body = await r.json().catch(() => null);
    if (!r.ok)
      throw Error(
        body?.message || "Không thực hiện được thao tác. Hãy tải lại trang hoặc đăng nhập.",
      );
    return body;
  }
  const field = (name, label, type = "text", value = "", required = true) =>
    `<label>${label}<input name="${name}" type="${type}" value="${esc(value)}" ${required ? "required" : ""}></label>`;
  const items = () => readCart().map((i) => ({ productDetailId: i.id, quantity: i.quantity }));
  const saveCart = (cart) => localStorage.setItem("clothing-cart", JSON.stringify(cart));
  const imgUrl = (u) => (u?.startsWith("assets/") ? "/store/" + u : u);
  let view =
    query.get("view") ||
    {
      "cart.html": "cart",
      "checkout.html": "checkout",
      "login-register.html": "account",
      "product-details.html": "product",
    }[filename] ||
    "catalog";
  if (query.has("id")) view = "product";
  try {
    if (view === "catalog") {
      const page = Number(query.get("page") || 1),
        qs = new URLSearchParams({ page, q: query.get("q") || "" });
      if (query.get("categoryId")) qs.set("categoryId", query.get("categoryId"));
      const [data, categories] = await Promise.all([
        api("/api/catalog?" + qs),
        api("/api/catalog/categories"),
      ]);
      root.innerHTML = `<h1>Bộ sưu tập của cửa hàng</h1><p class="shop-muted">Chọn màu, size và đặt hàng theo tồn kho thực tế.</p><form class="shop-toolbar" method="get"><label>Tìm sản phẩm<input name="q" value="${esc(query.get("q"))}"></label><label>Danh mục<select name="categoryId"><option value="">Tất cả</option>${categories.map((c) => `<option value="${c.ID}" ${query.get("categoryId") === String(c.ID) ? "selected" : ""}>${esc(c.CATEGORY_NAME)}</option>`).join("")}</select></label><button>Tìm kiếm</button></form><div class="shop-grid">${data.items.map((p) => `<article class="shop-card">${p.THUMBNAIL_URL ? `<img src="${esc(imgUrl(p.THUMBNAIL_URL))}" alt="${esc(p.PRODUCT_NAME)}">` : "<p>Chưa có ảnh</p>"}<p>${esc(p.CATEGORY_NAME)}</p><h2>${esc(p.PRODUCT_NAME)}</h2><p>${money(p.PRICE)}</p><a href="/store/product-details.html?live=1&id=${p.ID}">Chọn màu và size →</a></article>`).join("") || "<p>Chưa có sản phẩm đang bán.</p>"}</div><div class="shop-toolbar">${page > 1 ? `<a href="?${new URLSearchParams({ ...Object.fromEntries(query), page: page - 1 })}">← Trang trước</a>` : ""}<span>Trang ${page} · ${data.total} sản phẩm</span>${page * data.size < data.total ? `<a href="?${new URLSearchParams({ ...Object.fromEntries(query), page: page + 1 })}">Trang sau →</a>` : ""}</div>`;
    } else if (view === "product") {
      const d = await api("/api/catalog/" + Number(query.get("id"))),
        p = d.product,
        colors = [...new Map(d.variants.map((v) => [v.COLOR_ID, v.COLOR_NAME])).entries()];
      let color = colors[0]?.[0],
        selected;
      root.innerHTML = `<a href="/shop/index.html">← Sản phẩm</a><div class="shop-two"><section class="shop-gallery"><img id="hero" alt="Ảnh sản phẩm"><div class="thumbs" id="thumbs"></div></section><section><h1>${esc(p.PRODUCT_NAME)}</h1><p>${esc(p.DESCRIPTION)}</p><form id="buy"><label>Màu<select id="color">${colors.map(([id, name]) => `<option value="${id}">${esc(name)}</option>`).join("")}</select></label><label>Size<select id="size"></select></label><p id="price"></p>${field("quantity", "Số lượng", "number", 1)}<button>Thêm vào giỏ</button></form><p id="shop-message" class="shop-alert" role="status"></p><a href="/store/cart.html?live=1">Xem giỏ hàng →</a></section></div>`;
      const gallery = () => {
        const own = d.images.filter((i) => i.COLOR_ID === color),
          common = d.images.filter((i) => i.COLOR_ID == null);
        const urls = (own.length ? own : common).map((i) => imgUrl(i.IMAGE_URL));
        if (!urls.length && p.THUMBNAIL_URL) urls.push(imgUrl(p.THUMBNAIL_URL));
        const hero = document.querySelector("#hero");
        if (urls.length) {
          hero.hidden = false;
          hero.src = urls[0];
        } else {
          hero.hidden = true;
        }
        document.querySelector("#thumbs").innerHTML = urls
          .map((u) => `<img src="${esc(u)}" alt="Xem ảnh">`)
          .join("");
      };
      const size = () => {
        selected = d.variants.find((v) => v.ID === Number(document.querySelector("#size").value));
        document.querySelector("#price").textContent = selected
          ? `${money(selected.PRICE)} · Còn ${selected.QUANTITY}`
          : "Chưa có biến thể";
        const q = document.querySelector("[name=quantity]");
        q.min = 1;
        q.max = selected?.QUANTITY || 0;
        document.querySelector("#buy button").disabled = !selected || selected.QUANTITY === 0;
      };
      const changeColor = () => {
        color = Number(document.querySelector("#color").value);
        document.querySelector("#size").innerHTML = d.variants
          .filter((v) => v.COLOR_ID === color)
          .map((v) => `<option value="${v.ID}">${esc(v.SIZE_NAME)}</option>`)
          .join("");
        gallery();
        size();
      };
      document.querySelector("#color").onchange = changeColor;
      document.querySelector("#size").onchange = size;
      document.querySelector("#thumbs").onclick = (e) => {
        if (e.target.tagName === "IMG") document.querySelector("#hero").src = e.target.src;
      };
      changeColor();
      document.querySelector("#buy").onsubmit = (e) => {
        e.preventDefault();
        const quantity = Number(e.target.elements.namedItem("quantity").value),
          cart = readCart(),
          existing = cart.find((i) => i.id === selected.ID);
        if (
          !Number.isInteger(quantity) ||
          quantity < 1 ||
          quantity + (existing?.quantity || 0) > selected.QUANTITY
        ) {
          document.querySelector("#shop-message").textContent = "Số lượng vượt tồn kho.";
          return;
        }
        if (existing) existing.quantity += quantity;
        else
          cart.push({
            id: selected.ID,
            quantity,
            name: p.PRODUCT_NAME,
            color: selected.COLOR_NAME,
            size: selected.SIZE_NAME,
            price: selected.PRICE,
          });
        saveCart(cart);
        document.querySelector("#shop-message").textContent = "Đã thêm đúng biến thể vào giỏ hàng.";
      };
    } else if (view === "account") {
      let customer;
      try {
        customer = await api("/api/customer/me");
      } catch {}
      if (customer) {
        root.innerHTML = `<h1>Xin chào ${esc(customer.FULL_NAME)}</h1><p>${esc(customer.EMAIL)}</p><a href="/store/checkout.html?live=1">Tiếp tục thanh toán →</a><p><button id="customer-logout">Đăng xuất</button></p>`;
        document.querySelector("#customer-logout").onclick = async () => {
          await api("/api/customer/logout", "POST");
          location.reload();
        };
      } else {
        root.innerHTML = `<div class="shop-two"><section class="shop-card"><h1>Đăng nhập</h1><form id="customer-login">${field("email", "Email", "email")}${field("password", "Mật khẩu", "password")}<button>Đăng nhập</button></form></section><section class="shop-card"><h1>Tạo tài khoản</h1><form id="customer-register">${field("name", "Họ tên")}${field("phone", "Điện thoại", "tel")}${field("email", "Email", "email")}${field("password", "Mật khẩu (12–72 ký tự)", "password")}<button>Tạo tài khoản</button></form></section></div><p id="shop-message" class="shop-alert" role="status"></p>`;
        for (const kind of ["login", "register"])
          document.querySelector("#customer-" + kind).onsubmit = async (e) => {
            e.preventDefault();
            const button = e.target.querySelector("button");
            button.disabled = true;
            try {
              await api(
                "/api/customer/" + kind,
                "POST",
                Object.fromEntries(new FormData(e.target)),
              );
              csrf = await api("/api/csrf");
              location.href = "/store/checkout.html?live=1";
            } catch (err) {
              document.querySelector("#shop-message").textContent = err.message;
            } finally {
              button.disabled = false;
            }
          };
      }
    } else if (view === "cart" || view === "checkout") {
      const cart = readCart();
      if (!cart.length) {
        root.innerHTML =
          '<h1>Giỏ hàng đang trống</h1><a href="/shop/index.html">Khám phá sản phẩm →</a>';
        return;
      }
      root.innerHTML = `<h1>${view === "cart" ? "Giỏ hàng" : "Thanh toán"}</h1><table><thead><tr><th>Sản phẩm / Biến thể</th><th>Số lượng</th><th></th></tr></thead><tbody>${cart.map((i, index) => `<tr><td>${esc(i.name)}<br>${esc(i.color)} / ${esc(i.size)}</td><td><input type="number" data-index="${index}" min="1" max="100000" value="${i.quantity}" aria-label="Số lượng"></td><td><button data-remove="${index}">Gỡ</button></td></tr>`).join("")}</tbody></table><p class="shop-muted">Giá, voucher và tồn kho sẽ được máy chủ kiểm tra lại khi đặt hàng.</p><div class="shop-toolbar"><label>Mã giảm giá<input id="voucher" placeholder="Nhập voucher"></label><button id="apply">Áp dụng / Tính lại</button></div><p id="totals"></p><p id="shop-message" class="shop-alert" role="alert"></p><div id="checkout-fields"></div>`;
      const report = (e) => (document.querySelector("#shop-message").textContent = e.message);
      let shippingOffers = [],
        quoteSequence = 0,
        quoteSubtotal = 0,
        quoteDiscount = 0;
      function invalidate() {
        shippingOffers = [];
        const button = document.querySelector("#order button");
        if (button) button.disabled = true;
        const select = document.querySelector("#shipping-service");
        if (select) select.innerHTML = '<option value="">Tính lại phí giao hàng</option>';
      }
      function showTotal() {
        const select = document.querySelector("#shipping-service"),
          offer = shippingOffers.find((o) => o.serviceCode === select?.value);
        document.querySelector("#totals").textContent =
          `Tiền hàng ${money(quoteSubtotal)} · Giảm ${money(quoteDiscount)} · ${offer ? "Phí Viettel Post " + money(offer.fee) + " · Tổng " + money(quoteSubtotal - quoteDiscount + Number(offer.fee)) : "Nhập địa chỉ và tính cước để xem tổng thanh toán"}`;
        const b = document.querySelector("#order button");
        if (b) b.disabled = !offer;
      }
      async function quote() {
        const sequence = ++quoteSequence;
        invalidate();
        try {
          const form = document.querySelector("#order"),
            destination = form ? Object.fromEntries(new FormData(form)) : {};
          const d = await api("/api/catalog/quote", "POST", {
            code: document.querySelector("#voucher").value,
            items: items(),
            province: destination.province,
            ward: destination.ward,
            address: destination.address,
            paymentMethod: destination.paymentMethod,
          });
          if (sequence !== quoteSequence) return;
          quoteSubtotal = Number(d.subtotal);
          quoteDiscount = Number(d.discount);
          shippingOffers = d.services || [];
          const select = document.querySelector("#shipping-service");
          if (select) {
            select.innerHTML =
              '<option value="">Chọn dịch vụ Viettel Post</option>' +
              shippingOffers
                .map(
                  (o) =>
                    `<option value="${esc(o.serviceCode)}">${esc(o.serviceName)} · ${money(o.fee)} · ${esc(o.deliveryTime)}</option>`,
                )
                .join("");
            if (shippingOffers.length) select.value = shippingOffers[0].serviceCode;
          }
          showTotal();
          document.querySelector("#shop-message").textContent =
            d.shippingStatus && !d.shippingStatus.configured
              ? "Shop chưa kích hoạt Viettel Post. Chưa thể đặt đơn giao hàng."
              : "";
        } catch (e) {
          if (sequence === quoteSequence) {
            document.querySelector("#totals").textContent = "Chưa có báo giá giao hàng hợp lệ";
            report(e);
          }
        }
      }
      document.querySelector("#apply").onclick = quote;
      root.onchange = (e) => {
        if (e.target.id === "shipping-service") {
          showTotal();
          return;
        }
        if (["province", "ward", "address", "paymentMethod"].includes(e.target.name)) {
          quote();
          return;
        }
        if (e.target.dataset.index !== undefined) {
          const c = readCart(),
            q = Number(e.target.value);
          if (!Number.isInteger(q) || q < 1) {
            e.target.value = c[e.target.dataset.index].quantity;
            return;
          }
          c[e.target.dataset.index].quantity = q;
          saveCart(c);
          quote();
        }
      };
      root.onclick = (e) => {
        if (e.target.dataset.remove !== undefined) {
          const c = readCart();
          c.splice(Number(e.target.dataset.remove), 1);
          saveCart(c);
          location.reload();
        }
      };
      await quote();
      if (view === "cart") {
        document.querySelector("#checkout-fields").innerHTML =
          '<a href="/store/checkout.html?live=1">Tiến hành thanh toán →</a>';
        return;
      }
      let c;
      try {
        c = await api("/api/customer/me");
      } catch {
        document.querySelector("#checkout-fields").innerHTML =
          '<p><a href="/shop/index.html?view=account">Đăng nhập hoặc tạo tài khoản để đặt hàng →</a></p>';
        return;
      }
      const provinces = await api("/api/shipping/provinces");
      document.querySelector("#checkout-fields").innerHTML =
        `<form id="order"><div class="shop-two"><section>${field("name", "Người nhận", "text", c.FULL_NAME)}${field("phone", "Điện thoại", "tel", c.PHONE)}<label>Tỉnh / thành phố<select name="province" required><option value="">Chọn tỉnh/thành</option>${provinces.map((p) => `<option value="${esc(p.name)}">${esc(p.name)}</option>`).join("")}</select></label>${field("ward", "Phường / xã")}${field("address", "Địa chỉ cụ thể")}</section><section><label>Thanh toán<select name="paymentMethod"><option value="COD">Thanh toán khi nhận hàng (COD)</option><option value="BANK">Chuyển khoản thủ công · shop sẽ liên hệ</option></select></label>${field("note", "Ghi chú", "text", "", false)}<label>Dịch vụ Viettel Post<select id="shipping-service" name="shippingService" required><option value="">Nhập địa chỉ để tính cước</option></select></label><p>Địa chỉ nhận và thông tin kiện hàng được gửi tới Viettel Post để lấy báo giá. Cước được kiểm tra lại trước khi đặt; chưa tạo vận đơn tại hãng.</p><button disabled>Đặt hàng</button></section></div></form>`;
      document.querySelector("#voucher").oninput = () => {
        quoteSequence++;
        invalidate();
      };
      document.querySelector("#order").oninput = (e) => {
        if (["province", "ward", "address", "paymentMethod"].includes(e.target.name)) {
          quoteSequence++;
          invalidate();
        }
      };
      document.querySelector("#order").onsubmit = async (e) => {
        e.preventDefault();
        const button = e.target.querySelector("button");
        button.disabled = true;
        try {
          const selected = shippingOffers.find(
            (o) => o.serviceCode === document.querySelector("#shipping-service").value,
          );
          if (!selected) throw Error("Tính lại phí và chọn dịch vụ giao hàng");
          const order = await api("/api/checkout", "POST", {
            ...Object.fromEntries(new FormData(e.target)),
            expectedShippingFee: selected.fee,
            voucherCode: document.querySelector("#voucher").value,
            items: items(),
          });
          saveCart([]);
          root.innerHTML = `<h1>Đặt hàng thành công</h1><p>Mã đơn: <b>${esc(order.billCode)}</b></p><p>Tổng thanh toán: ${money(order.totalAmount)}</p><a href="/shop/index.html">Tiếp tục mua sắm →</a>`;
        } catch (e) {
          report(e);
        } finally {
          button.disabled = false;
        }
      };
    }
  } catch (e) {
    root.innerHTML = `<p class="shop-alert">${esc(e.message)}</p><a href="/shop/index.html">Về danh sách sản phẩm</a>`;
  }
})();
