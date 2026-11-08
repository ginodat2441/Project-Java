'use strict';
(async()=>{
 const form=document.querySelector('#forgot-form')||document.querySelector('#reset-form'),message=document.querySelector('#message'),button=form.querySelector('button[type=submit]');
 const instructions=document.querySelector('#recovery-instructions');
 function showMessage(text,state){message.textContent=text;message.className=state;message.setAttribute('role',state==='error'?'alert':'status');}
 const token=new URLSearchParams(location.hash.slice(1)).get('token');if(token)history.replaceState(null,'',location.pathname);
 let csrf;try{const r=await fetch('/api/csrf');if(!r.ok)throw Error();csrf=await r.json();button.disabled=false;}catch{showMessage('Không kết nối được máy chủ. Hãy tải lại trang.','error');return;}
 if(form.id==='reset-form'&&!token){showMessage('Thiếu liên kết khôi phục. Hãy mở liên kết trong thư hoặc gửi yêu cầu mới.','error');button.disabled=true;return;}
 form.onsubmit=async e=>{e.preventDefault();const data=Object.fromEntries(new FormData(form));
  if(form.id==='reset-form'&&data.password!==data.confirmation){showMessage('Hai mật khẩu không khớp.','error');return;}
  button.disabled=true;showMessage('Đang xử lý…','loading');if(instructions)instructions.hidden=true;
  try{
   const reset=form.id==='reset-form',body=reset?{token,password:data.password}:{identifier:data.identifier.trim()};
   const r=await fetch('/api/auth/'+(reset?'reset-password':'forgot-password'),{method:'POST',headers:{'Content-Type':'application/json',[csrf.headerName]:csrf.token},body:JSON.stringify(body)}),result=await r.json();
   if(!r.ok)throw Error(result.message||'Không thực hiện được yêu cầu. Hãy thử lại.');showMessage(result.message,'success');
   if(instructions)instructions.hidden=result.delivery!=='local';
   if(reset){form.reset();form.hidden=true;}else button.disabled=false;
  }catch(error){showMessage(error.message,'error');button.disabled=false;}
 };
})();
