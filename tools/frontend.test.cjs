const {test}=require('node:test');
const assert=require('node:assert/strict');
const fs=require('node:fs');
const path=require('node:path');
const {JSDOM}=require('../.local/dom-tests/node_modules/jsdom');
const root=path.resolve(__dirname,'..');
const staticRoot=path.join(root,'backend_java/src/main/resources/static');
const tick=()=>new Promise(r=>setImmediate(r));
async function waitFor(fn){for(let i=0;i<50;i++){if(fn())return;await tick();}throw Error('DOM did not reach expected state');}
const response=data=>({ok:true,status:200,json:async()=>data,text:async()=>JSON.stringify(data)});
test('Order issue picker opens linked sale document with order code without posting or deducting stock',async()=>{
 const dom=new JSDOM(fs.readFileSync(path.join(staticRoot,'admin/index.html'),'utf8'),{url:'http://localhost/admin',runScripts:'dangerously'}),w=dom.window,d=w.document;let mutations=0,requested='';
 w.HTMLDialogElement.prototype.showModal=function(){this.open=true;};w.HTMLDialogElement.prototype.close=function(){this.open=false;};
 w.fetch=async(url,options={})=>{
  if(options.method&&options.method!=='GET')mutations++;
  if(url==='/api/csrf')return response({token:'test',headerName:'X-XSRF-TOKEN'});
  if(url==='/api/admin/me')return response({username:'qa',roles:[{authority:'ROLE_ADMIN'}]});
  if(url==='/api/admin/lookups')return response({brands:[],materials:[],colors:[],categories:[]});
  if(url.startsWith('/api/admin/dashboard'))return response({revenue:[],statuses:[],topProducts:[],lowStock:[]});
  if(url.startsWith('/api/admin/inventory/documents?'))return response({items:[],total:0,size:20});
  if(url.startsWith('/api/admin/inventory/order-issues?')){requested=url;return response([{BILL_ID:12,BILL_CODE:'DEMO-HD-012',RECEIVER_NAME:'Nguyễn An',ORDER_STATUS:2,DOCUMENT_ID:9,DOC_CODE:'BH000009'}]);}
  if(url==='/api/admin/inventory/documents/9')return response({document:{ID:9,DOC_CODE:'BH000009',DOC_TYPE:'SALE',STATUS:'POSTED',BILL_ID:12,BILL_CODE:'DEMO-HD-012',RECEIVER_NAME:'Nguyễn An',ORDER_STATUS:2},lines:[{SKU:'SP001-DEN-M',QUANTITY:2,STOCK_DELTA:-2}]});
  throw Error('Unexpected '+url);
 };
 installAdminScripts(w);await waitFor(()=>d.querySelector('[data-section=documents]'));d.querySelector('[data-section=documents]').click();await waitFor(()=>d.querySelector('[data-action=order-issue]'));d.querySelector('[data-action=order-issue]').click();await waitFor(()=>d.querySelector('#issue-order-query')&&!d.querySelector('#issue-order-search').disabled);d.querySelector('#issue-order-query').value='DEMO-HD-012';d.querySelector('#issue-order-search').click();await waitFor(()=>requested.includes('DEMO-HD-012'));await tick();d.querySelector('[data-action=choose-order-issue]').click();await waitFor(()=>d.querySelector('[data-action=open-linked-order]'));assert.equal(d.querySelector('[data-action=open-linked-order]').dataset.id,'12');assert.match(d.querySelector('#fields').textContent,/DEMO-HD-012/);assert.match(d.querySelector('#fields').textContent,/Nguyễn An/);assert.match(d.querySelector('#fields').textContent,/SP001-DEN-M/);assert.equal(mutations,0);assert.equal(d.querySelector('#editor footer').hidden,true);dom.window.close();
});
test('Admin can complete missing opening costs without replacing recorded receipt prices or stock',async()=>{
 const dom=new JSDOM(fs.readFileSync(path.join(staticRoot,'admin/index.html'),'utf8'),{url:'http://localhost/admin',runScripts:'outside-only'}),w=dom.window,d=w.document;let saved;
 w.HTMLDialogElement.prototype.showModal=function(){this.open=true;};w.HTMLDialogElement.prototype.close=function(){this.open=false;};
 w.fetch=async(url,options={})=>{
  if(url==='/api/csrf')return response({token:'test',headerName:'X-XSRF-TOKEN'});
  if(url==='/api/admin/me')return response({username:'qa',roles:[{authority:'ROLE_ADMIN'}]});
  if(url==='/api/admin/lookups')return response({brands:[],materials:[],colors:[],categories:[]});
  if(url.startsWith('/api/admin/dashboard'))return response({revenue:[],statuses:[],topProducts:[],lowStock:[]});
  if(url.startsWith('/api/admin/stock?'))return response({items:[{ID:61,PRODUCT_ID:1,PRODUCT_CODE:'SP001',QUANTITY:200,AVERAGE_COST:saved?72561.1:null}],total:1,size:30,page:1,totals:{},globalTotals:{},products:[]});
  if(url==='/api/admin/opening-cost/1'&&options.method==='POST'){saved=JSON.parse(options.body);return response(null);}
  if(url==='/api/admin/opening-cost/1')return response({product:{PRODUCT_CODE:'SP001',PRODUCT_NAME:'Áo'},quantity:200,openingQuantity:90,receipts:[{ID:1,DOC_CODE:'NK1',SKU:'SP001-M',QUANTITY:10,UNIT_COST:null},{ID:2,DOC_CODE:'NK2',SKU:'SP001-M',QUANTITY:100,UNIT_COST:100000}]});
  throw Error('Unexpected '+url);
 };
 w.eval(fs.readFileSync(path.join(staticRoot,'admin-assets/admin.js'),'utf8'));await waitFor(()=>d.querySelector('[data-section=stock]'));d.querySelector('[data-section=stock]').click();await waitFor(()=>d.querySelector('[data-action=opening-cost]'));d.querySelector('[data-action=opening-cost]').click();await waitFor(()=>d.querySelector('[name=openingUnitCost]'));
 assert.equal(d.querySelector('[data-cost-line="2"]'),null);assert.match(d.querySelector('#fields').textContent,/90/);assert.equal(d.querySelector('[name=openingUnitCost]').required,true);
 d.querySelector('[name=openingUnitCost]').value='50000';d.querySelector('[data-cost-line="1"]').value='1222';d.querySelector('[data-cost-line="1"]').dispatchEvent(new w.Event('input',{bubbles:true}));assert.match(d.querySelector('#opening-cost-preview').textContent,/14.512.220/);
 d.querySelector('[name=reason]').value='Opening invoice';d.querySelector('#editor').dispatchEvent(new w.Event('submit',{cancelable:true}));await waitFor(()=>saved);assert.deepEqual(saved,{openingUnitCost:50000,reason:'Opening invoice',receipts:[{lineId:1,unitCost:1222}]});await waitFor(()=>!d.querySelector('#dialog').open&&!d.querySelector('#editor footer button').disabled&&!d.querySelector('[data-action=opening-cost]'));dom.window.close();
});
test('Stock valuation keeps global total while filtering product and its value range',async()=>{
 const dom=new JSDOM(fs.readFileSync(path.join(staticRoot,'admin/index.html'),'utf8'),{url:'http://localhost/admin',runScripts:'outside-only'}),w=dom.window,d=w.document;let requested='';
 w.fetch=async url=>{
  if(url==='/api/csrf')return response({token:'test',headerName:'X-XSRF-TOKEN'});
  if(url==='/api/admin/me')return response({username:'qa',roles:[{authority:'ROLE_ADMIN'}]});
  if(url==='/api/admin/lookups')return response({brands:[],materials:[],colors:[],categories:[]});
  if(url.startsWith('/api/admin/dashboard'))return response({revenue:[],statuses:[],topProducts:[],lowStock:[]});
  if(url.startsWith('/api/admin/stock?')){requested=url;return response({items:[{ID:1,PRODUCT_CODE:'SP001',PRODUCT_NAME:'Áo',QUANTITY:2,RESERVED:0,PHYSICAL:2,AVERAGE_COST:50000,INVENTORY_VALUE:100000,PRODUCT_INVENTORY_VALUE:200000}],total:1,size:30,page:1,products:[{ID:5,NAME:'SP001 · Áo'}],globalTotals:{INVENTORY_VALUE:900000},totals:{INVENTORY_VALUE:200000}});}
  throw Error('Unexpected '+url);
 };
 w.eval(fs.readFileSync(path.join(staticRoot,'admin-assets/admin.js'),'utf8'));await waitFor(()=>d.querySelector('[data-section=stock]'));d.querySelector('[data-section=stock]').click();await waitFor(()=>d.querySelector('#stock-totals'));
 assert.match(d.querySelector('.metrics').textContent,/900/);assert.match(d.querySelector('#stock-totals').textContent,/200/);assert.match(d.querySelector('thead').textContent,/Giữ cho đơn/);
 d.querySelector('[name=productId]').value='5';d.querySelector('[name=minValue]').value='100000';d.querySelector('[name=maxValue]').value='300000';d.querySelector('[name=threshold]').value='0';d.querySelector('#filter').dispatchEvent(new w.Event('submit',{cancelable:true,bubbles:true}));await waitFor(()=>requested.includes('productId=5')&&requested.includes('minValue=100000')&&requested.includes('maxValue=300000')&&requested.includes('threshold=0'));await tick();
 assert.match(d.querySelector('.metrics').textContent,/900/);d.querySelector('#clear-stock-filter').click();await waitFor(()=>!requested.includes('productId=')&&!requested.includes('minValue='));await tick();dom.window.close();
});
test('Product list shows current inventory value and all-page revenue totals with revenue-only date filters',async()=>{
 const dom=new JSDOM(fs.readFileSync(path.join(staticRoot,'admin/index.html'),'utf8'),{url:'http://localhost/admin',runScripts:'outside-only'}),w=dom.window,d=w.document;let requested='';
 w.fetch=async url=>{
  if(url==='/api/csrf')return response({token:'test',headerName:'X-XSRF-TOKEN'});
  if(url==='/api/admin/me')return response({username:'qa',roles:[{authority:'ROLE_ADMIN'}]});
  if(url==='/api/admin/lookups')return response({brands:[],materials:[],colors:[],categories:[]});
  if(url.startsWith('/api/admin/dashboard'))return response({revenue:[],statuses:[],topProducts:[],lowStock:[]});
  if(url.startsWith('/api/admin/products?')){requested=url;return response({items:[{ID:1,PRODUCT_CODE:'SP001',PRODUCT_NAME:'Áo',STOCK:2,INVENTORY_VALUE:100000,TOTAL_REVENUE:url.includes('from=')?0:300000,STATUS:1}],total:25,size:20,page:1,totals:{INVENTORY_VALUE:700000,TOTAL_REVENUE:url.includes('from=')?0:900000,UNKNOWN_COST_PRODUCTS:0}});}
  throw Error('Unexpected '+url);
 };
 w.eval(fs.readFileSync(path.join(staticRoot,'admin-assets/admin.js'),'utf8'));await waitFor(()=>d.querySelector('[data-section=products]'));d.querySelector('[data-section=products]').click();await waitFor(()=>d.querySelector('#product-totals'));
 assert.match(d.querySelector('#content').textContent,/Giá trị tồn kho/);assert.match(d.querySelector('#content').textContent,/Tổng doanh thu/);assert.match(d.querySelector('#product-totals').textContent,/700/);assert.match(d.querySelector('#product-totals').textContent,/900/);
 d.querySelector('[name=from]').value='2026-01-01';d.querySelector('[name=to]').value='2026-01-31';d.querySelector('#filter').dispatchEvent(new w.Event('submit',{cancelable:true,bubbles:true}));await waitFor(()=>requested.includes('from=2026-01-01')&&requested.includes('to=2026-01-31'));await tick();
 assert.match(d.querySelector('#product-totals').textContent,/700/);assert.doesNotMatch(d.querySelector('#product-totals').textContent,/900/);assert.match(d.querySelector('.report-note').textContent,/hiện tại/);dom.window.close();
});
function installAdminScripts(w){for(const file of ['operations.js','admin.js']){const script=w.document.createElement('script');script.textContent=fs.readFileSync(path.join(staticRoot,'admin-assets',file),'utf8');w.document.head.append(script);}}

test('Password visibility supports login and dynamically added admin inputs without submitting',async()=>{
 const dom=new JSDOM('<body><form><label>Mật khẩu<input type="password" name="password"></label></form></body>',{url:'http://localhost/login',runScripts:'outside-only'}),w=dom.window,d=w.document;
 w.eval(fs.readFileSync(path.join(staticRoot,'admin-assets/password-visibility.js'),'utf8'));
 const input=d.querySelector('input'),toggle=d.querySelector('.password-toggle');assert.equal(toggle.type,'button');toggle.click();assert.equal(input.type,'text');assert.equal(toggle.getAttribute('aria-pressed'),'true');toggle.click();assert.equal(input.type,'password');
 const dynamic=d.createElement('input');dynamic.type='password';d.querySelector('form').append(dynamic);await waitFor(()=>d.querySelectorAll('.password-toggle').length===2);assert.equal(d.querySelectorAll('.password-input').length,2);dom.window.close();
});

test('Reports submit period and date filters and show revenue by variant',async()=>{
 const dom=new JSDOM(fs.readFileSync(path.join(staticRoot,'admin/index.html'),'utf8'),{url:'http://localhost/admin',runScripts:'dangerously'}),w=dom.window,d=w.document;let requested='';
 w.HTMLDialogElement.prototype.showModal=function(){this.open=true;};w.HTMLDialogElement.prototype.close=function(){this.open=false;};
 const totals={NET:1000,RETURN_VALUE:200,NET_AFTER_RETURN:800,SOLD:2,RETURNED:1,ORDERS:1,SHIPPING:30000,GROSS:1200,DISCOUNT:200};
 w.fetch=async url=>{
  if(url==='/api/csrf')return response({token:'test',headerName:'X-XSRF-TOKEN'});if(url==='/api/admin/me')return response({username:'qa',roles:[{authority:'ROLE_ADMIN'}]});if(url==='/api/admin/lookups')return response({brands:[],materials:[],colors:[],categories:[]});
  if(url.startsWith('/api/admin/dashboard'))return response({revenue:[],statuses:[],topProducts:[],lowStock:[]});
  if(url.startsWith('/api/admin/reports?')){requested=url;return response({from:'2026-01-01',to:'2026-12-31',totals,series:[{PERIOD:'2026-Q1',...totals}],products:{items:[{ID:1,PRODUCT_CODE:'SP001',PRODUCT_NAME:'Áo',...totals}],total:1,size:20},variants:[{SKU:'SP001-DEN-M',COLOR_NAME:'Đen',SIZE_NAME:'M',...totals}]});}
  throw Error('Unexpected '+url);
 };
 installAdminScripts(w);await waitFor(()=>d.querySelector('[data-section=reports]'));d.querySelector('[data-section=reports]').click();await waitFor(()=>d.querySelector('[name=period]')||d.querySelector('#notice').className==='error');assert.notEqual(d.querySelector('#notice').className,'error',d.querySelector('#notice').textContent);
 assert.match(d.querySelector('#content').textContent,/Doanh thu ròng/);assert.match(d.querySelector('#content').textContent,/2026-Q1/);
 d.querySelector('[name=period]').value='DAY';d.querySelector('[name=year]').value='2025';d.querySelector('[name=month]').value='2';d.querySelector('#filter').dispatchEvent(new w.Event('submit',{cancelable:true,bubbles:true}));await waitFor(()=>requested.includes('period=DAY')&&requested.includes('year=2025')&&requested.includes('month=2')).catch(()=>{throw Error(requested+'; '+d.querySelector('#notice').textContent);});
 await waitFor(()=>d.querySelector('[data-action=product-report]'));d.querySelector('[data-action=product-report]').click();await waitFor(()=>d.querySelector('#dialog').open);assert.match(d.querySelector('#fields').textContent,/SP001-DEN-M/);dom.window.close();
});

test('Inventory UI saves a draft then requires explicit confirmation before posting stock',async()=>{
 const dom=new JSDOM(fs.readFileSync(path.join(staticRoot,'admin/index.html'),'utf8'),{url:'http://localhost/admin',runScripts:'dangerously'}),w=dom.window,d=w.document;let created,posted=0;
 w.HTMLDialogElement.prototype.showModal=function(){this.open=true;};w.HTMLDialogElement.prototype.close=function(){this.open=false;};
 w.fetch=async(url,options={})=>{
  if(url==='/api/csrf')return response({token:'test',headerName:'X-XSRF-TOKEN'});if(url==='/api/admin/me')return response({username:'qa',roles:[{authority:'ROLE_ADMIN'}]});if(url==='/api/admin/lookups')return response({brands:[],materials:[],colors:[],categories:[]});if(url.startsWith('/api/admin/dashboard'))return response({revenue:[],statuses:[],topProducts:[],lowStock:[]});
  if(url.startsWith('/api/admin/inventory/documents?'))return response({items:created?[{ID:5,DOC_CODE:'NK000005',DOC_TYPE:'RECEIPT',STATUS:posted?'POSTED':'DRAFT'}]:[],total:created?1:0,size:20});
  if(url==='/api/admin/inventory/variants')return response([{ID:9,SKU:'SP001-DEN-M',PRODUCT_NAME:'Áo',QUANTITY:3}]);
  if(url==='/api/admin/inventory/documents'&&options.method==='POST'){created=JSON.parse(options.body);return response(5);}
  if(url==='/api/admin/inventory/documents/5')return response({document:{ID:5,DOC_CODE:'NK000005',DOC_TYPE:'RECEIPT',STATUS:posted?'POSTED':'DRAFT',CREATOR:'qa',REASON:'Nhập hàng'},lines:[{PRODUCT_DETAIL_ID:9,SKU:'SP001-DEN-M',QUANTITY:2,BEFORE_QUANTITY:posted?3:null,AFTER_QUANTITY:posted?5:null,STOCK_DELTA:posted?2:null}]});
  if(url==='/api/admin/inventory/documents/5/approve'){posted++;return response(null);}throw Error('Unexpected '+url);
 };
 installAdminScripts(w);await waitFor(()=>d.querySelector('[data-section=documents]'));d.querySelector('[data-section=documents]').click();await waitFor(()=>d.querySelector('[data-action=create]'));d.querySelector('[data-action=create]').click();await waitFor(()=>d.querySelector('.document-line'));
 d.querySelector('[name=reason]').value='Nhập hàng';d.querySelector('[name=variantId]').value='9';d.querySelector('[name=lineQuantity]').value='2';d.querySelector('[name=unitCost]').value='125000.50';d.querySelector('[name=unitCost]').dispatchEvent(new w.Event('input',{bubbles:true}));assert.match(d.querySelector('#receipt-total').textContent,/250/);d.querySelector('#editor').dispatchEvent(new w.Event('submit',{cancelable:true}));await waitFor(()=>created);assert.equal(posted,0);assert.equal(created.lines[0].variantId,9);assert.equal(created.lines[0].unitCost,125000.50);await waitFor(()=>d.querySelector('[data-action=detail]'));
 d.querySelector('[data-action=detail]').click();await waitFor(()=>d.querySelector('[data-action=approve-document]'));d.querySelector('[data-action=approve-document]').click();assert.equal(posted,0);d.querySelector('#confirm-document-action').click();await waitFor(()=>posted===1);await waitFor(()=>d.querySelector('#dialog').open&&!d.querySelector('[data-action=approve-document]'));assert.match(d.querySelector('#fields').textContent,/Đã xác nhận/);dom.window.close();
});

test('Reset page consumes fragment token without displaying it and clears password after success',async()=>{
 const dom=new JSDOM(fs.readFileSync(path.join(staticRoot,'reset-password.html'),'utf8'),{url:'http://localhost/reset-password#token=one-use-test-token',runScripts:'outside-only'}),w=dom.window,d=w.document;let request;
 w.fetch=async(url,options={})=>{if(url==='/api/csrf')return response({token:'csrf',headerName:'X-XSRF-TOKEN'});request=JSON.parse(options.body);return response({message:'Đổi mật khẩu thành công'});};
 w.eval(fs.readFileSync(path.join(staticRoot,'admin-assets/recovery.js'),'utf8'));await waitFor(()=>!d.querySelector('button[type=submit]').disabled);assert.equal(w.location.hash,'');
 d.querySelector('[name=password]').value='temporary-new-password';d.querySelector('[name=confirmation]').value='temporary-new-password';d.querySelector('form').dispatchEvent(new w.Event('submit',{cancelable:true}));await waitFor(()=>d.querySelector('form').hidden);assert.equal(request.token,'one-use-test-token');assert.equal(d.querySelector('[name=password]').value,'');assert.equal(d.body.textContent.includes('one-use-test-token'),false);dom.window.close();
});

test('Forgot password styles accepted local request as success, shows next steps and keeps real errors red',async()=>{
 const dom=new JSDOM(fs.readFileSync(path.join(staticRoot,'forgot-password.html'),'utf8'),{url:'http://localhost/forgot-password',runScripts:'outside-only'}),w=dom.window,d=w.document;let fail=false;
 w.fetch=async url=>url==='/api/csrf'?response({token:'csrf',headerName:'X-XSRF-TOKEN'}):fail?{ok:false,json:async()=>({message:'Vui lòng chờ 10 phút'})}:response({message:'Đã tiếp nhận yêu cầu','delivery':'local'});
 const style=d.createElement('style');style.textContent=fs.readFileSync(path.join(staticRoot,'admin-assets/admin.css'),'utf8');d.head.append(style);
 w.eval(fs.readFileSync(path.join(staticRoot,'admin-assets/recovery.js'),'utf8'));await waitFor(()=>!d.querySelector('button[type=submit]').disabled);d.querySelector('[name=identifier]').value='test-admin';
 d.querySelector('form').dispatchEvent(new w.Event('submit',{cancelable:true}));await waitFor(()=>d.querySelector('#message').className==='success');assert.equal(d.querySelector('#recovery-instructions').hidden,false);assert.equal(w.getComputedStyle(d.querySelector('#message')).color,'rgb(23, 99, 69)');
 fail=true;d.querySelector('form').dispatchEvent(new w.Event('submit',{cancelable:true}));await waitFor(()=>d.querySelector('#message').className==='error');assert.equal(d.querySelector('#recovery-instructions').hidden,true);assert.equal(d.querySelector('#message').getAttribute('role'),'alert');dom.window.close();
});

test('Login throttle displays remaining wait and enables login when it expires',async()=>{
 const start=Date.now(),dom=new JSDOM(fs.readFileSync(path.join(staticRoot,'login.html'),'utf8'),{url:'http://localhost/login?lockedUntil='+(start+90000),runScripts:'outside-only'}),w=dom.window;
 let now=start,countdown;w.Date.now=()=>now;w.setInterval=fn=>{countdown=fn;return 1;};w.clearInterval=()=>{};w.fetch=async()=>response({token:'csrf-test'});
 w.eval(fs.readFileSync(path.join(staticRoot,'admin-assets/login.js'),'utf8'));await waitFor(()=>w.document.querySelector('#csrf').value==='csrf-test');
 const button=w.document.querySelector('button[type=submit]');assert.equal(button.disabled,true);assert.match(button.textContent,/1:30/);assert.match(w.document.querySelector('#message').textContent,/quá nhiều lần/);
 now=start+91000;countdown();assert.equal(button.disabled,false);assert.equal(button.textContent,'Đăng nhập');assert.match(w.document.querySelector('#message').textContent,/có thể đăng nhập lại/);dom.window.close();
});

test('Image dialog hides generic save, ignores submit and uploads through its own button',async()=>{
 const dom=new JSDOM(fs.readFileSync(path.join(staticRoot,'admin/index.html'),'utf8'),{url:'http://localhost/admin',runScripts:'outside-only'}),w=dom.window,d=w.document;
 const style=d.createElement('style');style.textContent=fs.readFileSync(path.join(staticRoot,'admin-assets/admin.css'),'utf8');d.head.append(style);
 w.HTMLDialogElement.prototype.showModal=function(){this.open=true;};w.HTMLDialogElement.prototype.close=function(){this.open=false;};
 let uploaded=0;
 w.fetch=async(url,options={})=>{
  if(url==='/api/csrf')return response({token:'test',headerName:'X-XSRF-TOKEN'});
  if(url==='/api/admin/me')return response({username:'qa',roles:[{authority:'ROLE_ADMIN'}]});
  if(url==='/api/admin/lookups')return response({brands:[],materials:[],colors:[],categories:[]});
  if(url.startsWith('/api/admin/dashboard'))return response({revenue:[],statuses:[],topProducts:[],lowStock:[]});
  if(url.startsWith('/api/admin/products?'))return response({items:[{ID:2,PRODUCT_NAME:'Jeans',STATUS:1}],total:1,page:1,size:20});
  if(url==='/api/admin/products/2')return response({PRODUCT_NAME:'Jeans',images:uploaded?[{ID:7,IMAGE_URL:'/uploads/test.jpg',SORT_ORDER:0}]:[]});
  if(url==='/api/admin/products/2/images'&&options.method==='POST'){
   assert.ok(options.body instanceof w.FormData);assert.equal(options.body.get('file').name,'test.jpg');uploaded++;return response(7);
  }
  throw Error('Unexpected endpoint '+url);
 };
 w.eval(fs.readFileSync(path.join(staticRoot,'admin-assets/admin.js'),'utf8'));await waitFor(()=>d.querySelector('[data-section=products]'));
 d.querySelector('[data-section=products]').click();await waitFor(()=>d.querySelector('[data-action=images]'));
 d.querySelector('[data-action=images]').click();await waitFor(()=>d.querySelector('[data-action=upload]'));
 assert.equal(d.querySelector('#editor footer').hidden,true);
 assert.equal(w.getComputedStyle(d.querySelector('#editor footer')).display,'none');
 assert.equal(d.querySelector('#editor footer button').disabled,true);
 d.querySelector('#editor').dispatchEvent(new w.Event('submit',{cancelable:true}));await tick();
 assert.equal(d.querySelector('#form-error').textContent,'');assert.equal(uploaded,0);assert.equal(d.querySelector('#dialog').open,true);
 Object.defineProperty(d.querySelector('[name=files]'),'files',{value:[new w.File(['test'],'test.jpg',{type:'image/jpeg'})]});
 d.querySelector('[data-action=upload]').click();await waitFor(()=>d.querySelector('.image-row'));
 assert.equal(uploaded,1);assert.equal(d.querySelector('#editor footer').hidden,true);assert.equal(d.querySelector('#form-error').textContent,'');
 dom.window.close();
});

test('Store gallery follows color, retains image across sizes, and cart stores variant ID',async()=>{
 const dom=new JSDOM('<main class="live-shop"></main>',{url:'http://localhost/shop/index.html?id=1',runScripts:'outside-only'}),w=dom.window;
 w.fetch=async url=>response(url==='/api/csrf'?{token:'test',headerName:'X-XSRF-TOKEN'}:{product:{ID:1,PRODUCT_NAME:'Áo thun basic',DESCRIPTION:'Cotton',THUMBNAIL_URL:'/cover.png'},variants:[
  {ID:11,COLOR_ID:1,COLOR_NAME:'Đen',SIZE_NAME:'M',PRICE:199000,QUANTITY:3},
  {ID:12,COLOR_ID:1,COLOR_NAME:'Đen',SIZE_NAME:'L',PRICE:199000,QUANTITY:5},
  {ID:13,COLOR_ID:2,COLOR_NAME:'Trắng',SIZE_NAME:'M',PRICE:199000,QUANTITY:2},
  {ID:14,COLOR_ID:3,COLOR_NAME:'Xanh',SIZE_NAME:'M',PRICE:199000,QUANTITY:2}],
  images:[{COLOR_ID:1,IMAGE_URL:'/black.png'},{COLOR_ID:2,IMAGE_URL:'/white.png'},{COLOR_ID:null,IMAGE_URL:'/common.png'}]});
 w.eval(fs.readFileSync(path.join(staticRoot,'admin-assets/store.js'),'utf8'));await waitFor(()=>w.document.querySelector('#color'));
 const d=w.document,hero=d.querySelector('#hero'),size=d.querySelector('#size'),color=d.querySelector('#color');
 assert.equal(hero.getAttribute('src'),'/black.png');size.value='12';size.dispatchEvent(new w.Event('change'));assert.equal(hero.getAttribute('src'),'/black.png');
 color.value='2';color.dispatchEvent(new w.Event('change'));assert.equal(hero.getAttribute('src'),'/white.png');
 d.querySelector('[name=quantity]').value='1';d.querySelector('#buy').dispatchEvent(new w.Event('submit',{cancelable:true}));
 assert.deepEqual(JSON.parse(w.localStorage.getItem('clothing-cart')).map(i=>[i.id,i.quantity]),[[13,1]]);
 d.querySelector('[name=quantity]').value='2';d.querySelector('#buy').dispatchEvent(new w.Event('submit',{cancelable:true}));assert.match(d.querySelector('#shop-message').textContent,/vượt tồn/);
 color.value='3';color.dispatchEvent(new w.Event('change'));assert.equal(hero.getAttribute('src'),'/common.png');dom.window.close();
});

test('Admin adds a third color without losing variant fields and submits four variants',async()=>{
 const dom=new JSDOM(fs.readFileSync(path.join(staticRoot,'admin/index.html'),'utf8'),{url:'http://localhost/admin',runScripts:'outside-only'}),w=dom.window;
 w.HTMLDialogElement.prototype.showModal=function(){this.open=true;};w.HTMLDialogElement.prototype.close=function(){this.open=false;};
 let saved;
 w.fetch=async(url,options={})=>{
  if(options.method==='POST'&&url==='/api/admin/colors'){assert.deepEqual(JSON.parse(options.body),{name:'Xanh navy',hexCode:'#123456'});return response({ID:3,NAME:'Xanh navy',HEX_CODE:'#123456'});}
  if(options.method==='POST'&&url==='/api/admin/categories'){assert.deepEqual(JSON.parse(options.body),{name:'Polo',parentId:5,status:1});return response(6);}
  if(options.method==='POST'&&url==='/api/admin/products'){saved=JSON.parse(options.body);return response(42);}
  if(url==='/api/csrf')return response({token:'test',headerName:'X-XSRF-TOKEN'});
  if(url==='/api/admin/me')return response({username:'qa',roles:[{authority:'ROLE_ADMIN'}]});
  if(url==='/api/admin/lookups')return response({brands:[{ID:1,NAME:'Routine'}],materials:[{ID:1,NAME:'Cotton'}],colors:[{ID:1,NAME:'Đen'},{ID:2,NAME:'Trắng'}],categories:[{ID:5,NAME:'Áo thun'}]});
  if(url.startsWith('/api/admin/dashboard'))return response({revenue:[],statuses:[],topProducts:[],lowStock:[]});
  if(url.startsWith('/api/admin/products?'))return response({items:[],total:0,page:1,size:20});
  throw Error('Unexpected endpoint '+url);
 };
 w.eval(fs.readFileSync(path.join(staticRoot,'admin-assets/admin.js'),'utf8'));await waitFor(()=>w.document.querySelector('[data-section=products]'));
 w.document.querySelector('[data-section=products]').click();await waitFor(()=>w.document.querySelector('[data-action=create]'));
 w.document.querySelector('[data-action=create]').click();await waitFor(()=>w.document.querySelector('#variants .variant'));
 const d=w.document;
 assert.equal(d.querySelector('[name=brandName]').value,'');assert.equal(d.querySelector('[name=materialName]').value,'');
 d.querySelector('[name=brandName]').value='Thương hiệu mới';d.querySelector('[name=materialName]').value='Linen';
 d.querySelector('[data-action=add-variant]').click();d.querySelector('[data-action=add-variant]').click();
 assert.equal(d.querySelector('[name=code]').readOnly,true);assert.equal(d.querySelector('[name=code]').value,'');d.querySelector('[name=name]').value='Áo thun basic';
 const category=d.querySelector('#product-category-search'),results=d.querySelector('#product-category-results');
 assert.equal(d.querySelector('#editor select[name=categoryId]'),null);assert.equal(results.hidden,true);
 d.querySelector('#category-toggle').click();assert.equal(results.hidden,false);assert.match(results.textContent,/Chưa chọn/);assert.match(results.textContent,/Áo thun/);
 results.querySelector('[data-result="1"]').click();assert.equal(d.querySelector('#editor [name=categoryId]').value,'5');assert.equal(results.hidden,true);
 d.querySelector('#category-toggle').click();assert.match(results.textContent,/Chưa chọn/);results.querySelector('[data-result="0"]').click();assert.equal(category.value,'');assert.equal(d.querySelector('#editor [name=categoryId]').value,'');
 category.value='ao';category.dispatchEvent(new w.Event('input'));assert.equal(results.hidden,false);assert.match(results.textContent,/Áo thun/);
 results.querySelector('[data-result]').click();assert.equal(d.querySelector('#editor [name=categoryId]').value,'5');assert.equal(category.value,'Áo thun');
 category.value='missing';category.dispatchEvent(new w.Event('input'));assert.equal(d.querySelector('#editor [name=categoryId]').value,'');assert.equal(category.checkValidity(),false);assert.match(results.textContent,/Không tìm thấy/);
 category.value='ao';category.dispatchEvent(new w.Event('input'));category.dispatchEvent(new w.KeyboardEvent('keydown',{key:'Enter',cancelable:true}));assert.equal(category.checkValidity(),true);assert.equal(d.querySelector('#editor [name=categoryId]').value,'5');
 const rows=[...d.querySelectorAll('.variant')];
 rows.forEach((row,i)=>{row.querySelector('[name=colorId]').value=i===2?'2':'1';row.querySelector('[name=size]').value=i===1?'L':'M';row.querySelector('[name=size]').dispatchEvent(new w.Event('input',{bubbles:true}));row.querySelector('[name=price]').value='199000';assert.equal(row.querySelector('[name=quantity]'),null);assert.equal(row.querySelector('[name=sku]').readOnly,true);});
 d.querySelector('[data-action=add-variant]').click();const navy=d.querySelectorAll('.variant')[3];assert.equal(navy.querySelector('[name=colorId]').value,'');
 d.querySelector('[name=newColorName]').value='Xanh navy';d.querySelector('[name=newColorHex]').value='#123456';d.querySelector('[data-action=create-color]').click();await waitFor(()=>navy.querySelector('[name=colorId]').value==='3');
 assert.equal(rows[0].querySelector('[name=colorId]').value,'1');assert.equal(rows[2].querySelector('[name=colorId]').value,'2');assert.equal(rows[0].querySelector('[name=sku]').value,'SP[tự tăng]-DEN-M');
 assert.equal(navy.querySelector('[name=colorId]').options.length,4);navy.querySelector('[name=size]').value='XL';navy.querySelector('[name=size]').dispatchEvent(new w.Event('input',{bubbles:true}));assert.equal(navy.querySelector('[name=sku]').value,'SP[tự tăng]-XANH-NAVY-XL');navy.querySelector('[name=price]').value='210000';assert.equal(navy.querySelector('[name=quantity]'),null);
 d.querySelector('#category-add').click();d.querySelector('#category-create-save').click();assert.ok(d.querySelector('#category-create-error').textContent);d.querySelector('#category-new-name').value='Polo';d.querySelector('#category-new-parent').value='5';d.querySelector('#category-create-save').click();await waitFor(()=>d.querySelector('#editor [name=categoryId]').value==='6');assert.equal(category.value,'Áo thun → Polo');assert.equal(category.checkValidity(),true);assert.equal(d.querySelector('#dialog').open,true);assert.equal(d.querySelector('#category-create-panel').hidden,true);assert.equal(d.querySelector('[name=name]').value,'Áo thun basic');assert.equal(rows[0].querySelector('[name=price]').value,'199000');d.querySelector('#category-toggle').click();assert.match(results.textContent,/Polo/);d.querySelector('#category-toggle').click();
 d.querySelector('#editor').dispatchEvent(new w.Event('submit',{cancelable:true}));await waitFor(()=>saved);
 assert.equal(saved.variants.length,4);assert.ok(saved.variants.every(v=>v.quantity===0));assert.equal(saved.categoryId,6);assert.equal(saved.status,0);assert.equal(saved.brandName,'Thương hiệu mới');assert.equal(saved.materialName,'Linen');
 assert.deepEqual(saved.variants.map(v=>[v.colorId,v.size]),[[1,'M'],[1,'L'],[2,'M'],[3,'XL']]);assert.equal(saved.variants[0].id,null);assert.equal(saved.variants[0].price,199000);await tick();dom.window.close();
});

test('Create product includes image previews, cover and color in one multipart save and preserves failed form',async()=>{
 const dom=new JSDOM(fs.readFileSync(path.join(staticRoot,'admin/index.html'),'utf8'),{url:'http://localhost/admin',runScripts:'outside-only'}),w=dom.window,d=w.document;
 w.HTMLDialogElement.prototype.showModal=function(){this.open=true;};w.HTMLDialogElement.prototype.close=function(){this.open=false;};
 w.URL.createObjectURL=()=>'/preview.png';w.URL.revokeObjectURL=()=>{};let saved,metadata,calls=0;
 const read=blob=>new Promise(resolve=>{const reader=new w.FileReader();reader.onload=()=>resolve(JSON.parse(reader.result));reader.readAsText(blob);});
 w.fetch=async(url,options={})=>{
  if(url==='/api/csrf')return response({token:'test',headerName:'X-XSRF-TOKEN'});
  if(url==='/api/admin/me')return response({username:'qa',roles:[{authority:'ROLE_ADMIN'}]});
  if(url==='/api/admin/lookups')return response({brands:[{ID:1,NAME:'Routine'}],materials:[{ID:1,NAME:'Cotton'}],colors:[{ID:1,NAME:'Đen'}],categories:[]});
  if(url.startsWith('/api/admin/dashboard'))return response({revenue:[],statuses:[],topProducts:[],lowStock:[]});
  if(url.startsWith('/api/admin/products?'))return response({items:[],total:0,page:1,size:20});
  if(url==='/api/admin/products/with-images'){
   assert.ok(options.body instanceof w.FormData);saved=await read(options.body.get('product'));metadata=await read(options.body.get('images'));assert.equal(options.body.getAll('files').length,2);calls++;
   if(calls===1)return {ok:false,status:409,text:async()=>JSON.stringify({message:'Ảnh kiểm thử bị lỗi'})};return response(42);
  }
  throw Error('Unexpected endpoint '+url);
 };
 w.eval(fs.readFileSync(path.join(staticRoot,'admin-assets/admin.js'),'utf8'));await waitFor(()=>d.querySelector('[data-section=products]'));d.querySelector('[data-section=products]').click();await waitFor(()=>d.querySelector('[data-action=create]'));d.querySelector('[data-action=create]').click();await waitFor(()=>d.querySelector('#variants .variant'));
 for(const [name,value] of Object.entries({code:'NEW',name:'Sản phẩm có ảnh',brandName:'Nhãn mới',materialName:'Vải mới',status:'1'}))d.querySelector('#editor [name='+name+']').value=value;
 const variant=d.querySelector('.variant');variant.querySelector('[name=colorId]').value='1';variant.querySelector('[name=sku]').value='NEW-M';
 const picker=d.querySelector('[name=productFiles]');Object.defineProperty(picker,'files',{value:[new w.File(['jpeg'],'a.jpg',{type:'image/jpeg'}),new w.File(['png'],'b.png',{type:'image/png'})]});picker.dispatchEvent(new w.Event('change'));
 assert.equal(d.querySelectorAll('[data-photo]').length,2);const second=d.querySelectorAll('[data-photo]')[1];second.querySelector('[name=draftCover]').click();second.querySelector('[name=photoColor]').value='1';second.querySelector('[name=photoColor]').dispatchEvent(new w.Event('change',{bubbles:true}));
 d.querySelector('#editor').dispatchEvent(new w.Event('submit',{cancelable:true}));await waitFor(()=>d.querySelector('#form-error').textContent.includes('bị lỗi'));
 assert.equal(d.querySelector('#dialog').open,true);assert.equal(d.querySelector('[name=name]').value,'Sản phẩm có ảnh');assert.equal(d.querySelectorAll('[data-photo]').length,2);
 d.querySelector('#editor').dispatchEvent(new w.Event('submit',{cancelable:true}));await waitFor(()=>!d.querySelector('#dialog').open);
 assert.equal(saved.brandName,'Nhãn mới');assert.equal(saved.materialName,'Vải mới');assert.equal(saved.status,1);assert.deepEqual(metadata,[{colorId:null,order:0,cover:false},{colorId:1,order:1,cover:true}]);dom.window.close();
});

test('Editing a product has no stock input and preserves existing stock in the update payload',async()=>{
 const dom=new JSDOM(fs.readFileSync(path.join(staticRoot,'admin/index.html'),'utf8'),{url:'http://localhost/admin',runScripts:'outside-only'}),w=dom.window,d=w.document;let saved;
 w.HTMLDialogElement.prototype.showModal=function(){this.open=true;};w.HTMLDialogElement.prototype.close=function(){this.open=false;};
 w.fetch=async(url,options={})=>{
  if(url==='/api/csrf')return response({token:'test',headerName:'X-XSRF-TOKEN'});if(url==='/api/admin/me')return response({username:'qa',roles:[{authority:'ROLE_ADMIN'}]});if(url==='/api/admin/lookups')return response({brands:[{ID:1,NAME:'Brand'}],materials:[{ID:1,NAME:'Cotton'}],colors:[{ID:1,NAME:'Đen'}],categories:[]});if(url.startsWith('/api/admin/dashboard'))return response({revenue:[],statuses:[],topProducts:[],lowStock:[]});
  if(url.startsWith('/api/admin/products?'))return response({items:[{ID:2,PRODUCT_NAME:'Jeans'}],total:1,size:20});
  if(url==='/api/admin/products/2'&&options.method==='PUT'){saved=JSON.parse(options.body);return response(2);}
  if(url==='/api/admin/products/2')return response({ID:2,PRODUCT_CODE:'SP002',PRODUCT_NAME:'Jeans',BRAND_ID:1,MATERIAL_ID:1,STATUS:0,images:[],variants:[{ID:22,COLOR_ID:1,SIZE_NAME:'32',SKU:'SP002-DEN-32',PRICE:199000,QUANTITY:7,STATUS:1}]});throw Error('Unexpected '+url);
 };
 w.eval(fs.readFileSync(path.join(staticRoot,'admin-assets/admin.js'),'utf8'));await waitFor(()=>d.querySelector('[data-section=products]'));d.querySelector('[data-section=products]').click();await waitFor(()=>d.querySelector('[data-action=edit]'));d.querySelector('[data-action=edit]').click();await waitFor(()=>d.querySelector('.variant'));
 assert.equal(d.querySelector('#editor [name=quantity]'),null);d.querySelector('[name=price]').value='259000';d.querySelector('#editor').dispatchEvent(new w.Event('submit',{cancelable:true}));await waitFor(()=>saved);assert.equal(saved.variants[0].quantity,7);assert.equal(saved.variants[0].price,259000);await waitFor(()=>!d.querySelector('#dialog').open&&!d.querySelector('#editor footer button').disabled&&d.querySelector('#content [data-action=edit]'));dom.window.close();
});
