(() => {
  'use strict';
  const login=document.querySelector('#client-login-form'),register=document.querySelector('#client-register-form');
  if(!login||!register)return;
  const input=id=>document.getElementById('login-register-'+id);
  const fields={username:'reg-username',email:'reg-email',phone:'reg-phone',fullName:'reg-firstname',firstName:'reg-firstname',lastName:'reg-lastname',password:'reg-password',confirmPassword:'reg-confirm-password',terms:'terms'};
  const notice=document.createElement('div');notice.className='client-auth-notice';notice.setAttribute('role','alert');notice.hidden=true;
  notice.innerHTML='<i class="bi bi-exclamation-circle" aria-hidden="true"></i><div><strong>Chưa thể tiếp tục</strong><p></p></div>';
  document.querySelector('.login-register-wraper .tab-content').before(notice);
  login.noValidate=register.noValidate=true;
  const emailValid=value=>{
    if(value.length>255||!/^[A-Za-z0-9.!#$%&'*+/=?^_`{|}~-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,63}$/.test(value))return false;
    const [local,domain]=value.split('@');return local.length<=64&&!local.startsWith('.')&&!local.endsWith('.')&&!value.includes('..')&&domain.split('.').every(s=>s.length>0&&s.length<=63&&!s.startsWith('-')&&!s.endsWith('-'));
  };
  const normalizePhone=value=>value.trim().replace(/[() .-]/g,'').replace(/^\+84/,'0');
  const nameValid=value=>/^[\p{L}][\p{L}\p{M} '\-]*$/u.test(value)&&value.length<=99;
  const passwordMessage=value=>value.length<12||new TextEncoder().encode(value).length>72||value!==value.trim()||/[\u0000-\u001f\u007f]/.test(value)||!/\p{L}/u.test(value)||!/[0-9]/.test(value)
    ? 'Dùng từ 12 ký tự, có chữ và số, tối đa 72 byte; không có khoảng trắng đầu/cuối.'
    : ['password1234','password12345','password123456','qwerty123456','admin12345678','123456789abc'].includes(value.toLowerCase())?'Mật khẩu quá phổ biến. Hãy chọn mật khẩu riêng khó đoán hơn.':'';
  function notify(message){notice.querySelector('p').textContent=message;notice.hidden=false;notice.scrollIntoView?.({block:'nearest',behavior:'smooth'});}
  function clear(form){form.querySelectorAll('.client-field-error').forEach(n=>n.remove());form.querySelectorAll('[aria-invalid]').forEach(n=>{n.removeAttribute('aria-invalid');n.classList.remove('is-invalid');const ids=(n.getAttribute('aria-describedby')||'').split(' ').filter(id=>id&&!id.endsWith('-error'));if(ids.length)n.setAttribute('aria-describedby',ids.join(' '));else n.removeAttribute('aria-describedby');});}
  function showErrors(form,values){
    clear(form);let first;
    for(const [key,text] of Object.entries(values)){
      const control=form===login?input(key==='email'?'login-email':'login-password'):input(fields[key]);if(!control)continue;
      control.classList.add('is-invalid');control.setAttribute('aria-invalid','true');
      const line=document.createElement('p');line.id=control.id+'-error';line.className='client-field-error';line.textContent=text;(control.closest('.client-auth-password')||control).after(line);
      const hint=(control.getAttribute('aria-describedby')||'').split(' ').filter(id=>id&&!id.endsWith('-error'));
      control.setAttribute('aria-describedby',[...hint,line.id].join(' '));first ||= control;
    }
    notify('Vui lòng kiểm tra các thông tin được đánh dấu.');first?.focus();
  }
  function validate(form){
    clear(form);notice.hidden=true;const errors={};
    if(form===login){
      if(!emailValid(input('login-email').value.trim()))errors.email='Nhập email đúng định dạng.';
      const password=input('login-password').value;if(!password.trim()||new TextEncoder().encode(password).length>72)errors.password='Nhập mật khẩu tài khoản (tối đa 72 byte).';
    }else{
      if(!/^[A-Za-z0-9][A-Za-z0-9_.-]{2,29}$/.test(input('reg-username').value.trim()))errors.username='Tên đăng nhập phải có 3–30 ký tự hợp lệ, bắt đầu bằng chữ hoặc số.';
      if(!emailValid(input('reg-email').value.trim()))errors.email='Nhập email đúng định dạng, ví dụ ten@gmail.com.';
      if(!/^0[35789][0-9]{8}$/.test(normalizePhone(input('reg-phone').value)))errors.phone='Nhập số di động Việt Nam hợp lệ, gồm 10 số.';
      if(!nameValid(input('reg-firstname').value.trim())||input('reg-firstname').value.trim().length>50)errors.firstName='Tên chỉ gồm chữ, tối đa 50 ký tự.';
      if(!nameValid(input('reg-lastname').value.trim()))errors.lastName='Họ và tên đệm chỉ gồm chữ, tối đa 99 ký tự.';
      const password=input('reg-password').value,problem=passwordMessage(password);if(problem)errors.password=problem;
      if(password!==input('reg-confirm-password').value)errors.confirmPassword='Mật khẩu nhập lại chưa khớp.';
      if(!input('terms').checked)errors.terms='Vui lòng đọc và đồng ý với điều khoản trước khi đăng ký.';
    }
    if(Object.keys(errors).length){showErrors(form,errors);return false;}return true;
  }
  function success(title,description,label,destination){
    document.getElementById('client-auth-success')?.remove();const dialog=document.createElement('dialog');dialog.id='client-auth-success';dialog.className='client-auth-success';dialog.setAttribute('aria-labelledby','client-auth-success-title');
    dialog.innerHTML='<i class="bi bi-check-circle" aria-hidden="true"></i><h2 id="client-auth-success-title"></h2><p></p><button type="button" class="btn btn-primary w-100" autofocus></button>';
    dialog.querySelector('h2').textContent=title;dialog.querySelector('p').textContent=description;dialog.querySelector('button').textContent=label;
    const proceed=()=>{dialog.close();dialog.remove();location.assign(destination);};dialog.querySelector('button').onclick=proceed;dialog.addEventListener('cancel',e=>{e.preventDefault();proceed();});document.body.append(dialog);dialog.showModal();
  }
  function safeNext(){const next=new URLSearchParams(location.search).get('next');if(!next)return 'index.html';try{const url=new URL(next,location.href),base=new URL('.',location.href);return url.origin===location.origin&&url.pathname.startsWith(base.pathname)&&!url.pathname.endsWith('login-register.html')?url.pathname+url.search+url.hash:'index.html';}catch{return 'index.html';}}
  const prefill=new URLSearchParams(location.search).get('email');if(prefill&&emailValid(prefill))input('login-email').value=prefill;
  for(const id of ['login-password','reg-password','reg-confirm-password']){
    const control=input(id),wrap=document.createElement('div'),toggle=document.createElement('button');wrap.className='client-auth-password';control.before(wrap);wrap.append(control);toggle.type='button';toggle.className='client-password-toggle';toggle.setAttribute('aria-label','Hiện mật khẩu');toggle.setAttribute('aria-pressed','false');toggle.innerHTML='<i class="bi bi-eye" aria-hidden="true"></i>';
    toggle.onclick=()=>{const show=control.type==='password';control.type=show?'text':'password';toggle.setAttribute('aria-label',show?'Ẩn mật khẩu':'Hiện mật khẩu');toggle.setAttribute('aria-pressed',String(show));toggle.innerHTML=show?'<i class="bi bi-eye-slash" aria-hidden="true"></i>':'<i class="bi bi-eye" aria-hidden="true"></i>';};wrap.append(toggle);
  }
  for(const [id,text] of [['reg-username','3–30 chữ không dấu, số hoặc dấu . _ -; bắt đầu bằng chữ hoặc số.'],['reg-phone','Ví dụ: 0912345678 hoặc +84912345678.'],['reg-password','Từ 12 ký tự, có chữ và số. Có thể dùng cụm từ dễ nhớ; tối đa 72 byte.']]){const control=input(id),hint=document.createElement('small');hint.id=control.id+'-hint';hint.className='client-auth-hint';hint.textContent=text;(control.closest('.client-auth-password')||control).after(hint);control.setAttribute('aria-describedby',hint.id);}
  [login,register].forEach(form=>form.addEventListener('input',e=>{const control=e.target;if(control.hasAttribute('aria-invalid')){control.removeAttribute('aria-invalid');control.classList.remove('is-invalid');document.getElementById(control.id+'-error')?.remove();}}));
  window.ClothingAuthUI={notice,notify,showErrors,validate,success,safeNext,normalizePhone};
})();
