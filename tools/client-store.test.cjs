const { test } = require('node:test'),
  assert = require('node:assert/strict'),
  fs = require('node:fs'),
  path = require('node:path');
const { JSDOM } = require('../.local/dom-tests/node_modules/jsdom');
const root = path.join(__dirname, '..'),
  script = fs.readFileSync(path.join(root, 'giao dien/assets/js/client-store.js'), 'utf8');
const tick = () => new Promise((r) => setImmediate(r));
const cartFixture = () => ({items:[
  {productDetailId:12,productId:1,productName:'Áo A',colorName:'Đen',sizeName:'M',price:100000,quantity:2,stock:10,available:true},
  {productDetailId:13,productId:2,productName:'Áo B',colorName:'Trắng',sizeName:'L',price:150000,quantity:1,stock:10,available:true}
],subtotal:350000,totalQuantity:3,checkoutAllowed:true});

test('checkbox selection recalculates payable subtotal and persists selected IDs only',async()=>{
 const dom=await page('cart.html',true,false,cartFixture()),w=dom.window,d=w.document;
 const checks=[...d.querySelectorAll('.client-cart-select')];assert.equal(checks.length,2);
 checks[1].checked=false;checks[1].dispatchEvent(new w.Event('change'));
 assert.equal(d.querySelector('.summary-total .summary-value').textContent,'200.000 ₫');
 assert.match(d.querySelector('.checkout-button a').href,/items=12$/);
 checks[0].checked=false;checks[0].dispatchEvent(new w.Event('change'));
 assert.equal(d.querySelector('.summary-total .summary-value').textContent,'0 ₫');
 assert.equal(d.querySelector('.checkout-button a').getAttribute('aria-disabled'),'true');
 dom.window.close();
});

test('checkout uses fixed COD and has no editable amount or payment-method selector',async()=>{
 const dom=await page('checkout.html',true,false,cartFixture()),d=dom.window.document;
 assert.equal(d.querySelector('main select[name="paymentMethod"]'),null);
 assert.equal(d.querySelector('main [name="paymentMethod"]').value,'COD');
 assert.ok(d.querySelector('main input[readonly]'));
 assert.equal(d.querySelector('main [name="totalAmount"]'),null);
 dom.window.close();
});
async function page(name, authenticated = true, unavailable = false, cartData = null) {
  const dom = new JSDOM(fs.readFileSync(path.join(root, 'giao dien', name), 'utf8'), {
      url: 'http://localhost/store/' + name,
      runScripts: 'outside-only',
    }),
    w = dom.window;
  w.localStorage.setItem('clothing-cart', JSON.stringify([{ id: 999, quantity: 3 }]));
  w.fetch = async (url) => {
    let data,
      status = 200;
    if (url === '/api/customer/me') {
      if (!authenticated) {
        data = { message: 'Đăng nhập' };
        status = 400;
      } else data = { FULL_NAME: 'Khách mới', EMAIL: 'new@example.test', PHONE: '0901234567' };
    } else if (url === '/api/customer/cart')
      data = cartData || { items: [], subtotal: 0, totalQuantity: 0, checkoutAllowed: false };
    else if (url === '/api/customer/orders' || url === '/api/customer/addresses') data = [];
    else if (url.startsWith('/api/catalog?')) data = { items: [], total: 0, size: 20 };
    else if (url === '/api/catalog/categories') data = [];
    else if (url === '/api/catalog/filters') data = { brands: [], colors: [] };
    else throw Error('Unexpected API ' + url);
    if (unavailable) {
      data = {};
      status = 500;
    }
    return { ok: status === 200, status, json: async () => data };
  };
  w.eval(script);
  for (let i = 0; i < 60 && !w.document.body.classList.contains('client-ready'); i++) await tick();
  assert.ok(w.document.body.classList.contains('client-ready'));
  return dom;
}
test('signed-in header shows logout, never login/register, and discards shared browser cart', async () => {
  const dom = await page('index.html'),
    d = dom.window.document;
  assert.ok(d.querySelector('[data-client-logout]'));
  assert.equal(d.querySelector('.account-dropdown a[href="login-register.html"]'), null);
  assert.equal(dom.window.localStorage.getItem('clothing-cart'), null);
  dom.window.close();
});
test('new customer sees own identity with empty orders, addresses and no saved demo profile', async () => {
  const dom = await page('account.html'),
    d = dom.window.document;
  assert.equal(d.querySelector('.user-info h4').textContent, 'Khách mới');
  assert.match(d.querySelector('.orders-grid').textContent, /chưa có đơn/);
  assert.match(d.querySelector('#addresses').textContent, /chưa lưu địa chỉ/);
  assert.equal(d.querySelector('#firstName').value, 'mới');
  assert.doesNotMatch(d.querySelector('#account').textContent, /Sarah|ORD-2024/);
  dom.window.close();
});
test('guest cart never displays sample rows or another browser cart', async () => {
  const dom = await page('cart.html', false),
    d = dom.window.document;
  assert.equal(d.querySelectorAll('main .cart-item').length, 0);
  assert.match(d.querySelector('.client-cart-empty').textContent, /Đăng nhập/);
  assert.equal(d.querySelector('.summary-total .summary-value').textContent, '0 ₫');
  dom.window.close();
});
test('database failure clears sample account data and exposes an error', async () => {
  const dom = await page('account.html', false, true),
    d = dom.window.document;
  assert.doesNotMatch(d.querySelector('#account').textContent, /Sarah|ORD-2024/);
  assert.ok(d.querySelector('[role="alert"]'));
  assert.equal(d.querySelector('#email').value, '');
  dom.window.close();
});
test('category has real filter controls and no template merchandise for an empty API result', async () => {
  const dom = await page('category.html'),
    d = dom.window.document;
  assert.ok(d.querySelector('.client-filters [name="categoryId"]'));
  assert.equal(d.querySelectorAll('main .product-box:not([hidden])').length, 0);
  dom.window.close();
});
