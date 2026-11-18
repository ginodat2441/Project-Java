const {test}=require('node:test'),assert=require('node:assert/strict'),fs=require('fs'),path=require('path');
const {JSDOM}=require('../.local/dom-tests/node_modules/jsdom');const root=path.join(__dirname,'..');
const read=p=>fs.readFileSync(path.join(root,p),'utf8');
const orders=[['COD',0,0],['SEPAY',0,0],['SEPAY',1,0],['COD',0,2],['COD',1,3],['SEPAY',0,4],['COD',0,6]].map(([paymentMethod,paymentStatus,status],i)=>({id:i+1,billCode:'TEST-'+i,paymentMethod,paymentStatus,status,totalAmount:230000,createdAt:'2026-11-18T12:00:00'}));
const tick=async()=>{for(let i=0;i<12;i++)await new Promise(r=>setImmediate(r));};
test('customer tabs classify COD, prepaid, shipping and terminal orders correctly',async()=>{
 const dom=new JSDOM(read('client/account.html'),{url:'http://localhost:5500/client/account.html#orders',runScripts:'outside-only'});const w=dom.window;
 try{
  w.sessionStorage.setItem('clothingShopToken','fixture');
  w.fetch=async url=>{const p=new URL(url).pathname;let data;
   if(p==='/api/auth/me')data={customerId:1,fullName:'Người thử',email:'test@example.test'};
   else if(p==='/api/bills')data=orders;
   else if(p==='/api/addresses')data=[];
   else if(p.startsWith('/api/carts'))data={items:[],totalItems:0,totalAmount:0};
   else data={items:[],total:0,totalPages:0};
   return{ok:true,status:200,json:async()=>({success:true,data})};
  };
  w.eval(read('client/assets/js/client-store.js'));await tick();
  const d=w.document;
  assert.equal(d.querySelector('#orders .pagination-wrapper').hidden,true);
  for(const [key,count] of [['all',7],['payment',1],['processing',2],['shipping',1],['history',3]]){
   d.querySelector(`[data-order-group="${key}"]`).click();assert.equal(d.querySelectorAll('#orders .orders-grid .order-card').length,count,key);
  }
  d.querySelector('[data-order-group=shipping]').click();assert.match(d.querySelector('#orders .orders-grid').textContent,/COD/);
  assert.doesNotMatch(d.querySelector('#orders .orders-grid').textContent,/TEST-1/);
 }finally{w.close();}
});
async function admin(){
 const dom=new JSDOM(read('admin/admin_code/src/main/resources/static/admin/index.html'),{url:'http://localhost:8888/admin',runScripts:'outside-only'});const w=dom.window,calls=[];
 w.HTMLDialogElement.prototype.showModal=function(){this.open=true;};
 w.eval(read('admin/admin_code/src/main/resources/static/admin/js/core/state.js')+read('admin/admin_code/src/main/resources/static/admin/js/core/ui.js')+read('admin/admin_code/src/main/resources/static/admin/js/pages/orders/detail.js')+read('admin/admin_code/src/main/resources/static/admin/js/pages/orders/editor-list.js')+`
 window.fixture={setFilter:(f)=>filter=f,setPage:(p)=>page=p,getPage:()=>page,getFilter:()=>filter};
 window.load=async()=>{};
 `);
 w.api=async(url,method,body)=>{calls.push({url,method,body});if(url.includes('/orders/1'))return{order:{ID:1,BILL_CODE:'TEST',STATUS:1,PAYMENT_METHOD:'SEPAY',PAYMENT_STATUS:1,TOTAL_AMOUNT:230000},lines:[],history:[],inventoryDocuments:[]};return{items:[{ID:2,BILL_CODE:'REFUNDED',STATUS:6,PAYMENT_METHOD:'COD',PAYMENT_STATUS:2,TOTAL_AMOUNT:100000}],total:1,size:20};};
 return{w,calls,close:()=>w.close()};
}
test('admin filters whole dataset by group and preserves group while paging',async()=>{const p=await admin();try{
 p.w.fixture.setFilter({orderGroup:'payment',q:'TEST',status:'0'});p.w.fixture.setPage(2);
 const html=await p.w.AdminPages.orders.render();assert.match(p.calls[0].url,/orderGroup=payment/);assert.match(p.calls[0].url,/page=2/);assert.match(html,/Đã hoàn tiền/);assert.match(html,/Giao thất bại/);assert.match(html,/value="SEPAY"/);
 await p.w.AdminPages.orders.action('order-group-shipping',NaN);assert.equal(p.w.fixture.getPage(),1);assert.equal(p.w.fixture.getFilter().orderGroup,'shipping');assert.equal(p.w.fixture.getFilter().q,'TEST');assert.equal(p.w.fixture.getFilter().status,undefined);
}finally{p.close();}});
test('admin order details lock payment fields and preserve method in shipping request',async()=>{const p=await admin();try{
 p.w.document.querySelector('#content').innerHTML=await p.w.AdminPages.orders.render();
 await p.w.AdminPages.orders.action('detail',1);const d=p.w.document;assert.equal(d.querySelector('#fields [name=paymentMethod]').disabled,true);assert.equal(d.querySelector('#fields [name=paymentStatus]').disabled,true);assert.equal(d.querySelector('#fields [name=paymentMethod]').value,'SEPAY');assert.equal(d.querySelector('#filter [name=paymentMethod]').disabled,false);assert.ok(d.querySelectorAll('.order-detail-section').length>=3);
 d.querySelector('[data-action=save-shipping]').click();await tick();const request=p.calls.find(c=>c.method==='PUT');assert.equal(request.body.paymentMethod,'SEPAY');assert.equal(request.body.paymentStatus,1);
}finally{p.close();}});
