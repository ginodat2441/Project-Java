(() => {
  'use strict';
  const esc=s=>String(s??'').replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
  const when=value=>value?new Date(value).toLocaleString('vi-VN'):'Đang cập nhật';
  const labels={PENDING:'Chờ cửa hàng duyệt',APPROVED:'Đã chấp nhận',REJECTED:'Đã từ chối'};
  let cache;
  const overview=()=>cache??=(window.ClientStore.api('/api/order-requests').catch(e=>{cache=null;throw e;}));
  async function mount(host,bill,refresh){
    const previous=host.querySelector(':scope > .client-order-support');previous?.remove();
    const panel=document.createElement('section');panel.className='client-order-support';panel.setAttribute('aria-label','Hủy và trả hàng');host.append(panel);
    try{
      const data=(await overview()).find(row=>Number(row.billId)===Number(bill.id));
      if(!panel.isConnected||!data)return;
      const requests=data.requests||[],latest=requests[0];
      panel.innerHTML=`${latest?`<div class="order-request-status" data-state="${esc(latest.STATUS)}"><strong>${latest.REQUEST_TYPE==='CANCEL'?'Yêu cầu hủy đơn':'Yêu cầu trả hàng'} · ${esc(labels[latest.STATUS])}</strong><p>${esc(latest.REASON)}</p>${latest.RESPONSE_NOTE?`<p>Phản hồi cửa hàng: ${esc(latest.RESPONSE_NOTE)}</p>`:''}<small>${when(latest.CREATED_AT)}</small></div>`:''}<div class="order-support-actions"></div>`;
      const actions=panel.querySelector('.order-support-actions');
      for(const [type,allowed,title] of [['CANCEL',data.canCancel,'Yêu cầu hủy đơn'],['RETURN',data.canReturn,'Yêu cầu trả hàng / hoàn tiền']])if(allowed){
        const button=document.createElement('button');button.type='button';button.className='btn btn-outline-secondary';button.textContent=title;button.onclick=()=>requestDialog(bill,type,refresh);actions.append(button);
      }
      if(data.canReturn){const note=document.createElement('p');note.className='order-support-note';note.textContent='Có thể yêu cầu trả hàng đến '+when(data.returnDeadline)+'. Cửa hàng sẽ duyệt trước khi tiếp nhận hàng.';panel.append(note);}
      if(latest?.STATUS==='APPROVED'&&latest.REQUEST_TYPE==='RETURN'){
        const note=document.createElement('p');note.className='order-support-note';note.textContent=['FULL','PARTIAL'].includes(data.returnStatus)?'Cửa hàng đã ghi nhận hàng hoàn. Theo dõi trạng thái thanh toán và phản hồi để biết tiến độ hoàn tiền.':'Cửa hàng đã duyệt. Liên hệ cửa hàng theo phần phản hồi để gửi trả; tiền được xử lý sau khi hàng được tiếp nhận và kiểm tra.';panel.append(note);
      }
      if(requests.length>1){const history=document.createElement('details');history.innerHTML=`<summary>Lịch sử ${requests.length} yêu cầu</summary><ol>${requests.map(r=>`<li><strong>${r.REQUEST_TYPE==='CANCEL'?'Hủy đơn':'Trả hàng'} · ${esc(labels[r.STATUS])}</strong><p>${esc(r.REASON)}</p>${r.RESPONSE_NOTE?`<p>${esc(r.RESPONSE_NOTE)}</p>`:''}<small>${when(r.CREATED_AT)}</small></li>`).join('')}</ol>`;panel.append(history);}
      if(data.refunds?.length){const refunds=document.createElement('div');refunds.className='order-refund-progress';refunds.innerHTML='<h4>Tiến độ hoàn tiền</h4>'+data.refunds.map(r=>`<p>${Number(r.AMOUNT).toLocaleString('vi-VN')} ₫ · ${esc(({PENDING:'Đang chờ chi trả',PAID:'Cửa hàng đã xác nhận hoàn tiền',FAILED:'Chi trả thất bại, cửa hàng cần kiểm tra'})[r.STATUS])}${r.PAID_AT?' · '+when(r.PAID_AT):''}</p>`).join('');panel.append(refunds);}
      if(!panel.textContent.trim())panel.remove();
    }catch(e){panel.innerHTML='<p class="order-support-error" role="alert"></p><button type="button" class="btn btn-outline-secondary">Thử tải lại yêu cầu</button>';panel.querySelector('p').textContent=e.message;panel.querySelector('button').onclick=()=>{cache=null;mount(host,bill,refresh);};}
  }
  function requestDialog(bill,type,refresh){
    const cancel=type==='CANCEL',dialog=document.createElement('dialog');dialog.className='client-request-dialog';dialog.setAttribute('aria-labelledby','order-request-title');
    dialog.innerHTML=`<form><h2 id="order-request-title">${cancel?'Yêu cầu hủy đơn':'Yêu cầu trả hàng'}</h2><p class="order-support-note">Đơn ${esc(bill.billCode)}. ${cancel?'Yêu cầu cần được cửa hàng chấp nhận. Đơn chưa bị hủy ngay sau khi gửi.':'Áp dụng trong 15 ngày từ khi nhận hàng. Hàng và tiền chỉ được xử lý sau khi cửa hàng duyệt, tiếp nhận và kiểm tra.'}</p><label>Lý do<select name="reasonType">${(cancel?['Thay đổi nhu cầu mua hàng','Muốn thay đổi sản phẩm','Thông tin giao hàng chưa đúng','Lý do khác']:['Sản phẩm bị lỗi hoặc hư hỏng','Giao sai hoặc thiếu sản phẩm','Sản phẩm không đúng mô tả','Lý do khác']).map(s=>`<option>${s}</option>`).join('')}</select></label><label>Mô tả chi tiết<textarea name="detail" required minlength="10" maxlength="450" rows="4" placeholder="Mô tả rõ vấn đề để cửa hàng kiểm tra và hỗ trợ"></textarea></label><p class="order-support-note">Từ 10 đến 450 ký tự. Không gửi mật khẩu hoặc thông tin thẻ.</p><p class="order-support-error" role="alert" hidden></p><div class="order-support-actions"><button type="button" data-close>Quay lại</button><button class="btn btn-primary" type="submit">Gửi yêu cầu</button></div></form>`;
    document.body.append(dialog);let busy=false;
    dialog.addEventListener('close',()=>dialog.remove());dialog.addEventListener('cancel',e=>{if(busy)e.preventDefault();});dialog.querySelector('[data-close]').onclick=()=>dialog.close();
    dialog.querySelector('form').onsubmit=async e=>{
      e.preventDefault();if(busy)return;
      const form=e.currentTarget,error=form.querySelector('[role=alert]'),detail=form.elements.detail.value.trim();
      if(detail.length<10){error.hidden=false;error.textContent='Vui lòng mô tả ít nhất 10 ký tự.';form.elements.detail.focus();return;}
      busy=true;form.querySelectorAll('button').forEach(b=>b.disabled=true);error.hidden=true;
      try{
        await window.ClientStore.api('/api/order-requests','POST',{billId:bill.id,type,reason:form.elements.reasonType.value+' — '+detail});cache=null;
        form.innerHTML='<h2 id="order-request-title">Đã gửi yêu cầu</h2><p>Cửa hàng sẽ kiểm tra và phản hồi tại đơn hàng của bạn. Bạn có thể tải lại trang để theo dõi tiến độ.</p><button type="button" class="btn btn-primary">Về đơn hàng</button>';
        form.querySelector('button').onclick=()=>dialog.close();form.querySelector('button').focus();await refresh();
      }catch(e){error.hidden=false;error.textContent=e.message;form.querySelectorAll('button').forEach(b=>b.disabled=false);}
      finally{busy=false;}
    };
    dialog.showModal();dialog.querySelector('select').focus();
  }
  window.ClientOrderRequests={mount,invalidate:()=>cache=null};
})();
