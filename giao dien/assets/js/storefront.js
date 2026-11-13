(function () {
  "use strict";

  const isLoopbackHost = window.location.hostname === "localhost" || window.location.hostname === "127.0.0.1";
  const useSameOriginApi = !isLoopbackHost || window.location.port === "3000";
  const defaultApiBase = useSameOriginApi ? "/api" : "http://localhost:8080/api";
  const apiBase = (window.CLOTHING_SHOP_API_BASE || defaultApiBase).replace(/\/+$/, "");
  const tokenKey = "clothingShopToken";
  const userKey = "clothingShopUser";
  const orderKey = "clothingShopLastOrder";
  const money = new Intl.NumberFormat("vi-VN", { style: "currency", currency: "VND" });
  const sePayQrBank = "Vietcombank";
  const sePayQrAccount = "SBSEPAYGL9TEMZRXDAK";

  const stylesheet = document.createElement("link");
  stylesheet.rel = "stylesheet";
  stylesheet.href = "assets/css/storefront.css";
  document.head.appendChild(stylesheet);

  function escapeHtml(value) {
    return String(value == null ? "" : value).replace(/[&<>"']/g, function (char) {
      return { "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" }[char];
    });
  }

  function localizeStaticText() {
    document.documentElement.lang = "vi";
    const pageTitles = {
      "index.html": "Trang chủ",
      "category.html": "Danh mục sản phẩm",
      "search-results.html": "Kết quả tìm kiếm",
      "product-details.html": "Chi tiết sản phẩm",
      "cart.html": "Giỏ hàng",
      "checkout.html": "Thanh toán",
      "login-register.html": "Đăng nhập / Đăng ký",
      "account.html": "Tài khoản của tôi",
      "order-confirmation.html": "Chi tiết đơn hàng",
      "size-guide.html": "Hướng dẫn chọn size",
      "about.html": "Giới thiệu",
      "contact.html": "Liên hệ",
      "faq.html": "Câu hỏi thường gặp",
      "payment-methods.html": "Phương thức thanh toán",
      "privacy.html": "Chính sách quyền riêng tư",
      "return-policy.html": "Chính sách đổi trả",
      "shiping-info.html": "Thông tin giao hàng",
      "support.html": "Trung tâm hỗ trợ",
      "tos.html": "Điều khoản sử dụng",
      "blog.html": "Bài viết",
      "blog-details.html": "Bài viết",
      "starter-page.html": "Thông tin cửa hàng",
      "404.html": "Không tìm thấy trang"
    };
    const filename = window.location.pathname.split("/").pop().toLowerCase();
    if (pageTitles[filename]) document.title = pageTitles[filename] + " | eStore";
    const navigation = document.querySelector("#navmenu > ul");
    navigation?.querySelectorAll("a").forEach(function (link) {
      if (link.getAttribute("href") === filename) link.classList.add("active");
    });
  }

  function storedUser() {
    try {
      return JSON.parse(sessionStorage.getItem(userKey) || "null");
    } catch (error) {
      console.error("Không đọc được thông tin tài khoản trong phiên.", error);
      return null;
    }
  }

  function authHeaders() {
    const token = sessionStorage.getItem(tokenKey);
    return token ? { Authorization: "Bearer " + token } : {};
  }

  async function api(path, options) {
    const settings = options || {};
    const headers = Object.assign({ Accept: "application/json" }, authHeaders(), settings.headers || {});
    if (settings.body !== undefined) headers["Content-Type"] = "application/json";
    let response;
    try {
      response = await fetch(apiBase + path, Object.assign({}, settings, { headers: headers }));
    } catch (error) {
      throw new Error("Không kết nối được máy chủ cửa hàng. Hãy kiểm tra backend và cấu hình CORS rồi thử lại.");
    }

    let result;
    try {
      result = await response.json();
    } catch (error) {
      throw new Error("Máy chủ trả về dữ liệu không hợp lệ (HTTP " + response.status + ").");
    }
    if (!response.ok || result.success === false) {
      if (response.status >= 500) {
        throw new Error("Máy chủ gặp sự cố khi xử lý dữ liệu. Vui lòng thử lại sau.");
      }
      throw new Error(result.message || "Yêu cầu thất bại (HTTP " + response.status + ").");
    }
    return result.data;
  }

  function setMessage(element, message, isError) {
    if (!element) return;
    element.textContent = message || "";
    element.className = "store-message" + (message ? (isError ? " is-error" : " is-success") : "");
    element.setAttribute("role", isError ? "alert" : "status");
  }

  function formatPrice(value) {
    return money.format(Number(value) || 0);
  }

  function sePayQrUrl(amount, description) {
    const numericAmount = Number(amount);
    if (!Number.isSafeInteger(numericAmount) || numericAmount <= 0) {
      throw new Error("Số tiền thanh toán SePay không hợp lệ.");
    }
    const url = new URL("https://vietqr.app/img");
    url.searchParams.set("bank", sePayQrBank);
    url.searchParams.set("acc", sePayQrAccount);
    url.searchParams.set("template", "compact");
    url.searchParams.set("amount", String(numericAmount));
    url.searchParams.set("des", String(description));
    return url.toString();
  }

  function setBusy(button, busy, label) {
    if (!button) return;
    if (busy) {
      button.dataset.originalText = button.textContent;
      button.disabled = true;
      button.textContent = label || "Đang xử lý…";
    } else {
      button.disabled = false;
      button.textContent = button.dataset.originalText || button.textContent;
      delete button.dataset.originalText;
    }
  }

  function updateHeader(user) {
    const userNow = user || storedUser();
    document.querySelectorAll(".account-dropdown .dropdown-header h6").forEach(function (title) {
      title.innerHTML = userNow
        ? "Xin chào, <span class=\"sitename\">" + escapeHtml(userNow.fullName || userNow.username) + "</span>"
        : "Chào mừng đến <span class=\"sitename\">eStore</span>";
    });
    document.querySelectorAll(".account-dropdown .dropdown-body").forEach(function (body) {
      body.innerHTML = userNow
        ? '<a class="dropdown-item d-flex align-items-center" href="account.html"><i class="bi bi-person-circle me-2"></i><span>Tài khoản của tôi</span></a><a class="dropdown-item d-flex align-items-center" href="account.html"><i class="bi bi-bag-check me-2"></i><span>Đơn hàng của tôi</span></a>'
        : '<a class="dropdown-item d-flex align-items-center" href="login-register.html"><i class="bi bi-box-arrow-in-right me-2"></i><span>Đăng nhập để xem đơn hàng</span></a>';
    });
    document.querySelectorAll(".account-dropdown .dropdown-footer").forEach(function (footer) {
      footer.innerHTML = userNow
        ? '<a href="account.html" class="btn btn-primary w-100 mb-2">Tài khoản của tôi</a><button type="button" class="btn btn-outline-primary w-100" data-sign-out>Đăng xuất</button>'
        : '<a href="login-register.html" class="btn btn-primary w-100 mb-2">Đăng nhập</a><a href="login-register.html#register" class="btn btn-outline-primary w-100">Đăng ký</a>';
    });
    document.querySelectorAll(".header-action-btn .badge").forEach(function (badge) {
      if (badge.closest('a[href="cart.html"]')) {
        const saved = Number(sessionStorage.getItem("clothingShopCartCount") || 0);
        badge.textContent = String(saved);
        badge.setAttribute("aria-label", saved + " sản phẩm trong giỏ");
      }
    });
    document.querySelectorAll("[data-sign-out]").forEach(function (button) {
      button.addEventListener("click", function () {
        [tokenKey, userKey, "clothingShopCartCount"].forEach(function (key) { sessionStorage.removeItem(key); });
        window.location.href = "index.html";
      });
    });
  }

  function setupSearch() {
    document.querySelectorAll(".search-form").forEach(function (form) {
      const input = form.querySelector('input[type="search"], input[type="text"]');
      if (!input) return;
      input.placeholder = "Tìm kiếm sản phẩm";
      form.addEventListener("submit", function (event) {
        event.preventDefault();
        const keyword = input.value.trim();
        window.location.href = "search-results.html" + (keyword ? "?keyword=" + encodeURIComponent(keyword) : "");
      });
    });
  }

  function titleBlock(title, subtitle) {
    return '<div class="page-title light-background"><div class="container d-lg-flex justify-content-between align-items-center"><div><h1 class="mb-1">' +
      escapeHtml(title) + '</h1><p class="mb-0">' + escapeHtml(subtitle || "") +
      '</p></div><nav class="breadcrumbs" aria-label="Điều hướng"><ol><li><a href="index.html">Trang chủ</a></li><li class="current">' +
      escapeHtml(title) + "</li></ol></nav></div></div>";
  }

  function productCard(product) {
    const available = Number(product.totalStock) > 0;
    return '<article class="col-sm-6 col-lg-4 col-xl-3"><div class="store-product-card h-100">' +
      '<a class="store-product-image" href="product-details.html?id=' + encodeURIComponent(product.id) + '">' +
      (product.thumbnailUrl
        ? '<img src="' + escapeHtml(product.thumbnailUrl) + '" alt="' + escapeHtml(product.productName) + '" loading="lazy">'
        : '<span class="store-product-image-placeholder">Chưa có hình ảnh</span>') +
      '</a><div class="store-product-info"><p class="text-muted mb-1">' + escapeHtml(product.brandName || "") + '</p>' +
      '<h2 class="h5"><a href="product-details.html?id=' + encodeURIComponent(product.id) + '">' + escapeHtml(product.productName) + '</a></h2>' +
      '<p class="store-product-price mb-1">' + formatPrice(product.minPrice) + '</p><p class="small mb-0 ' +
      (available ? "text-success" : "text-danger") + '">' + (available ? "Còn hàng" : "Tạm hết hàng") + "</p></div></div></article>";
  }

  function pageShell(title, subtitle, content) {
    const main = document.querySelector("main");
    if (!main) return null;
    main.innerHTML = titleBlock(title, subtitle) + '<section class="section"><div class="container">' + content + "</div></section>";
    return main;
  }

  async function loadProducts(keyword, gender) {
    const query = new URLSearchParams({ page: "1", size: "24" });
    if (keyword) query.set("keyword", keyword);
    if (gender !== null && gender !== undefined && gender !== "") query.set("gender", gender);
    const page = await api("/products?" + query.toString());
    const products = page.items || [];
    const content = '<form class="row g-2 mb-4" id="product-search"><div class="col"><label class="visually-hidden" for="product-keyword">Tìm sản phẩm</label>' +
      '<input class="form-control" id="product-keyword" name="keyword" value="' + escapeHtml(keyword || "") + '" placeholder="Nhập tên sản phẩm hoặc thương hiệu"></div>' +
      '<div class="col-auto"><button class="btn btn-primary" type="submit"><i class="bi bi-search me-1" aria-hidden="true"></i>Tìm kiếm</button></div>' +
      '<div class="col-auto"><label class="visually-hidden" for="product-gender">Lọc theo đối tượng</label><select class="form-select" id="product-gender" name="gender">' +
      '<option value="">Tất cả</option><option value="0">Nam</option><option value="1">Nữ</option><option value="2">Khác</option></select></div></form>' +
      '<p class="text-muted" id="product-count"></p><div class="row g-4" id="product-results">' +
      (products.length ? products.map(productCard).join("") : '<div class="col-12"><div class="alert alert-info">Chưa tìm thấy sản phẩm phù hợp.</div></div>') +
      "</div>";
    const shell = pageShell(keyword ? "Kết quả tìm kiếm" : "Sản phẩm", "Khám phá các sản phẩm đang được cửa hàng kinh doanh.", content);
    const count = shell.querySelector("#product-count");
    count.textContent = "Tìm thấy " + page.total + " sản phẩm";
    const genderSelect = shell.querySelector("#product-gender");
    genderSelect.value = gender == null ? "" : String(gender);
    shell.querySelector("#product-search").addEventListener("submit", function (event) {
      event.preventDefault();
      const formData = new FormData(event.currentTarget);
      const params = new URLSearchParams();
      const term = String(formData.get("keyword") || "").trim();
      const selectedGender = String(formData.get("gender") || "");
      if (term) params.set("keyword", term);
      if (selectedGender) params.set("gender", selectedGender);
      const queryString = params.toString();
      window.location.href = (keyword ? "search-results.html" : "category.html") + (queryString ? "?" + queryString : "");
    });
  }

  function currentUserId() {
    const user = storedUser();
    return user && user.customerId;
  }

  function cartPath() {
    return "/carts/customer/" + encodeURIComponent(currentUserId());
  }

  function showSignInNotice(title, subtitle) {
    pageShell(title, subtitle, '<div class="alert alert-info">Đăng nhập để xem thông tin và tiếp tục mua sắm.</div><a class="btn btn-primary" href="login-register.html">Đăng nhập</a> <a class="btn btn-outline-primary" href="login-register.html#register">Tạo tài khoản</a>');
  }

  async function renderProductDetail() {
    const id = new URLSearchParams(window.location.search).get("id");
    if (!id || !/^\d+$/.test(id)) {
      await loadProducts("", "");
      return;
    }
    const [product, variants] = await Promise.all([
      api("/products/" + encodeURIComponent(id)),
      api("/products/" + encodeURIComponent(id) + "/details")
    ]);
    const colors = Array.from(new Map(variants.map(function (item) { return [item.colorName, item]; })).values());
    const image = product.thumbnailUrl || (variants[0] && variants[0].imageUrl);
    const productImage = image
      ? '<img class="img-fluid rounded-3 w-100" src="' + escapeHtml(image) + '" alt="' + escapeHtml(product.productName) + '">'
      : '<div class="store-product-image-placeholder store-product-detail-placeholder">Chưa có hình ảnh</div>';
    const content = '<div class="row g-5"><div class="col-lg-6">' + productImage + '</div><div class="col-lg-6"><p class="text-muted mb-1">' + escapeHtml(product.brandName || "") +
      '</p><h2>' + escapeHtml(product.productName) + '</h2><p class="store-product-price fs-4">' + formatPrice(product.minPrice) +
      '</p>' + (product.materialName ? '<p>' + escapeHtml("Chất liệu: " + product.materialName) + '</p>' : '') +
      '</p><p class="text-success">Tổng tồn kho: ' + Number(product.totalStock || 0) + '</p><div class="mb-3"><label class="form-label" for="variant-color">Màu sắc</label>' +
      '<select class="form-select" id="variant-color">' + colors.map(function (color) { return '<option value="' + escapeHtml(color.colorName) + '">' + escapeHtml(color.colorName) + "</option>"; }).join("") +
      '</select></div><div class="mb-3"><label class="form-label" for="variant-size">Kích cỡ</label><select class="form-select" id="variant-size"></select>' +
      '<a class="d-inline-block mt-2" href="size-guide.html">Xem hướng dẫn chọn size</a></div><div class="mb-3"><label class="form-label" for="variant-quantity">Số lượng</label>' +
      '<input class="form-control" id="variant-quantity" type="number" value="1" min="1" max="100"></div><p class="store-message" id="product-message" role="status"></p>' +
      '<button class="btn btn-primary" id="add-to-cart" type="button">Thêm vào giỏ</button> <a class="btn btn-outline-secondary" href="category.html">Tiếp tục mua sắm</a></div></div>';
    const shell = pageShell("Chi tiết sản phẩm", "Thông tin sản phẩm và các lựa chọn màu sắc, kích cỡ.", content);
    const colorSelect = shell.querySelector("#variant-color");
    const sizeSelect = shell.querySelector("#variant-size");
    const quantity = shell.querySelector("#variant-quantity");
    const message = shell.querySelector("#product-message");
    function updateSizes() {
      const selectedVariants = variants.filter(function (variant) { return variant.colorName === colorSelect.value; });
      sizeSelect.innerHTML = selectedVariants.map(function (variant) {
        return '<option value="' + escapeHtml(variant.id) + '" data-stock="' + Number(variant.quantity || 0) + '">' +
          escapeHtml(variant.sizeName) + " — " + formatPrice(variant.price) + " (" + Number(variant.quantity || 0) + " còn)</option>";
      }).join("");
      shell.querySelector("#add-to-cart").disabled = !selectedVariants.some(function (variant) { return Number(variant.quantity) > 0; });
    }
    colorSelect.addEventListener("change", updateSizes);
    updateSizes();
    shell.querySelector("#add-to-cart").addEventListener("click", async function (event) {
      if (!currentUserId()) {
        window.location.href = "login-register.html?next=" + encodeURIComponent(window.location.pathname + window.location.search);
        return;
      }
      const button = event.currentTarget;
      const selectedVariant = variants.find(function (variant) { return String(variant.id) === sizeSelect.value; });
      const qty = Number(quantity.value);
      if (!selectedVariant || qty < 1 || qty > Number(selectedVariant.quantity)) {
        setMessage(message, "Vui lòng chọn biến thể còn hàng và số lượng phù hợp.", true);
        return;
      }
      setBusy(button, true);
      try {
        const cart = await api(cartPath() + "/items", {
          method: "POST",
          body: JSON.stringify({ productDetailId: selectedVariant.id, quantity: qty })
        });
        sessionStorage.setItem("clothingShopCartCount", String(cart.totalItems || 0));
        updateHeader();
        setMessage(message, "Đã thêm sản phẩm vào giỏ hàng.", false);
      } catch (error) {
        setMessage(message, error.message, true);
      } finally {
        setBusy(button, false);
      }
    });
  }

  async function renderCart() {
    if (!currentUserId()) return showSignInNotice("Giỏ hàng", "Đăng nhập để xem giỏ hàng của bạn.");
    const cart = await api(cartPath());
    sessionStorage.setItem("clothingShopCartCount", String(cart.totalItems || 0));
    const rows = (cart.items || []).map(function (item) {
      return '<div class="store-cart-row row align-items-center g-3 py-3 border-bottom" data-item-id="' + escapeHtml(item.id) + '">' +
        '<div class="col-4 col-md-2">' + (item.imageUrl
          ? '<img class="img-fluid rounded" src="' + escapeHtml(item.imageUrl) + '" alt="' + escapeHtml(item.productName) + '">'
          : '<div class="store-product-image-placeholder rounded">Chưa có hình ảnh</div>') + '</div>' +
        '<div class="col-8 col-md-4"><h2 class="h6 mb-1">' + escapeHtml(item.productName) + '</h2><p class="small text-muted mb-0">' + escapeHtml(item.colorName) + " / " + escapeHtml(item.sizeName) + '</p>' +
        '<p class="small mb-0">' + formatPrice(item.unitPrice) + " / sản phẩm</p></div><div class=\"col-6 col-md-3\"><label class=\"visually-hidden\" for=\"quantity-" + escapeHtml(item.id) + '\">Số lượng</label>' +
        '<input class="form-control store-cart-quantity" id="quantity-' + escapeHtml(item.id) + '" type="number" min="1" max="' + Math.max(1, Number(item.stock || 1)) + '" value="' + Number(item.quantity || 1) + '"></div>' +
        '<div class="col-4 col-md-2 fw-semibold">' + formatPrice(item.totalPrice) + '</div><div class="col-2 col-md-1"><button class="btn btn-outline-danger btn-sm" type="button" data-remove-item aria-label="Xóa sản phẩm"><i class="bi bi-trash" aria-hidden="true"></i></button></div></div>';
    }).join("");
    const content = (rows ? '<div id="cart-items">' + rows + '</div>' : '<div class="alert alert-info">Giỏ hàng đang trống.</div>') +
      '<div class="row justify-content-end mt-4"><div class="col-md-6 col-lg-4"><div class="border rounded-3 p-4"><h2 class="h5">Tóm tắt giỏ hàng</h2>' +
      '<div class="d-flex justify-content-between"><span>Tạm tính</span><strong id="cart-total">' + formatPrice(cart.totalAmount) + '</strong></div>' +
      '<p class="small text-muted mt-2">Phí giao hàng được tính khi xác nhận đơn hàng.</p><a class="btn btn-primary w-100" href="checkout.html"' +
      (cart.totalItems ? "" : ' aria-disabled="true" tabindex="-1"') + '>Tiến hành thanh toán</a></div></div></div>' +
      '<div class="mt-4"><a href="category.html">Tiếp tục mua sắm</a></div><p id="cart-message" class="store-message" role="status"></p>';
    const shell = pageShell("Giỏ hàng", "Kiểm tra sản phẩm và số lượng trước khi đặt hàng.", content);
    const message = shell.querySelector("#cart-message");
    shell.querySelectorAll(".store-cart-row").forEach(function (row) {
      const itemId = row.dataset.itemId;
      row.querySelector(".store-cart-quantity").addEventListener("change", async function (event) {
        const quantity = Math.min(100, Math.max(1, Number(event.currentTarget.value) || 1));
        event.currentTarget.value = String(quantity);
        try {
          await api(cartPath() + "/items/" + encodeURIComponent(itemId), {
            method: "PUT",
            body: JSON.stringify({ quantity: quantity })
          });
          await renderCart();
        } catch (error) {
          setMessage(message, error.message, true);
        }
      });
      row.querySelector("[data-remove-item]").addEventListener("click", async function () {
        try {
          await api(cartPath() + "/items/" + encodeURIComponent(itemId), { method: "DELETE" });
          await renderCart();
        } catch (error) {
          setMessage(message, error.message, true);
        }
      });
    });
    updateHeader();
  }

  function addressCard(address, index) {
    return '<label class="store-address-choice d-flex gap-3 p-3 border rounded-3 mb-2"><input type="radio" name="addressId" value="' +
      escapeHtml(address.id) + '"' + ((address.isDefault || index === 0) ? " checked" : "") + '><span><strong>' +
      escapeHtml(address.receiverName) + " · " + escapeHtml(address.receiverPhone) + '</strong><br>' +
      escapeHtml(address.detailAddress) + ", " + escapeHtml(address.ward) + ", " + escapeHtml(address.province) +
      (address.isDefault ? " · Mặc định" : "") + "</span></label>";
  }

  async function renderCheckout() {
    if (!currentUserId()) return showSignInNotice("Thanh toán", "Đăng nhập để chọn địa chỉ và đặt hàng.");
    const [cart, addresses] = await Promise.all([api(cartPath()), api("/addresses")]);
    if (!cart.totalItems) {
      return pageShell("Thanh toán", "Xác nhận đơn hàng của bạn.", '<div class="alert alert-info">Giỏ hàng đang trống. <a href="category.html">Xem sản phẩm</a></div>');
    }
    const savedAddresses = addresses.map(addressCard).join("");
    const content = '<div class="row g-4"><div class="col-lg-7"><h2 class="h4">Địa chỉ nhận hàng</h2>' +
      '<div id="saved-addresses">' + (savedAddresses || '<p class="text-muted">Bạn chưa lưu địa chỉ nhận hàng. Hãy nhập địa chỉ bên dưới.</p>') + '</div>' +
      '<details class="mt-3" ' + (addresses.length ? "" : "open") + '><summary class="mb-3">Thêm địa chỉ giao hàng</summary><div class="row g-3">' +
      '<div class="col-md-6"><label class="form-label" for="receiver-name">Tên người nhận</label><input class="form-control" id="receiver-name" maxlength="150"></div>' +
      '<div class="col-md-6"><label class="form-label" for="receiver-phone">Số điện thoại</label><input class="form-control" id="receiver-phone" maxlength="20"></div>' +
      '<div class="col-md-6"><label class="form-label" for="province">Tỉnh / Thành phố</label><input class="form-control" id="province" maxlength="100"></div>' +
      '<div class="col-md-6"><label class="form-label" for="ward">Phường / Xã</label><input class="form-control" id="ward" maxlength="100"></div>' +
      '<div class="col-12"><label class="form-label" for="detail-address">Địa chỉ cụ thể</label><input class="form-control" id="detail-address" maxlength="300" placeholder="Số nhà, tên đường"></div>' +
      '<div class="col-12"><div class="form-check"><input class="form-check-input" id="default-address" type="checkbox"><label class="form-check-label" for="default-address">Lưu làm địa chỉ mặc định</label></div></div>' +
      '<div class="col-12"><button class="btn btn-outline-primary" id="save-address" type="button">Lưu địa chỉ</button></div></div></details>' +
      '<h2 class="h4 mt-4">Ghi chú đơn hàng</h2><label class="visually-hidden" for="order-note">Ghi chú</label><textarea class="form-control" id="order-note" rows="3" maxlength="500" placeholder="Ví dụ: giao giờ hành chính"></textarea></div>' +
      '<div class="col-lg-5"><div class="border rounded-3 p-4"><h2 class="h4">Đơn hàng</h2><ul class="list-group list-group-flush mb-3">' +
      cart.items.map(function (item) { return '<li class="list-group-item d-flex justify-content-between gap-3 px-0"><span>' + escapeHtml(item.productName) + " · " +
        escapeHtml(item.colorName) + " / " + escapeHtml(item.sizeName) + " × " + Number(item.quantity) + "</span><strong>" + formatPrice(item.totalPrice) + "</strong></li>"; }).join("") +
      '</ul><div class="d-flex justify-content-between"><span>Tạm tính</span><strong>' + formatPrice(cart.totalAmount) + '</strong></div><p class="small text-muted mt-2">Phí giao hàng sẽ được cộng theo cấu hình cửa hàng.</p>' +
      '<div class="mb-3"><label class="form-label" for="payment-method">Thanh toán</label><select class="form-select" id="payment-method"><option value="COD">Thanh toán khi nhận hàng (COD)</option><option value="SEPAY">Chuyển khoản QR qua SePay</option></select><p class="small text-muted mt-2 mb-0">Sau khi đặt hàng, mã QR sẽ hiển thị để bạn quét và chuyển khoản.</p></div>' +
      '<p class="store-message" id="checkout-message" role="status"></p><button class="btn btn-primary w-100" id="place-order" type="button">Đặt hàng</button></div></div></div>';
    const shell = pageShell("Thanh toán", "Chọn địa chỉ nhận hàng và phương thức thanh toán.", content);
    const message = shell.querySelector("#checkout-message");
    const paymentMethod = shell.querySelector("#payment-method");
    const placeOrderButton = shell.querySelector("#place-order");
    paymentMethod.addEventListener("change", function () {
      placeOrderButton.textContent = paymentMethod.value === "SEPAY" ? "Tạo đơn và hiện mã QR" : "Đặt hàng";
    });
    let currentAddresses = addresses.slice();
    shell.querySelector("#save-address").addEventListener("click", async function (event) {
      const button = event.currentTarget;
      const address = {
        receiverName: shell.querySelector("#receiver-name").value.trim(),
        receiverPhone: shell.querySelector("#receiver-phone").value.trim(),
        province: shell.querySelector("#province").value.trim(),
        ward: shell.querySelector("#ward").value.trim(),
        detailAddress: shell.querySelector("#detail-address").value.trim(),
        isDefault: shell.querySelector("#default-address").checked
      };
      if (Object.values(address).some(function (value) { return typeof value === "string" && !value; })) {
        setMessage(message, "Vui lòng nhập đầy đủ thông tin địa chỉ.", true);
        return;
      }
      setBusy(button, true);
      try {
        const created = await api("/addresses", { method: "POST", body: JSON.stringify(address) });
        currentAddresses.push(created);
        shell.querySelector("#saved-addresses")?.insertAdjacentHTML("beforeend", addressCard(created, currentAddresses.length - 1));
        shell.querySelector("details").open = false;
        shell.querySelectorAll('input[name="addressId"]').forEach(function (input) { input.checked = String(input.value) === String(created.id); });
        setMessage(message, "Đã lưu địa chỉ nhận hàng.", false);
      } catch (error) {
        setMessage(message, error.message, true);
      } finally {
        setBusy(button, false);
      }
    });
    shell.querySelector("#place-order").addEventListener("click", async function (event) {
      const addressId = shell.querySelector('input[name="addressId"]:checked')?.value;
      if (!addressId) {
        setMessage(message, "Vui lòng chọn hoặc thêm địa chỉ nhận hàng.", true);
        return;
      }
      const button = event.currentTarget;
      setBusy(button, true);
      try {
        const selectedPaymentMethod = paymentMethod.value;
        const result = await api(cartPath() + (selectedPaymentMethod === "SEPAY" ? "/checkout/sepay" : "/checkout"), {
          method: "POST",
          body: JSON.stringify({
            addressId: Number(addressId),
            paymentMethod: selectedPaymentMethod,
            note: shell.querySelector("#order-note").value.trim()
          })
        });
        const bill = result;
        sessionStorage.setItem(orderKey, String(bill.id));
        sessionStorage.setItem("clothingShopCartCount", "0");
        if (selectedPaymentMethod === "SEPAY") {
          window.location.href = "order-confirmation.html?orderId=" + encodeURIComponent(bill.id);
          return;
        }
        window.location.href = "order-confirmation.html?orderId=" + encodeURIComponent(bill.id);
      } catch (error) {
        setMessage(message, error.message, true);
        setBusy(button, false);
      }
    });
  }

  async function renderAuth() {
    const content = '<div class="row justify-content-center"><div class="col-lg-8"><ul class="nav nav-tabs mb-4" role="tablist">' +
      '<li class="nav-item"><button class="nav-link active" id="login-tab" type="button">Đăng nhập</button></li>' +
      '<li class="nav-item"><button class="nav-link" id="register-tab" type="button">Tạo tài khoản</button></li></ul>' +
      '<p class="store-message" id="auth-message" role="status"></p><form id="login-form"><h2 class="h4 mb-3">Đăng nhập</h2>' +
      '<div class="mb-3"><label class="form-label" for="login-email">Email</label><input class="form-control" id="login-email" type="email" autocomplete="email" required></div>' +
      '<div class="mb-3"><label class="form-label" for="login-password">Mật khẩu</label><input class="form-control" id="login-password" type="password" autocomplete="current-password" required></div>' +
      '<button class="btn btn-primary" type="submit">Đăng nhập</button></form><form id="register-form" hidden><h2 class="h4 mb-3">Tạo tài khoản</h2>' +
      '<div class="mb-3"><label class="form-label" for="register-username">Tên đăng nhập</label><input class="form-control" id="register-username" minlength="3" maxlength="100" autocomplete="username" required></div>' +
      '<div class="mb-3"><label class="form-label" for="register-name">Họ và tên</label><input class="form-control" id="register-name" maxlength="150" autocomplete="name" required></div>' +
      '<div class="mb-3"><label class="form-label" for="register-email">Email</label><input class="form-control" id="register-email" type="email" maxlength="255" autocomplete="email" required></div>' +
      '<div class="mb-3"><label class="form-label" for="register-phone">Số điện thoại (không bắt buộc)</label><input class="form-control" id="register-phone" autocomplete="tel"></div>' +
      '<div class="mb-3"><label class="form-label" for="register-password">Mật khẩu (ít nhất 8 ký tự)</label><input class="form-control" id="register-password" type="password" minlength="8" maxlength="72" autocomplete="new-password" required></div>' +
      '<button class="btn btn-primary" type="submit">Tạo tài khoản</button></form></div></div>';
    const shell = pageShell("Tài khoản", "Đăng nhập hoặc đăng ký để quản lý đơn hàng.", content);
    const loginForm = shell.querySelector("#login-form");
    const registerForm = shell.querySelector("#register-form");
    const message = shell.querySelector("#auth-message");
    const loginTab = shell.querySelector("#login-tab");
    const registerTab = shell.querySelector("#register-tab");
    function showRegister(show) {
      loginForm.hidden = show;
      registerForm.hidden = !show;
      loginTab.classList.toggle("active", !show);
      registerTab.classList.toggle("active", show);
      if (show) history.replaceState(null, "", "#register");
    }
    loginTab.addEventListener("click", function () { showRegister(false); });
    registerTab.addEventListener("click", function () { showRegister(true); });
    if (window.location.hash === "#register") showRegister(true);

    async function authenticate(form, endpoint, payload) {
      const button = form.querySelector('button[type="submit"]');
      setBusy(button, true);
      setMessage(message, "", false);
      try {
        const auth = await api(endpoint, { method: "POST", body: JSON.stringify(payload) });
        sessionStorage.setItem(tokenKey, auth.accessToken);
        sessionStorage.setItem(userKey, JSON.stringify(auth.user));
        updateHeader(auth.user);
        const next = new URLSearchParams(window.location.search).get("next");
        window.location.href = next && next.startsWith("/") ? next : "account.html";
      } catch (error) {
        setMessage(message, error.message, true);
        setBusy(button, false);
      }
    }
    loginForm.addEventListener("submit", function (event) {
      event.preventDefault();
      authenticate(loginForm, "/auth/login", {
        email: shell.querySelector("#login-email").value.trim(),
        password: shell.querySelector("#login-password").value
      });
    });
    registerForm.addEventListener("submit", function (event) {
      event.preventDefault();
      authenticate(registerForm, "/auth/register", {
        username: shell.querySelector("#register-username").value.trim(),
        fullName: shell.querySelector("#register-name").value.trim(),
        email: shell.querySelector("#register-email").value.trim(),
        phone: shell.querySelector("#register-phone").value.trim(),
        password: shell.querySelector("#register-password").value
      });
    });
  }

  function orderStatus(status) {
    return ({
      0: "Chờ xác nhận",
      1: "Đã xác nhận",
      2: "Đang giao",
      3: "Hoàn thành",
      4: "Đã hủy",
      5: "Yêu cầu trả hàng"
    })[status] || "Đang cập nhật";
  }

  function paymentStatus(status) {
    return ({ 0: "Chưa thanh toán", 1: "Đã thanh toán", 2: "Đã hoàn tiền" })[status] || "Đang cập nhật";
  }

  function paymentMethodLabel(method) {
    return method === "SEPAY" ? "SePay" : method === "COD" ? "COD" : method;
  }

  async function renderAccount() {
    if (!currentUserId()) return showSignInNotice("Tài khoản của tôi", "Đăng nhập để xem hồ sơ và đơn hàng.");
    const [user, bills] = await Promise.all([api("/auth/me"), api("/bills")]);
    sessionStorage.setItem(userKey, JSON.stringify(user));
    const orders = bills.length ? '<div class="table-responsive"><table class="table align-middle"><thead><tr><th>Mã đơn</th><th>Ngày đặt</th><th>Trạng thái</th><th>Thanh toán</th><th>Tổng tiền</th><th></th></tr></thead><tbody>' +
      bills.map(function (bill) {
        return '<tr><td>' + escapeHtml(bill.billCode) + '</td><td>' + escapeHtml(new Date(bill.createdAt).toLocaleDateString("vi-VN")) +
          '</td><td>' + orderStatus(bill.status) + '</td><td>' + escapeHtml(paymentMethodLabel(bill.paymentMethod)) + ' · ' + escapeHtml(paymentStatus(bill.paymentStatus)) +
          '</td><td>' + formatPrice(bill.totalAmount) + '</td><td><button class="btn btn-sm btn-outline-primary" type="button" data-order-id="' +
          escapeHtml(bill.id) + '">Chi tiết</button>' + (bill.status === 0 && bill.paymentStatus !== 1 ? ' <button class="btn btn-sm btn-outline-danger" type="button" data-cancel-id="' + escapeHtml(bill.id) + '">Hủy</button>' : "") + "</td></tr>";
      }).join("") + "</tbody></table></div>" : '<div class="alert alert-info">Bạn chưa có đơn hàng nào.</div>';
    const content = '<div class="row g-4"><div class="col-lg-4"><div class="border rounded-3 p-4"><h2 class="h5">Thông tin tài khoản</h2><p class="mb-1"><strong>' +
      escapeHtml(user.fullName) + '</strong></p><p class="mb-1">' + escapeHtml(user.email) + '</p><p class="mb-0">' + escapeHtml(user.phone || "Chưa cập nhật số điện thoại") + '</p></div></div>' +
      '<div class="col-lg-8"><h2 class="h4">Đơn hàng của tôi</h2><p class="store-message" id="account-message" role="status"></p>' + orders + '</div></div>';
    const shell = pageShell("Tài khoản của tôi", "Thông tin cá nhân và lịch sử mua hàng.", content);
    const message = shell.querySelector("#account-message");
    shell.querySelectorAll("[data-order-id]").forEach(function (button) {
      button.addEventListener("click", function () {
        window.location.href = "order-confirmation.html?orderId=" + encodeURIComponent(button.dataset.orderId);
      });
    });
    shell.querySelectorAll("[data-cancel-id]").forEach(function (button) {
      button.addEventListener("click", async function () {
        if (!window.confirm("Bạn có chắc muốn hủy đơn hàng này?")) return;
        setBusy(button, true);
        try {
          await api("/bills/" + encodeURIComponent(button.dataset.cancelId) + "/cancel", { method: "PATCH" });
          await renderAccount();
        } catch (error) {
          setMessage(message, error.message, true);
          setBusy(button, false);
        }
      });
    });
  }

  async function renderOrderConfirmation() {
    if (!currentUserId()) return showSignInNotice("Đơn hàng", "Đăng nhập để theo dõi đơn hàng.");
    const requestedId = new URLSearchParams(window.location.search).get("orderId") || sessionStorage.getItem(orderKey);
    if (!requestedId || !/^\d+$/.test(requestedId)) {
      const paymentNotice = new URLSearchParams(window.location.search).has("payment")
        ? '<div class="alert alert-warning">Không thể xác minh đơn hàng. Đăng nhập để xem trạng thái thanh toán trong mục <a href="account.html">đơn hàng của tôi</a>.</div>'
        : "";
      return pageShell("Đơn hàng của tôi", "Theo dõi trạng thái các đơn đã đặt.", paymentNotice + '<div class="alert alert-info">Chưa có đơn hàng cần hiển thị. <a href="account.html">Xem đơn hàng của tôi</a></div>');
    }
    const [bill, lines, history] = await Promise.all([
      api("/bills/" + encodeURIComponent(requestedId)),
      api("/bills/" + encodeURIComponent(requestedId) + "/lines"),
      api("/bills/" + encodeURIComponent(requestedId) + "/history")
    ]);
    const paymentNotice = bill.paymentMethod === "SEPAY"
      ? bill.paymentStatus === 1
        ? '<div class="alert alert-success" role="status">Thanh toán SePay đã được xác nhận.</div>'
        : '<div class="alert alert-warning" role="status">Đơn hàng đang chờ SePay xác nhận giao dịch chuyển khoản.</div>'
      : "";
    const paymentLabel = paymentMethodLabel(bill.paymentMethod) + " · " + paymentStatus(bill.paymentStatus);
    const paymentQr = bill.paymentMethod === "SEPAY" && bill.paymentStatus === 0 && bill.status !== 4
      ? '<div class="border rounded-3 p-3 mt-3 text-center"><h3 class="h5">Quét mã QR để chuyển khoản</h3>' +
        '<img class="img-fluid" style="max-width:320px" src="' + escapeHtml(sePayQrUrl(bill.totalAmount, bill.billCode)) +
        '" alt="Mã QR thanh toán SePay"><p class="mb-1">Số tiền: <strong>' + formatPrice(bill.totalAmount) +
        '</strong></p><p class="mb-2">Nội dung chuyển khoản: <strong>' + escapeHtml(bill.billCode) +
        '</strong></p><p class="small text-muted">Vui lòng giữ nguyên số tiền và nội dung để hệ thống tự đối soát.</p>' +
        '<button class="btn btn-outline-primary" type="button" data-refresh-payment>Kiểm tra trạng thái thanh toán</button></div>'
      : "";
    const content = paymentNotice + '<div class="alert alert-info"><h2 class="h4">Thông tin đơn hàng</h2><p class="mb-0">Mã đơn: <strong>' + escapeHtml(bill.billCode) +
      '</strong> · Trạng thái: ' + orderStatus(bill.status) + '</p></div><div class="row g-4"><div class="col-lg-7"><h3 class="h5">Sản phẩm</h3>' +
      '<ul class="list-group mb-4">' + lines.map(function (line) {
        return '<li class="list-group-item d-flex justify-content-between gap-3"><span>' + escapeHtml(line.productName) + " · " + escapeHtml(line.colorName) + " / " +
          escapeHtml(line.sizeName) + " × " + Number(line.quantity) + "</span><strong>" + formatPrice(line.totalPrice) + "</strong></li>";
      }).join("") + '</ul><h3 class="h5">Cập nhật đơn hàng</h3><ul class="list-group">' + history.map(function (entry) {
        return '<li class="list-group-item">' + escapeHtml(entry.note || orderStatus(entry.status)) + ' <span class="text-muted small">' +
          escapeHtml(new Date(entry.createdAt).toLocaleString("vi-VN")) + "</span></li>";
      }).join("") + '</ul></div><div class="col-lg-5"><div class="border rounded-3 p-4"><h3 class="h5">Thông tin giao hàng</h3><p>' +
      escapeHtml(bill.receiverName) + " · " + escapeHtml(bill.receiverPhone) + "<br>" + escapeHtml(bill.shippingAddress) +
      '</p><p>Thanh toán: ' + escapeHtml(paymentLabel) +
      '</p><div class="d-flex justify-content-between"><span>Tạm tính</span><span>' + formatPrice(bill.subTotal) +
      '</span></div><div class="d-flex justify-content-between"><span>Giao hàng</span><span>' + formatPrice(bill.shippingFee) +
      '</span></div><hr><div class="d-flex justify-content-between fw-bold"><span>Tổng cộng</span><span>' + formatPrice(bill.totalAmount) +
      '</span></div>' + paymentQr + '</div></div></div><a class="btn btn-outline-primary mt-4" href="account.html">Về đơn hàng của tôi</a>';
    const shell = pageShell("Chi tiết đơn hàng", "Thông tin trạng thái và giao nhận.", content);
    shell.querySelector("[data-refresh-payment]")?.addEventListener("click", function () {
      window.location.reload();
    });
  }

  function renderError(error) {
    const main = document.querySelector("main");
    if (!main) return;
    main.innerHTML = titleBlock("Không tải được dữ liệu", "Đã xảy ra lỗi khi kết nối hoặc đọc dữ liệu từ cửa hàng.") +
      '<section class="section"><div class="container"><div class="alert alert-danger" role="alert">' + escapeHtml(error.message) +
      '</div><button class="btn btn-outline-primary" type="button" onclick="window.location.reload()">Thử lại</button></div></section>';
  }

  function renderStaticPage(filename) {
    const pages = {
      "about.html": {
        title: "Giới thiệu",
        subtitle: "Cửa hàng thời trang trực tuyến eStore.",
        content: '<div class="row justify-content-center"><div class="col-lg-9"><h2 class="h4">Thời trang cho phong cách hằng ngày</h2><p>eStore giúp bạn khám phá các sản phẩm quần áo, xem thông tin màu sắc, kích cỡ và tình trạng hàng trước khi đặt mua.</p><p>Thông tin sản phẩm và đơn hàng được tải trực tiếp từ hệ thống cửa hàng. Nếu cần tư vấn về kích cỡ, hãy xem <a href="size-guide.html">hướng dẫn chọn size</a> hoặc liên hệ với cửa hàng.</p><a class="btn btn-primary" href="category.html">Xem sản phẩm</a></div></div>'
      },
      "contact.html": {
        title: "Liên hệ",
        subtitle: "Thông tin hỗ trợ mua hàng.",
        content: '<div class="row g-4"><div class="col-lg-7"><h2 class="h4">Cần hỗ trợ?</h2><p>Để được hỗ trợ nhanh, vui lòng chuẩn bị mã đơn hàng (nếu có) và đăng nhập để xem trạng thái đơn trong mục tài khoản.</p><a class="btn btn-primary me-2" href="account.html">Đơn hàng của tôi</a><a class="btn btn-outline-primary" href="faq.html">Xem câu hỏi thường gặp</a></div><div class="col-lg-5"><div class="border rounded-3 p-4"><h2 class="h5">Kênh liên hệ</h2><p class="mb-0">Cửa hàng chưa cấu hình số điện thoại hoặc email hỗ trợ trên website. Vui lòng cập nhật thông tin liên hệ chính thức trước khi đưa trang vào hoạt động.</p></div></div></div>'
      },
      "faq.html": {
        title: "Câu hỏi thường gặp",
        subtitle: "Thông tin nhanh về sản phẩm, thanh toán và đơn hàng.",
        content: '<div class="accordion" id="store-faq"><div class="accordion-item"><h2 class="accordion-header"><button class="accordion-button" type="button" data-bs-toggle="collapse" data-bs-target="#faq-size">Làm sao để chọn đúng kích cỡ?</button></h2><div id="faq-size" class="accordion-collapse collapse show" data-bs-parent="#store-faq"><div class="accordion-body">Đo vòng ngực, eo và hông rồi đối chiếu <a href="size-guide.html">bảng size tham khảo</a>. Nếu sản phẩm có hướng dẫn riêng, hãy ưu tiên thông tin trên trang sản phẩm.</div></div></div><div class="accordion-item"><h2 class="accordion-header"><button class="accordion-button collapsed" type="button" data-bs-toggle="collapse" data-bs-target="#faq-payment">Website hiện hỗ trợ thanh toán nào?</button></h2><div id="faq-payment" class="accordion-collapse collapse" data-bs-parent="#store-faq"><div class="accordion-body">Bạn có thể thanh toán khi nhận hàng (COD) hoặc thanh toán trực tuyến qua cổng SePay. Trạng thái chỉ được xác nhận sau khi máy chủ nhận IPN hợp lệ và đối chiếu đúng hóa đơn, số tiền.</div></div></div><div class="accordion-item"><h2 class="accordion-header"><button class="accordion-button collapsed" type="button" data-bs-toggle="collapse" data-bs-target="#faq-shipping">Phí giao hàng là bao nhiêu?</button></h2><div id="faq-shipping" class="accordion-collapse collapse" data-bs-parent="#store-faq"><div class="accordion-body">Phí mặc định hiện là 30.000 đồng và có thể được cửa hàng cấu hình lại. Tổng tiền và phí giao hàng được hiển thị trong thông tin đơn.</div></div></div><div class="accordion-item"><h2 class="accordion-header"><button class="accordion-button collapsed" type="button" data-bs-toggle="collapse" data-bs-target="#faq-cancel">Tôi có thể hủy đơn hàng không?</button></h2><div id="faq-cancel" class="accordion-collapse collapse" data-bs-parent="#store-faq"><div class="accordion-body">Bạn có thể gửi yêu cầu hủy khi đơn còn ở trạng thái chờ xác nhận và chưa thanh toán, trong mục <a href="account.html">đơn hàng của tôi</a>.</div></div></div><div class="accordion-item"><h2 class="accordion-header"><button class="accordion-button collapsed" type="button" data-bs-toggle="collapse" data-bs-target="#faq-status">Theo dõi đơn hàng ở đâu?</button></h2><div id="faq-status" class="accordion-collapse collapse" data-bs-parent="#store-faq"><div class="accordion-body">Đăng nhập rồi mở mục tài khoản để xem trạng thái, chi tiết và lịch sử cập nhật đơn hàng.</div></div></div></div>'
      },
      "payment-methods.html": {
        title: "Phương thức thanh toán",
        subtitle: "Phương thức thanh toán hiện được hệ thống hỗ trợ.",
        content: '<div class="row g-4"><div class="col-md-6"><div class="h-100 border rounded-3 p-4"><h2 class="h4">Thanh toán khi nhận hàng (COD)</h2><p class="mb-0">Thanh toán cho đơn vị giao hàng khi nhận sản phẩm. Vui lòng kiểm tra thông tin người nhận và số điện thoại trước khi đặt hàng.</p></div></div><div class="col-md-6"><div class="h-100 border rounded-3 p-4"><h2 class="h4">Thanh toán trực tuyến qua SePay</h2><p class="mb-0">Bạn sẽ được chuyển đến cổng SePay để thanh toán. Đơn hàng chỉ được đánh dấu đã thanh toán sau khi máy chủ nhận IPN hợp lệ và đối chiếu hóa đơn, số tiền.</p></div></div></div>'
      },
      "privacy.html": {
        title: "Chính sách quyền riêng tư",
        subtitle: "Thông tin về dữ liệu được sử dụng để vận hành việc mua hàng.",
        content: '<div class="row justify-content-center"><div class="col-lg-9"><p>Website sử dụng thông tin tài khoản, địa chỉ nhận hàng và nội dung đơn hàng để cung cấp các chức năng đăng nhập, giao hàng và tra cứu đơn.</p><h2 class="h5 mt-4">Dữ liệu tài khoản</h2><p>Mật khẩu được lưu ở dạng mã hóa một chiều trong hệ thống. Không gửi mật khẩu hoặc mã truy cập cho người khác.</p><h2 class="h5 mt-4">Quản lý thông tin</h2><p>Thông tin đơn hàng cần được giữ để cửa hàng xử lý và hỗ trợ sau khi đặt mua. Các yêu cầu cập nhật hoặc xóa dữ liệu cần được cửa hàng xử lý theo chính sách áp dụng.</p><div class="alert alert-warning">Cửa hàng cần rà soát và bổ sung thông tin pháp lý, thời hạn lưu trữ và kênh tiếp nhận yêu cầu trước khi vận hành chính thức.</div></div></div>'
      },
      "return-policy.html": {
        title: "Chính sách đổi trả",
        subtitle: "Các thao tác hiện được hỗ trợ trên hệ thống.",
        content: '<div class="row justify-content-center"><div class="col-lg-9"><h2 class="h4">Hủy đơn hàng</h2><p>Khách hàng có thể gửi yêu cầu hủy đơn khi đơn còn ở trạng thái chờ xác nhận. Mở mục <a href="account.html">đơn hàng của tôi</a>, chọn đơn phù hợp và nhấn hủy.</p><h2 class="h5 mt-4">Đổi hoặc trả sản phẩm</h2><p>Yêu cầu đổi hoặc trả sau khi nhận hàng chưa được xử lý trực tiếp trên website. Vui lòng liên hệ cửa hàng để xác nhận điều kiện và quy trình áp dụng.</p><div class="alert alert-warning">Cửa hàng cần công bố thời hạn, điều kiện đổi trả và thông tin liên hệ chính thức trước khi nhận đơn hàng.</div></div></div>'
      },
      "shiping-info.html": {
        title: "Thông tin giao hàng",
        subtitle: "Thông tin giao nhận đang được áp dụng trong hệ thống.",
        content: '<div class="row g-4"><div class="col-md-6"><div class="h-100 border rounded-3 p-4"><h2 class="h5">Phí giao hàng</h2><p class="mb-0">Phí mặc định là 30.000 đồng mỗi đơn và có thể thay đổi theo cấu hình của cửa hàng. Phí được tính vào tổng đơn hàng khi thanh toán.</p></div></div><div class="col-md-6"><div class="h-100 border rounded-3 p-4"><h2 class="h5">Theo dõi đơn hàng</h2><p class="mb-0">Đăng nhập và mở mục <a href="account.html">đơn hàng của tôi</a> để xem trạng thái và lịch sử cập nhật.</p></div></div><div class="col-12"><div class="alert alert-warning mb-0">Thời gian giao hàng và đơn vị vận chuyển chưa được tích hợp; cửa hàng cần cập nhật chính sách giao nhận thực tế.</div></div></div>'
      },
      "support.html": {
        title: "Trung tâm hỗ trợ",
        subtitle: "Hướng dẫn sử dụng các chức năng mua hàng.",
        content: '<div class="row g-4"><div class="col-md-4"><div class="h-100 border rounded-3 p-4"><h2 class="h5">Chọn sản phẩm</h2><p>Xem màu sắc, kích cỡ, giá và tình trạng hàng trên trang chi tiết sản phẩm.</p><a href="size-guide.html">Hướng dẫn chọn size</a></div></div><div class="col-md-4"><div class="h-100 border rounded-3 p-4"><h2 class="h5">Thanh toán</h2><p>Chọn COD hoặc thanh toán trực tuyến qua SePay khi đặt hàng.</p><a href="payment-methods.html">Chi tiết thanh toán</a></div></div><div class="col-md-4"><div class="h-100 border rounded-3 p-4"><h2 class="h5">Đơn hàng</h2><p>Đăng nhập để xem trạng thái, lịch sử và yêu cầu hủy đơn đang chờ xác nhận.</p><a href="account.html">Mở tài khoản</a></div></div></div><p class="mt-4">Cần thêm trợ giúp? Xem <a href="faq.html">câu hỏi thường gặp</a> hoặc <a href="contact.html">thông tin liên hệ</a>.</p>'
      },
      "tos.html": {
        title: "Điều khoản sử dụng",
        subtitle: "Các nguyên tắc cơ bản khi sử dụng website mua hàng.",
        content: '<div class="row justify-content-center"><div class="col-lg-9"><h2 class="h5">Thông tin sản phẩm</h2><p>Giá, kích cỡ và tình trạng tồn kho được hiển thị theo dữ liệu cửa hàng. Đơn hàng chỉ được tiếp nhận khi hệ thống xác nhận thành công.</p><h2 class="h5 mt-4">Tài khoản</h2><p>Bạn cần cung cấp thông tin nhận hàng chính xác và bảo vệ thông tin đăng nhập của mình.</p><h2 class="h5 mt-4">Thanh toán và hủy đơn</h2><p>Phương thức thanh toán hiện hỗ trợ là COD. Đơn hàng chỉ có thể được hủy khi đang chờ xác nhận.</p><h2 class="h5 mt-4">Chính sách cửa hàng</h2><p>Các điều khoản pháp lý, quyền và nghĩa vụ cụ thể cần được cửa hàng rà soát và bổ sung trước khi website được đưa vào vận hành chính thức.</p></div></div>'
      },
      "starter-page.html": {
        title: "Thông tin cửa hàng",
        subtitle: "Trang giới thiệu các chức năng mua sắm.",
        content: '<div class="row g-3"><div class="col-md-4"><a class="d-block border rounded-3 p-4 h-100" href="category.html"><h2 class="h5">Sản phẩm</h2><p class="mb-0">Tìm kiếm và xem các sản phẩm đang kinh doanh.</p></a></div><div class="col-md-4"><a class="d-block border rounded-3 p-4 h-100" href="size-guide.html"><h2 class="h5">Chọn kích cỡ</h2><p class="mb-0">Tham khảo hướng dẫn đo và bảng size.</p></a></div><div class="col-md-4"><a class="d-block border rounded-3 p-4 h-100" href="account.html"><h2 class="h5">Đơn hàng</h2><p class="mb-0">Đăng nhập để theo dõi các đơn đã đặt.</p></a></div></div>'
      },
      "404.html": {
        title: "Không tìm thấy trang",
        subtitle: "Đường dẫn bạn truy cập không tồn tại hoặc đã được thay đổi.",
        content: '<div class="text-center"><i class="bi bi-search display-4 text-muted" aria-hidden="true"></i><p class="mt-3">Hãy quay về trang chủ hoặc xem danh sách sản phẩm.</p><a class="btn btn-primary me-2" href="index.html">Về trang chủ</a><a class="btn btn-outline-primary" href="category.html">Xem sản phẩm</a></div>'
      }
    };
    const page = pages[filename];
    if (page) {
      pageShell(page.title, page.subtitle, page.content);
      return;
    }
    if (filename === "blog.html" || filename === "blog-details.html") {
      pageShell("Bài viết", "Chưa có bài viết từ cửa hàng.");
    }
  }

  function selectedView() {
    const file = window.location.pathname.split("/").pop().toLowerCase();
    if (file === "index.html" || file === "") return ["products", "", ""];
    if (file === "category.html") return ["products", "", new URLSearchParams(window.location.search).get("gender")];
    if (file === "search-results.html") return ["products", new URLSearchParams(window.location.search).get("keyword") || "", new URLSearchParams(window.location.search).get("gender")];
    if (file === "product-details.html") return ["product"];
    if (file === "cart.html") return ["cart"];
    if (file === "checkout.html") return ["checkout"];
    if (file === "login-register.html") return ["auth"];
    if (file === "account.html") return ["account"];
    if (file === "order-confirmation.html") return ["order"];
    if (["about.html", "contact.html", "faq.html", "payment-methods.html", "privacy.html", "return-policy.html",
      "shiping-info.html", "support.html", "tos.html", "blog.html", "blog-details.html", "starter-page.html", "404.html"].includes(file)) {
      return ["static", file];
    }
    return null;
  }

  async function mountStorefront() {
    localizeStaticText();
    updateHeader();
    setupSearch();
    const view = selectedView();
    if (!view) return;
    try {
      if (view[0] === "products") await loadProducts(view[1], view[2]);
      else if (view[0] === "product") await renderProductDetail();
      else if (view[0] === "cart") await renderCart();
      else if (view[0] === "checkout") await renderCheckout();
      else if (view[0] === "auth") await renderAuth();
      else if (view[0] === "account") await renderAccount();
      else if (view[0] === "order") await renderOrderConfirmation();
      else if (view[0] === "static") renderStaticPage(view[1]);
      updateHeader();
    } catch (error) {
      console.error("Không thể tải dữ liệu cửa hàng.", error);
      renderError(error);
    }
  }

  mountStorefront();
})();
