'use strict';
const message=document.querySelector('#message'),submit=document.querySelector('button[type=submit]');
const params=new URLSearchParams(location.search),now=Date.now();
const requested=Number(params.get('lockedUntil'));
const unlockAt=Number.isFinite(requested)?Math.min(requested,now+600000):0;
let ready=false,timer;
function updateLogin(){
 const remaining=Math.max(0,Math.ceil((unlockAt-Date.now())/1000));
 submit.disabled=!ready||remaining>0;
 if(remaining>0){
  const time=Math.floor(remaining/60)+':'+String(remaining%60).padStart(2,'0');
  message.textContent='Bạn đã đăng nhập sai quá nhiều lần. Vui lòng chờ '+time+' rồi thử lại.';
  submit.textContent='Thử lại sau '+time;
 }else{
  submit.textContent='Đăng nhập';
  if(unlockAt>0){message.textContent='Bạn có thể đăng nhập lại. Nhập tên tài khoản (không phải email) và mật khẩu.';if(timer)clearInterval(timer);}
 }
}
if(params.has('error'))message.textContent='Tài khoản hoặc mật khẩu không đúng, hoặc tài khoản đã bị khóa. Dùng tên tài khoản, không phải email.';
if(params.has('logout'))message.textContent='Bạn đã đăng xuất.';
updateLogin();if(unlockAt>now)timer=setInterval(updateLogin,1000);
fetch('/api/csrf').then(r=>{if(!r.ok)throw Error();return r.json();}).then(c=>{document.querySelector('#csrf').value=c.token;ready=true;updateLogin();}).catch(()=>{message.textContent='Không thể kết nối máy chủ. Hãy tải lại trang.';submit.disabled=true;if(timer)clearInterval(timer);});
