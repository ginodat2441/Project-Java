const {test}=require('node:test'),assert=require('node:assert/strict'),fs=require('fs'),path=require('path');
const {JSDOM}=require('../.local/dom-tests/node_modules/jsdom');
const source=fs.readFileSync(path.join(__dirname,'../client/assets/js/client-order-requests.js'),'utf8');
const tick=async()=>{for(let i=0;i<8;i++)await new Promise(r=>setImmediate(r));};
async function setup(data,{fail=false}={}){
 const dom=new JSDOM('<article id="order"></article>',{url:'http://localhost/client/account.html',runScripts:'outside-only'}),w=dom.window,calls=[];
 w.HTMLDialogElement.prototype.showModal=function(){this.open=true;};w.HTMLDialogElement.prototype.close=function(){this.open=false;this.dispatchEvent(new w.Event('close'));};
 w.ClientStore={api:async(url,method,body)=>{calls.push({url,method,body});if(method==='POST'){if(fail)throw Error('Đơn đã xuất giao');return 1;}return[data];}};
 w.eval(source);let refresh=0;await w.ClientOrderRequests.mount(w.document.querySelector('article'),{id:7,billCode:'HD-TEST'},async()=>refresh++);
 return {w,calls,refresh:()=>refresh,close:()=>w.close()};
}
test('customer sends a reasoned request, never cancels immediately or changes cart',async()=>{const p=await setup({billId:7,canCancel:true,canReturn:false,requests:[]});try{
 p.w.document.querySelector('.order-support-actions button').click();const form=p.w.document.querySelector('dialog form');
 form.elements.detail.value='Tôi muốn đổi địa chỉ giao hàng';form.dispatchEvent(new p.w.Event('submit',{cancelable:true}));await tick();
 const post=p.calls.find(c=>c.method==='POST');assert.equal(post.url,'/api/order-requests');assert.equal(post.body.type,'CANCEL');assert.equal(post.body.billId,7);assert.match(post.body.reason,/địa chỉ/);assert.equal(p.refresh(),1);assert.match(form.textContent,/Đã gửi yêu cầu/);assert.equal(p.calls.some(c=>c.url.includes('/cancel')||c.url.includes('/carts')),false);
 }finally{p.close();}});
test('pending history is escaped and does not offer duplicate actions',async()=>{const p=await setup({billId:7,canCancel:false,canReturn:false,requests:[{STATUS:'PENDING',REQUEST_TYPE:'CANCEL',REASON:'<img src=x onerror=alert(1)>',CREATED_AT:'2026-11-19T12:00:00'}]});try{assert.equal(p.w.document.querySelectorAll('.order-support-actions button').length,0);assert.equal(p.w.document.querySelector('img'),null);assert.match(p.w.document.body.textContent,/Chờ cửa hàng duyệt/);}finally{p.close();}});
test('ineligible orders have no cancellation or return button',async()=>{const p=await setup({billId:7,canCancel:false,canReturn:false,requests:[]});try{assert.equal(p.w.document.querySelectorAll('button').length,0);}finally{p.close();}});
test('request validation and server failures keep reason available for correction',async()=>{const p=await setup({billId:7,canCancel:false,canReturn:true,returnDeadline:'2026-12-01T12:00:00',requests:[]},{fail:true});try{
 p.w.document.querySelector('button').click();const form=p.w.document.querySelector('form');form.elements.detail.value='ngắn';form.dispatchEvent(new p.w.Event('submit',{cancelable:true}));await tick();assert.equal(p.calls.some(c=>c.method==='POST'),false);
 form.elements.detail.value='Sản phẩm bị lỗi đường may ở tay áo';form.dispatchEvent(new p.w.Event('submit',{cancelable:true}));await tick();assert.match(form.querySelector('[role=alert]').textContent,/Đơn đã xuất giao/);assert.equal(form.querySelector('[type=submit]').disabled,false);assert.equal(form.elements.detail.value,'Sản phẩm bị lỗi đường may ở tay áo');
 }finally{p.close();}});
