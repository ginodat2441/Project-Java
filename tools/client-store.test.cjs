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

test('checkout offers COD and SePay without an editable amount',async()=>{
 const dom=await page('checkout.html',true,false,cartFixture()),d=dom.window.document;
 assert.deepEqual([...d.querySelectorAll('main select[name="paymentMethod"] option')].map((n)=>n.value),['COD','SEPAY']);
 assert.equal(d.querySelector('main [name="totalAmount"]'),null);
 dom.window.close();
});
test('SePay checkout submits the selected method and displays the returned QR',async()=>{
 const dom=await page('checkout.html',true,false,cartFixture(),{
   '/api/catalog/quote':{subtotal:350000,discount:0,services:[{serviceCode:'EXPRESS',serviceName:'Giao nhanh',fee:30000}]},
   '/api/customer/cart/checkout':{id:123,billCode:'BILL123',totalAmount:380000,paymentQrUrl:'https://example.test/qr.png'},
   '/api/customer/orders/123':{order:{paymentStatus:1}}
 }),w=dom.window,d=w.document;
 const form=d.querySelector('main .checkout-form');
 for(const [name,value] of Object.entries({name:'Khách mới',phone:'0901234567',province:'Hà Nội',ward:'Ba Đình',address:'Số 1'}))
   form.elements.namedItem(name).value=value;
 form.elements.namedItem('paymentMethod').value='SEPAY';
 d.querySelector('[data-quote]').click();
 await tick();
 d.querySelector('.client-shipping-options button').click();
 form.dispatchEvent(new w.Event('submit',{bubbles:true,cancelable:true}));
 for(let i=0;i<20&&!d.querySelector('main img[alt="Mã QR thanh toán SePay"]');i++) await tick();
 const checkout=dom.requests.find((r)=>r.url==='/api/customer/cart/checkout');
 assert.equal(JSON.parse(checkout.options.body).paymentMethod,'SEPAY');
 assert.equal(d.querySelector('main img[alt="Mã QR thanh toán SePay"]').src,'https://example.test/qr.png');
 d.querySelector('.client-sepay-payment button').click();
 await tick();
 assert.match(d.querySelector('.client-sepay-payment [role="status"]').textContent,/đã được xác nhận/);
 assert.equal(d.querySelector('.client-sepay-payment img[alt="Mã QR thanh toán SePay"]').hidden,true);
 dom.window.close();
});
async function page(name, authenticated = true, unavailable = false, cartData = null, responses = {}) {
  const dom = new JSDOM(fs.readFileSync(path.join(root, 'giao dien', name), 'utf8'), {
      url: 'http://localhost/store/' + name,
      runScripts: 'outside-only',
    }),
    w = dom.window;
  dom.requests = [];
  w.localStorage.setItem('clothing-cart', JSON.stringify([{ id: 999, quantity: 3 }]));
  w.fetch = async (url, options = {}) => {
    let data,
      status = 200;
    dom.requests.push({url,options});
    if (url === '/api/csrf') data = {token:'test-token',headerName:'X-CSRF-TOKEN'};
    else if (Object.hasOwn(responses,url)) data=responses[url];
    else if (url === '/api/catalog/quote') data={subtotal:350000,discount:0,services:[]};
    else if (url === '/api/customer/me') {
      if (!authenticated) {
        data = { message: 'Đăng nhập' };
        status = 400;
      } else data = { FULL_NAME: 'Khách mới', EMAIL: 'new@example.test', PHONE: '0901234567' };
    } else if (url === '/api/customer/cart')
      data = cartData || { items: [], subtotal: 0, totalQuantity: 0, checkoutAllowed: false };
    else if (url === '/api/customer/orders' || url === '/api/customer/addresses') data = [];
    else if (url.startsWith('/api/customer/orders/')) data={order:{paymentStatus:0}};
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
