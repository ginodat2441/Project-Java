const {test}=require('node:test'),assert=require('node:assert/strict'),fs=require('node:fs'),path=require('node:path'),{JSDOM}=require('../.local/dom-tests/node_modules/jsdom');
const tick=()=>new Promise(r=>setImmediate(r));async function wait(fn){for(let i=0;i<100;i++){if(fn())return;await tick();}throw Error('Expected checkout state');}
const response=d=>({ok:true,status:200,json:async()=>d});
test('COD checkout quotes only selected items, invalidates edited destination and submits server fee',async()=>{
 const root=path.join(__dirname,'..'),dom=new JSDOM(fs.readFileSync(path.join(root,'giao dien/checkout.html'),'utf8'),{url:'http://localhost/store/checkout.html?items=1',runScripts:'outside-only'}),w=dom.window,d=w.document;let submitted,calls=[];w.alert=()=>{};
 w.fetch=async(url,options={})=>{
  if(url==='/api/csrf')return response({token:'t',headerName:'X-CSRF-TOKEN'});
  if(url==='/api/customer/me')return response({FULL_NAME:'Demo',EMAIL:'demo@example.test',PHONE:'0900000000'});
  if(url==='/api/customer/cart')return response({items:[{productDetailId:1,productId:1,productName:'Áo',colorName:'Đen',sizeName:'M',price:200000,quantity:2,available:true,stock:10},{productDetailId:2,productId:1,productName:'Quần',price:300000,quantity:1,available:true,stock:10}],subtotal:700000,totalQuantity:3,checkoutAllowed:true});
  if(url.startsWith('/api/catalog?'))return response({items:[],total:0,size:15});
  if(url==='/api/catalog/quote'){calls.push(JSON.parse(options.body));return response({subtotal:400000,discount:0,services:[{serviceCode:'VCN',serviceName:'Nhanh',fee:36000}]});}
  if(url==='/api/customer/cart/checkout'){submitted=JSON.parse(options.body);return response({billCode:'HD1'});}throw Error(url);
 };
 w.eval(fs.readFileSync(path.join(root,'giao dien/assets/js/client-store.js'),'utf8'));await wait(()=>d.body.classList.contains('client-ready'));
 const form=d.querySelector('main .checkout-form'),button=form.querySelector('[type=submit]');assert.equal(button.disabled,true);
 for(const [name,value]of Object.entries({province:'Hà Nội',ward:'Cầu Giấy',address:'12 Demo'}))form.querySelector('[name='+name+']').value=value;
 form.querySelector('[data-quote]').click();await wait(()=>form.querySelector('.client-shipping-options button'));form.querySelector('.client-shipping-options button').click();assert.equal(button.disabled,false);
 form.querySelector('[name=ward]').value='Phường mới';form.querySelector('[name=ward]').dispatchEvent(new w.Event('input',{bubbles:true}));assert.equal(button.disabled,true);
 form.querySelector('[data-quote]').click();await wait(()=>form.querySelector('.client-shipping-options button'));form.querySelector('.client-shipping-options button').click();form.dispatchEvent(new w.Event('submit',{cancelable:true}));await wait(()=>submitted);
 assert.equal(submitted.paymentMethod,'COD');assert.equal(submitted.shippingService,'VCN');assert.equal(submitted.expectedShippingFee,36000);assert.deepEqual(submitted.items,[{productDetailId:1,quantity:2}]);assert.equal(calls.at(-1).ward,'Phường mới');dom.window.close();
});
