/* Native eStore integration: keep the team's template and populate it from Java APIs. */
(() => {
  'use strict';
  const $ = (s, root = document) => root.querySelector(s);
  const $$ = (s, root = document) => [...root.querySelectorAll(s)];
  const esc = (value) =>
    String(value ?? '').replace(
      /[&<>"']/g,
      (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' })[c],
    );
  const money = (value) => Number(value || 0).toLocaleString('vi-VN') + ' ₫';
  function paymentQrSource(value) {
    if (typeof value !== 'string' || !value.trim()) return null;
    try {
      const url = new URL(value, location.origin);
      return url.protocol === 'https:' || url.origin === location.origin ? url.href : null;
    } catch {
      return null;
    }
  }
  function showSePayPayment(form, order) {
    const panel = document.createElement('section');
    panel.className = 'client-sepay-payment border rounded-3 p-4 text-center';
    panel.setAttribute('aria-live', 'polite');
    const title = document.createElement('h2');
    title.className = 'h4';
    title.textContent = 'Thanh toán qua SePay';
    panel.append(title);

    const billCode = String(order.billCode || '');
    const amount = Number(order.totalAmount);
    const info = document.createElement('p');
    info.textContent =
      `Mã đơn: ${billCode || 'Đang cập nhật'} · Số tiền: ` +
      (Number.isSafeInteger(amount) && amount > 0 ? money(amount) : 'Đang cập nhật');
    panel.append(info);

    const status = document.createElement('p');
    status.className = 'alert alert-warning';
    status.setAttribute('role', 'status');
    status.textContent = 'Đơn hàng đang chờ xác nhận thanh toán.';
    panel.append(status);

    const qrSource = paymentQrSource(order.paymentQrUrl);
    if (qrSource && Number.isSafeInteger(amount) && amount > 0 && billCode) {
      const qr = document.createElement('img');
      qr.className = 'img-fluid mb-3';
      qr.style.maxWidth = '320px';
      qr.src = qrSource;
      qr.alt = 'Mã QR thanh toán SePay';
      panel.append(qr);

      const instructions = document.createElement('p');
      instructions.textContent = 'Giữ nguyên số tiền và nội dung chuyển khoản để hệ thống đối soát.';
      panel.append(instructions);
    } else {
      const warning = document.createElement('p');
      warning.className = 'alert alert-danger';
      warning.setAttribute('role', 'alert');
      warning.textContent =
        'Đơn đã được tạo nhưng backend chưa trả mã QR hợp lệ (paymentQrUrl), số tiền hoặc mã đơn.';
      panel.append(warning);
    }

    if (order.id != null) {
      const refresh = document.createElement('button');
      refresh.className = 'btn btn-outline-primary';
      refresh.type = 'button';
      refresh.textContent = 'Kiểm tra trạng thái thanh toán';
      refresh.onclick = (event) =>
        action(event.currentTarget, async () => {
          const result = await api('/api/customer/orders/' + encodeURIComponent(order.id));
          const paymentStatus = Number(result.order?.paymentStatus ?? result.paymentStatus);
          if (!Number.isInteger(paymentStatus))
            throw Error('Backend chưa trả trạng thái thanh toán của đơn hàng.');
          const paid = paymentStatus === 1;
          status.className = paid ? 'alert alert-success' : 'alert alert-warning';
          status.textContent = paid
            ? 'Thanh toán SePay đã được xác nhận.'
            : 'Đơn hàng đang chờ xác nhận thanh toán.';
          const qr = $('img', panel);
          if (paid && qr) qr.hidden = true;
        });
      panel.append(refresh);
    }

    const accountLink = document.createElement('a');
    accountLink.className = 'btn btn-link d-block mt-2';
    accountLink.href = 'account.html#orders';
    accountLink.textContent = 'Xem đơn hàng của tôi';
    panel.append(accountLink);
    form.replaceWith(panel);
  }
  const file = location.pathname.split('/').pop();
  const query = new URLSearchParams(location.search);
  let customer = null,
    cart = { items: [], totalQuantity: 0, subtotal: 0 },
    identityError = null;
  const imageUrl = (value) => {
    if (!value) return '';
    if (value.startsWith('assets/')) return '/store/' + value;
    return value.startsWith('/store/assets/') || value.startsWith('/uploads/') ? value : '';
  };
  function image(img, url, name) {
    if (!img) return;
    const src = imageUrl(url);
    img.alt = name || 'Sản phẩm';
    if (src) {
      img.src = src;
      img.hidden = false;
    } else {
      img.removeAttribute('src');
      img.hidden = true;
    }
    img.onerror = () => {
      img.hidden = true;
    };
  }
  async function api(url, method = 'GET', data) {
    const headers = { Accept: 'application/json' };
    if (method !== 'GET') {
      const tokenResponse = await fetch('/api/csrf', { credentials: 'same-origin' });
      if (!tokenResponse.ok) throw Error('Không lấy được phiên bảo mật. Hãy tải lại trang.');
      const token = await tokenResponse.json();
      headers[token.headerName] = token.token;
      headers['Content-Type'] = 'application/json';
    }
    let response;
    try {
      response = await fetch(url, {
        method,
        headers,
        credentials: 'same-origin',
        ...(data === undefined ? {} : { body: JSON.stringify(data) }),
      });
    } catch {
      throw Error('Không kết nối được máy chủ. Vui lòng kiểm tra backend Java.');
    }
    const body = await response.json().catch(() => null);
    if (!response.ok) {
      const error = Error(
        response.status >= 500
          ? 'Máy chủ chưa xử lý được yêu cầu. Vui lòng thử lại sau.'
          : body?.message || 'Yêu cầu không hợp lệ. Hãy tải lại trang.',
      );
      error.status = response.status;
      throw error;
    }
    return body;
  }
  function message(root, text, error = false) {
    if (!root) return;
    let box = root.querySelector(':scope > .client-message');
    if (!box) {
      box = document.createElement('div');
      box.className = 'client-message';
      root.prepend(box);
    }
    box.textContent = text;
    box.setAttribute('role', error ? 'alert' : 'status');
    return box;
  }
  async function action(button, fn) {
    if (button.disabled) return;
    button.disabled = true;
    try {
      await fn();
    } catch (e) {
      message($('main'), e.message, true);
    } finally {
      button.disabled = false;
    }
  }
  async function logout() {
    await api('/api/customer/logout', 'POST');
    localStorage.removeItem('clothing-cart');
    location.assign('/store/index.html');
  }
  function badges() {
    $$(
      '.header-actions a[href*="cart.html"] .badge, .header-actions a[href*="cart.html"] .cart-count',
    ).forEach((n) => (n.textContent = cart.totalQuantity || 0));
    $$('.header-actions a[href*="wishlist"] .badge').forEach((n) => (n.textContent = '0'));
  }
  function header() {
    $$('a[href="product-details.html"]').forEach((link) => (link.href = 'category.html'));
    const menu = $('.account-dropdown .dropdown-menu');
    const guestActions = $('.client-guest-actions');
    if (guestActions) guestActions.hidden = !!customer;
    const accountDropdown = $('.account-dropdown');
    if (accountDropdown) accountDropdown.hidden = !customer;
    if (menu) {
      menu.innerHTML = `<div class="dropdown-header"><h6>${customer ? 'Xin chào ' + esc(customer.FULL_NAME) : 'Chào mừng bạn'}</h6><p class="mb-0">${customer ? esc(customer.EMAIL) : identityError ? 'Chưa tải được trạng thái tài khoản' : 'Đăng nhập để quản lý tài khoản và giỏ hàng'}</p></div><div class="dropdown-body"><a class="dropdown-item" href="account.html#settings">Hồ sơ của tôi</a><a class="dropdown-item" href="account.html#orders">Đơn hàng của tôi</a><a class="dropdown-item" href="cart.html">Giỏ hàng của tôi</a></div><div class="dropdown-footer">${customer ? '<button type="button" class="btn btn-primary w-100" data-client-logout>Đăng xuất</button>' : '<a class="btn btn-primary w-100 mb-2" href="login-register.html">Đăng nhập</a><a class="btn btn-outline-primary w-100" href="login-register.html#register">Đăng ký</a>'}</div>`;
      $('[data-client-logout]', menu)?.addEventListener('click', (event) =>
        action(event.currentTarget, logout),
      );
    }
    $$('header form').forEach((form) => {
      const input = $('input[type="text"],input[type="search"]', form);
      if (!input) return;
      form.action = 'search-results.html';
      form.method = 'get';
      input.name = 'q';
      input.value = query.get('q') || '';
      input.placeholder = 'Tìm quần áo…';
    });
    badges();
  }
  async function refreshCart() {
    cart = await api('/api/customer/cart');
    badges();
    return cart;
  }
  const productLink = (id) => '/store/product-details.html?id=' + id;
  function fillCard(card, product) {
    card.dataset.productId = product.ID;
    card.removeAttribute('data-aos');
    card.classList.add('aos-animate');
    $$(
      '.product-rating,.original-price,.old-price,.product-badge,.product-tags,.discount-badge,.product-label,.product-rating-container,.product-color-options',
      card,
    ).forEach((n) => (n.hidden = true));
    const title = $('.product-title', card) || $('h3,h4,h5,h6', card);
    if (title)
      title.innerHTML = `<a href="${productLink(product.ID)}">${esc(product.PRODUCT_NAME)}</a>`;
    $$('.current-price,.price-new', card).forEach((n) => (n.textContent = money(product.PRICE)));
    const price = $('.product-price,.price', card);
    if (price && !$('.current-price,.price-new', price)) price.textContent = money(product.PRICE);
    $$('.product-category', card).forEach(
      (n) => (n.textContent = product.CATEGORY_NAME || 'Quần áo'),
    );
    $$('img', card).forEach((n) => image(n, product.THUMBNAIL_URL, product.PRODUCT_NAME));
    $$('a', card).forEach((n) => (n.href = productLink(product.ID)));
    $$('button', card).forEach((button) => {
      if (/wishlist|heart/.test(button.className) || $('.bi-heart', button)) {
        button.hidden = true;
        return;
      }
      button.onclick = () => location.assign(productLink(product.ID));
      button.title = 'Xem sản phẩm và chọn màu, kích cỡ';
    });
  }
  async function catalog() {
    const cataloguePages = ['category.html', 'search-results.html'];
    const isListing = cataloguePages.includes(file);
    // Clear template cards before requesting data, so errors never leave fake merchandise.
    const templates = $$('.product-card,.product-box').map((node) => ({
      node,
      clone: node.cloneNode(true),
    }));
    templates.forEach(({ node }) => (node.hidden = true));
    const qs = new URLSearchParams();
    for (const key of [
      'q',
      'categoryId',
      'brandId',
      'colorId',
      'minPrice',
      'maxPrice',
      'sort',
      'page',
    ])
      if (query.has(key)) qs.set(key, query.get(key));
    const data = await api('/api/catalog?' + (isListing ? qs : 'page=1'));
    let mainIndex = 0;
    templates.forEach(({ node }, i) => {
      const index = node.closest('main') ? mainIndex++ : i % Math.max(1, data.items.length);
      if (data.items[index]) {
        fillCard(node, data.items[index]);
        node.hidden = false;
      }
    });
    if (!data.items.length && templates.length)
      message($('main'), 'Chưa có sản phẩm phù hợp. Hãy thử điều kiện tìm kiếm khác.');
    if (!isListing) return;
    const first = templates.find(({ node }) => node.closest('main'));
    const grid = first?.node.parentElement;
    // Category cards use a Bootstrap column wrapper; search cards are direct grid children.
    if (first) {
      const wrapped = grid.className.includes('col-');
      const parent = wrapped ? grid.parentElement : grid;
      parent.classList.add('client-product-grid');
      const template = wrapped ? grid.cloneNode(true) : first.clone;
      const siblings = wrapped
        ? [...parent.children].filter((n) => $('.product-card,.product-box', n))
        : [...parent.children].filter(
            (n) => n.classList.contains('product-card') || n.classList.contains('product-box'),
          );
      siblings.forEach((n) => n.remove());
      for (const product of data.items) {
        const node = template.cloneNode(true),
          card = wrapped ? $('.product-card,.product-box', node) : node;
        card.hidden = false;
        fillCard(card, product);
        node.removeAttribute('data-aos');
        node.classList.add('aos-animate');
        if (wrapped) node.className = 'client-product-cell';
        parent.append(node);
      }
    }
    const [categories, filters] = await Promise.all([
      api('/api/catalog/categories'),
      api('/api/catalog/filters'),
    ]);
    const form = document.createElement('form');
    form.className = 'client-filters';
    form.method = 'get';
    const options = (rows, key, label) =>
      rows
        .map(
          (row) =>
            `<option value="${row.ID}" ${query.get(key) === String(row.ID) ? 'selected' : ''}>${esc(row[label])}</option>`,
        )
        .join('');
    form.innerHTML = `<label>Tìm sản phẩm<input name="q" maxlength="200" value="${esc(query.get('q') || '')}"></label><label>Danh mục<select name="categoryId"><option value="">Tất cả danh mục</option>${options(categories, 'categoryId', 'CATEGORY_NAME')}</select></label><label>Thương hiệu<select name="brandId"><option value="">Tất cả thương hiệu</option>${options(filters.brands, 'brandId', 'BRAND_NAME')}</select></label><label>Màu sắc<select name="colorId"><option value="">Tất cả màu</option>${options(filters.colors, 'colorId', 'COLOR_NAME')}</select></label><label>Giá từ (₫)<input name="minPrice" type="number" min="0" value="${esc(query.get('minPrice') || '')}"></label><label>Giá đến (₫)<input name="maxPrice" type="number" min="0" value="${esc(query.get('maxPrice') || '')}"></label><label>Sắp xếp<select name="sort"><option value="newest">Mới nhất</option><option value="price-asc">Giá tăng dần</option><option value="price-desc">Giá giảm dần</option><option value="name">Tên sản phẩm</option></select></label><button class="btn btn-primary">Tìm kiếm và lọc</button><a href="category.html">Xóa bộ lọc</a>`;
    $('[name="sort"]', form).value = query.get('sort') || 'newest';
    form.onsubmit = (event) => {
      event.preventDefault();
      const params = new URLSearchParams();
      for (const [key, value] of new FormData(form)) if (value !== '') params.set(key, value);
      location.assign('category.html?' + params);
    };
    const filtersHost =
      $('main .widgets-container') ||
      $('#category-header .container') ||
      $('main section .container');
    if (file !== 'search-results.html') {
      if (filtersHost?.classList.contains('widgets-container')) filtersHost.replaceChildren(form);
      else filtersHost?.prepend(form);
    }
    const oldFilters = $('#category-header .filter-container');
    if (oldFilters && !oldFilters.contains(form)) oldFilters.hidden = true;
    const searchHeader = $('.search-results-header');
    if (searchHeader) {
      $$('.sort-options,.active-filters', searchHeader).forEach((n) => (n.hidden = true));
      const title = $('h1,h2', searchHeader);
      if (title)
        title.textContent = `Tìm kiếm: ${query.get('q') || 'Tất cả sản phẩm'} · ${data.total} kết quả`;
      const description =
        $('.search-results-count', searchHeader) || $('h1 + p,h2 + p', searchHeader);
      if (description) description.textContent = `${data.total} sản phẩm phù hợp`;
      $('.search-filters', searchHeader)?.remove();
      const searchForm = $('form.search-form', searchHeader);
      if (searchForm) {
        searchForm.method = 'get';
        searchForm.action = 'search-results.html';
        const input = $('input', searchForm);
        input.name = 'q';
        input.value = query.get('q') || '';
      }
    }
    const pagination =
      $('#category-pagination .container') ||
      $('#category-pagination') ||
      document.createElement('div');
    pagination.classList.add('client-pagination');
    pagination.replaceChildren();
    const page = Number(query.get('page') || 1),
      pages = Math.max(1, Math.ceil(data.total / data.size));
    for (const [label, target] of [
      ['← Trang trước', page - 1],
      ['Trang sau →', page + 1],
    ])
      if (target >= 1 && target <= pages) {
        const a = document.createElement('a');
        const params = new URLSearchParams(query);
        params.set('page', target);
        a.href = 'category.html?' + params;
        a.textContent = label;
        pagination.append(a);
      }
    pagination.append(document.createTextNode(`Trang ${page}/${pages} · ${data.total} sản phẩm`));
    if (!pagination.isConnected) $('main').append(pagination);
  }
  async function detail() {
    const host = $('#product-details') || $('main');
    const info = $('.product-info', host);
    const id = Number(query.get('id'));
    if (!Number.isSafeInteger(id) || id < 1) {
      host.hidden = true;
      message($('main'), 'Hãy chọn sản phẩm từ danh mục để xem đúng thông tin.');
      const a = document.createElement('a');
      a.href = 'category.html';
      a.textContent = 'Xem danh mục sản phẩm';
      $('main').append(a);
      return;
    }
    // Hide all demo specifications/reviews; only API data is shown below.
    $$(
      '.product-rating,.original-price,.discount-badge,.additional-info,.wishlist-btn',
      host,
    ).forEach((n) => (n.hidden = true));
    const data = await api('/api/catalog/' + id),
      product = data.product,
      variants = data.variants;
    $('.product-title', info).textContent = product.PRODUCT_NAME;
    $('.product-category', info).textContent = 'Quần áo';
    $('.product-short-description', info).textContent =
      product.DESCRIPTION || 'Chọn màu và kích cỡ phù hợp trước khi thêm vào giỏ.';
    const desc = $('#description .product-description');
    if (desc) desc.textContent = product.DESCRIPTION || 'Cửa hàng chưa bổ sung mô tả chi tiết.';
    const specs = $('#specifications');
    if (specs) specs.textContent = 'Màu sắc và kích cỡ được hiển thị theo các biến thể đang bán.';
    const reviews = $('#reviews');
    if (reviews) reviews.textContent = 'Chưa có dữ liệu đánh giá của sản phẩm.';
    const reviewTab = $('#reviews-tab');
    if (reviewTab) reviewTab.textContent = 'Đánh giá';
    const urls = [
      ...new Set([product.THUMBNAIL_URL, ...data.images.map((n) => n.IMAGE_URL)].filter(Boolean)),
    ];
    const mainImage =
      $('.main-product-image img,.product-main-image img,.product-images img', host) ||
      $('img', host);
    image(mainImage, urls[0], product.PRODUCT_NAME);
    $$('.thumbnail-item', host).forEach((node, i) => {
      if (!urls[i]) node.hidden = true;
      else {
        image($('img', node), urls[i], product.PRODUCT_NAME);
        node.onclick = () => image(mainImage, urls[i], product.PRODUCT_NAME);
      }
    });
    const colors = $('.color-options', info),
      sizes = $('.size-options', info),
      qty = $('.quantity-input', info),
      add = $('.add-to-cart-btn', info),
      buy = $('.buy-now-btn', info);
    colors.replaceChildren();
    sizes.replaceChildren();
    qty.value = 1;
    let color = null,
      selected = null;
    const sync = () => {
      $('.current-price', info).textContent = selected ? money(selected.PRICE) : 'Chọn biến thể';
      $('.stock-count', info).textContent = selected ? `Còn ${selected.QUANTITY} sản phẩm` : '';
      $('.product-availability span', info).textContent =
        selected && selected.QUANTITY > 0 ? 'Còn hàng' : 'Chọn màu và kích cỡ còn hàng';
      qty.max = selected?.QUANTITY || 1;
      qty.value = Math.min(Math.max(1, Number(qty.value) || 1), Number(qty.max) || 1);
      add.disabled = buy.disabled = !selected || selected.QUANTITY < 1;
      $('.product-colors .selected-option', info).textContent =
        variants.find((v) => v.COLOR_ID === color)?.COLOR_NAME || '';
      $('.product-sizes .selected-option', info).textContent = selected?.SIZE_NAME || '';
    };
    const chooseColor = (value) => {
      color = value;
      const colorImage =
        data.images.find((photo) => photo.COLOR_ID === value) ||
        data.images.find((photo) => photo.COLOR_ID == null);
      image(mainImage, colorImage?.IMAGE_URL || product.THUMBNAIL_URL, product.PRODUCT_NAME);
      selected = null;
      sizes.replaceChildren();
      $$('.client-variant', colors).forEach((n) =>
        n.setAttribute('aria-pressed', String(Number(n.dataset.color) === color)),
      );
      variants
        .filter((v) => v.COLOR_ID === color)
        .forEach((variant) => {
          const b = document.createElement('button');
          b.type = 'button';
          b.className = 'client-variant';
          b.textContent = variant.SIZE_NAME;
          b.disabled = variant.QUANTITY < 1;
          b.setAttribute('aria-pressed', 'false');
          b.onclick = () => {
            selected = variant;
            $$('button', sizes).forEach((n) => n.setAttribute('aria-pressed', String(n === b)));
            sync();
          };
          sizes.append(b);
        });
      sync();
    };
    [...new Map(variants.map((v) => [v.COLOR_ID, v])).values()].forEach((v) => {
      const b = document.createElement('button');
      b.type = 'button';
      b.className = 'client-variant';
      b.textContent = v.COLOR_NAME;
      b.dataset.color = v.COLOR_ID;
      b.onclick = () => chooseColor(v.COLOR_ID);
      colors.append(b);
    });
    $('.decrease', info).onclick = () => {
      qty.value = Math.max(1, Number(qty.value) - 1);
    };
    $('.increase', info).onclick = () => {
      qty.value = Math.min(Number(qty.max), Number(qty.value) + 1);
    };
    const addItem = async (go) => {
      if (!customer) {
        location.assign('login-register.html');
        return;
      }
      const quantity = Number(qty.value);
      if (!selected || !Number.isInteger(quantity) || quantity < 1 || quantity > selected.QUANTITY)
        throw Error('Chọn màu, kích cỡ và số lượng còn hàng hợp lệ.');
      cart = await api('/api/customer/cart/items', 'POST', {
        productDetailId: selected.ID,
        quantity,
      });
      badges();
      message(info, 'Đã lưu sản phẩm vào giỏ hàng của bạn.');
      if (go) location.assign('cart.html');
    };
    add.onclick = () => action(add, () => addItem(false));
    buy.onclick = () => action(buy, () => addItem(true));
    sync();
  }
  function selectedCartIds() {
    const allowed = new Set(cart.items.map((line) => line.productDetailId));
    const key = 'store-cart-selection:' + (customer?.EMAIL || 'guest');
    try {
      const saved = sessionStorage.getItem(key);
      return new Set(
        (saved === null ? [...allowed] : JSON.parse(saved)).filter((id) => allowed.has(id)),
      );
    } catch {
      return new Set(allowed);
    }
  }
  function saveCartSelection(ids) {
    sessionStorage.setItem(
      'store-cart-selection:' + (customer?.EMAIL || 'guest'),
      JSON.stringify([...ids]),
    );
  }
  async function cartPage() {
    const host = $('#cart'),
      items = $('.cart-items', host),
      template = $('.cart-item', items).cloneNode(true);
    $$('.cart-item', items).forEach((n) => n.remove());
    const empty = document.createElement('p');
    empty.className = 'client-cart-empty';
    items.prepend(empty);
    const summary = $('.cart-summary', host);
    const selectAll = document.createElement('label');
    selectAll.className = 'client-select-all';
    selectAll.innerHTML = '<input type="checkbox" aria-label="Chọn tất cả sản phẩm"> Chọn tất cả';
    items.prepend(selectAll);
    const render = () => {
      $$('.cart-item', items).forEach((n) => n.remove());
      const chosen = selectedCartIds();
      const syncSelection = () => {
        saveCartSelection(chosen);
        const selectedLines = cart.items.filter((line) => chosen.has(line.productDetailId));
        const total = selectedLines.reduce(
          (sum, line) => sum + Number(line.price) * line.quantity,
          0,
        );
        $$('.summary-item', summary).forEach((node, index) => {
          if (index === 0) $('.summary-value', node).textContent = money(total);
          else node.hidden = true;
        });
        $('.summary-total .summary-value', summary).textContent = money(total);
        let count = $('.client-selected-count', summary);
        if (!count) {
          count = document.createElement('p');
          count.className = 'client-selected-count';
          summary.prepend(count);
        }
        count.textContent =
          'Đã chọn ' +
          selectedLines.length +
          ' mặt hàng (' +
          selectedLines.reduce((sum, line) => sum + line.quantity, 0) +
          ' sản phẩm)';
        const allowed =
          selectedLines.length > 0 &&
          selectedLines.every((line) => line.available && line.quantity <= line.stock);
        const checkout = $('.checkout-button a', summary);
        checkout.href = customer
          ? 'checkout.html?items=' + [...chosen].join(',')
          : 'login-register.html';
        checkout.textContent = customer ? 'Mua các sản phẩm đã chọn' : 'Đăng nhập để mua hàng';
        checkout.setAttribute('aria-disabled', String(!!customer && !allowed));
        checkout.classList.toggle('disabled', !!customer && !allowed);
        checkout.onclick = (e) => {
          if (customer && !allowed) {
            e.preventDefault();
            message(host, 'Chọn ít nhất một sản phẩm còn đủ hàng trước khi thanh toán.', true);
          }
        };
        const all = $('input', selectAll);
        all.checked = cart.items.length > 0 && chosen.size === cart.items.length;
        all.indeterminate = chosen.size > 0 && chosen.size < cart.items.length;
        all.disabled = !cart.items.length;
      };
      $('input', selectAll).onchange = (e) => {
        chosen.clear();
        if (e.target.checked) cart.items.forEach((line) => chosen.add(line.productDetailId));
        $$('.client-cart-select', items).forEach(
          (input) => (input.checked = chosen.has(Number(input.value))),
        );
        syncSelection();
      };
      empty.textContent = cart.items.length
        ? ''
        : customer
          ? 'Giỏ hàng của bạn đang trống.'
          : 'Đăng nhập để xem giỏ hàng riêng của bạn.';
      for (const line of cart.items) {
        const row = template.cloneNode(true);
        row.removeAttribute('data-aos');
        row.classList.add('aos-animate');
        row.dataset.variantId = line.productDetailId;
        const choose = document.createElement('input');
        choose.type = 'checkbox';
        choose.className = 'client-cart-select';
        choose.value = line.productDetailId;
        choose.checked = chosen.has(line.productDetailId);
        choose.setAttribute(
          'aria-label',
          'Chọn ' + line.productName + ' ' + line.colorName + ' ' + line.sizeName,
        );
        choose.onchange = () => {
          if (choose.checked) chosen.add(line.productDetailId);
          else chosen.delete(line.productDetailId);
          syncSelection();
        };
        $('.product-info', row).prepend(choose);
        $('.product-title', row).innerHTML =
          `<a href="${productLink(line.productId)}">${esc(line.productName)}</a>`;
        image($('img', row), line.imageUrl, line.productName);
        $('.product-color', row).textContent = 'Màu: ' + line.colorName;
        $('.product-size', row).textContent = 'Kích cỡ: ' + line.sizeName;
        $('.current-price', row).textContent = money(line.price);
        $('.original-price', row)?.remove();
        $('.item-total span', row).textContent = money(line.price * line.quantity);
        const input = $('.quantity-input', row);
        input.value = line.quantity;
        input.max = Math.max(1, line.stock);
        input.min = 1;
        const update = async (q) => {
          if (!Number.isInteger(q) || q < 1 || q > line.stock)
            throw Error('Số lượng vượt tồn kho hoặc không hợp lệ.');
          cart = await api('/api/customer/cart/items/' + line.productDetailId, 'PUT', {
            quantity: q,
          });
          badges();
          render();
        };
        $('.decrease', row).onclick = (e) =>
          action(e.currentTarget, () => update(line.quantity - 1));
        $('.increase', row).onclick = (e) =>
          action(e.currentTarget, () => update(line.quantity + 1));
        input.onchange = () =>
          update(Number(input.value)).catch((e) => {
            input.value = line.quantity;
            message(host, e.message, true);
          });
        $('.remove-item', row).onclick = (e) =>
          action(e.currentTarget, async () => {
            cart = await api('/api/customer/cart/items/' + line.productDetailId, 'DELETE');
            badges();
            render();
          });
        if (!line.available || line.quantity > line.stock)
          message(row, 'Sản phẩm hiện không đủ hàng. Hãy cập nhật giỏ trước khi thanh toán.', true);
        items.insertBefore(row, $('.cart-actions', items));
      }
      syncSelection();
    };
    $('.coupon-form', host).hidden = true;
    $('.cart-actions .btn-outline-accent', host).onclick = (e) =>
      action(e.currentTarget, async () => {
        await refreshCart();
        render();
      });
    $('.cart-actions .btn-outline-danger', host).onclick = (e) =>
      action(e.currentTarget, async () => {
        if (!customer) return;
        cart = await api('/api/customer/cart', 'DELETE');
        badges();
        render();
      });
    $('.continue-shopping a', summary).href = 'category.html';
    $('.payment-methods', summary).hidden = true;
    message(summary, 'Tạm tính tiền hàng. Phí giao và ưu đãi được tính lại ở bước thanh toán.');
    render();
  }
  async function accountPage() {
    const host = $('#account'),
      user = $('.user-info', host);
    $('.user-info h4', host).textContent = customer?.FULL_NAME || 'Tài khoản của bạn';
    $('img', user).hidden = true;
    $('.user-status', user).textContent = customer?.EMAIL || '';
    const avatar = $('.user-avatar', user);
    if (avatar) {
      const icon = document.createElement('i');
      icon.className = 'bi bi-person-circle';
      icon.style.fontSize = '3rem';
      icon.setAttribute('aria-hidden', 'true');
      avatar.prepend(icon);
    }
    $$('.menu-nav .badge', host).forEach((n) => (n.textContent = '0'));
    // These panels have no customer APIs; remove demo content from the rendered view.
    for (const id of ['wishlist', 'wallet', 'reviews']) {
      const panel = $('#' + id, host);
      if (panel) panel.textContent = 'Chưa có dữ liệu trong mục này.';
    }
    const orders = $('.orders-grid', host);
    orders.replaceChildren();
    $$('#orders .pagination,#orders .pagination-wrap,#orders .pagination-container', host).forEach(
      (n) => (n.hidden = true),
    );
    const addressPanel = $('#addresses', host);
    addressPanel.replaceChildren();
    $$('#settings input', host).forEach((n) => {
      if (n.type === 'checkbox') n.checked = false;
      else n.value = '';
    });
    $$('.preferences-list', host).forEach((n) => {
      n.closest('.settings-section').hidden = true;
    });
    $$('.logout-link', host).forEach((n) => {
      n.href = '#';
      n.onclick = (e) => {
        e.preventDefault();
        action(n, logout);
      };
    });
    $$('.help-link', host).forEach((n) => (n.href = 'support.html'));
    const forms = $$('form', host);
    forms.forEach((f) => {
      f.classList.remove('php-email-form');
      $$('.loading,.sent-message,.error-message', f).forEach((n) => (n.hidden = true));
    });
    if (!customer) {
      message(
        host,
        identityError?.message || 'Vui lòng đăng nhập để xem tài khoản và đơn hàng của bạn.',
        !!identityError,
      );
      forms.forEach((f) => $$('input,button', f).forEach((n) => (n.disabled = true)));
      return;
    }
    const profile = $('#firstName').closest('form'),
      password = $('#currentPassword').closest('form');
    const setProfile = () => {
      const words = customer.FULL_NAME.trim().split(/\s+/);
      $('#firstName').value = words.pop() || '';
      $('#lastName').value = words.join(' ');
      $('#email').value = customer.EMAIL;
      $('#phone').value = customer.PHONE || '';
    };
    setProfile();
    profile.onsubmit = (e) => {
      e.preventDefault();
      const button = $('[type="submit"]', profile);
      action(button, async () => {
        customer = await api('/api/customer/profile', 'PUT', {
          name: ($('#lastName').value.trim() + ' ' + $('#firstName').value.trim()).trim(),
          email: $('#email').value.trim(),
          phone: $('#phone').value.trim(),
          gender: customer.GENDER ?? null,
          birthDate: customer.BIRTH_DATE?.slice(0, 10) || null,
        });
        header();
        setProfile();
        $('.user-info h4', host).textContent = customer.FULL_NAME;
        $('.user-info .user-status', host).textContent = customer.EMAIL;
        message(profile, 'Đã lưu thông tin vào tài khoản của bạn.');
      });
    };
    password.onsubmit = (e) => {
      e.preventDefault();
      action($('[type="submit"]', password), async () => {
        if ($('#newPassword').value !== $('#confirmPassword').value)
          throw Error('Mật khẩu xác nhận không khớp.');
        await api('/api/customer/password', 'PUT', {
          currentPassword: $('#currentPassword').value,
          newPassword: $('#newPassword').value,
        });
        window.alert('Đã đổi mật khẩu. Vui lòng đăng nhập lại.');
        location.assign('login-register.html');
      });
    };
    const [orderData, addresses] = await Promise.all([
      api('/api/customer/orders'),
      api('/api/customer/addresses'),
    ]);
    const statuses = [
      'Chờ xác nhận',
      'Đã xác nhận',
      'Đang giao',
      'Hoàn thành',
      'Đã hủy',
      'Trả hàng',
      'Hoàn hàng',
    ];
    const drawOrders = () => {
      const keyword = $('#orders .search-box input', host)?.value.toLowerCase() || '';
      orders.replaceChildren();
      for (const order of orderData.filter((n) =>
        String(n.billCode).toLowerCase().includes(keyword),
      )) {
        const node = document.createElement('article');
        node.className = 'order-card';
        node.innerHTML = `<h3>${esc(order.billCode)}</h3><p>${esc(statuses[order.status] || 'Đang cập nhật')}</p><p>${money(order.totalAmount)}</p><button class="btn btn-outline-primary" type="button">Xem chi tiết</button>`;
        $('button', node).onclick = (e) =>
          action(e.currentTarget, async () => {
            const data = await api('/api/customer/orders/' + order.id);
            let lines = $('.client-order-lines', node);
            if (!lines) {
              lines = document.createElement('div');
              lines.className = 'client-order-lines';
              node.append(lines);
            }
            lines.innerHTML =
              '<p>' +
              esc(data.order.shippingAddress) +
              '</p>' +
              data.lines
                .map(
                  (l) =>
                    '<p>' +
                    esc(l.productName) +
                    ' · ' +
                    esc(l.colorName) +
                    ' / ' +
                    esc(l.sizeName) +
                    ' × ' +
                    l.quantity +
                    ' — ' +
                    money(l.unitPrice * l.quantity) +
                    '</p>',
                )
                .join('');
          });
        orders.append(node);
      }
      if (!orders.children.length) message(orders, 'Bạn chưa có đơn hàng phù hợp.');
    };
    drawOrders();
    $('#orders .search-box input', host).oninput = drawOrders;
    $('#orders .filter-btn', host).closest('.dropdown').hidden = true;
    $('.menu-nav a[href="#orders"] .badge', host).textContent = orderData.length;
    const drawAddresses = (rows) => {
      addressPanel.innerHTML =
        '<h2>Địa chỉ của tôi</h2><div class="client-address-list"></div><button class="btn btn-primary" type="button" data-add-address>Thêm địa chỉ</button>';
      const list = $('.client-address-list', addressPanel);
      if (!rows.length) message(list, 'Bạn chưa lưu địa chỉ nào.');
      for (const a of rows) {
        const card = document.createElement('div');
        card.className = 'client-address-card';
        card.innerHTML = `<h3>${esc(a.RECEIVER_NAME)} ${a.IS_DEFAULT === 1 ? '· Mặc định' : ''}</h3><p>${esc(a.RECEIVER_PHONE)}</p><p>${esc([a.DETAIL_ADDRESS, a.WARD, a.PROVINCE].join(', '))}</p><button class="btn btn-outline-primary" data-edit>Sửa</button><button class="btn btn-outline-primary" data-default>Mặc định</button><button class="btn btn-outline-danger" data-delete>Xóa</button>`;
        $('[data-edit]', card).onclick = () => editAddress(a);
        $('[data-default]', card).onclick = (e) =>
          action(e.currentTarget, async () =>
            drawAddresses(await api('/api/customer/addresses/' + a.ID + '/default', 'POST')),
          );
        $('[data-delete]', card).onclick = (e) =>
          action(e.currentTarget, async () => {
            if (confirm('Xóa địa chỉ này khỏi sổ địa chỉ?'))
              drawAddresses(await api('/api/customer/addresses/' + a.ID, 'DELETE'));
          });
        list.append(card);
      }
      $('[data-add-address]', addressPanel).onclick = () => editAddress(null);
    };
    const editAddress = (a) => {
      const form = document.createElement('form');
      form.className = 'client-address-card';
      const fields = [
        ['name', 'Người nhận', 'RECEIVER_NAME', 150],
        ['phone', 'Điện thoại', 'RECEIVER_PHONE', 20],
        ['province', 'Tỉnh/thành phố', 'PROVINCE', 100],
        ['ward', 'Phường/xã', 'WARD', 100],
        ['address', 'Địa chỉ chi tiết', 'DETAIL_ADDRESS', 300],
      ];
      form.innerHTML =
        fields
          .map(
            ([name, label, key, max]) =>
              `<label class="d-block mb-3">${label}<input class="form-control" name="${name}" required maxlength="${max}" value="${esc(a?.[key] || '')}"></label>`,
          )
          .join('') +
        '<label><input type="checkbox" name="isDefault"> Đặt làm mặc định</label><p><button class="btn btn-primary" type="submit">Lưu địa chỉ</button> <button class="btn btn-link" type="button">Hủy</button></p>';
      $('[name="isDefault"]', form).checked = a?.IS_DEFAULT === 1;
      $('[type="button"]', form).onclick = () => form.remove();
      form.onsubmit = (e) => {
        e.preventDefault();
        action($('[type="submit"]', form), async () => {
          const data = Object.fromEntries(new FormData(form));
          data.isDefault = $('[name="isDefault"]', form).checked;
          drawAddresses(
            await api('/api/customer/addresses' + (a ? '/' + a.ID : ''), a ? 'PUT' : 'POST', data),
          );
        });
      };
      addressPanel.append(form);
    };
    drawAddresses(addresses);
    if (location.hash) {
      const tab = $(`.menu-nav a[href="${location.hash}"]`, host);
      if (tab && window.bootstrap) bootstrap.Tab.getOrCreateInstance(tab).show();
    }
  }
  async function checkoutPage() {
    const form = $('main .checkout-form');
    if (!form) return;
    // Retain the template shell; replace unsupported sample payment controls inside the form.
    form.replaceChildren();
    const summary = $('main .order-summary-content');
    if (summary) summary.replaceChildren();
    const requestedIds = query.has('items')
      ? new Set(query.get('items').split(',').filter(Boolean).map(Number))
      : selectedCartIds();
    const checkoutItems = cart.items.filter((line) => requestedIds.has(line.productDetailId));
    const itemCount = $('main .order-summary-header .item-count');
    if (itemCount)
      itemCount.textContent =
        checkoutItems.reduce((sum, line) => sum + line.quantity, 0) + ' sản phẩm';
    const selectionValid =
      checkoutItems.length > 0 &&
      checkoutItems.length === requestedIds.size &&
      checkoutItems.every((line) => line.available && line.quantity <= line.stock);
    if (!customer || !selectionValid) {
      message(
        form,
        customer
          ? 'Giỏ hàng trống hoặc có sản phẩm chưa đủ hàng. Hãy kiểm tra giỏ.'
          : 'Vui lòng đăng nhập trước khi thanh toán.',
      );
      return;
    }
    const fields = [
      ['name', 'Họ tên người nhận', customer.FULL_NAME, 150],
      ['phone', 'Điện thoại', customer.PHONE || '', 20],
      ['province', 'Tỉnh/thành phố', '', 100],
      ['ward', 'Phường/xã', '', 100],
      ['address', 'Địa chỉ chi tiết', '', 300],
    ];
    form.innerHTML =
      fields
        .map(
          ([name, label, value, max]) =>
            `<label class="d-block mb-3">${label}<input class="form-control" name="${name}" value="${esc(value)}" required maxlength="${max}"></label>`,
        )
        .join('') +
      '<label class="d-block mb-3">Phương thức thanh toán<select class="form-select" name="paymentMethod"><option value="COD">Thanh toán khi nhận hàng (COD)</option><option value="SEPAY">Chuyển khoản QR qua SePay</option></select></label><label class="d-block mb-3">Mã ưu đãi<input name="voucherCode" class="form-control" maxlength="50"></label><label class="d-block mb-3">Ghi chú<textarea name="note" class="form-control" maxlength="500"></textarea></label><button type="button" class="btn btn-outline-primary" data-quote>Tính phí giao hàng</button><div class="client-shipping-options my-3"></div><button type="submit" class="btn btn-primary" disabled>Đặt hàng</button>';
    if (summary) {
      for (const line of checkoutItems) {
        const p = document.createElement('p');
        p.textContent = `${line.productName} · ${line.colorName} / ${line.sizeName} × ${line.quantity}: ${money(line.price * line.quantity)}`;
        summary.append(p);
      }
      message(
        summary,
        'Tiền hàng: ' +
          money(checkoutItems.reduce((sum, line) => sum + Number(line.price) * line.quantity, 0)) +
          '. Chọn dịch vụ giao hàng để xem tổng thanh toán.',
      );
    }
    let selected = null,
      quoted = null;
    const button = $('[type="submit"]', form),
      shipping = $('.client-shipping-options', form);
    const payload = () => ({
      ...Object.fromEntries(new FormData(form)),
      paymentMethod: $('[name="paymentMethod"]', form).value,
      items: checkoutItems.map((n) => ({
        productDetailId: n.productDetailId,
        quantity: n.quantity,
      })),
    });
    let quoteVersion = 0;
    form.oninput = () => {
      quoteVersion++;
      selected = null;
      quoted = null;
      button.disabled = true;
      shipping.replaceChildren();
    };
    $('[data-quote]', form).onclick = (e) =>
      action(e.currentTarget, async () => {
        if (!form.reportValidity()) return;
        selected = null;
        button.disabled = true;
        shipping.replaceChildren();
        const version = ++quoteVersion;
        const p = payload();
        const result = await api('/api/catalog/quote', 'POST', { ...p, code: p.voucherCode });
        if (version !== quoteVersion) return;
        quoted = result;
        for (const service of quoted.services || []) {
          const b = document.createElement('button');
          b.type = 'button';
          b.className = 'btn btn-outline-primary m-1';
          b.textContent = service.serviceName + ' · ' + money(service.fee);
          b.onclick = () => {
            selected = service;
            button.disabled = false;
            message(
              shipping,
              'Tổng thanh toán: ' +
                money(Number(quoted.subtotal) - Number(quoted.discount) + Number(service.fee)),
            );
          };
          shipping.append(b);
        }
        if (!quoted.services?.length)
          message(shipping, 'Chưa có dịch vụ vận chuyển cho địa chỉ này.');
      });
    form.onsubmit = (e) => {
      e.preventDefault();
      action(button, async () => {
        if (!selected || !quoted)
          throw Error('Tính phí và chọn dịch vụ vận chuyển trước khi đặt hàng.');
        const orderPayload = {
          ...payload(),
          shippingService: selected.serviceCode,
          expectedShippingFee: selected.fee,
        };
        const order = await api('/api/customer/cart/checkout', 'POST', orderPayload);
        saveCartSelection(new Set());
        if (orderPayload.paymentMethod === 'SEPAY') showSePayPayment(form, order);
        else {
          window.alert('Đặt hàng COD thành công! Mã đơn: ' + order.billCode);
          location.assign('account.html#orders');
        }
      });
    };
  }
  async function init() {
    localStorage.removeItem('clothing-cart'); // Never transfer the old browser-wide cart to another customer.
    try {
      customer = await api('/api/customer/me');
    } catch (e) {
      if (![400, 401, 403].includes(e.status)) identityError = e;
    }
    if (customer) {
      try {
        await refreshCart();
      } catch (e) {
        identityError = e;
        message($('main'), e.message, true);
      }
    }
    header();
    if (file === 'login-register.html' && location.hash === '#register' && window.bootstrap)
      bootstrap.Tab.getOrCreateInstance(
        $('[data-bs-target="#login-register-registration-form"]'),
      ).show();
    const jobs = [catalog()];
    if (file === 'account.html') jobs.push(accountPage());
    if (file === 'cart.html') jobs.push(cartPage());
    if (file === 'product-details.html')
      jobs.push(
        detail().catch((e) => {
          const section = $('#product-details');
          if (section) section.hidden = true;
          throw e;
        }),
      );
    if (file === 'checkout.html') jobs.push(checkoutPage());
    if (file === 'order-confirmation.html') {
      const confirmation = $('#order-confirmation');
      if (confirmation) confirmation.hidden = true;
      message($('main'), 'Xem xác nhận và trạng thái đơn hàng trong tài khoản của bạn.');
      const a = document.createElement('a');
      a.href = 'account.html#orders';
      a.textContent = 'Xem đơn hàng của tôi';
      $('main').append(a);
    }
    for (const result of await Promise.allSettled(jobs))
      if (result.status === 'rejected') message($('main'), result.reason.message, true);
    document.body.classList.add('client-ready');
    window.AOS?.refresh();
  }
  window.ClientStore = { api };
  init().catch((e) => {
    message($('main'), e.message, true);
    document.body.classList.add('client-ready');
  });
})();
