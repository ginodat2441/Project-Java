const { test } = require('node:test'),
  assert = require('node:assert/strict'),
  fs = require('node:fs'),
  path = require('node:path');
const { JSDOM } = require('../.local/dom-tests/node_modules/jsdom');
const root = path.join(__dirname, '..'),
  script = fs.readFileSync(path.join(root, 'client/assets/js/client-store.js'), 'utf8');
const tick = () => new Promise((r) => setImmediate(r));
const cartFixture = () => ({items:[
  {id:12,productDetailId:12,productId:1,productName:'Áo A',colorName:'Đen',sizeName:'M',unitPrice:100000,quantity:2,stock:10,available:1},
  {id:13,productDetailId:13,productId:2,productName:'Áo B',colorName:'Trắng',sizeName:'L',unitPrice:150000,quantity:1,stock:10,available:1}
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
 assert.deepEqual([...d.querySelectorAll('main input[name="paymentMethod"]')].map((n)=>n.value),['COD','SEPAY']);
 assert.equal(d.querySelector('main [name="totalAmount"]'),null);
 dom.window.close();
});
test('new address picker loads Viettel Post provinces and wards through the backend',async()=>{
 const dom=await page('checkout.html',true,false,cartFixture()),w=dom.window,d=w.document;
 const address=d.querySelector('main [name="addressId"]');
 address.value='new';address.dispatchEvent(new w.Event('change'));
 for(let i=0;i<20&&!dom.requests.some((r)=>r.url==='/api/delivery-address/provinces');i++)await tick();
 const addressMessage=d.querySelector('[data-address-message]');
 for(let i=0;i<20&&!addressMessage.hidden;i++)await tick();
 const province=d.querySelector('[data-picker="province"] input[type="search"]');
 province.focus();
 province.value='Hà Nội';province.dispatchEvent(new w.Event('input'));
 for(let i=0;i<20&&!d.querySelector('[data-picker="province"] [role="option"]');i++)await tick();
 const option=d.querySelector('[data-picker="province"] [role="option"]');
 assert.ok(option,addressMessage.textContent);
 option.click();
 for(let i=0;i<20&&!dom.requests.some((r)=>r.url==='/api/delivery-address/wards?province=01');i++)await tick();
 assert.ok(dom.requests.some((r)=>r.url==='/api/delivery-address/provinces'));
 assert.ok(dom.requests.some((r)=>r.url==='/api/delivery-address/wards?province=01'));
 dom.window.close();
});
test('SePay checkout submits the selected method and displays the returned QR',async()=>{
 const dom=await page('checkout.html',true,false,cartFixture(),{
   '/api/bills':{id:123,billCode:'BILL123',paymentMethod:'SEPAY',totalAmount:380000},
   '/api/bills/123/sepay-payment':{qrUrl:'https://example.test/qr?des=BILL123'},
   '/api/bills/123':{paymentMethod:'SEPAY',paymentStatus:1,status:0}
 }),w=dom.window,d=w.document;
 const form=d.querySelector('main .checkout-form');
 form.querySelector('[name="paymentMethod"][value="SEPAY"]').checked=true;
 form.dispatchEvent(new w.Event('submit',{bubbles:true,cancelable:true}));
 for(let i=0;i<20&&!d.querySelector('main img[alt="Mã QR thanh toán SePay"]');i++) await tick();
 const checkout=dom.requests.find((r)=>r.url==='/api/bills' && r.options.method==='POST');
 assert.equal(JSON.parse(checkout.options.body).paymentMethod,'SEPAY');
 assert.equal(d.querySelector('main img[alt="Mã QR thanh toán SePay"]').src,'https://example.test/qr?des=BILL123');
 d.querySelector('main section[role="status"] button').click();
 await tick();
 assert.match(d.querySelector('main section[role="status"] p[role="status"]').textContent,/Đã thanh toán/);
 assert.equal(d.querySelector('main section[role="status"] img[alt="Mã QR thanh toán SePay"]').hidden,true);
 dom.window.close();
});
async function page(name, authenticated = true, unavailable = false, cartData = null, responses = {}) {
  const dom = new JSDOM(fs.readFileSync(path.join(root, 'client', name.split('?')[0]), 'utf8'), {
      url: 'http://localhost:8081/' + name,
      runScripts: 'outside-only',
    }),
    w = dom.window;
  dom.requests = [];
  w.localStorage.setItem('clothing-cart', JSON.stringify([{ id: 999, quantity: 3 }]));
  if (authenticated) w.sessionStorage.setItem('clothingShopToken','test-token');
  w.fetch = async (url, options = {}) => {
    let data, status = 200;
    dom.requests.push({url,options});
    const route = new URL(url,w.location.href).pathname;
    if (Object.hasOwn(responses,route)) data = responses[route];
    else if (route === '/api/auth/me') {
      if (!authenticated) status = 401;
      else data = {customerId:1,fullName:'Khách mới',email:'new@example.test',phone:'0901234567'};
    } else if (route.startsWith('/api/carts/'))
      data = cartData || {items:[], totalItems:0,totalAmount:0};
    else if (route === '/api/bills/checkout-quote') data = {subtotal:350000,shippingFee:30000,totalAmount:380000};
    else if (route === '/api/bills/123/sepay-payment' || route === '/api/bills' || route === '/api/products/categories') data = [];
    else if (route === '/api/vouchers') data = [];
    else if (route === '/api/delivery-address/provinces') data = [{code:'01',name:'Hà Nội',id:1}];
    else if (route === '/api/delivery-address/wards') data = ['Ba Đình'];
    else if (route === '/api/addresses') data = name === 'checkout.html'
      ? [{id:1,isDefault:true,receiverName:'Khách mới',receiverPhone:'0901234567',province:'Hà Nội',ward:'Ba Đình',detailAddress:'Số 1'}] : [];
    else if (route === '/api/products' || route === '/api/brands') data = {items:[],total:0,size:20};
    else throw Error('Unexpected API ' + url);
    if (unavailable) status = 500;
    return {ok:status===200,status,json:async()=>({success:status===200,data})};
  };
  w.eval(script);
  for (let i = 0; i < 60 && !w.document.body.classList.contains('client-ready'); i++) await tick();
  assert.ok(w.document.body.classList.contains('client-ready'));
  return dom;
}
test('signed-in header shows logout, never login/register, and ignores shared browser cart', async () => {
  const dom = await page('index.html'),
    d = dom.window.document;
  assert.ok(d.querySelector('[data-client-logout]'));
  assert.equal(d.querySelector('.account-dropdown a[href="login-register.html"]'), null);
  assert.doesNotMatch(d.querySelector('main').textContent, /Giỏ hàng của người khác/);
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
  const dom = await page('account.html', true, true),
    d = dom.window.document;
  assert.doesNotMatch(d.querySelector('#account').textContent, /Sarah|ORD-2024/);
  assert.ok(d.querySelector('[role="alert"]'));
  assert.equal(d.querySelector('#email').value, '');
  dom.window.close();
});
test('category has real filter controls and no template merchandise for an empty API result', async () => {
  const dom = await page('category.html?categoryId=1'),
    d = dom.window.document;
  assert.ok(d.querySelector('.client-filters [name="categoryId"]'));
  assert.equal(d.querySelectorAll('main .product-box:not([hidden])').length, 0);
  dom.window.close();
});
