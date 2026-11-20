/* Native eStore integration: keep the team's template and populate it from Java APIs. */
(() => {
  'use strict';
  const $ = (s, root = document) => root.querySelector(s);
  const $$ = (s, root = document) => [...root.querySelectorAll(s)];
  const orderStatuses = [
    'Chờ xác nhận',
    'Đã xác nhận',
    'Đang giao',
    'Hoàn thành',
    'Đã hủy',
    'Trả hàng',
    'Giao thất bại',
  ];
  const esc = (value) =>
    String(value ?? '').replace(
      /[&<>"']/g,
      (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' })[c],
    );
  const money = (value) => Number(value || 0).toLocaleString('vi-VN') + ' ₫';
  const paymentStatusLabel = (status) =>
    ({ 0: 'Chưa thanh toán', 1: 'Đã thanh toán', 2: 'Đã hoàn tiền' })[status] ||
    'Đang cập nhật';
  function billStatusLabel(bill, statuses) {
    if (bill.paymentMethod === 'SEPAY' && bill.paymentStatus === 1 && bill.status === 0)
      return 'Đã thanh toán - Chờ xác nhận';
    return statuses[bill.status] || 'Đang cập nhật';
  }
  function cancellationNotice(bill) {
    if (Number(bill.status) !== 4) return '';
    if (Number(bill.paymentStatus) === 1)
      return 'Đơn đã hủy. Do đơn đã thanh toán, cửa hàng cần xử lý hoàn tiền thủ công.';
    if (bill.paymentMethod === 'SEPAY')
      return 'Đơn đã hủy. Nếu bạn đã chuyển khoản trước khi trạng thái thanh toán được cập nhật, vui lòng liên hệ cửa hàng để đối soát.';
    return 'Đơn hàng đã được hủy.';
  }
  const sePayQrBank = 'Vietcombank';
  const sePayQrAccount = 'SBSEPAYGL9TEMZRXDAK';
  function sePayQrUrl(amount, description) {
    const value = Number(amount);
    if (!Number.isSafeInteger(value) || value <= 0)
      throw Error('Số tiền thanh toán SePay không hợp lệ.');
    const url = new URL('https://vietqr.app/img');
    url.searchParams.set('bank', sePayQrBank);
    url.searchParams.set('acc', sePayQrAccount);
    url.searchParams.set('template', 'compact');
    url.searchParams.set('amount', String(value));
    url.searchParams.set('des', String(description));
    return url.toString();
  }
  const isLoopbackHost =
    location.hostname === 'localhost' || location.hostname === '127.0.0.1';
  const useSameOriginApi = !isLoopbackHost || location.port === '3000';
  const apiBase = (
    window.CLOTHING_SHOP_API_BASE ||
    (useSameOriginApi ? '/api' : 'http://localhost:8080/api')
  ).replace(/\/+$/, '');
  const file = location.pathname.split('/').pop();
  const query = new URLSearchParams(location.search);
  const tokenKey = 'clothingShopToken';
  const userKey = 'clothingShopUser';
  const pendingVoucherKey = 'clothingShopPendingVoucher';
  let customer = null,
    cart = { items: [], totalQuantity: 0, subtotal: 0 },
    identityError = null;
  const imageUrl = (value) => {
    if (!value) return '';
    if (value.startsWith('assets/')) return value;
    if (value.startsWith('/store/assets/')) return value.slice('/store/'.length);
    if (value.startsWith('/uploads/'))
      return new URL(value, new URL(apiBase, location.origin)).href;
    if (/^https:\/\/res\.cloudinary\.com\/[^/]+\/image\/upload\//.test(value)) return value;
    return '';
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
  async function api(url, method = 'GET', data, options = {}) {
    const headers = { Accept: 'application/json' };
    const token = sessionStorage.getItem(tokenKey);
    if (token) headers.Authorization = 'Bearer ' + token;
    if (data !== undefined) headers['Content-Type'] = 'application/json';
    let response;
    try {
      const requestUrl = url.startsWith('/api/') ? apiBase + url.slice(4) : url;
      response = await fetch(requestUrl, {
        method,
        headers,
        credentials: 'same-origin',
        ...(options.signal ? {signal:options.signal} : {}),
        ...(data === undefined ? {} : { body: JSON.stringify(data) }),
      });
    } catch (error) {
      if (error.name === 'AbortError') throw error;
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
    if (!body || body.success !== true)
      throw Error(body?.message || 'Máy chủ trả về dữ liệu không hợp lệ.');
    return body.data;
  }
  const addressCatalogBase = 'https://provinces.open-api.vn/api/v2';
  let provinceCatalogPromise = null;
  const wardCatalogPromises = new Map();
  async function addressCatalogRequest(url, failureMessage) {
    let response;
    try {
      response = await fetch(url, { headers: { Accept: 'application/json' } });
    } catch {
      throw Error(failureMessage);
    }
    if (!response.ok) throw Error(failureMessage);
    let data;
    try {
      data = await response.json();
    } catch {
      throw Error(failureMessage);
    }
    return data;
  }
  function getAddressProvinces() {
    if (!provinceCatalogPromise) {
      provinceCatalogPromise = addressCatalogRequest(
        addressCatalogBase + '/',
        'Không tải được danh sách tỉnh/thành phố. Hãy thử lại.',
      )
        .then((provinces) => {
          if (!Array.isArray(provinces) || provinces.some((item) => !item?.name || !item?.code))
            throw Error('Danh sách tỉnh/thành phố không hợp lệ.');
          return provinces;
        })
        .catch((error) => {
          provinceCatalogPromise = null;
          throw error;
        });
    }
    return provinceCatalogPromise;
  }
  function getAddressWards(provinceCode) {
    if (!wardCatalogPromises.has(provinceCode)) {
      wardCatalogPromises.set(
        provinceCode,
        addressCatalogRequest(
          addressCatalogBase + '/p/' + encodeURIComponent(provinceCode) + '?depth=2',
          'Không tải được danh sách phường/xã. Hãy chọn lại tỉnh/thành phố.',
        )
          .then((province) => {
            if (!Array.isArray(province?.wards))
              throw Error('Danh sách phường/xã không hợp lệ.');
            return province.wards;
          })
          .catch((error) => {
            wardCatalogPromises.delete(provinceCode);
            throw error;
          }),
      );
    }
    return wardCatalogPromises.get(provinceCode);
  }
  function addressOption(select, label, value, code) {
    const option = document.createElement('option');
    option.textContent = label;
    option.value = value;
    if (code) option.dataset.code = code;
    select.append(option);
    return option;
  }
  function normalizeProvinceName(value) {
    return String(value || '')
      .trim()
      .replace(/^(tỉnh|thành phố|tp\.?)\s+/i, '')
      .toLocaleLowerCase('vi');
  }
  async function populateAddressSelects(container, currentProvince = '', currentWard = '') {
    const provinceSelect = $('[name="province"]', container);
    const wardSelect = $('[name="ward"]', container);
    if (!provinceSelect || !wardSelect) throw Error('Không tìm thấy trường chọn địa chỉ.');

    provinceSelect.replaceChildren();
    addressOption(provinceSelect, 'Đang tải tỉnh/thành phố…', '');
    wardSelect.replaceChildren();
    addressOption(wardSelect, 'Chọn tỉnh/thành phố trước', '');
    provinceSelect.required = wardSelect.required = true;
    provinceSelect.disabled = wardSelect.disabled = true;
    const provinces = await getAddressProvinces();
    provinceSelect.replaceChildren();
    addressOption(provinceSelect, 'Chọn tỉnh/thành phố', '');
    provinces.forEach((province) =>
      addressOption(provinceSelect, province.name, province.name, province.code),
    );
    let selectedProvince = provinces.find((province) => province.name === currentProvince);
    if (!selectedProvince && currentProvince) {
      selectedProvince = provinces.find(
        (province) =>
          normalizeProvinceName(province.name) === normalizeProvinceName(currentProvince),
      );
    }
    if (selectedProvince) {
      provinceSelect.value = selectedProvince.name;
    } else if (currentProvince) {
      addressOption(provinceSelect, currentProvince, currentProvince);
      provinceSelect.value = currentProvince;
    }
    provinceSelect.disabled = false;

    let wardRequest = 0;
    const loadWards = async (selectedWard = '') => {
      const request = ++wardRequest;
      const selectedOption = $('option:checked', provinceSelect);
      const provinceCode = selectedOption?.dataset.code;
      wardSelect.replaceChildren();
      addressOption(
        wardSelect,
        provinceCode ? 'Đang tải phường/xã…' : 'Chọn tỉnh/thành phố trước',
        '',
      );
      wardSelect.disabled = true;
      if (!provinceSelect.value) return;
      if (!provinceCode) {
        if (selectedWard) {
          addressOption(wardSelect, selectedWard, selectedWard);
          wardSelect.value = selectedWard;
        }
        wardSelect.disabled = false;
        return;
      }
      try {
        const wards = await getAddressWards(provinceCode);
        if (request !== wardRequest) return;
        wardSelect.replaceChildren();
        addressOption(wardSelect, 'Chọn phường/xã', '');
        wards.forEach((ward) => addressOption(wardSelect, ward.name, ward.name, ward.code));
        if (selectedWard && !wards.some((ward) => ward.name === selectedWard)) {
          addressOption(wardSelect, selectedWard, selectedWard);
        }
        wardSelect.value = selectedWard || '';
        wardSelect.disabled = false;
      } catch (error) {
        if (request !== wardRequest) return;
        wardSelect.replaceChildren();
        addressOption(wardSelect, 'Chọn phường/xã', '');
        wardSelect.disabled = false;
        throw error;
      }
    };
    provinceSelect.onchange = () => {
      loadWards().catch((error) => message(container, error.message, true));
    };
    if (provinceSelect.value) await loadWards(currentWard);
    else wardSelect.disabled = true;
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
    sessionStorage.removeItem(tokenKey);
    sessionStorage.removeItem(userKey);
    location.assign('index.html');
  }
  function badges() {
    $$(
      '.header-actions a[href*="cart.html"] .badge, .header-actions a[href*="cart.html"] .cart-count',
    ).forEach((n) => (n.textContent = cart.totalQuantity || 0));
    $$('.header-actions a[href*="wishlist"] .badge').forEach((n) => (n.textContent = '0'));
  }
  function header() {
    $$('a[href="product-details.html"]').forEach((link) => (link.href = 'products.html'));
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
      input.placeholder = 'Tìm tên sản phẩm, thương hiệu…';
    });
    badges();
  }
  async function refreshCart() {
    const data = await api(cartPath());
    cart = {
      items: (data.items || []).map((item) => ({
        ...item,
        productDetailId: item.productDetailId,
        id: item.id,
        price: item.unitPrice,
        available: item.available === 1,
      })),
      totalQuantity: data.totalItems,
      subtotal: data.totalAmount,
    };
    badges();
    return cart;
  }
  const cartPath = () => '/api/carts/customer/' + encodeURIComponent(customer.customerId);
  const productLink = (id) => 'product-details.html?id=' + encodeURIComponent(id);
  function fillCard(card, product) {
    card.dataset.productId = product.id;
    card.removeAttribute('data-aos');
    card.classList.add('aos-animate');
    $$(
      '.product-rating,.original-price,.old-price,.product-badge,.product-tags,.discount-badge,.product-label,.product-rating-container,.product-color-options',
      card,
    ).forEach((n) => (n.hidden = true));
    const title = $('.product-title', card) || $('h3,h4,h5,h6', card);
    if (title)
      title.innerHTML = `<a href="${productLink(product.id)}">${esc(product.productName)}</a>`;
    $$('.current-price,.price-new', card).forEach((n) => (n.textContent = money(product.minPrice)));
    const price = $('.product-price,.price', card);
    if (price && !$('.current-price,.price-new', price)) price.textContent = money(product.minPrice);
    $$('.product-category', card).forEach(
      (n) => (n.textContent = product.brandName || 'Quần áo'),
    );
    $$('img', card).forEach((n) => image(n, product.thumbnailUrl, product.productName));
    $$('a', card).forEach((n) => (n.href = productLink(product.id)));
    $$('button', card).forEach((button) => {
      if (/wishlist|heart/.test(button.className) || $('.bi-heart', button)) {
        button.hidden = true;
        return;
      }
      button.onclick = () => location.assign(productLink(product.id));
      button.title = 'Xem sản phẩm và chọn màu, kích cỡ';
    });
  }
  async function catalog() {
    if (file === 'category.html' && !query.has('categoryId')) {
      $$('.product-card,.product-box').forEach((node) => (node.hidden = true));
      const main = $('main');
      [...main.children].filter((node) => !node.classList.contains('page-title')).forEach((node) => (node.hidden = true));
      $('.page-title h1').textContent = 'Danh mục sản phẩm';
      $('.breadcrumbs .current').textContent = 'Danh mục';
      const section = document.createElement('section');
      section.className = 'container client-category-directory';
      section.innerHTML = '<h2>Chọn danh mục bạn muốn khám phá</h2><p>Các danh mục đang được cửa hàng kinh doanh.</p>';
      main.append(section);
      const categories = await api('/api/products/categories');
      const list = document.createElement('ul');
      list.className = 'client-category-links';
      for (const category of categories) {
        const row = document.createElement('li');
        const link = document.createElement('a');
        link.href = 'category.html?categoryId=' + category.id;
        const parent = categories.find((c) => c.id === category.parentId);
        link.textContent = (parent ? parent.categoryName + ' / ' : '') + category.categoryName;
        row.append(link); list.append(row);
      }
      section.append(list);
      if (!categories.length) message(section, 'Cửa hàng chưa có danh mục đang kinh doanh.');
      const all = document.createElement('a'); all.href = 'products.html';
      all.className = 'btn btn-primary'; all.textContent = 'Xem tất cả sản phẩm'; section.append(all);
      return;
    }
    const cataloguePages = ['products.html', 'category.html', 'search-results.html'];
    const isListing = cataloguePages.includes(file);
    // Clear template cards before requesting data, so errors never leave fake merchandise.
    const templates = $$('.product-card,.product-box').map((node) => ({
      node,
      clone: node.cloneNode(true),
    }));
    templates.forEach(({ node }) => (node.hidden = true));
    const qs = new URLSearchParams({ page: query.get('page') || '1', size: isListing ? '15' : '24' });
    const keyword = query.get('q') || query.get('keyword');
    if (keyword) qs.set('keyword', keyword);
    for (const key of ['brandId', 'gender', 'categoryId', 'sort'])
      if (query.has(key)) qs.set(key, query.get(key));
    const data = await api('/api/products?' + qs);
    let mainIndex = 0;
    templates.forEach(({ node }, i) => {
      const index = node.closest('main') ? mainIndex++ : i % Math.max(1, data.items.length);
      if (data.items[index]) {
        fillCard(node, data.items[index]);
        node.hidden = false;
      }
    });
    if (!data.items.length && templates.length && file !== 'search-results.html' && (isListing || file==='index.html'))
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
      if (!data.items.length && file === 'search-results.html') {
        const empty = document.createElement('div');
        empty.className = 'client-search-empty';
        empty.innerHTML = '<i class="bi bi-search" aria-hidden="true"></i><h3>Chưa tìm thấy sản phẩm phù hợp</h3><p>Thử tên sản phẩm ngắn hơn, tìm không dấu hoặc bỏ bớt bộ lọc để khám phá thêm sản phẩm.</p><a class="btn btn-primary" href="products.html">Khám phá tất cả sản phẩm</a>';
        parent.append(empty);
      }
    }
    const [brandsPage, categories] = await Promise.all([api('/api/brands?page=1&size=100'), api('/api/products/categories')]);
    const brands = brandsPage.items || [];
    const form = document.createElement('form');
    form.className = 'client-filters';
    form.method = 'get';
    const options = (rows, key, label) =>
      rows
        .map(
          (row) =>
            `<option value="${row.id}" ${query.get(key) === String(row.id) ? 'selected' : ''}>${esc(row[label])}</option>`,
        )
        .join('');
    form.innerHTML = `<label>Tìm sản phẩm<input name="q" maxlength="120" value="${esc(keyword || '')}"></label><label>Thương hiệu<select name="brandId"><option value="">Tất cả thương hiệu</option>${options(brands, 'brandId', 'brandName')}</select></label><label>Đối tượng<select name="gender"><option value="">Tất cả</option><option value="0">Nam</option><option value="1">Nữ</option><option value="2">Khác</option></select></label><button class="btn btn-primary">Tìm kiếm và lọc</button><a href="category.html">Xóa bộ lọc</a>`;
    $('[name="gender"]', form).value = query.get('gender') || '';
    const categoryLabel = document.createElement('label');
    categoryLabel.innerHTML = `Danh mục<select name="categoryId"><option value="">Tất cả danh mục</option>${options(categories, 'categoryId', 'categoryName')}</select>`;
    const sortLabel = document.createElement('label');
    sortLabel.innerHTML = 'Sắp xếp<select name="sort"><option value="newest">Mới nhất</option><option value="price_asc">Giá tăng dần</option><option value="price_desc">Giá giảm dần</option></select>';
    form.insertBefore(categoryLabel, form.querySelector('button'));
    form.insertBefore(sortLabel, form.querySelector('button'));
    $('[name="sort"]', form).value = query.get('sort') || 'newest';
    const listingPath = file === 'search-results.html' ? file : 'products.html';
    $('a', form).href = listingPath;
    if (file === 'search-results.html') {
      const keywordInput = $('[name="q"]', form);
      const hiddenKeyword = document.createElement('input');
      hiddenKeyword.type = 'hidden'; hiddenKeyword.name = 'q'; hiddenKeyword.value = keyword || '';
      keywordInput.closest('label').replaceWith(hiddenKeyword);
      $('button', form).textContent = 'Áp dụng bộ lọc';
      $('a', form).href = listingPath + '?' + new URLSearchParams(keyword ? {q: keyword} : {});
      form.setAttribute('aria-label', 'Lọc và sắp xếp kết quả tìm kiếm');
    }
    if (file === 'category.html') {
      const selectedCategory = categories.find((c) => String(c.id) === query.get('categoryId'));
      $('.page-title h1').textContent = selectedCategory?.categoryName || 'Danh mục không tồn tại';
      $('.breadcrumbs .current').textContent = selectedCategory?.categoryName || 'Danh mục';
    }
    form.onsubmit = (event) => {
      event.preventDefault();
      const params = new URLSearchParams();
      for (const [key, value] of new FormData(form)) if (value !== '') params.set(key, value);
      location.assign(listingPath + '?' + params);
    };
    const filtersHost =
      $('main .widgets-container') ||
      $('#category-header .container') ||
      $('main section .container');
    if (file !== 'search-results.html') {
      if (filtersHost?.classList.contains('widgets-container')) filtersHost.replaceChildren(form);
      else filtersHost?.prepend(form);
    } else $('#search-results-header .search-results-header')?.append(form);
    const oldFilters = $('#category-header .filter-container');
    if (oldFilters && !oldFilters.contains(form)) {
      oldFilters.hidden = true;
      const oldHeader = $('#category-header');
      if (oldHeader && !oldHeader.contains(form)) oldHeader.hidden = true;
    }
    const searchHeader = $('#search-results-header .search-results-header');
    if (searchHeader) {
      $$('.sort-options,.active-filters', searchHeader).forEach((n) => (n.hidden = true));
      const title = $('h1,h2', searchHeader);
      if (title)
        title.textContent = keyword ? `Kết quả cho “${keyword}”` : 'Khám phá sản phẩm';
      const description =
        $('.search-results-count', searchHeader) || $('h1 + p,h2 + p', searchHeader);
      if (description) description.textContent = `${data.total} sản phẩm phù hợp${keyword ? ' với từ khóa của bạn' : ''}`;
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
    pagination.hidden = !data.total;
    const page = Number(data.page || 1),
      pages = Math.max(1, data.totalPages || 1);
    for (const [label, target] of [
      ['← Trang trước', page - 1],
      ...Array.from({length: Math.min(5, pages)}, (_, i) => {
        const target = Math.min(Math.max(1, page - 2), Math.max(1, pages - 4)) + i;
        return [String(target), target];
      }),
      ['Trang sau →', page + 1],
    ])
      if (target >= 1 && target <= pages) {
        const a = document.createElement('a');
        const params = new URLSearchParams(query);
        params.set('page', target);
        a.href = file + '?' + params;
        a.textContent = label;
        if (target === page) a.setAttribute('aria-current', 'page');
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
      a.href = 'products.html';
      a.textContent = 'Xem danh mục sản phẩm';
      $('main').append(a);
      return;
    }
    // Hide all demo specifications/reviews; only API data is shown below.
    $$(
      '.product-rating,.original-price,.discount-badge,.additional-info,.wishlist-btn',
      host,
    ).forEach((n) => (n.hidden = true));
    const [product, variants, gallery] = await Promise.all([
      api('/api/products/' + id),
      api('/api/products/' + id + '/details'),
      api('/api/products/' + id + '/images'),
    ]);
    $('.product-title', info).textContent = product.productName;
    $('.product-category', info).textContent = 'Quần áo';
    $('.product-short-description', info).textContent =
      product.materialName || 'Chọn màu và kích cỡ phù hợp trước khi thêm vào giỏ.';
    const desc = $('#description .product-description');
    if (desc) desc.textContent = product.materialName || 'Cửa hàng chưa bổ sung mô tả chi tiết.';
    const specs = $('#specifications');
    if (specs) specs.textContent = 'Màu sắc và kích cỡ được hiển thị theo các biến thể đang bán.';
    const reviews = $('#reviews');
    if (reviews) reviews.textContent = 'Chưa có dữ liệu đánh giá của sản phẩm.';
    const reviewTab = $('#reviews-tab');
    if (reviewTab) reviewTab.textContent = 'Đánh giá';
    const urls = [
      ...new Set([
        product.thumbnailUrl,
        ...gallery.map((image) => image.imageUrl),
        ...variants.map((variant) => variant.imageUrl),
      ].filter(Boolean)),
    ];
    const mainImage =
      $('.main-product-image img,.product-main-image img,.product-images img', host) ||
      $('img', host);
    image(mainImage, urls[0], product.productName);
    const thumbnailWrapper = $('.product-thumbnails-slider .swiper-wrapper', host);
    thumbnailWrapper.replaceChildren();
    urls.forEach((url, index) => {
      const thumbnail = document.createElement('div');
      thumbnail.className = 'swiper-slide thumbnail-item' + (index === 0 ? ' active' : '');
      thumbnail.dataset.image = url;
      const thumbnailImage = document.createElement('img');
      thumbnailImage.className = 'img-fluid';
      image(thumbnailImage, url, product.productName);
      thumbnail.append(thumbnailImage);
      thumbnail.onclick = () => image(mainImage, url, product.productName);
      thumbnailWrapper.append(thumbnail);
    });
    $('.product-thumbnails-slider', host)?.swiper?.update();
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
      $('.current-price', info).textContent = selected ? money(selected.price) : 'Chọn biến thể';
      $('.stock-count', info).textContent = selected ? `Còn ${selected.quantity} sản phẩm` : '';
      $('.product-availability span', info).textContent =
        selected && selected.quantity > 0 ? 'Còn hàng' : 'Chọn màu và kích cỡ còn hàng';
      qty.max = selected?.quantity || 1;
      qty.value = Math.min(Math.max(1, Number(qty.value) || 1), Number(qty.max) || 1);
      add.disabled = buy.disabled = !selected || selected.quantity < 1;
      $('.product-colors .selected-option', info).textContent =
        color || '';
      $('.product-sizes .selected-option', info).textContent = selected?.sizeName || '';
    };
    const chooseColor = (value) => {
      color = value;
      const colorImage = variants.find((variant) => variant.colorName === value && variant.imageUrl);
      image(mainImage, colorImage?.imageUrl || product.thumbnailUrl, product.productName);
      selected = null;
      sizes.replaceChildren();
      $$('.client-variant', colors).forEach((n) =>
        n.setAttribute('aria-pressed', String(n.dataset.color === color)),
      );
      variants
        .filter((v) => v.colorName === color)
        .forEach((variant) => {
          const b = document.createElement('button');
          b.type = 'button';
          b.className = 'client-variant';
          b.textContent = variant.sizeName;
          b.disabled = variant.quantity < 1;
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
    [...new Map(variants.map((v) => [v.colorName, v])).values()].forEach((v) => {
      const b = document.createElement('button');
      b.type = 'button';
      b.className = 'client-variant';
      b.textContent = v.colorName;
      b.dataset.color = v.colorName;
      b.onclick = () => chooseColor(v.colorName);
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
        const next = 'product-details.html?id=' + id;
        location.assign('login-register.html?next=' + encodeURIComponent(next));
        return;
      }
      const quantity = Number(qty.value);
      if (!selected || !Number.isInteger(quantity) || quantity < 1 || quantity > selected.quantity)
        throw Error('Chọn màu, kích cỡ và số lượng còn hàng hợp lệ.');
      if (quantity > 100) throw Error('Mỗi biến thể được mua tối đa 100 sản phẩm trong một đơn.');
      if (go) {
        location.assign(`checkout.html?buyNow=1&productId=${id}&variantId=${selected.id}&quantity=${quantity}`);
        return;
      }
      await api(cartPath() + '/items', 'POST', {
        productDetailId: selected.id,
        quantity,
      });
      await refreshCart();
      badges();
      addedToCartDialog(product.productName);
    };
    add.onclick = () => action(add, () => addItem(false));
    buy.onclick = () => action(buy, () => addItem(true));
    sync();
  }
  function addedToCartDialog(productName) {
    document.getElementById('client-added-dialog')?.remove();
    const dialog = document.createElement('dialog');
    dialog.id = 'client-added-dialog'; dialog.className = 'client-added-dialog';
    dialog.setAttribute('aria-labelledby', 'client-added-title');
    dialog.setAttribute('aria-describedby', 'client-added-description');
    dialog.innerHTML = `<h2 id="client-added-title">Đã thêm sản phẩm vào giỏ hàng</h2><p id="client-added-description">${esc(productName)} đã được lưu vào giỏ hàng của bạn.</p><div class="client-dialog-actions"><button type="button" class="btn btn-outline-primary" autofocus>Xem tiếp sản phẩm</button><a class="btn btn-primary" href="cart.html">Đi đến trang giỏ hàng</a></div>`;
    $('button', dialog).onclick = () => dialog.close();
    dialog.addEventListener('close', () => dialog.remove());
    document.body.append(dialog); dialog.showModal();
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
          cart = await api(cartPath() + '/items/' + line.id, 'PUT', {
            quantity: q,
          });
          await refreshCart();
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
            cart = await api(cartPath() + '/items/' + line.id, 'DELETE');
            await refreshCart();
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
        cart = await api(cartPath() + '/items', 'DELETE');
        await refreshCart();
        badges();
        render();
      });
    $('.continue-shopping a', summary).href = 'products.html';
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
    $$('#orders .pagination,#orders .pagination-wrap,#orders .pagination-container,#orders .pagination-wrapper', host).forEach(
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
    const profile = $('#firstName').closest('form');
    profile.reset();
    $$('#settings form', host).forEach((form) =>
      form.querySelectorAll('input,button').forEach((n) => (n.disabled = true)),
    );
    message($('#settings', host), 'Cập nhật hồ sơ và mật khẩu chưa được hỗ trợ bởi API hiện tại.');
    let [orderData, addresses] = await Promise.all([
      api('/api/bills'),
      api('/api/addresses'),
    ]);
    const orderGroups = [['all','Tất cả'],['payment','Chờ thanh toán'],['processing','Chờ xử lý'],['shipping','Đang vận chuyển'],['history','Lịch sử mua']];
    const groupOf = (order) => {
      if ([3,4,5,6].includes(Number(order.status))) return 'history';
      if (Number(order.status) === 2) return 'shipping';
      if (order.paymentMethod !== 'COD' && Number(order.paymentStatus) === 0) return 'payment';
      return 'processing';
    };
    let currentGroup = orderGroups.some(([key])=>key === query.get('orderGroup')) ? query.get('orderGroup') : 'all';
    const tabs = document.createElement('div'); tabs.className = 'client-order-tabs';
    tabs.setAttribute('role','group'); tabs.setAttribute('aria-label','Lọc đơn hàng theo trạng thái');
    orders.before(tabs);
    const refresh = document.createElement('button'); refresh.type='button'; refresh.className='btn btn-outline-primary mb-3';refresh.textContent='Làm mới đơn hàng';
    tabs.before(refresh);
    refresh.onclick=()=>action(refresh,async()=>{window.ClientOrderRequests?.invalidate();orderData=await api('/api/bills');drawOrders();});
    const drawOrders = () => {
      tabs.innerHTML = orderGroups.map(([key,label])=>`<button type="button" data-order-group="${key}" aria-pressed="${key===currentGroup}">${label} <span>${orderData.filter(o=>key==='all'||groupOf(o)===key).length}</span></button>`).join('');
      $('.menu-nav a[href="#orders"] .badge', host).textContent = orderData.length;
      const keyword = $('#orders .search-box input', host)?.value.toLowerCase() || '';
      orders.replaceChildren();
      for (const order of orderData.filter((n) =>
        String(n.billCode).toLowerCase().includes(keyword) && (currentGroup==='all'||groupOf(n)===currentGroup),
      )) {
        const node = document.createElement('article');
        node.className = 'order-card';
        const paymentMethod = order.paymentMethod === 'SEPAY' ? 'SePay' : 'COD';
        node.innerHTML = `<div class="client-order-head"><h3>${esc(order.billCode)}</h3><p class="client-order-state" data-state="${Number(order.status)}">${esc(billStatusLabel(order, orderStatuses))}</p></div><p>Thanh toán: ${paymentMethod} · ${paymentStatusLabel(order.paymentStatus)} · <strong>${money(order.totalAmount)}</strong></p><p class="client-order-date">Ngày đặt: ${esc(order.createdAt ? new Date(order.createdAt).toLocaleString('vi-VN') : 'Đang cập nhật')}</p>${cancellationNotice(order) ? `<p class="alert alert-warning">${esc(cancellationNotice(order))}</p>` : ''}<button class="btn btn-outline-primary" type="button">Xem chi tiết</button><a class="btn btn-primary ms-2" href="order-confirmation.html?orderId=${order.id}">${groupOf(order)==='payment'?'Tiếp tục thanh toán':'Xem trạng thái đơn'}</a>`;
        $('button', node).onclick = (e) =>
          action(e.currentTarget, async () => {
            const [bill, linesData] = await Promise.all([
              api('/api/bills/' + order.id),
              api('/api/bills/' + order.id + '/lines'),
            ]);
            let lines = $('.client-order-lines', node);
            if (!lines) {
              lines = document.createElement('div');
              lines.className = 'client-order-lines';
              node.append(lines);
            }
            lines.innerHTML = `<p>${esc(billStatusLabel(bill, orderStatuses))}</p><p>Thanh toán: ${bill.paymentMethod === 'SEPAY' ? 'SePay' : 'COD'} · ${paymentStatusLabel(bill.paymentStatus)}</p><p>${esc(bill.shippingAddress)}</p>` +
              linesData
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
                    money(l.totalPrice) +
                    '</p>',
                )
                .join('');
            if (bill.paymentMethod === 'SEPAY' && bill.paymentStatus === 0) {
              const checkPayment = document.createElement('button');
              checkPayment.type = 'button';
              checkPayment.className = 'btn btn-outline-primary';
              checkPayment.textContent = 'Kiểm tra trạng thái thanh toán';
              checkPayment.onclick = (event) =>
                action(event.currentTarget, async () => {
                  const latest = await api('/api/bills/' + order.id);
                  if (latest.paymentMethod !== 'SEPAY')
                    throw Error('Đơn hàng này không được ghi nhận bằng SePay.');
                  order.paymentMethod = latest.paymentMethod;
                  order.paymentStatus = latest.paymentStatus;
                  order.status = latest.status;
                  const orderStatus = $$('p', node)[0];
                  if (orderStatus) orderStatus.textContent = billStatusLabel(latest, orderStatuses);
                  const detailStatus = $$('p', lines)[0];
                  if (detailStatus) detailStatus.textContent = billStatusLabel(latest, orderStatuses);
                  const detailPayment = $$('p', lines)[1];
                  if (detailPayment)
                    detailPayment.textContent =
                      'Thanh toán: SePay · ' + paymentStatusLabel(latest.paymentStatus);
                  const listPayment = $$('p', node)[1];
                  if (listPayment)
                    listPayment.textContent =
                      'Thanh toán: SePay · ' +
                      paymentStatusLabel(latest.paymentStatus) +
                      ' · ' +
                      money(latest.totalAmount);
                  drawOrders();
                });
              lines.append(checkPayment);
            }
          });
        orders.append(node);
        window.ClientOrderRequests?.mount(node,order,async()=>{orderData=await api('/api/bills');drawOrders();});
      }
      if (!orders.children.length) message(orders, 'Bạn chưa có đơn hàng phù hợp.');
    };
    tabs.onclick=(event)=>{
      const button=event.target.closest('[data-order-group]');if(!button)return;
      currentGroup=button.dataset.orderGroup;
      const url=new URL(location.href);url.searchParams.set('orderGroup',currentGroup);history.replaceState(null,'',url);
      drawOrders();
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
        card.innerHTML = `<h3>${esc(a.receiverName)} ${a.isDefault ? '· Mặc định' : ''}</h3><p>${esc(a.receiverPhone)}</p><p>${esc([a.detailAddress, a.ward, a.province].join(', '))}</p><button class="btn btn-outline-primary" data-edit>Sửa</button><button class="btn btn-outline-primary" data-default>Mặc định</button><button class="btn btn-outline-danger" data-delete>Xóa</button>`;
        $('[data-edit]', card).onclick = () => editAddress(a);
        $('[data-default]', card).onclick = (e) =>
          action(e.currentTarget, async () => {
            await api('/api/addresses/' + a.id, 'PUT', {
              receiverName: a.receiverName,
              receiverPhone: a.receiverPhone,
              province: a.province,
              ward: a.ward,
              detailAddress: a.detailAddress,
              isDefault: true,
            });
            drawAddresses(await api('/api/addresses'));
          });
        $('[data-delete]', card).onclick = (e) =>
          action(e.currentTarget, async () => {
            if (confirm('Xóa địa chỉ này khỏi sổ địa chỉ?')) {
              await api('/api/addresses/' + a.id, 'DELETE');
              drawAddresses(await api('/api/addresses'));
            }
          });
        list.append(card);
      }
      $('[data-add-address]', addressPanel).onclick = () => editAddress(null);
    };
    const editAddress = async (a) => {
      const form = document.createElement('form');
      form.className = 'client-address-card';
      const fields = [
        ['receiverName', 'Người nhận', 'receiverName', 150],
        ['receiverPhone', 'Điện thoại', 'receiverPhone', 20],
        ['detailAddress', 'Địa chỉ chi tiết', 'detailAddress', 300],
      ];
      form.innerHTML =
        fields
          .map(
            ([name, label, key, max]) =>
              `<label class="d-block mb-3">${label}<input class="form-control" name="${name}" required maxlength="${max}" value="${esc(a?.[key] || '')}"></label>`,
          )
          .join('') +
        '<label class="d-block mb-3">Tỉnh/thành phố<select class="form-select" name="province" required></select></label><label class="d-block mb-3">Phường/xã<select class="form-select" name="ward" required></select></label>' +
        '<label><input type="checkbox" name="isDefault"> Đặt làm mặc định</label><p><button class="btn btn-primary" type="submit">Lưu địa chỉ</button> <button class="btn btn-link" type="button">Hủy</button></p>';
      $('[name="isDefault"]', form).checked = a?.isDefault === true;
      $('[type="button"]', form).onclick = () => form.remove();
      addressPanel.append(form);
      try {
        await populateAddressSelects(form, a?.province, a?.ward);
      } catch (error) {
        message(form, error.message, true);
      }
      form.onsubmit = (e) => {
        e.preventDefault();
        action($('[type="submit"]', form), async () => {
          if (!$('[name="province"]', form).value || !$('[name="ward"]', form).value)
            throw Error('Vui lòng chọn tỉnh/thành phố và phường/xã.');
          const data = Object.fromEntries(new FormData(form));
          data.isDefault = $('[name="isDefault"]', form).checked;
          await api('/api/addresses' + (a ? '/' + a.id : ''), a ? 'PUT' : 'POST', data);
          drawAddresses(await api('/api/addresses'));
        });
      };
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
    form.replaceChildren();
    const summary = $('main .order-summary-content');
    if (summary) summary.replaceChildren();
    const directPurchase = query.get('buyNow') === '1';
    const requestedIds = query.has('items')
      ? new Set(query.get('items').split(',').filter(Boolean).map(Number))
      : selectedCartIds();
    let checkoutItems = cart.items.filter((line) => requestedIds.has(line.productDetailId));
    if (directPurchase && customer) {
      const productId = Number(query.get('productId')), variantId = Number(query.get('variantId')),
        quantity = Number(query.get('quantity'));
      if (![productId, variantId, quantity].every((n) => Number.isSafeInteger(n) && n > 0) || quantity > 100)
        throw Error('Thông tin mua ngay không hợp lệ. Vui lòng chọn lại sản phẩm.');
      const [product, variants] = await Promise.all([api('/api/products/' + productId), api('/api/products/' + productId + '/details')]);
      const variant = variants.find((item) => item.id === variantId);
      if (!variant || quantity > variant.quantity) throw Error('Biến thể không còn đủ hàng. Vui lòng chọn lại.');
      checkoutItems = [{productDetailId: variant.id, productName: product.productName,
        colorName: variant.colorName, sizeName: variant.sizeName, quantity,
        price: variant.price, stock: variant.quantity, available: true}];
    }
    const itemCount = $('main .order-summary-header .item-count');
    if (itemCount)
      itemCount.textContent =
        checkoutItems.reduce((sum, line) => sum + line.quantity, 0) + ' sản phẩm';
    const selectionValid =
      checkoutItems.length > 0 &&
      (directPurchase || checkoutItems.length === requestedIds.size) &&
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
    const quote = await api('/api/bills/checkout-quote', 'POST', {
      items: checkoutItems.map((item) => ({productDetailId: item.productDetailId, quantity: item.quantity})),
    });
    const [addresses, availableVouchers] = await Promise.all([
      api('/api/addresses'),
      api('/api/vouchers'),
    ]);
    const addressOptions = addresses
      .map(
        (address) =>
          `<option value="${address.id}" ${address.isDefault ? 'selected' : ''}>${esc(address.receiverName)} · ${esc(address.receiverPhone)} · ${esc(address.detailAddress)}, ${esc(address.ward)}, ${esc(address.province)}</option>`,
      )
      .join('');
    const voucherCode = sessionStorage.getItem(pendingVoucherKey);
    sessionStorage.removeItem(pendingVoucherKey);
    const voucherOptions = availableVouchers
      .map((voucher) => {
        const discount =
          voucher.discountType === 'PERCENT'
            ? `${Number(voucher.discountValue)}% (tối đa ${money(voucher.maxDiscount)})`
            : money(voucher.discountValue);
        return `<option value="${esc(voucher.code)}" ${voucher.code === voucherCode ? 'selected' : ''}>${esc(voucher.code)} · giảm ${esc(discount)} · đơn từ ${money(voucher.minOrder)}</option>`;
      })
      .join('');
    form.innerHTML =
      `<label class="d-block mb-3">Địa chỉ nhận hàng<select class="form-select" name="addressId"><option value="">Chọn địa chỉ</option>${addressOptions}<option value="new" ${addresses.length ? '' : 'selected'}>Nhập địa chỉ mới</option></select></label>` +
      '<div class="client-new-address" hidden><label class="d-block mb-3">Người nhận<input class="form-control" name="receiverName" maxlength="150"></label><label class="d-block mb-3">Điện thoại<input class="form-control" name="receiverPhone" maxlength="20"></label><label class="d-block mb-3">Tỉnh/thành phố<select class="form-select" name="province"></select></label><label class="d-block mb-3">Phường/xã<select class="form-select" name="ward"></select></label><label class="d-block mb-3">Địa chỉ chi tiết<input class="form-control" name="detailAddress" maxlength="300"></label></div>' +
      `<fieldset class="mb-3"><legend class="form-label">Phương thức thanh toán</legend><label class="d-block"><input type="radio" name="paymentMethod" value="COD" checked> Thanh toán khi nhận hàng (COD)</label><label class="d-block"><input type="radio" name="paymentMethod" value="SEPAY"> Chuyển khoản qua SePay</label></fieldset><div class="client-voucher mb-3"><label class="d-block">Voucher dành cho tài khoản<select name="voucherCode" class="form-select"><option value="">Không sử dụng voucher</option>${voucherOptions}</select></label>${availableVouchers.length ? '' : '<p>Hiện chưa có voucher phù hợp với tài khoản của bạn.</p>'}<button type="button" class="btn btn-outline-primary mt-2" data-voucher-apply ${availableVouchers.length ? '' : 'disabled'}>Áp dụng voucher</button><p class="client-voucher-message" role="status"></p></div><label class="d-block mb-3">Ghi chú<textarea name="note" class="form-control" maxlength="500"></textarea></label><p class="client-checkout-total"></p><button type="submit" class="btn btn-primary">Đặt hàng</button>`;
    const addressSelect = $('[name="addressId"]', form);
    const newAddress = $('.client-new-address', form);
    let addressSelectsLoaded = false;
    addressSelect.onchange = () => {
      newAddress.hidden = addressSelect.value !== 'new';
      $$('input,select', newAddress).forEach((input) => (input.required = !newAddress.hidden));
      if (!newAddress.hidden && !addressSelectsLoaded) {
        addressSelectsLoaded = true;
        populateAddressSelects(newAddress).catch((error) => {
          addressSelectsLoaded = false;
          message(form, error.message, true);
        });
      }
    };
    addressSelect.dispatchEvent(new Event('change'));
    if (summary) {
      for (const line of checkoutItems) {
        const p = document.createElement('p');
        p.textContent = `${line.productName} · ${line.colorName} / ${line.sizeName} × ${line.quantity}: ${money(line.price * line.quantity)}`;
        summary.append(p);
      }
    }
    const summaryTotal = summary ? document.createElement('p') : null;
    if (summaryTotal) {
      summaryTotal.className = 'fw-bold';
      summary.append(summaryTotal);
    }
    let appliedVoucherCode = '',
      voucherQuote = null,
      quoteSequence = 0;
    const voucherInput = $('[name="voucherCode"]', form),
      voucherMessage = $('.client-voucher-message', form),
      totals = $('.client-checkout-total', form);
    const renderTotals = () => {
    const currentQuote = voucherQuote || quote;
    totals.textContent = voucherQuote
      ? `Tiền hàng: ${money(voucherQuote.subtotal)} · Giảm ${money(voucherQuote.discount)} · Phí giao hàng: ${money(voucherQuote.shippingFee)} · Tổng thanh toán: ${money(voucherQuote.total)}`
      : `Tiền hàng: ${money(quote.subtotal)} · Phí giao hàng: ${money(quote.shippingFee)} · Tổng thanh toán: ${money(quote.totalAmount)}`;
    if (summaryTotal) summaryTotal.textContent = 'Tổng thanh toán: ' + money(currentQuote.total ?? currentQuote.totalAmount);
    };
    renderTotals();
    voucherInput.oninput = () => {
      quoteSequence++;
      appliedVoucherCode = '';
      voucherQuote = null;
      voucherMessage.textContent = '';
      renderTotals();
    };
    voucherInput.onchange = voucherInput.oninput;
    $('[data-voucher-apply]', form).onclick = (event) =>
      action(event.currentTarget, async () => {
        const code = voucherInput.value.trim().toUpperCase();
        if (!code) throw Error('Nhập mã voucher trước khi áp dụng.');
        const sequence = ++quoteSequence;
        const quote = await api('/api/bills/quote', 'POST', {
          code,
          items: checkoutItems.map((item) => ({
            productDetailId: item.productDetailId,
            quantity: item.quantity,
          })),
        });
        if (sequence !== quoteSequence) return;
        voucherQuote = quote;
        appliedVoucherCode = code;
        voucherMessage.textContent = `Đã áp dụng ${code}. Giảm ${money(quote.discount)} trên các sản phẩm đủ điều kiện.`;
        voucherMessage.setAttribute('role', 'status');
        renderTotals();
      });
    form.onsubmit = (event) => {
      event.preventDefault();
      if (!form.reportValidity()) return;
      const button = $('[type="submit"]', form);
      action(button, async () => {
        const enteredCode = voucherInput.value.trim().toUpperCase();
        if (enteredCode && enteredCode !== appliedVoucherCode)
          throw Error('Hãy áp dụng lại voucher trước khi đặt hàng.');
        let addressId = Number(addressSelect.value);
        if (addressSelect.value === 'new') {
          if (!addressSelectsLoaded || !$('[name="province"]', newAddress).value || !$('[name="ward"]', newAddress).value)
            throw Error('Vui lòng chọn tỉnh/thành phố và phường/xã.');
          const address = Object.fromEntries(new FormData(form));
          const created = await api('/api/addresses', 'POST', {
            receiverName: address.receiverName,
            receiverPhone: address.receiverPhone,
            province: address.province,
            ward: address.ward,
            detailAddress: address.detailAddress,
            isDefault: addresses.length === 0,
          });
          addressId = created.id;
        }
        if (!Number.isSafeInteger(addressId) || addressId < 1)
          throw Error('Vui lòng chọn hoặc nhập địa chỉ nhận hàng.');
        const paymentMethod = $('input[name="paymentMethod"]:checked', form)?.value;
        if (paymentMethod !== 'COD' && paymentMethod !== 'SEPAY')
          throw Error('Vui lòng chọn phương thức thanh toán.');
        const order = await api('/api/bills', 'POST', {
          addressId,
          paymentMethod,
          note: $('[name="note"]', form).value.trim(),
          voucherCode: appliedVoucherCode || null,
          items: checkoutItems.map((item) => ({
            productDetailId: item.productDetailId,
            quantity: item.quantity,
          })),
        });
        // The order is committed: prevent resubmitting even if a later payment/cart request fails.
        form.onsubmit = (e) => e.preventDefault();
        if (order.paymentMethod !== paymentMethod)
          throw Error(
            `Đơn ${order.billCode} đã được tạo, nhưng backend trả về phương thức ${order.paymentMethod || 'không xác định'} thay vì ${paymentMethod}.`,
          );
        let paymentWarning = '';
        if (paymentMethod === 'SEPAY') {
          try { await api('/api/bills/' + order.id + '/sepay-payment', 'POST'); }
          catch { paymentWarning = 'Đơn đã được tạo. Hãy mở chi tiết đơn để tiếp tục thanh toán SePay.'; }
        }
        if (!directPurchase) saveCartSelection(new Set());
        const removals = await Promise.allSettled(
          directPurchase ? [] : checkoutItems.map((item) => api(cartPath() + '/items/' + item.id, 'DELETE')),
        );
        const failedRemoval = removals.find((result) => result.status === 'rejected');
        let cartWarning = failedRemoval ? failedRemoval.reason.message : paymentWarning;
        if (!cartWarning && !directPurchase) {
          try {
            await refreshCart();
          } catch (error) {
            cartWarning = error.message;
          }
        }
        const result = document.createElement('section');
        result.className = cartWarning ? 'alert alert-warning' : 'alert alert-success';
        result.setAttribute('role', 'status');
        const resultText = document.createElement('p');
        resultText.textContent = cartWarning
          ? `Đơn ${order.billCode} đã được tạo với phương thức ${paymentMethod}, nhưng giỏ hàng chưa cập nhật được: ${cartWarning}`
          : `Đặt hàng thành công với phương thức ${paymentMethod}. Mã đơn: ${order.billCode}.`;
        result.append(resultText);
        if (paymentMethod === 'SEPAY') {
          const qr = document.createElement('img');
          qr.className = 'img-fluid d-block mx-auto mb-3';
          qr.style.maxWidth = '320px';
          qr.src = sePayQrUrl(order.totalAmount, order.billCode);
          qr.alt = 'Mã QR thanh toán SePay';
          result.append(qr);
          const instructions = document.createElement('p');
          instructions.textContent =
            `Chuyển ${money(order.totalAmount)} đến tài khoản SePay và giữ nguyên nội dung ${order.billCode}.`;
          result.append(instructions);
          const paymentStatus = document.createElement('p');
          paymentStatus.className = 'alert alert-warning';
          paymentStatus.setAttribute('role', 'status');
          paymentStatus.textContent = 'Chưa ghi nhận thanh toán. Hãy hoàn tất chuyển khoản rồi kiểm tra lại.';
          result.append(paymentStatus);
          const checkPayment = document.createElement('button');
          checkPayment.type = 'button';
          checkPayment.className = 'btn btn-outline-primary';
          checkPayment.textContent = 'Kiểm tra trạng thái thanh toán';
          checkPayment.onclick = (event) =>
            action(event.currentTarget, async () => {
              const latest = await api('/api/bills/' + order.id);
              if (latest.paymentMethod !== 'SEPAY')
                throw Error('Đơn hàng này không được ghi nhận bằng SePay.');
              if (!Number.isInteger(latest.paymentStatus))
                throw Error('Máy chủ chưa trả trạng thái thanh toán hợp lệ.');
              const paid = latest.paymentStatus === 1;
              paymentStatus.className = paid ? 'alert alert-success' : 'alert alert-warning';
              paymentStatus.textContent = paid
                ? latest.status === 0
                  ? 'Đã thanh toán - Chờ xác nhận.'
                  : 'Đã thanh toán. Trạng thái đơn: ' +
                    (orderStatuses[latest.status] || 'Đang cập nhật') +
                    '.'
                : 'Chưa ghi nhận thanh toán. Hãy hoàn tất chuyển khoản rồi kiểm tra lại.';
              if (paid) qr.hidden = true;
            });
          result.append(checkPayment);
        }
        const ordersLink = document.createElement('a');
        ordersLink.className = 'btn btn-primary ms-2';
        ordersLink.href = 'order-confirmation.html?orderId=' + encodeURIComponent(order.id);
        ordersLink.textContent = 'Chi tiết và trạng thái thanh toán';
        form.replaceWith(result);
        result.append(ordersLink);
      });
    };
  }
  async function orderConfirmationPage() {
    const host = $('#order-confirmation');
    const orderId = query.get('orderId');
    if (!customer) {
      host.hidden = false;
      message(host, 'Vui lòng đăng nhập để kiểm tra đơn hàng.', true);
      return;
    }
    if (!orderId || !/^\d+$/.test(orderId)) {
      host.hidden = false;
      message(host, 'Không tìm thấy mã đơn hàng cần kiểm tra.', true);
      return;
    }
    let [bill, lines, history] = await Promise.all([
      api('/api/bills/' + encodeURIComponent(orderId)),
      api('/api/bills/' + encodeURIComponent(orderId) + '/lines'),
      api('/api/bills/' + encodeURIComponent(orderId) + '/history'),
    ]);
    const render = (current) => {
      window.ClientOrderRequests?.invalidate();
      host.hidden = false;
      host.innerHTML =
        `<div class="container"><div class="alert alert-info"><h2 class="h4">Đơn ${esc(current.billCode)}</h2><p class="mb-1">Trạng thái đơn: ${esc(billStatusLabel(current, orderStatuses))}</p><p class="mb-0">Thanh toán: ${current.paymentMethod === 'SEPAY' ? 'SePay' : 'COD'} · ${paymentStatusLabel(current.paymentStatus)}</p>${cancellationNotice(current) ? `<p class="mt-2 mb-0">${esc(cancellationNotice(current))}</p>` : ''}</div>` +
        '<div class="row g-4"><div class="col-lg-7"><h3 class="h5">Sản phẩm</h3><ul class="list-group mb-4">' +
        lines
          .map(
            (line) =>
              `<li class="list-group-item d-flex justify-content-between"><span>${esc(line.productName)} · ${esc(line.colorName)} / ${esc(line.sizeName)} × ${line.quantity}</span><strong>${money(line.totalPrice)}</strong></li>`,
          )
          .join('') +
        '</ul><h3 class="h5">Lịch sử đơn hàng</h3><ul class="list-group">' +
        history
          .map(
            (item) =>
              `<li class="list-group-item">${esc(item.note || orderStatuses[item.newStatus] || 'Cập nhật đơn hàng')} · ${esc(item.createdAt)}</li>`,
          )
          .join('') +
        '</ul></div><div class="col-lg-5"><div class="border rounded-3 p-4"><h3 class="h5">Thông tin giao hàng</h3><p>' +
        `${esc(current.receiverName)} · ${esc(current.receiverPhone)}<br>${esc(current.shippingAddress)}</p>` +
        `<p>Tổng thanh toán: <strong>${money(current.totalAmount)}</strong></p></div></div></div><a class="btn btn-outline-primary mt-4" href="account.html#orders">Về đơn hàng của tôi</a></div>`;
      const paymentPanel = host.querySelector('.col-lg-5 > div');
      if (current.paymentMethod === 'SEPAY' && current.paymentStatus === 0 && current.status !== 4) {
        const qr = document.createElement('img');
        qr.className = 'img-fluid d-block mx-auto mb-3';
        qr.style.maxWidth = '320px';
        qr.src = sePayQrUrl(current.totalAmount, current.billCode);
        qr.alt = 'Mã QR thanh toán SePay';
        paymentPanel.append(qr);
        const note = document.createElement('p');
        note.textContent =
          `Chuyển ${money(current.totalAmount)} và giữ nguyên nội dung ${current.billCode}.`;
        paymentPanel.append(note);
        const check = document.createElement('button');
        check.className = 'btn btn-outline-primary';
        check.type = 'button';
        check.textContent = 'Kiểm tra trạng thái thanh toán';
        check.onclick = (event) =>
          action(event.currentTarget, async () => {
            const latest = await api('/api/bills/' + encodeURIComponent(orderId));
            if (latest.paymentMethod !== 'SEPAY')
              throw Error('Đơn hàng này không được ghi nhận bằng SePay.');
            if (!Number.isInteger(latest.paymentStatus))
              throw Error('Máy chủ chưa trả trạng thái thanh toán hợp lệ.');
            render(latest);
          });
        paymentPanel.append(check);
      } else if (current.paymentMethod === 'SEPAY' && current.paymentStatus === 1) {
        const paid = document.createElement('p');
        paid.className = 'alert alert-success';
        paid.setAttribute('role', 'status');
        paid.textContent =
          current.status === 0
            ? 'Đã thanh toán - Chờ xác nhận.'
            : 'Đã thanh toán. Trạng thái đơn: ' +
              (orderStatuses[current.status] || 'Đang cập nhật') +
              '.';
        paymentPanel.append(paid);
      }
      window.ClientOrderRequests?.mount(paymentPanel,current,async()=>{
        bill=await api('/api/bills/'+encodeURIComponent(orderId));
        history=await api('/api/bills/'+encodeURIComponent(orderId)+'/history');
        render(bill);
      });
      const refreshOrder=document.createElement('button');
      refreshOrder.type='button';refreshOrder.className='btn btn-outline-primary mt-3';refreshOrder.textContent='Cập nhật trạng thái đơn';
      refreshOrder.onclick=event=>action(event.currentTarget,async()=>{
        [bill,history]=await Promise.all([api('/api/bills/'+encodeURIComponent(orderId)),api('/api/bills/'+encodeURIComponent(orderId)+'/history')]);
        render(bill);
      });
      paymentPanel.append(refreshOrder);
    };
    render(bill);
  }
  async function customerVouchersPage() {
    const section = $('#customer-vouchers');
    if (!section) return;
    const list = $('[data-voucher-list]', section);
    section.hidden = false;
    if (!customer) {
      list.innerHTML =
        '<p>Đăng nhập để xem voucher đủ điều kiện dành riêng cho tài khoản của bạn. <a href="login-register.html">Đăng nhập</a></p>';
      return;
    }
    const vouchers = await api('/api/vouchers');
    if (!vouchers.length) {
      list.innerHTML = '<p>Hiện chưa có voucher phù hợp với tài khoản của bạn.</p>';
      return;
    }
    list.innerHTML = vouchers
      .map((voucher) => {
        const discount =
          voucher.discountType === 'PERCENT'
            ? `${Number(voucher.discountValue)}% · tối đa ${money(voucher.maxDiscount)}`
            : money(voucher.discountValue);
        const expiry = voucher.endsAt
          ? `Hạn đến ${new Date(voucher.endsAt).toLocaleDateString('vi-VN')}`
          : 'Không giới hạn thời gian';
        return `<article class="client-voucher-card"><strong>${esc(voucher.code)}</strong><p>Giảm ${esc(discount)}</p><p>Đơn tối thiểu ${money(voucher.minOrder)} · ${esc(expiry)}</p><button class="btn btn-outline-primary" type="button" data-select-voucher="${esc(voucher.code)}">Chọn voucher</button></article>`;
      })
      .join('');
    list.onclick = (event) => {
      const button = event.target.closest('[data-select-voucher]');
      if (!button) return;
      sessionStorage.setItem(pendingVoucherKey, button.dataset.selectVoucher);
      location.assign('cart.html');
    };
  }
  async function init() {
    // API-rendered controls and products must stay visible independently of template scroll effects.
    $$('main [data-aos]').forEach((node) => node.removeAttribute('data-aos'));
    if (sessionStorage.getItem(tokenKey)) {
      try {
        const user = await api('/api/auth/me');
        customer = {
          customerId: user.customerId,
          FULL_NAME: user.fullName,
          EMAIL: user.email,
          PHONE: user.phone,
        };
        sessionStorage.setItem(userKey, JSON.stringify(user));
      } catch (e) {
        sessionStorage.removeItem(tokenKey);
        sessionStorage.removeItem(userKey);
        if (![400, 401, 403].includes(e.status)) identityError = e;
      }
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
    if (file === 'index.html' || file === '') jobs.push(customerVouchersPage());
    if (file === 'order-confirmation.html') jobs.push(orderConfirmationPage());
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
