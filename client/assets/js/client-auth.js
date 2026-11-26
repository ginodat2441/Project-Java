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
  const useSameOriginApi = !isLoopbackHost || ['3000', '8081'].includes(location.port);
  const apiBase = (
    window.CLOTHING_SHOP_API_BASE ||
    (useSameOriginApi ? '/api' : 'http://localhost:8080/api')
  ).replace(/\/+$/, '');
  // Keep cookies on the same host as Live Server (localhost and 127.0.0.1 differ).
  const adminBase = (window.CLOTHING_SHOP_ADMIN_BASE ||
    (isLoopbackHost ? `${location.protocol}//${location.hostname}:8888` : location.origin)).replace(/\/+$/, '');
  const ui = window.ClothingAuthUI;
  const notice = ui.notice;

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
      throw Error('Không kết nối được cửa hàng. Vui lòng thử lại.');
    }

    const body = await response.json().catch(() => null);
    if (endpoint === 'login' && response.status === 401) {
      return loginManager(payload);
    }
    if (!response.ok || body?.success !== true || !body.data?.accessToken || !body.data?.user) {
      if (response.status >= 500)
        throw Error('Máy chủ chưa xử lý được yêu cầu. Vui lòng thử lại sau.');
      const error = Error(body?.message || 'Email hoặc mật khẩu không chính xác.');
      error.fieldErrors = body?.fieldErrors;
      throw error;
    }
    return body.data;
  }

  async function loginManager(payload) {
    let csrfResponse, response;
    try {
      csrfResponse = await fetch(adminBase + '/api/csrf', { credentials: 'include', headers: { Accept: 'application/json' } });
      if (!csrfResponse.ok) throw Error('csrf');
      const csrf = await csrfResponse.json();
      if (!csrf.token || !csrf.headerName) throw Error('csrf');
      response = await fetch(adminBase + '/login', {
        method: 'POST', credentials: 'include',
        headers: { Accept: 'application/json', 'Content-Type': 'application/x-www-form-urlencoded', [csrf.headerName]: csrf.token },
        body: new URLSearchParams({ username: payload.email, password: payload.password }),
      });
    } catch {
      throw Error('Không kết nối được trang quản trị. Hãy kiểm tra backend Admin đã chạy chưa.');
    }
    const result = await response.json().catch(() => null);
    if (!response.ok || result?.success !== true || result.redirect !== '/admin') {
      throw Error('Email hoặc mật khẩu không chính xác, tài khoản bị khóa hoặc tạm giới hạn đăng nhập.');
    }
    return { managerRedirect: adminBase + '/admin' };
  }

  async function handleSubmit(form, endpoint, getPayload) {
    const button = form.querySelector('[type="submit"]');
    if (button.disabled) return;
    const label = button.textContent;
    button.disabled = true;
    button.textContent = 'Đang xử lý…';
    notice.hidden = true;
    try {
      const payload=getPayload();
      const auth = await submit(endpoint, payload);
      if (auth.managerRedirect) {
        sessionStorage.removeItem(tokenKey);
        sessionStorage.removeItem(userKey);
        form.reset();
        ui.success('Đăng nhập thành công','Chào mừng bạn trở lại không gian quản trị Clothing Shop.','Vào trang quản trị',auth.managerRedirect);
        return;
      }
      if (endpoint === 'register') {
        sessionStorage.removeItem(tokenKey);sessionStorage.removeItem(userKey);
        form.reset();
        const destination=new URL('login-register.html',location.href);destination.searchParams.set('email',payload.email);
        const next=new URLSearchParams(location.search).get('next');if(next)destination.searchParams.set('next',ui.safeNext());
        ui.success('Đăng ký thành công','Tài khoản đã được lưu. Hãy đăng nhập để bắt đầu mua sắm tại Clothing Shop.','Đến trang đăng nhập',destination.pathname+destination.search);
        return;
      }
      sessionStorage.setItem(tokenKey, auth.accessToken);
      sessionStorage.setItem(userKey, JSON.stringify(auth.user));
      form.reset();
      ui.success('Đăng nhập thành công','Chào mừng bạn trở lại Clothing Shop.','Tiếp tục mua sắm',ui.safeNext());
    } catch (error) {
      if(error.fieldErrors)ui.showErrors(form,error.fieldErrors);
      else ui.notify(error.message);
    } finally {
      button.disabled = false;
      button.textContent = label;
    }
  }

  login.addEventListener('submit', (event) => {
    event.preventDefault();
    if (!ui.validate(login)) return;
    const email = input('login-email').value.trim();
    if (!email.includes('@')) {
      ui.notify('Vui lòng dùng email để đăng nhập.');
      return;
    }
    handleSubmit(login, 'login', () => ({
      email: email.toLowerCase(),
      password: input('login-password').value,
    }));
  });

  register.addEventListener('submit', (event) => {
    event.preventDefault();
    if (!ui.validate(register)) return;
    const password = input('reg-password').value;
    if (password !== input('reg-confirm-password').value) {
      ui.showErrors(register, { confirmPassword: 'Nhập lại mật khẩu phải khớp với ô Mật khẩu.' });
      input('reg-confirm-password').focus();
      return;
    }
    if (new TextEncoder().encode(password).length > 72) {
      ui.showErrors(register,{password:'Mật khẩu không được vượt quá 72 byte UTF-8.'});
      return;
    }
    handleSubmit(register, 'register', () => ({
      username: input('reg-username').value.trim().toLowerCase(),
      fullName: `${input('reg-lastname').value.trim()} ${input('reg-firstname').value.trim()}`.trim(),
      phone: ui.normalizePhone(input('reg-phone').value),
      email: input('reg-email').value.trim().toLowerCase(),
      password,
    }));
  });
})();
