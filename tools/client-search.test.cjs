const {JSDOM}=require('../.local/dom-tests/node_modules/jsdom');
const {test}=require('node:test'),assert=require('node:assert/strict'),fs=require('fs');
const root=require('node:path').join(__dirname,'..')+'/';
const source=fs.readFileSync(root+'client/assets/js/client-search.js','utf8');
const delay=ms=>new Promise(r=>setTimeout(r,ms));
function setup(api){const dom=new JSDOM('<header><form class="search-form"><div><input type="search"><button type="submit">Go</button></div></form></header>',{url:'http://localhost/client/index.html',runScripts:'outside-only'});dom.window.ClientStore={api};dom.window.eval(source);return dom;}
test('autocomplete uses real API and renders names safely; keyboard and clearing work',async()=>{
 let url;const dom=setup(async u=>{url=u;return {total:2,items:[{id:4,productName:'<img onerror=alert(1)>',minPrice:100000,totalStock:2},{id:5,productName:'Áo sơ mi',minPrice:200000,totalStock:0}]};});
 const w=dom.window,input=w.document.querySelector('input');input.value='ao so mi';input.dispatchEvent(new w.Event('input'));await delay(300);
 assert.equal(new URL(url,'http://localhost').searchParams.get('size'),'5');assert.equal(w.document.querySelectorAll('[role=option]').length,2);assert.equal(w.document.querySelector('.clothing-search-name').textContent,'<img onerror=alert(1)>');assert.equal(w.document.querySelector('.clothing-search-all').textContent,'Xem tất cả 2 sản phẩm');
 input.dispatchEvent(new w.KeyboardEvent('keydown',{key:'ArrowUp'}));assert.equal(input.getAttribute('aria-activedescendant'),'clothing-search-options-1-5');input.dispatchEvent(new w.KeyboardEvent('keydown',{key:'Escape'}));assert.equal(input.getAttribute('aria-expanded'),'false');
 w.document.querySelector('.clothing-search-clear').click();assert.equal(input.value,'');dom.window.close();
});
test('stale responses cannot replace the latest search',async()=>{
 let resolveOld;const dom=setup(u=>new URL(u,'http://localhost').searchParams.get('keyword')==='ao'?new Promise(r=>resolveOld=r):Promise.resolve({total:1,items:[{id:2,productName:'Quần mới',minPrice:50}]}));
 const w=dom.window,input=w.document.querySelector('input');input.value='ao';input.dispatchEvent(new w.Event('input'));await delay(280);input.value='quan';input.dispatchEvent(new w.Event('input'));await delay(280);resolveOld({total:1,items:[{id:1,productName:'Áo cũ'}]});await delay(10);assert.equal(w.document.querySelector('.clothing-search-name').textContent,'Quần mới');w.close();
});
test('empty and unavailable API states remain usable',async()=>{const dom=setup(async()=>{throw Error('offline')});const w=dom.window,input=w.document.querySelector('input');input.value='ao';input.dispatchEvent(new w.Event('input'));await delay(300);assert.match(w.document.querySelector('[role=status]').textContent,/Enter/);input.value='';const e=new w.Event('submit',{cancelable:true});w.document.querySelector('form').dispatchEvent(e);assert.ok(e.defaultPrevented);assert.equal(input.getAttribute('aria-expanded'),'true');w.close();});
