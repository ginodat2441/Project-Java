const { test } = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const { JSDOM } = require('../.local/dom-tests/node_modules/jsdom');
const root = path.join(__dirname, '..');
const html = fs.readFileSync(path.join(root, 'giao dien/login-register.html'), 'utf8');
const script = fs.readFileSync(path.join(root, 'giao dien/assets/js/client-auth.js'), 'utf8');
const tick = () => new Promise(r => setImmediate(r));
async function setup(kind, status = 200) {
  const dom = new JSDOM(html, { url: 'http://localhost/store/login-register.html', runScripts: 'outside-only' });
  const w = dom.window, calls = [], alerts = [];
  w.TextEncoder = TextEncoder;
  w.alert = text => alerts.push(text);
  w.fetch = async (url, options = {}) => {
    calls.push({ url, ...options });
    return { ok: url === '/api/csrf' || status === 200, status: url === '/api/csrf' ? 200 : status,
      json: async () => url === '/api/csrf' ? { token: 'csrf-test', headerName: 'X-CSRF-TOKEN' } : status === 200 ? { success: true } : { message: 'Email đã được đăng ký.' } };
  };
  w.eval(script);
  const values = kind === 'register' ? { 'reg-firstname': 'Khải', 'reg-lastname': 'Đào', 'reg-phone': '0901234567', 'reg-email': 'TEST@example.test', 'reg-password': 'Password@12345', 'reg-confirm-password': 'Password@12345' } : { 'login-email': 'TEST@example.test', 'login-password': 'Password@12345' };
  for (const [id, value] of Object.entries(values)) w.document.getElementById('login-register-' + id).value = value;
  w.document.getElementById('login-register-terms').checked = true;
  const form = w.document.querySelector(kind === 'register' ? '#login-register-registration-form form' : '#login-register-login-form form');
  return { dom, w, form, calls, alerts };
}
for (const kind of ['register', 'login']) test(kind + ' sends real API payload with CSRF and only announces success after response', async () => {
  const { dom, w, form, calls, alerts } = await setup(kind);
  form.dispatchEvent(new w.Event('submit', { cancelable: true }));
  await tick(); await tick();
  const write = calls.find(c => c.method === 'POST');
  assert.equal(write.url, '/api/customer/' + kind);
  assert.equal(write.headers['X-CSRF-TOKEN'], 'csrf-test');
  assert.equal(JSON.parse(write.body).email, 'test@example.test');
  if (kind === 'register') assert.equal(JSON.parse(write.body).name, 'Đào Khải');
  assert.equal(alerts.length, 1);
  assert.equal(w.localStorage.length, 0);
  dom.window.close();
});
test('mismatched confirmation never calls API', async () => {
  const { dom, w, form, calls } = await setup('register');
  w.document.getElementById('login-register-reg-confirm-password').value = 'OtherPassword123';
  form.dispatchEvent(new w.Event('submit', { cancelable: true })); await tick();
  assert.equal(calls.length, 0); assert.match(w.document.querySelector('[role="alert"]').textContent, /không khớp/);
  dom.window.close();
});
test('server rejection never shows success and allows retry', async () => {
  const { dom, w, form, alerts } = await setup('register', 400);
  form.dispatchEvent(new w.Event('submit', { cancelable: true })); await tick(); await tick();
  assert.equal(alerts.length, 0); assert.equal(form.querySelector('[type="submit"]').disabled, false);
  assert.match(w.document.querySelector('[role="alert"]').textContent, /đã được đăng ký/);
  dom.window.close();
});

test('manager username uses Spring Security form login with CSRF and preserves username case', async () => {
  const { dom, w, form, calls, alerts } = await setup('login');
  w.document.getElementById('login-register-login-email').value = 'TestAdmin';
  w.fetch = async (url, options = {}) => {
    calls.push({url,...options});
    return url === '/api/csrf'
      ? {ok:true,json:async()=>({token:'csrf-test',headerName:'X-CSRF-TOKEN'})}
      : {ok:true,status:200,url:'http://localhost/admin'};
  };
  form.dispatchEvent(new w.Event('submit', {cancelable:true}));await tick();await tick();
  const request=calls.find(c=>c.method==='POST');
  assert.equal(request.url,'/login');
  assert.equal(request.body.get('username'),'TestAdmin');
  assert.equal(request.headers['X-CSRF-TOKEN'],'csrf-test');
  assert.equal(calls.some(c=>c.url==='/api/customer/login'),false);
  assert.equal(alerts.length,1);dom.window.close();
});

test('redirect to login error never shows a manager success popup', async () => {
  const { dom, w, form, alerts } = await setup('login');
  w.document.getElementById('login-register-login-email').value = 'TestAdmin';
  w.fetch = async url => url === '/api/csrf'
    ? {ok:true,json:async()=>({token:'csrf-test',headerName:'X-CSRF-TOKEN'})}
    : {ok:true,status:200,url:'http://localhost/login?error'};
  form.dispatchEvent(new w.Event('submit', {cancelable:true}));await tick();await tick();
  assert.equal(alerts.length,0);assert.match(w.document.querySelector('[role="alert"]').textContent,/không đúng/);
  dom.window.close();
});

test('email-shaped manager username can still use manager authentication', async () => {
  const { dom, w, form, alerts } = await setup('login');
  let managerCalls=0;
  w.fetch=async url=>{
    if(url==='/api/csrf')return {ok:true,json:async()=>({token:'csrf-test',headerName:'X-CSRF-TOKEN'})};
    if(url==='/api/customer/login')return {ok:false,status:400,json:async()=>({message:'Thử lại sau 10 phút'})};
    assert.equal(url,'/login');managerCalls++;return {ok:true,status:200,url:'http://localhost/admin'};
  };
  form.dispatchEvent(new w.Event('submit',{cancelable:true}));await tick();await tick();
  assert.equal(managerCalls,1);assert.equal(alerts.length,1);dom.window.close();
});
