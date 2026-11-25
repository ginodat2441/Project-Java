const {test}=require('node:test'),assert=require('node:assert/strict'),fs=require('node:fs'),path=require('node:path');
const {JSDOM}=require('../.local/dom-tests/node_modules/jsdom');
const root=path.join(__dirname,'..');
const source=(fs.readFileSync(path.join(root,'client/assets/js/client-auth-ui.js'),'utf8')+fs.readFileSync(path.join(root,'client/assets/js/client-auth.js'),'utf8')).replaceAll('location.assign(', 'window.navigate(');
async function setup(customerStatus,adminSuccess=true){
 const dom=new JSDOM(fs.readFileSync(path.join(root,'client/login-register.html'),'utf8'),{url:'http://127.0.0.1:5500/client/login-register.html',runScripts:'outside-only'});
 const w=dom.window,calls=[],destinations=[];w.TextEncoder=TextEncoder;w.navigate=x=>destinations.push(x);
 w.HTMLDialogElement.prototype.showModal=function(){this.open=true;};w.HTMLDialogElement.prototype.close=function(){this.open=false;};
 w.fetch=async(url,options={})=>{calls.push({url,options});let status=200,body;
  if(url.endsWith('/auth/login')){status=customerStatus;body=status===200?{success:true,data:{accessToken:'customer-token',user:{email:'user@example.test'}}}:{success:false};}
  else if(url.endsWith('/api/csrf'))body={token:'csrf-test',headerName:'X-XSRF-TOKEN'};
  else {status=adminSuccess?200:401;body=adminSuccess?{success:true,redirect:'/admin'}:{success:false};}
  return {ok:status===200,status,json:async()=>body};
 };
 w.eval(source);const form=w.document.querySelector('#client-login-form');form.reportValidity=()=>true;
 w.document.getElementById('login-register-login-email').value='admin@example.test';
 w.document.getElementById('login-register-login-password').value='ValidPassword123!';
 w.sessionStorage.setItem('clothingShopToken','old-customer-token');
 form.dispatchEvent(new w.Event('submit',{bubbles:true,cancelable:true}));
 for(let i=0;i<8;i++)await new Promise(r=>setImmediate(r));
 w.document.querySelector('#client-auth-success button')?.click();
 return {w,calls,destinations,close:()=>w.close()};
}
test('customer login stays on customer flow',async()=>{const p=await setup(200);try{assert.equal(p.calls.length,1);assert.deepEqual(p.destinations,['index.html']);assert.equal(p.w.sessionStorage.getItem('clothingShopToken'),'customer-token');}finally{p.close();}});
test('admin fallback creates CSRF-protected session on matching host before redirect',async()=>{const p=await setup(401);try{assert.deepEqual(p.destinations,['http://127.0.0.1:8888/admin']);assert.equal(p.calls[1].url,'http://127.0.0.1:8888/api/csrf');const o=p.calls[2].options;assert.equal(o.credentials,'include');assert.equal(o.headers['X-XSRF-TOKEN'],'csrf-test');assert.equal(new URLSearchParams(o.body).get('username'),'admin@example.test');assert.equal(p.w.sessionStorage.getItem('clothingShopToken'),null);}finally{p.close();}});
test('bad admin password never redirects',async()=>{const p=await setup(401,false);try{assert.deepEqual(p.destinations,[]);assert.match(p.w.document.querySelector('[role="alert"]').textContent,/Email hoặc mật khẩu/);}finally{p.close();}});
test('server failure does not fall through to admin',async()=>{const p=await setup(500);try{assert.equal(p.calls.length,1);assert.deepEqual(p.destinations,[]);}finally{p.close();}});
