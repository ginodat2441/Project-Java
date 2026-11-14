(() => {
  'use strict';

  const login = document.querySelector('#client-login-form');
  const register = document.querySelector('#client-register-form');
  if (!login || !register) return;

  const input = (id) => document.getElementById(`login-register-${id}`);
  const tokenKey = 'clothingShopToken';
  const userKey = 'clothingShopUser';
  const isLoopbackHost =
    location.hostname === 'localhost' || location.hostname === '127.0.0.1';
  const useSameOriginApi = !isLoopbackHost || location.port === '3000';
  const apiBase = (
    window.CLOTHING_SHOP_API_BASE ||
    (useSameOriginApi ? '/api' : 'http://localhost:8080/api')
  ).replace(/\/+$/, '');
  const notice = document.createElement('div');
  notice.className = 'alert alert-danger';
  notice.setAttribute('role', 'alert');
  notice.hidden = true;
  document.querySelector('.login-register-wraper .tab-content').before(notice);

  async function submit(endpoint, payload) {
    let response;
    try {
      response = await fetch(apiBase + '/auth/' + endpoint, {
        method: 'POST',
        headers: { Accept: 'application/json', 'Content-Type': 'application/json' },
        credentials: 'same-origin',
        body: JSON.stringify(payload),
      });
    } catch {
      throw Error('Không kết nối được máy chủ. Hãy chạy backend Java và thử lại.');
    }

    const body = await response.json().catch(() => null);
    if (!response.ok || body?.success !== true || !body.data?.accessToken || !body.data?.user) {
      if (response.status >= 500)
        throw Error('Máy chủ chưa xử lý được yêu cầu. Vui lòng thử lại sau.');
      throw Error(body?.message || 'Email hoặc mật khẩu không chính xác.');
    }
    return body.data;
  }

  async function handleSubmit(form, endpoint, getPayload) {
    const button = form.querySelector('[type="submit"]');
    const label = button.textContent;
    button.disabled = true;
    button.textContent = 'Đang xử lý…';
    notice.hidden = true;
    try {
      const auth = await submit(endpoint, getPayload());
      sessionStorage.setItem(tokenKey, auth.accessToken);
      sessionStorage.setItem(userKey, JSON.stringify(auth.user));
      form.reset();
      const next = new URLSearchParams(location.search).get('next');
      const destination =
        endpoint === 'login' &&
        next &&
        !next.startsWith('//') &&
        !/^[a-z][a-z\d+.-]*:/i.test(next)
          ? next
          : 'index.html';
      location.assign(destination);
    } catch (error) {
      notice.textContent = error.message;
      notice.hidden = false;
      notice.scrollIntoView?.({ block: 'nearest', behavior: 'smooth' });
    } finally {
      button.disabled = false;
      button.textContent = label;
    }
  }

  login.addEventListener('submit', (event) => {
    event.preventDefault();
    if (!login.reportValidity()) return;
    const email = input('login-email').value.trim();
    if (!email.includes('@')) {
      notice.textContent = 'Đăng nhập khách hàng cần sử dụng địa chỉ email.';
      notice.hidden = false;
      return;
    }
    handleSubmit(login, 'login', () => ({
      email: email.toLowerCase(),
      password: input('login-password').value,
    }));
  });

  register.addEventListener('submit', (event) => {
    event.preventDefault();
    if (!register.reportValidity()) return;
    const password = input('reg-password').value;
    if (password !== input('reg-confirm-password').value) {
      notice.textContent = 'Mật khẩu xác nhận không khớp.';
      notice.hidden = false;
      return;
    }
    if (new TextEncoder().encode(password).length > 72) {
      notice.textContent = 'Mật khẩu không được vượt quá 72 byte UTF-8.';
      notice.hidden = false;
      return;
    }
    handleSubmit(register, 'register', () => ({
      username: input('reg-username').value.trim(),
      fullName: `${input('reg-lastname').value.trim()} ${input('reg-firstname').value.trim()}`.trim(),
      phone: input('reg-phone').value.trim(),
      email: input('reg-email').value.trim().toLowerCase(),
      password,
    }));
  });
})();
