/* Customer forms only. Account records live in Oracle; authentication uses the server session. */
(() => {
  'use strict';
  const login = document.querySelector('#login-register-login-form form');
  const register = document.querySelector('#login-register-registration-form form');
  if (!login || !register) return;
  const input = id => document.getElementById(`login-register-${id}`);
  const notice = document.createElement('div');
  notice.className = 'alert alert-danger';
  notice.setAttribute('role', 'alert');
  notice.hidden = true;
  document.querySelector('.login-register-wraper .tab-content').before(notice);

  function popup(message) {
    if (!window.bootstrap?.Modal) { window.alert(message); return Promise.resolve(); }
    return new Promise(resolve => {
      const host = document.createElement('div');
      host.className = 'modal';
      host.tabIndex = -1;
      host.setAttribute('aria-labelledby', 'client-auth-success-title');
      host.innerHTML = '<div class="modal-dialog modal-dialog-centered"><div class="modal-content"><div class="modal-header"><h2 class="modal-title fs-5" id="client-auth-success-title">Thành công</h2></div><div class="modal-body"></div><div class="modal-footer"><button type="button" class="btn btn-primary" data-bs-dismiss="modal">Tiếp tục</button></div></div></div>';
      host.querySelector('.modal-body').textContent = message;
      document.body.append(host);
      const modal = new bootstrap.Modal(host);
      host.addEventListener('hidden.bs.modal', () => { modal.dispose(); host.remove(); resolve(); }, { once: true });
      modal.show();
    });
  }

  async function submit(kind, payload) {
    let csrfResponse;
    try { csrfResponse = await fetch('/api/csrf', { credentials: 'same-origin' }); }
    catch { throw Error('Không kết nối được máy chủ. Hãy chạy backend Java và thử lại.'); }
    if (!csrfResponse.ok) throw Error('Không lấy được phiên bảo mật. Hãy tải lại trang.');
    const csrf = await csrfResponse.json();
    if (!csrf.headerName || !csrf.token) throw Error('Phiên bảo mật không hợp lệ. Hãy tải lại trang.');
    const managerLogin = async () => {
      // Reuse Spring Security's role checks, lockout and authenticated session.
      const response = await fetch('/login', {
        method: 'POST', credentials: 'same-origin',
        headers: { [csrf.headerName]: csrf.token },
        body: new URLSearchParams({ username: payload.email, password: payload.password })
      });
      const destination = new URL(response.url || '/login?error', window.location.origin);
      if (response.ok && destination.origin === location.origin && destination.pathname === '/admin') return '/admin';
      if (destination.searchParams.has('lockedUntil')) throw Error('Bạn đã thử đăng nhập quá nhiều lần. Vui lòng thử lại sau 10 phút.');
      if (response.status >= 500) throw Error('Máy chủ chưa xử lý được yêu cầu đăng nhập.');
      if (response.status === 403) throw Error('Phiên bảo mật đã hết hạn. Hãy tải lại trang.');
      throw Error('Tài khoản hoặc mật khẩu không đúng, hoặc tài khoản đã bị khóa.');
    };
    if (kind === 'login' && !payload.email.includes('@')) return managerLogin();
    let response;
    try {
      response = await fetch(`/api/customer/${kind}`, {
        method: 'POST', credentials: 'same-origin',
        headers: { 'Content-Type': 'application/json', [csrf.headerName]: csrf.token },
        body: JSON.stringify(kind === 'login' ? { ...payload, email: payload.email.toLowerCase() } : payload)
      });
    } catch { throw Error('Kết nối bị gián đoạn. Vui lòng kiểm tra lại trước khi gửi tiếp.'); }
    const body = await response.json().catch(() => null);
    if (kind === 'login' && response.status === 400 &&
        ['Email hoặc mật khẩu không đúng', 'Thử lại sau 10 phút'].includes(body?.message)) return managerLogin();
    if (!response.ok || body?.success !== true) {
      if (response.status >= 500) throw Error('Máy chủ chưa xử lý được yêu cầu. Vui lòng thử lại sau.');
      if (response.status === 403) throw Error('Phiên bảo mật đã hết hạn. Hãy tải lại trang và thử lại.');
      throw Error(body?.message || 'Không thực hiện được yêu cầu. Vui lòng thử lại.');
    }
    return '/store/index.html';
  }

  for (const [kind, form] of [['login', login], ['register', register]]) {
    let busy = false;
    form.addEventListener('submit', async event => {
      event.preventDefault();
      if (busy || !form.reportValidity()) return;
      notice.hidden = true;
      const button = form.querySelector('[type="submit"]');
      const label = button.textContent;
      try {
        let payload;
        if (kind === 'register') {
          const password = input('reg-password').value;
          if (password !== input('reg-confirm-password').value) throw Error('Mật khẩu xác nhận không khớp.');
          if (new TextEncoder().encode(password).length > 72) throw Error('Mật khẩu không được vượt quá 72 byte UTF-8.');
          payload = {
            name: `${input('reg-lastname').value.trim()} ${input('reg-firstname').value.trim()}`.trim(),
            phone: input('reg-phone').value.trim(),
            email: input('reg-email').value.trim().toLowerCase(), password
          };
          if (!input('reg-firstname').value.trim() || !input('reg-lastname').value.trim()) throw Error('Vui lòng nhập đầy đủ họ và tên.');
          if (payload.name.length > 150) throw Error('Họ tên tối đa 150 ký tự.');
          if (!payload.phone) throw Error('Vui lòng nhập số điện thoại.');
        } else payload = { email: input('login-email').value.trim(), password: input('login-password').value };
        busy = true;
        button.disabled = true;
        button.textContent = 'Đang xử lý…';
        const destination = await submit(kind, payload);
        form.reset();
        await popup(kind === 'register' ? 'Đăng ký thành công! Vui lòng đăng nhập bằng tài khoản vừa tạo.' : 'Đăng nhập thành công! Chào mừng bạn trở lại.');
        window.location.assign(kind === 'register' ? '/store/login-register.html' : destination);
      } catch (error) {
        notice.textContent = error.message;
        notice.hidden = false;
        notice.scrollIntoView?.({ block: 'nearest', behavior: 'smooth' });
      } finally { busy = false; button.disabled = false; button.textContent = label; }
    });
  }
})();
