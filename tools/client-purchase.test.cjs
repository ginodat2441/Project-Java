const {test}=require('node:test'), assert=require('node:assert/strict'), fs=require('fs'),path=require('path');
const {JSDOM}=require('../.local/dom-tests/node_modules/jsdom');
const root=path.join(__dirname,'..');
const source=fs.readFileSync(path.join(root,'client/assets/js/client-store.js'),'utf8').replaceAll('location.assign(', 'window.navigate(');
const product={id:1,productName:'Áo sơ mi',minPrice:200000,thumbnailUrl:'assets/img/product/product-1.webp',totalStock:10,status:1};
const variant={id:12,productId:1,colorName:'Trắng',sizeName:'M',price:200000,quantity:10};
const tick=async()=>{for(let i=0;i<15;i++)await new Promise(r=>setImmediate(r));};
async function page(url,{badQuote=false}={}){
 const filename=url.split('?')[0];const dom=new JSDOM(fs.readFileSync(path.join(root,'client',filename),'utf8'),{url:'http://127.0.0.1:5500/client/'+url,runScripts:'outside-only'});
 const w=dom.window,calls=[],destinations=[];w.navigate=x=>destinations.push(x);
 w.HTMLDialogElement.prototype.showModal=function(){this.open=true;};
 w.HTMLDialogElement.prototype.close=function(){this.open=false;this.dispatchEvent(new w.Event('close'));};
 w.sessionStorage.setItem('clothingShopToken','test-token');
 w.fetch=async(url,options={})=>{
  calls.push({url,options});const u=new URL(url),route=u.pathname;let data;
  if(route==='/api/auth/me')data={customerId:1,fullName:'Khách thử',email:'test@example.test'};
  else if(route==='/api/products/categories')data=[{id:5,categoryName:'Áo',parentId:null},{id:6,categoryName:'Áo sơ mi',parentId:5}];
  else if(route==='/api/products/1/details')data=[variant];
  else if(route==='/api/products/1')data=product;
  else if(route==='/api/products')data={items:Array.from({length:15},(_,i)=>({...product,id:i+1,minPrice:200000+i})),page:Number(u.searchParams.get('page')||1),totalPages:2,total:17};
  else if(route==='/api/brands')data={items:[]};
  else if(route==='/api/addresses')data=[{id:3,receiverName:'Khách thử',receiverPhone:'0900000000',province:'Hà Nội',ward:'Phường A',detailAddress:'Số 1',isDefault:true}];
  else if(route==='/api/bills/quote'){
   if(badQuote)return{ok:false,status:400,json:async()=>({success:false,message:'Không đủ tồn kho'})};
   data={subtotal:200000,shippingFee:30000,totalAmount:230000};
  } else if(route==='/api/bills')data={id:99,billCode:'TEST99',paymentMethod:'COD',totalAmount:230000};
  else if(route.endsWith('/items'))data={};
  else if(route.startsWith('/api/carts/'))data={items:[{id:50,productDetailId:12,productId:1,productName:'Áo trong giỏ',colorName:'Trắng',sizeName:'M',unitPrice:200000,quantity:2,stock:10,available:1}],totalItems:2,totalAmount:400000};
  else throw Error('Unexpected API '+route);
  return{ok:true,status:200,json:async()=>({success:true,data})};
 };
 w.eval(source);await tick();return{w,doc:w.document,calls,destinations,close:()=>w.close()};
}
async function selectVariant(p){p.doc.querySelector('.color-options button').click();p.doc.querySelector('.size-options button').click();await tick();}
test('separate product listing requests 15 rows, passes price sort and preserves pagination route',async()=>{
 const p=await page('products.html?sort=price_asc&page=1');try{
  const request=p.calls.find(x=>new URL(x.url).pathname==='/api/products');const u=new URL(request.url);
  assert.equal(u.searchParams.get('sort'),'price_asc');assert.equal(u.searchParams.get('size'),'15');
  assert.equal(p.doc.querySelectorAll('main .client-product-grid .product-box').length,15);
  assert.equal(p.doc.querySelector('[name=sort]').value,'price_asc');
  const next=[...p.doc.querySelectorAll('.client-pagination a')].find(a=>a.textContent.includes('Trang sau'));
  assert.match(next.href,/products\.html\?sort=price_asc&page=2/);
 }finally{p.close();}
});
test('category directory renders real category links instead of all products',async()=>{
 const p=await page('category.html');try{assert.equal(p.doc.querySelectorAll('.client-category-links a').length,2);assert.match(p.doc.querySelector('.client-category-links a').href,/categoryId=5/);assert.equal(p.calls.some(x=>new URL(x.url).pathname==='/api/products'),false);}finally{p.close();}
});
test('buy now goes straight to checkout without mutating cart',async()=>{
 const p=await page('product-details.html?id=1');try{await selectVariant(p);p.doc.querySelector('.buy-now-btn').click();await tick();assert.deepEqual(p.destinations,['checkout.html?buyNow=1&productId=1&variantId=12&quantity=1']);assert.equal(p.calls.some(x=>x.options.method==='POST'),false);}finally{p.close();}
});
test('add to cart confirms success in modal with two working choices',async()=>{
 const p=await page('product-details.html?id=1');try{await selectVariant(p);p.doc.querySelector('.add-to-cart-btn').click();await tick();const dialog=p.doc.querySelector('#client-added-dialog');assert.equal(dialog.open,true);assert.match(dialog.textContent,/Đã thêm sản phẩm vào giỏ hàng/);assert.equal(dialog.querySelector('button').textContent,'Xem tiếp sản phẩm');assert.match(dialog.querySelector('a').href,/cart.html$/);const req=p.calls.find(x=>x.url.endsWith('/items')&&x.options.method==='POST');assert.deepEqual(JSON.parse(req.options.body),{productDetailId:12,quantity:1});dialog.querySelector('button').click();assert.equal(p.doc.querySelector('#client-added-dialog'),null);}finally{p.close();}
});
test('direct checkout quotes real item then creates one order without removing existing cart item',async()=>{
 const p=await page('checkout.html?buyNow=1&productId=1&variantId=12&quantity=1');try{
  assert.match(p.doc.querySelector('.order-summary-content').textContent,/Áo sơ mi/);
  assert.match(p.doc.querySelector('.client-checkout-total').textContent,/230\.000/);
  const form=p.doc.querySelector('.checkout-form');form.reportValidity=()=>true;form.dispatchEvent(new p.w.Event('submit',{bubbles:true,cancelable:true}));await tick();
  const orders=p.calls.filter(x=>new URL(x.url).pathname==='/api/bills'&&x.options.method==='POST');assert.equal(orders.length,1);
  assert.deepEqual(JSON.parse(orders[0].options.body).items,[{productDetailId:12,quantity:1}]);
  assert.equal(p.calls.some(x=>x.url.includes('/api/carts/')&&['POST','PUT','DELETE'].includes(x.options.method)),false);
  assert.match(p.doc.querySelector('main').textContent,/TEST99/);
 }finally{p.close();}
});
test('invalid or unavailable buy-now selection cannot create order',async()=>{
 for(const url of ['checkout.html?buyNow=1&productId=1&variantId=999&quantity=1','checkout.html?buyNow=1&productId=1&variantId=12&quantity=0']){
  const p=await page(url);try{assert.equal(p.calls.some(x=>new URL(x.url).pathname==='/api/bills'),false);assert.ok(p.doc.querySelector('main [role=alert]'));}finally{p.close();}
 }
});
test('out-of-stock quote blocks checkout form submission',async()=>{const p=await page('checkout.html?buyNow=1&productId=1&variantId=12&quantity=1',{badQuote:true});try{assert.equal(p.doc.querySelector('.checkout-form button[type=submit]'),null);assert.match(p.doc.querySelector('main [role=alert]').textContent,/tồn kho/);}finally{p.close();}});
