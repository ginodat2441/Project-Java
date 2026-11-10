const {readAsset}=require('./helpers/frontend-assets.cjs');
const {test}=require('node:test'),assert=require('node:assert/strict'),fs=require('fs'),path=require('path');
const {JSDOM}=require('../.local/dom-tests/node_modules/jsdom');
const root=path.resolve(__dirname,'../backend_java/src/main/resources/static');
const response=data=>({ok:true,status:200,json:async()=>data,text:async()=>JSON.stringify(data)});
const tick=()=>new Promise(r=>setImmediate(r));async function wait(fn){for(let i=0;i<100;i++){if(fn())return;await tick();}throw Error('Expected UI state not reached');}
const sku={ID:108,PRODUCT_ID:1,PRODUCT_CODE:'SP108',PRODUCT_NAME:'Áo demo',SKU:'SP108-XANH-REU-XL',COLOR_NAME:'Xanh rêu',SIZE_NAME:'XL',PHYSICAL:90,RESERVED:5,QUANTITY:85,AVERAGE_COST:107000,KNOWN_VALUE:9630000,INVENTORY_VALUE:9630000,AVAILABLE_VALUE:9095000,PRODUCT_INVENTORY_VALUE:9630000};
const valuation={INVENTORY_VALUE:9630000,KNOWN_VALUE:9630000,PHYSICAL:90,RESERVED:5,QUANTITY:85,UNKNOWN_COST_SKUS:0};
const totals={NET:1000000,RETURN_VALUE:100000,NET_AFTER_RETURN:900000,SHIPPING:30000,COGS:500000,GROSS_PROFIT:400000,KNOWN_GROSS_PROFIT:400000,UNKNOWN_COST_SKUS:0,DAMAGED_COST:10000,SOLD:10,RETURNED:1,ORDERS:5};
async function setup(role='ADMIN'){
 const dom=new JSDOM(fs.readFileSync(path.join(root,'admin/index.html'),'utf8'),{url:'http://localhost/admin',runScripts:'dangerously'}),w=dom.window,d=w.document;const requests=[];let postedCost;
 w.HTMLDialogElement.prototype.showModal=function(){this.open=true;};w.HTMLDialogElement.prototype.close=function(){this.open=false;};
 w.fetch=async(url,options={})=>{requests.push({url,options});
  if(url==='/api/csrf')return response({token:'test',headerName:'X-XSRF-TOKEN'});
  if(url==='/api/admin/me')return response({username:'qa',roles:[{authority:'ROLE_'+role}]});
  if(url==='/api/admin/lookups')return response({brands:[],materials:[],colors:[],categories:[]});
  if(url==='/api/admin/shipping/status')return response({configured:false,originConfigured:false,tokenConfigured:false,parcelConfigured:false,environment:'production',weightPolicy:'Cần cân thực tế trước khi gửi'});
  if(url.startsWith('/api/admin/dashboard'))return response({inventory:valuation,totals,revenue:[],statuses:[],topProducts:[],lowStock:[]});
  if(url.startsWith('/api/admin/stock?'))return response({items:[sku],globalTotals:valuation,totals:valuation,products:[{ID:1,NAME:'SP108 · Áo demo'}],page:1,size:30,total:1});
  if(url.startsWith('/api/admin/inventory-history?'))return response({items:[{ID:1,SKU:sku.SKU,DOCUMENT_ID:5,DOC_CODE:'XK000005',BILL_ID:12,BILL_CODE:'HD12',MOVEMENT_TYPE:'DISPATCH',PHYSICAL_BEFORE:90,PHYSICAL_AFTER:85,RESERVED_BEFORE:5,RESERVED_AFTER:0,BEFORE_QUANTITY:85,AFTER_QUANTITY:85,VALUE_BEFORE:9630000,VALUE_AFTER:9095000,HISTORY_SCOPE:'PHYSICAL_V8'}],page:1,size:30,total:1});
  if(url.startsWith('/api/admin/inventory/summary?'))return response({items:[{...sku,OPENING:85,CLOSING:85,PHYSICAL_OPENING:90,PHYSICAL_CLOSING:85,RESERVED_OPENING:5,RESERVED_CLOSING:0,DISPATCH:5,LEGACY_ROWS:0}],totals:{OPENING:85,CLOSING:85,DISPATCH:5,RESERVED:5,RELEASED:0},page:1,size:30,total:1});
  if(url.startsWith('/api/admin/audit?'))return response({items:[{ID:1,USERNAME:'qa',ACTION:'STATUS_3',OBJECT_TYPE:'BILL',OBJECT_ID:12,BEFORE_DATA:'{"status":2}',AFTER_DATA:'{"status":3}'}],total:1,size:30,page:1});
  if(url.startsWith('/api/admin/categories?'))return response([{ID:1,CATEGORY_NAME:'Áo',CATEGORY_PATH:' / Áo',DEPTH:1,STATUS:1},{ID:2,PARENT_ID:1,CATEGORY_NAME:'Áo thun',CATEGORY_PATH:' / Áo / Áo thun',DEPTH:2,STATUS:1}]);
  if(url.startsWith('/api/admin/vouchers?'))return response([{ID:1,CODE:'DEMO10',EFFECTIVE_STATUS:'ACTIVE',USED_COUNT:2,REMAINING:8,STATUS:1}]);
  if(url.startsWith('/api/admin/orders?'))return response({items:[{ID:12,BILL_CODE:'HD12',STATUS:1,PAYMENT_STATUS:0,TOTAL_AMOUNT:100000}],total:1,size:20,page:1});
  if(url.startsWith('/api/admin/inventory/documents?'))return response({items:[],total:0,size:20,page:1});
  if(url.startsWith('/api/admin/reports?'))return response({from:'2026-01-01',to:'2026-12-31',totals,series:[{PERIOD:'2026-01',...totals}],products:{items:[{ID:1,PRODUCT_CODE:'SP108',...totals}],total:1,size:20,page:1},variants:[{...sku,...totals}]});
  if(url.startsWith('/api/admin/products?'))return response({items:[{ID:1,PRODUCT_CODE:'SP108',PRODUCT_NAME:'Áo demo',STATUS:1}],totals:{},total:1,size:20,page:1});
  if(url==='/api/admin/products/1')return response({ID:1,PRODUCT_CODE:'SP108',PRODUCT_NAME:'Áo demo',STATUS:1,variants:[],images:[]});
  if(url==='/api/admin/sku-cost/108'&&options.method==='POST'){postedCost=JSON.parse(options.body);return response(null);}
  if(url==='/api/admin/sku-cost/108')return response({stock:{...sku,ID:108,PHYSICAL_QUANTITY:90,RESERVED_QUANTITY:5,KNOWN_VALUE:9630000},receipts:[]});
  throw Error('Unmocked '+url);
 };
 for(const file of ['operations.js','workflow.js','admin.js']){const script=d.createElement('script');script.textContent=readAsset(file);d.head.append(script);}
 await wait(()=>d.querySelector('[data-section=stock]'));
 async function section(name,ready){d.querySelector(`[data-section=${name}]`).click();await wait(()=>d.querySelector(ready));assert.notEqual(d.querySelector('#notice').className,'error',d.querySelector('#notice').textContent);}
 return {dom,w,d,requests,section,get postedCost(){return postedCost;}};
}
test('SKU valuation shows warehouse and sellable values and traces immutable SKU history',async()=>{
 const h=await setup();await h.section('stock','[data-action=sku-history]');assert.match(h.d.querySelector('#content').textContent,/9.630.000/);assert.match(h.d.querySelector('#content').textContent,/9.095.000/);assert.match(h.d.querySelector('#content').textContent,/gồm hàng giữ/);
 h.d.querySelector('[name=stockState]').value='HELD';h.d.querySelector('#filter').dispatchEvent(new h.w.Event('submit',{cancelable:true,bubbles:true}));await wait(()=>h.requests.some(r=>r.url.includes('stockState=HELD')));await wait(()=>h.d.querySelector('[data-action=sku-history]'));h.d.querySelector('[data-action=sku-history]').click();await wait(()=>h.requests.some(r=>r.url.includes('variantId=108')));await wait(()=>h.d.querySelector('[data-action=trace-document]'));assert.match(h.d.querySelector('#content').textContent,/PHYSICAL_V8/);h.dom.window.close();
});
test('Cost snapshot previews scope and sends documentary evidence without changing quantity',async()=>{
 const h=await setup();await h.section('stock','[data-action=sku-cost]');h.d.querySelector('[data-action=sku-cost]').click();await wait(()=>h.d.querySelector('[name=evidence]'));assert.match(h.d.querySelector('#fields').textContent,/Không đổi số lượng/);h.d.querySelector('[name=unitCost]').value='107000';h.d.querySelector('[name=evidence]').value='Invoice 216';h.d.querySelector('[name=reason]').value='Verified opening balance';h.d.querySelector('#editor').dispatchEvent(new h.w.Event('submit',{cancelable:true}));await wait(()=>h.postedCost);assert.equal(h.postedCost.expectedQuantity,90);assert.equal(h.postedCost.evidence,'Invoice 216');await wait(()=>!h.d.querySelector('#dialog').open&&!h.d.querySelector('#editor footer button').disabled&&h.d.querySelector('[data-action=sku-cost]'));h.dom.window.close();
});
test('All operational screens render V8 filters and explain legacy physical-history limits',async()=>{
 const h=await setup();await h.section('inventoryReport','[data-action=export-balance]');assert.match(h.d.querySelector('#content').textContent,/Xuất giao/);assert.match(h.d.querySelector('#content').textContent,/lịch sử cũ/);
 await h.section('audit','[name=action]');assert.match(h.d.querySelector('#content').textContent,/Hoàn tất đơn/);assert.match(h.d.querySelector('#content').textContent,/"status":3/);
 await h.section('categories','[data-action=create]');assert.match(h.d.querySelector('#content').textContent,/Ẩn cha/);assert.match(h.d.querySelector('#content').textContent,/Áo thun/);
 await h.section('vouchers','[name=effective]');assert.match(h.d.querySelector('#content').textContent,/Đang áp dụng/);assert.match(h.d.querySelector('#content').textContent,/không trả lại/);
 await h.section('orders','[name=paymentMethod]');assert.ok(h.d.querySelector('[name=returnStatus]'));assert.match(h.d.querySelector('#content').textContent,/Tổng tiền đơn/);
 await h.section('reports','[data-action=export-skus]');assert.match(h.d.querySelector('#content').textContent,/Giá vốn hàng bán/);assert.match(h.d.querySelector('#content').textContent,/Lãi gộp/);h.dom.window.close();
});
test('STAFF sees stock history but no cost correction or audit controls',async()=>{
 const h=await setup('STAFF');assert.equal(h.d.querySelector('[data-section=audit]'),null);await h.section('stock','[data-action=sku-history]');assert.equal(h.d.querySelector('[data-action=sku-cost]'),null);h.dom.window.close();
});
test('Viettel status renders correct Vietnamese and a usable Windows configuration command',async()=>{
 const h=await setup();await h.section('shipping','pre');
 assert.equal(h.d.querySelector('#title').textContent,'Vận chuyển Viettel Post');
 const text=h.d.querySelector('#content').textContent;assert.match(text,/Trạng thái: Chưa hoàn tất cấu hình/);assert.match(text,/Địa chỉ kho gửi/);assert.match(text,/Khối lượng và kích thước kiện/);assert.match(text,/Chính thức/);assert.doesNotMatch(text,/[?\uFFFD]/);
 assert.equal(h.d.querySelector('pre').textContent,'powershell -NoProfile -ExecutionPolicy Bypass -File .\\tools\\configure-viettel.ps1');h.dom.window.close();
});
test('Product revenue opens read-only SKU report while Edit opens the product form',async()=>{
 const h=await setup();await h.section('products','[data-action=product-revenue]');
 h.d.querySelector('[data-action=product-revenue]').click();await wait(()=>h.d.querySelector('#dialog').open);
 assert.match(h.d.querySelector('#dialog-title').textContent,/Doanh thu/);assert.match(h.d.querySelector('#fields').textContent,/SP108-XANH-REU-XL/);assert.equal(h.d.querySelector('#fields [name=name]'),null);assert.equal(h.d.querySelector('#editor footer').hidden,true);assert.ok(h.requests.some(r=>r.url.startsWith('/api/admin/reports?')&&r.url.includes('productId=1')));
 h.d.querySelector('#close').click();h.d.querySelector('[data-action=edit]').click();await wait(()=>h.d.querySelector('#fields [name=name]'));assert.equal(h.d.querySelector('#fields [name=name]').value,'Áo demo');assert.match(h.d.querySelector('#dialog-title').textContent,/Sửa sản phẩm/);assert.equal(h.d.querySelector('#editor footer').hidden,false);h.dom.window.close();
});
