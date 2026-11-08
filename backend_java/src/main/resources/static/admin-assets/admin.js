'use strict';
const $=s=>document.querySelector(s),esc=s=>String(s??'').replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
const titles={dashboard:'Tổng quan',reports:'Thống kê doanh thu',accounts:'Tài khoản',categories:'Danh mục',products:'Sản phẩm',vouchers:'Voucher',orders:'Đơn hàng',stock:'Tồn kho',documents:'Phiếu nhập / xuất / hoàn',inventoryReport:'Báo cáo nhập xuất tồn',history:'Lịch sử tồn kho',audit:'Nhật ký quản trị'};
const statusNames=['Chờ xác nhận','Đã xác nhận','Đang giao','Hoàn thành','Đã hủy','Trả hàng','Giao thất bại'];
let csrf,lookups,me,section='dashboard',page=1,tab='customers',filter={},submitEditor;
const money=n=>Number(n||0).toLocaleString('vi-VN',{maximumFractionDigits:0})+' ₫';
const date=s=>s?String(s).replace('T',' ').slice(0,19):'—';
const active=n=>`<span class="pill">${Number(n)===1?'Hoạt động':'Đã ẩn / khóa'}</span>`;
function notice(s,error=false){$('#notice').textContent=s;$('#notice').className=error?'error':'success';}
async function api(path,method='GET',body){
 const options={method,headers:{}};
 if(method!=='GET'){if(!csrf)csrf=await fetch('/api/csrf').then(r=>r.json());options.headers[csrf.headerName]=csrf.token;if(body instanceof FormData)options.body=body;else if(body!==undefined){options.headers['Content-Type']='application/json';options.body=JSON.stringify(body);}}
 const r=await fetch(path,options);if(r.status===401){location.href='/login';throw Error('Phiên đã hết hạn');}
 let data=await r.text();try{data=data?JSON.parse(data):null;}catch{}
 if(!r.ok)throw Error(data?.message||(r.status===403?'Bạn không có quyền hoặc phiên đã thay đổi. Tải lại trang.':'Không thể thực hiện thao tác ('+r.status+').'));
 return data;
}
function table(rows,cols,actions){if(!rows.length)return '<div class="empty">Chưa có dữ liệu phù hợp.</div>';return `<div class="table-wrap"><table><thead><tr>${cols.map(c=>`<th>${esc(c[1])}</th>`).join('')}${actions?'<th>Thao tác</th>':''}</tr></thead><tbody>${rows.map(r=>`<tr>${cols.map(c=>`<td>${c[2]?c[2](r[c[0]],r):esc(r[c[0]])}</td>`).join('')}${actions?`<td>${actions(r)}</td>`:''}</tr>`).join('')}</tbody></table></div>`;}
function button(label,action,id,extra=''){return `<button type="button" data-action="${action}" data-id="${esc(id)}" ${extra}>${label}</button>`;}
function pager(result){return `<div class="pager">${button('← Trước','prev','',page<=1?'disabled':'')}<span>Trang ${page} · ${result.total} bản ghi</span>${button('Sau →','next','',page*result.size>=result.total?'disabled':'')}</div>`;}
function options(rows,selected,empty=true){return (empty?'<option value="">Chưa chọn</option>':'')+rows.map(r=>`<option value="${r.ID}" ${String(r.ID)===String(selected)?'selected':''}>${esc(r.NAME||r.ROLE_NAME)}</option>`).join('');}
function input(name,label,value='',type='text',required=true,extra=''){return `<label>${label}<input name="${name}" type="${type}" value="${esc(value)}" ${required?'required':''} ${extra}></label>`;}
function select(name,label,rows,value,empty=false){return `<label>${label}<select name="${name}" ${empty?'':'required'}>${options(rows,value,empty)}</select></label>`;}
function productCodeInput(product){return input('code','Mã sản phẩm (tự tăng)',product.PRODUCT_CODE||'','text',false,'readonly placeholder="Tự cấp khi lưu sản phẩm"');}
function movementName(value){return window.AdminV8?.kind[value]||{RECEIPT:'Nhập kho',ISSUE:'Xuất kho',SALE:'Giữ / xuất bán',CANCEL:'Hủy đơn hoàn giữ',RETURN:'Nhận hoàn',COUNT:'Kiểm kê',REVERSAL:'Đảo phiếu'}[value]||'Dữ liệu cũ';}
function variantSkuInput(variant){return input('sku','SKU (tự tạo)',variant.SKU||'','text',false,'readonly placeholder="Chọn màu và nhập size"');}
function refreshVariantSku(row,code){
 if(row.dataset.id)return;
 const color=lookups.colors.find(c=>String(c.ID)===row.querySelector('[name=colorId]').value),size=row.querySelector('[name=size]').value.trim();
 const part=s=>s.normalize('NFD').replace(/[\u0300-\u036f]/g,'').replace(/đ/g,'d').replace(/Đ/g,'D').toUpperCase().replace(/[^A-Z0-9]+/g,'-').replace(/^-|-$/g,'').slice(0,12);
 row.querySelector('[name=sku]').value=color&&size?(code||'SP[tự tăng]')+'-'+(part(color.NAME)||'C'+color.ID)+'-'+(part(size)||'SIZE'):'';
}
function namedLookup(name,label,rows,id){const current=rows.find(r=>String(r.ID)===String(id));return `<label>${label}<input name="${name}" list="${name}-choices" value="${esc(current?.NAME||'')}" placeholder="Nhập mới hoặc chọn từ gợi ý…" maxlength="150" required autocomplete="off"><datalist id="${name}-choices">${rows.map(r=>`<option value="${esc(r.NAME)}"></option>`).join('')}</datalist></label>`;}
function productPhotoFields(){return `<section class="product-photo-section"><h3>Hình ảnh sản phẩm</h3><p class="muted">Chọn nhiều ảnh, ảnh đại diện và màu ngay tại đây. Ảnh được tải lên khi bấm Lưu thay đổi. JPEG, PNG, WebP · tối đa 5 MB/ảnh, 25 MB/lần lưu.</p>${input('productFiles','Thêm ảnh','','file',false,'multiple accept="image/jpeg,image/png,image/webp"')}<div id="product-photos"></div></section><p class="muted">Biến thể mới bắt đầu tồn 0. Nhập hàng bằng phiếu kho được ADMIN xác nhận.</p>`;}
function bindProductPhotos(product){
 const container=$('#product-photos'),picker=$('#editor [name=productFiles]'),pending=[];let serial=0,cover=product.THUMBNAIL_URL?'existing':null;
 const imageUrl=u=>u?.startsWith('assets/')?'/store/'+u:u;
 function render(){container.innerHTML=(product.THUMBNAIL_URL?`<label class="draft-photo existing"><input type="radio" name="draftCover" value="existing" ${cover==='existing'?'checked':''}><img src="${esc(imageUrl(product.THUMBNAIL_URL))}" alt="Ảnh đại diện hiện tại"><span>Giữ ảnh đại diện hiện tại</span></label>`:'')+pending.map(p=>`<div class="draft-photo" data-photo="${p.key}"><img src="${esc(p.url)}" alt="${esc(p.file.name)}"><span>${esc(p.file.name)}</span><label>Ảnh đại diện<input type="radio" name="draftCover" value="${p.key}" ${cover===p.key?'checked':''}></label>${select('photoColor','Màu (hoặc ảnh chung)',lookups.colors,p.colorId,true)}${input('photoOrder','Thứ tự',p.order,'number',true,'min="0"')}<button type="button" data-photo-remove="${p.key}" class="secondary">Gỡ ảnh</button></div>`).join('');}
 picker.onchange=()=>{const files=[...picker.files];if(pending.length+files.length>20){$('#form-error').textContent='Mỗi lần lưu tối đa 20 ảnh.';return;}if(files.some(f=>f.size>5*1024*1024||f.size===0)){ $('#form-error').textContent='Mỗi ảnh phải từ 1 byte đến 5 MB.';return;}
  for(const file of files){const key=String(++serial);pending.push({key,file,url:URL.createObjectURL(file),colorId:null,order:pending.length});if(cover==null)cover=key;}picker.value='';$('#form-error').textContent='';render();};
 container.onchange=e=>{if(e.target.name==='draftCover'){cover=e.target.value;return;}const row=e.target.closest('[data-photo]'),p=pending.find(p=>p.key===row?.dataset.photo);if(p){p.colorId=row.querySelector('[name=photoColor]').value||null;p.order=Number(row.querySelector('[name=photoOrder]').value);}};
 container.onclick=e=>{const b=e.target.closest('[data-photo-remove]');if(!b)return;const index=pending.findIndex(p=>p.key===b.dataset.photoRemove);if(index<0)return;const removed=pending.splice(index,1)[0];URL.revokeObjectURL(removed.url);if(cover===removed.key)cover=product.THUMBNAIL_URL?'existing':pending[0]?.key??null;render();};
 $('#dialog').addEventListener('close',()=>pending.forEach(p=>URL.revokeObjectURL(p.url)),{once:true});render();
 return {pending,metadata:()=>pending.map(p=>({colorId:p.colorId?Number(p.colorId):null,order:p.order,cover:p.key===cover}))};
}
function categoryChoices(){return lookups.categories.map(c=>{const names=[c.NAME],seen=new Set([String(c.ID)]);let parent=c.PARENT_ID;while(parent!=null&&!seen.has(String(parent))){seen.add(String(parent));const p=lookups.categories.find(x=>String(x.ID)===String(parent));if(!p)break;names.unshift(p.NAME);parent=p.PARENT_ID;}return {id:String(c.ID),label:names.join(' → ')+(Number(c.STATUS)===0?' (đã ẩn)':'')};});}
function categorySearch(value){const selected=categoryChoices().find(c=>c.id===String(value));return `<div class="category-search"><label for="product-category-search">Danh mục</label><div class="category-input"><input id="product-category-search" type="search" autocomplete="off" placeholder="Gõ tên để tìm danh mục…" role="combobox" aria-autocomplete="list" aria-expanded="false" aria-controls="product-category-results" value="${esc(selected?.label||'')}"><button type="button" id="category-toggle" aria-label="Mở danh sách danh mục" aria-controls="product-category-results">⌄</button></div><input type="hidden" name="categoryId" value="${esc(selected?.id||'')}"><div id="product-category-results" class="category-results" role="listbox" aria-label="Kết quả danh mục" hidden></div></div>`;}
function bindCategorySearch(){
 const field=$('#product-category-search'),results=$('#product-category-results'),id=$('#editor [name=categoryId]');let choices=categoryChoices(),matches=[],cursor=-1;
 const normalize=s=>s.normalize('NFD').replace(/[\u0300-\u036f]/g,'').replace(/đ/g,'d').replace(/Đ/g,'D').toLowerCase();
 const close=()=>{results.hidden=true;field.setAttribute('aria-expanded','false');field.removeAttribute('aria-activedescendant');cursor=-1;};
 const choose=index=>{const c=matches[index];if(!c)return;field.value=c.id?c.label:'';id.value=c.id;field.setCustomValidity('');field.focus();close();};
 const search=(all=false)=>{const q=all?'':normalize(field.value.trim());matches=q?choices.filter(c=>normalize(c.label).includes(q)).slice(0,20):[{id:'',label:'Chưa chọn'},...choices];cursor=-1;results.innerHTML=matches.length?matches.map((c,i)=>`<button type="button" role="option" aria-selected="false" id="category-result-${i}" data-result="${i}">${esc(c.label)}</button>`).join(''):'<p role="status">Không tìm thấy danh mục phù hợp.</p>';results.hidden=false;field.setAttribute('aria-expanded','true');};
 field.oninput=()=>{id.value='';field.setCustomValidity(field.value.trim()?'Chọn một danh mục trong kết quả tìm kiếm, hoặc xóa nội dung để bỏ chọn.':'');search();};
 field.onfocus=()=>search();
 const toggle=$('#category-toggle');toggle.onmousedown=e=>e.preventDefault();toggle.onclick=()=>{if(results.hidden){field.focus();search(true);}else close();};
 field.onkeydown=e=>{if(e.key==='Escape'){close();return;}if((e.key==='ArrowDown'||e.key==='ArrowUp')&&matches.length){e.preventDefault();if(results.hidden)search();cursor=(cursor+(e.key==='ArrowDown'?1:matches.length-1)+matches.length)%matches.length;results.querySelectorAll('[role=option]').forEach((b,i)=>b.setAttribute('aria-selected',String(i===cursor)));field.setAttribute('aria-activedescendant','category-result-'+cursor);}else if(e.key==='Enter'&&!results.hidden){e.preventDefault();choose(cursor<0?0:cursor);}};
 results.onmousedown=e=>e.preventDefault();results.onclick=e=>{const b=e.target.closest('[data-result]');if(b)choose(Number(b.dataset.result));};
 field.onblur=close;
 const wrapper=field.closest('.category-search');
 wrapper.insertAdjacentHTML('afterend',`<div class="category-create"><button type="button" id="category-add" aria-expanded="false" aria-controls="category-create-panel">+ Thêm danh mục</button><div id="category-create-panel" hidden><label for="category-new-name">Tên danh mục mới</label><input id="category-new-name" maxlength="150" autocomplete="off"><label for="category-new-parent">Danh mục cha (không bắt buộc)</label><select id="category-new-parent"></select><p id="category-create-error" class="error" role="alert"></p><button type="button" id="category-create-save">Tạo và chọn</button> <button type="button" id="category-create-cancel">Hủy</button></div></div>`);
 const group=document.createElement('div');group.className='category-field';wrapper.before(group);group.append(wrapper,wrapper.nextElementSibling);
 const panel=$('#category-create-panel'),add=$('#category-add'),name=$('#category-new-name'),parent=$('#category-new-parent'),error=$('#category-create-error'),save=$('#category-create-save');
 const hide=()=>{panel.hidden=true;add.setAttribute('aria-expanded','false');};
 add.onclick=()=>{if(!panel.hidden){hide();return;}close();name.value=id.value?'':field.value.trim();parent.innerHTML='<option value="">Không có danh mục cha</option>'+choices.map(c=>`<option value="${esc(c.id)}">${esc(c.label)}</option>`).join('');error.textContent='';panel.hidden=false;add.setAttribute('aria-expanded','true');name.focus();};
 $('#category-create-cancel').onclick=hide;
 const create=async()=>{if(save.disabled)return;const categoryName=name.value.trim();if(!categoryName){error.textContent='Vui lòng nhập tên danh mục.';name.focus();return;}save.disabled=true;add.disabled=true;$('#category-create-cancel').disabled=true;const productSave=$('#editor footer button');productSave.disabled=true;error.textContent='';try{const parentId=parent.value?Number(parent.value):null;const newId=await api('/api/admin/categories','POST',{name:categoryName,parentId,status:1});lookups.categories.push({ID:newId,NAME:categoryName,PARENT_ID:parentId,STATUS:1});choices=categoryChoices();id.value=String(newId);field.value=choices.find(c=>c.id===String(newId)).label;field.setCustomValidity('');hide();field.focus();close();}catch(e){error.textContent=e.message;}finally{save.disabled=false;add.disabled=false;$('#category-create-cancel').disabled=false;productSave.disabled=false;}};
 save.onclick=create;
 [name,parent].forEach(control=>control.onkeydown=e=>{if(e.key==='Enter'){e.preventDefault();create();}});
}
const statusOptions=[{ID:1,NAME:'Hoạt động / đang bán'},{ID:0,NAME:'Ẩn / ngừng bán'}];
function formData(form){return Object.fromEntries(new FormData(form));}
function modal(title,html,save){$('#dialog-title').textContent=title;$('#fields').onclick=null;$('#fields').oninput=null;$('#fields').innerHTML=html;$('#form-error').textContent='';submitEditor=typeof save==='function'?save:null;$('#editor footer').hidden=!submitEditor;$('#editor footer button').disabled=!submitEditor;$('#dialog').showModal();}
$('#editor').onsubmit=async e=>{e.preventDefault();if(typeof submitEditor!=='function'||!e.target.reportValidity())return;const b=$('#editor footer button');b.disabled=true;try{await submitEditor(formData(e.target));$('#dialog').close();notice('Đã lưu thành công.');await load();}catch(e){$('#form-error').textContent=e.message;}finally{b.disabled=typeof submitEditor!=='function';}};
$('#close').onclick=()=>$('#dialog').close();
$('#logout').onclick=async()=>{try{await api('/logout','POST');location.href='/login';}catch(e){notice(e.message,true);}};
$('#nav').onclick=e=>{const b=e.target.closest('[data-section]');if(b){section=b.dataset.section;page=1;filter={};load();}};
$('#content').addEventListener('submit',e=>{if(e.target.id==='filter'){e.preventDefault();filter=formData(e.target);page=1;load();}});
$('#content').onclick=async e=>{const b=e.target.closest('[data-action]');if(!b)return;try{await action(b.dataset.action,Number(b.dataset.id));}catch(e){notice(e.message,true);}};
function searchToolbar(extra='',create=''){return `<form id="filter" class="toolbar"><input name="q" value="${esc(filter.q||'')}" placeholder="Tìm kiếm tên hoặc mã…" aria-label="Tìm kiếm">${extra}<button>Tìm kiếm</button>${create?button('+ '+create,'create',''):''}</form>`;}
async function load(){
 $('#title').textContent=titles[section];document.querySelectorAll('#nav button').forEach(b=>b.classList.toggle('active',b.dataset.section===section));
 $('#content').innerHTML='<p class="empty">Đang tải dữ liệu…</p>';
 try{
 let html='';
 if(window.AdminV8?.[section]?.render){html=await window.AdminV8[section].render();}
 else if(window.AdminModules?.[section]){html=await window.AdminModules[section].render();}
 else if(section==='dashboard'){
  const d=await api('/api/admin/dashboard?threshold='+Number(filter.threshold||5));
  html=`<form id="filter" class="toolbar">${input('threshold','Ngưỡng sắp hết hàng',filter.threshold||5,'number',true,'min="0"')}<button>Cập nhật</button></form><div class="grid"><section class="card"><h2>Doanh thu theo tháng</h2><p class="muted">Doanh thu ròng theo tháng ghi nhận trong năm hiện tại; trừ hàng hoàn, không gồm phí giao.</p>${table(d.revenue,[['MONTH','Tháng'],['REVENUE','Doanh thu',money]])}</section><section class="card"><h2>Đơn theo trạng thái</h2>${table(d.statuses,[['STATUS','Trạng thái',v=>esc(statusNames[v])],['TOTAL','Số đơn']])}</section><section class="card"><h2>Sản phẩm bán chạy</h2>${table(d.topProducts,[['PRODUCT_NAME','Sản phẩm'],['SOLD','SL ròng'],['RETURNED','SL hoàn'],['REVENUE','Doanh thu ròng',money]])}</section><section class="card"><h2>Biến thể sắp hết hàng</h2>${table(d.lowStock,[['SKU','SKU'],['PRODUCT_NAME','Sản phẩm'],['SIZE_NAME','Size'],['QUANTITY','Tồn']])}</section></div>`;
 } else if(section==='categories'){
  const rows=await api('/api/admin/categories');lookups=await api('/api/admin/lookups');
  html=`<div class="toolbar">${button('+ Thêm danh mục','create','')}</div>`+table(rows,[['ID','ID'],['CATEGORY_NAME','Danh mục'],['PARENT_ID','Danh mục cha',v=>esc(rows.find(r=>r.ID===v)?.CATEGORY_NAME||'—')],['STATUS','Trạng thái',active]],r=>button('Sửa / Ẩn / Khôi phục','edit',r.ID));
 } else if(section==='products'){
  const qs=new URLSearchParams({page,q:filter.q||''});if(filter.categoryId)qs.set('categoryId',filter.categoryId);if(filter.status!=='undefined'&&filter.status)qs.set('status',filter.status);
  for(const k of ['from','to'])if(filter[k])qs.set(k,filter[k]);
  const data=await api('/api/admin/products?'+qs);
  const totalsRow=`<tr id="product-totals"><th colspan="7">Tổng cộng</th><td>${data.totals?.INVENTORY_VALUE==null?'Chưa đủ giá vốn':money(data.totals.INVENTORY_VALUE)}</td><td>${money(data.totals?.KNOWN_VALUE||0)}</td><td>${esc(data.totals?.UNKNOWN_COST_SKUS||0)}</td><td>${money(data.totals?.TOTAL_REVENUE||0)}</td><td colspan="2"></td></tr>`;
  html=searchToolbar(select('categoryId','Danh mục',lookups.categories,filter.categoryId,true)+select('status','Trạng thái',statusOptions,filter.status,true)+input('from','Doanh thu từ ngày',filter.from||'','date',false)+input('to','Đến ngày',filter.to||'','date',false),'Sản phẩm')+`<p class="report-note">Giá trị tồn kho là số hiện tại (gồm hàng giữ tại kho). Bộ lọc ngày chỉ áp dụng cho doanh thu; hàng hoàn ghi giảm vào ngày xác nhận nhận hoàn. Tổng cộng gồm tất cả mã khớp bộ lọc, qua mọi trang.</p>`+table(data.items,[['ID','ID'],['PRODUCT_CODE','Mã'],['PRODUCT_NAME','Tên sản phẩm'],['CATEGORY_NAME','Danh mục'],['PHYSICAL','Tồn tại kho'],['RESERVED','Giữ'],['STOCK','Sẵn bán'],['INVENTORY_VALUE','Giá trị tồn kho',v=>v==null?'Chưa đủ giá vốn':money(v)],['KNOWN_VALUE','Giá trị có căn cứ',money],['UNKNOWN_COST_SKUS','SKU thiếu giá'],['TOTAL_REVENUE','Tổng doanh thu',v=>money(v??0)],['STATUS','Trạng thái',active]],r=>button('Sửa','edit',r.ID)+button('Ảnh','images',r.ID)+button('Doanh thu SKU','product-revenue',r.ID)).replace('</tbody>',`</tbody><tfoot>${totalsRow}</tfoot>`)+(data.items.length?'':`<div class="table-wrap"><table><tbody>${totalsRow}</tbody></table></div>`)+pager(data);
 } else if(section==='vouchers'){
  const rows=await api('/api/admin/vouchers');window.vouchers=rows;
  html=`<div class="toolbar">${button('+ Thêm voucher','create','')}</div>`+table(rows,[['CODE','Mã'],['DISCOUNT_TYPE','Loại'],['DISCOUNT_VALUE','Mức giảm'],['MIN_ORDER','Đơn tối thiểu',money],['STARTS_AT','Từ',date],['ENDS_AT','Đến',date],['USED_COUNT','Đã dùng'],['USAGE_LIMIT','Giới hạn'],['STATUS','Trạng thái',active]],r=>button('Sửa / Bật tắt','edit',r.ID));
 } else if(section==='accounts'){
  html=`<div class="tabs">${button('Khách hàng','customers','')}${button('Nhân viên quản trị','managers','')}</div>`;
  if(tab==='customers'){
   const data=await api('/api/admin/customers?'+new URLSearchParams({page,q:filter.q||''}));
   html+=searchToolbar()+table(data.items,[['FULL_NAME','Khách hàng'],['EMAIL','Email'],['PHONE','Điện thoại'],['STATUS','Trạng thái',active]],r=>button('Chi tiết','detail',r.ID)+button(r.STATUS?'Khóa':'Mở khóa',r.STATUS?'lock':'unlock',r.ID))+pager(data);
  }else{
   const rows=await api('/api/admin/managers?'+new URLSearchParams(filter));window.managers=rows;
   html+=searchToolbar(select('role','Vai trò',[{ID:'ADMIN',NAME:'Quản trị'},{ID:'STAFF',NAME:'Nhân viên'}],filter.role,true)+select('status','Trạng thái',statusOptions,filter.status,true),'Nhân viên')+table(rows,[['USERNAME','Tài khoản'],['FULL_NAME','Họ tên'],['EMAIL','Email'],['ROLE_CODE','Vai trò'],['STATUS','Trạng thái',active]],r=>button('Quyền / Khóa','edit',r.ID));
  }
 } else if(section==='orders'){
  const qs=new URLSearchParams({page,q:filter.q||''});for(const k of ['status','from','to','paymentMethod','paymentStatus','returnStatus'])if(filter[k]!==undefined&&filter[k]!=='')qs.set(k,filter[k]);
  const data=await api('/api/admin/orders?'+qs);
  html=searchToolbar(select('status','Trạng thái',statusNames.map((NAME,ID)=>({ID,NAME})),filter.status,true)+select('paymentMethod','Phương thức',[{ID:'COD',NAME:'COD'},{ID:'BANK',NAME:'Chuyển khoản'}],filter.paymentMethod,true)+select('paymentStatus','Thanh toán',[{ID:0,NAME:'Chưa thu'},{ID:1,NAME:'Đã thu'},{ID:2,NAME:'Đã hoàn'}],filter.paymentStatus,true)+select('returnStatus','Hàng hoàn',['NONE','REQUESTED','PARTIAL','FULL'].map(ID=>({ID,NAME:ID})),filter.returnStatus,true)+input('from','Từ ngày',filter.from||'','date',false)+input('to','Đến ngày',filter.to||'','date',false))+table(data.items,[['BILL_CODE','Mã đơn'],['RECEIVER_NAME','Người nhận'],['TOTAL_AMOUNT','Tổng tiền đơn',money],['STATUS','Trạng thái',v=>esc(statusNames[v])],['PAYMENT_STATUS','Trạng thái thanh toán',v=>v===1?'Đã nhận':'Chưa nhận'],['CREATED_AT','Ngày đặt',date]],r=>button('Chi tiết','detail',r.ID))+pager(data);
 } else if(section==='stock'){
  const qs=new URLSearchParams({page});for(const k of ['threshold','q','productId','minValue','maxValue'])if(filter[k]!==undefined&&filter[k]!=='')qs.set(k,filter[k]);
  const data=await api('/api/admin/stock?'+qs);
  const value=v=>v==null?'Chưa đủ giá vốn':money(v),summary=(title,total)=>`<div class="metric"><span>${title}</span><strong>${value(total?.INVENTORY_VALUE)}</strong>${Number(total?.UNKNOWN_COST_PRODUCTS||0)?`<small>${esc(total.UNKNOWN_COST_PRODUCTS)} mã thiếu giá vốn</small>`:''}</div>`;
  const totalRow=`<tr id="stock-totals"><th colspan="9">Tổng cộng theo bộ lọc (tất cả trang)</th><td>${value(data.totals?.INVENTORY_VALUE)}</td><td colspan="2"></td></tr>`;
  html=`<div class="metrics">${summary('Giá trị tồn kho tất cả sản phẩm',data.globalTotals)}${summary('Giá trị tồn kho theo bộ lọc',data.totals)}</div><p class="report-note">Giá trị tồn hiện tại = tồn sẵn bán × giá vốn bình quân. Khoảng giá trị lọc theo tổng của mã sản phẩm, gồm mọi màu/size. Giá trị từng dòng là của biến thể; không cộng lặp cột tổng theo mã.</p><form id="filter" class="toolbar">${input('q','Mã / tên sản phẩm / SKU',filter.q||'','text',false)}${select('productId','Sản phẩm',data.products||[],filter.productId,true)}${input('threshold','Tồn tối đa',filter.threshold??'','number',false,'min="0"')}${input('minValue','Giá trị sản phẩm từ (VNĐ)',filter.minValue??'','number',false,'min="0" step="0.01"')}${input('maxValue','Đến giá trị (VNĐ)',filter.maxValue??'','number',false,'min="0" step="0.01"')}<button>Lọc</button><button type="button" id="clear-stock-filter" class="secondary">Xóa bộ lọc</button></form>`+table(data.items,[['PRODUCT_CODE','Mã sản phẩm'],['PRODUCT_NAME','Sản phẩm'],['COLOR_NAME','Màu'],['SIZE_NAME','Size'],['SKU','SKU'],['QUANTITY','Tồn sẵn bán'],['RESERVED','Giữ cho đơn'],['PHYSICAL','Còn tại kho'],['AVERAGE_COST','Giá vốn bình quân',value],['INVENTORY_VALUE','Giá trị tồn biến thể',value],['PRODUCT_INVENTORY_VALUE','Tổng giá trị theo mã',value]],r=>button('Lập phiếu kho','edit',r.ID)+(r.AVERAGE_COST==null&&Number(r.QUANTITY)>0&&me.roles.some(role=>role.authority==='ROLE_ADMIN')?button('Bổ sung giá vốn','opening-cost',r.PRODUCT_ID):'')).replace('</tbody>',`</tbody><tfoot>${totalRow}</tfoot>`)+(data.items.length?'':`<table><tbody>${totalRow}</tbody></table>`)+pager(data);
 } else if(section==='history'||section==='audit'){
  const data=await api('/api/admin/'+(section==='history'?'inventory-history':'audit')+'?page='+page);
  html=table(data.items,section==='history'?[['DOC_CODE','Phiếu'],['MOVEMENT_TYPE','Nghiệp vụ',v=>esc(movementName(v))],['BEFORE_QUANTITY','Tồn trước'],['AFTER_QUANTITY','Tồn sau'],['SKU','SKU'],['QUANTITY_CHANGE','Thay đổi'],['REASON','Lý do'],['USERNAME','Người thao tác'],['BILL_ID','Đơn hàng'],['CREATED_AT','Thời điểm',date]]:[['USERNAME','Người thao tác'],['ACTION','Hành động'],['OBJECT_TYPE','Đối tượng'],['OBJECT_ID','ID'],['CREATED_AT','Thời điểm',date]])+pager(data);
 }
 $('#content').innerHTML=html;
 if(section==='stock')$('#clear-stock-filter').onclick=()=>{filter={};page=1;load();};
 if(section==='dashboard')$('#content').insertAdjacentHTML('afterbegin',`<div class="toolbar">${button('Thống kê theo ngày / tháng / quý / năm','open-reports','')}</div>`);
 }catch(e){$('#content').innerHTML='<div class="card">Không tải được dữ liệu. Hãy kiểm tra kết nối và thử lại.</div>';notice(e.message,true);}
}
async function action(a,id){
 if(a==='prev'||a==='next'){page+=a==='next'?1:-1;await load();return;}
 if(a==='customers'||a==='managers'){tab=a;page=1;await load();return;}
 if(a==='open-reports'){section='reports';page=1;filter={};await load();return;}
 if(window.AdminV8?.[section]?.action&&(await window.AdminV8[section].action(a,id))!==false)return;
 if(window.AdminModules?.[section])return window.AdminModules[section].action(a,id);
 if(section==='stock'&&a==='edit')return window.AdminModules.documents.create(null,null,id);
 if(section==='stock'&&a==='opening-cost'){
  const data=await api('/api/admin/opening-cost/'+id),p=data.product,missing=data.receipts.filter(l=>l.UNIT_COST==null);
  modal('Bổ sung giá vốn · '+p.PRODUCT_CODE,`<p>${esc(p.PRODUCT_NAME)} · Tổng tồn hiện tại: <b>${esc(data.quantity)}</b></p><p>Giá nhập mới đã được ghi nhận. Nhập giá vốn của tồn cũ và những phiếu cũ còn thiếu để tính đủ bình quân cho toàn mã. Số lượng tồn không thay đổi.</p><div class="grid">${data.openingQuantity>0?input('openingUnitCost','Đơn giá vốn của '+data.openingQuantity+' sản phẩm tồn đầu chưa có chứng từ','','number',true,'min="0" max="999999999999.99" step="0.01"'):''}${missing.map(l=>`<label>Phiếu ${esc(l.DOC_CODE)} · ${esc(l.SKU)} · SL ${esc(l.QUANTITY)}<input type="number" data-cost-line="${esc(l.ID)}" min="0" max="999999999999.99" step="0.01" required></label>`).join('')}${input('reason','Căn cứ / lý do bổ sung','','text',true,'maxlength="500"')}</div>${table(data.receipts,[['DOC_CODE','Phiếu nhập'],['SKU','SKU'],['QUANTITY','Số lượng'],['UNIT_COST','Đơn giá đã ghi',v=>v==null?'Cần bổ sung':money(v)]])}<p id="opening-cost-preview" aria-live="polite"></p><p class="muted">ADMIN xác nhận theo chứng từ thực tế. Thao tác được ghi nhật ký và không sửa giá nhập đã có.</p>`,async d=>{await api('/api/admin/opening-cost/'+id,'POST',{openingUnitCost:data.openingQuantity>0?Number(d.openingUnitCost):null,reason:d.reason,receipts:missing.map(l=>({lineId:l.ID,unitCost:Number(document.querySelector(`[data-cost-line="${l.ID}"]`).value)}))});});
  const calculate=()=>{const inputs=[...document.querySelectorAll('#fields input[type=number]')],ready=inputs.every(el=>el.value!==''&&el.checkValidity());if(!ready){$('#opening-cost-preview').textContent='Nhập đủ đơn giá còn thiếu để xem giá trị tồn và giá vốn bình quân.';return;}const receiptTotal=data.receipts.reduce((sum,l)=>sum+l.QUANTITY*Number(l.UNIT_COST??document.querySelector(`[data-cost-line="${l.ID}"]`).value),0),total=receiptTotal+data.openingQuantity*Number($('#fields [name=openingUnitCost]')?.value||0);$('#opening-cost-preview').textContent='Giá trị tồn sau bổ sung: '+money(total)+' · Giá vốn bình quân: '+money(total/data.quantity);};
  $('#fields').oninput=calculate;calculate();return;
 }
 if(a==='lock'||a==='unlock'){await api(`/api/admin/customers/${id}/status?status=${a==='lock'?0:1}`,'PUT');notice('Đã cập nhật tài khoản.');await load();return;}
 if(section==='categories'){
  const rows=await api('/api/admin/categories'),r=rows.find(r=>r.ID===id)||{},impact=id?await api('/api/admin/categories/'+id+'/impact'):null;
  modal(id?'Sửa danh mục':'Thêm danh mục',`${impact?`<p class="report-note">Ẩn nhánh ảnh hưởng ${esc(impact.CHILDREN)} danh mục con và ${esc(impact.PRODUCTS)} sản phẩm trên cửa hàng.</p>`:''}<div class="grid">${input('name','Tên danh mục',r.CATEGORY_NAME)}${select('parentId','Danh mục cha',rows.filter(x=>x.ID!==id).map(x=>({ID:x.ID,NAME:x.CATEGORY_NAME})),r.PARENT_ID,true)}${select('status','Trạng thái',statusOptions,r.STATUS??1)}</div>`,d=>api('/api/admin/categories'+(id?'/'+id:''),id?'PUT':'POST',{...d,parentId:d.parentId?Number(d.parentId):null,status:Number(d.status)}));
 } else if(section==='products'){
  if(a==='product-revenue')return window.AdminModules.reports.action('product-report',id);
  if(a==='images')return imageEditor(id);
  lookups=await api('/api/admin/lookups');
  const p=id?await api('/api/admin/products/'+id):{STATUS:0,variants:[]};let photos;
  modal(id?'Sửa sản phẩm':'Thêm sản phẩm',`<div class="grid">${productCodeInput(p)}${input('name','Tên sản phẩm',p.PRODUCT_NAME)}${namedLookup('brandName','Th\u01b0\u01a1ng hi\u1ec7u',lookups.brands,p.BRAND_ID)}${namedLookup('materialName','Ch\u1ea5t li\u1ec7u',lookups.materials,p.MATERIAL_ID)}${categorySearch(p.CATEGORY_ID)}${select('gender','Đối tượng',[{ID:0,NAME:'Nữ'},{ID:1,NAME:'Nam'},{ID:2,NAME:'Unisex'}],p.TARGET_GENDER??2)}${select('status','Trạng thái',statusOptions,p.STATUS)}<input type="hidden" name="thumbnailUrl" value="${esc(p.THUMBNAIL_URL)}"><label class="wide">Mô tả<textarea name="description">${esc(p.DESCRIPTION)}</textarea></label></div>${productPhotoFields()}<h3>Biến thể</h3><p class="muted">Mỗi dòng là một màu và size. Thêm dòng để bán nhiều màu/size cho cùng sản phẩm.</p><details class="color-creator"><summary>+ Thêm màu mới</summary><div class="toolbar"><label>Tên màu<input name="newColorName" maxlength="100" placeholder="Ví dụ: Xanh navy, Be, Đỏ đô"></label><label>Mã màu (không bắt buộc)<input name="newColorHex" maxlength="7" placeholder="#1A237E"></label><button type="button" data-action="create-color">Thêm màu</button></div><p id="color-message" role="status"></p></details><div id="variants"></div>${button('+ Thêm biến thể','add-variant','')}`,async d=>{
   const variants=[...document.querySelectorAll('.variant')].map(row=>({id:row.dataset.id?Number(row.dataset.id):null,colorId:Number(row.querySelector('[name=colorId]').value),size:row.querySelector('[name=size]').value,sku:row.querySelector('[name=sku]').value,price:Number(row.querySelector('[name=price]').value),quantity:Number(row.dataset.stockQuantity),status:Number(row.querySelector('[name=vstatus]').value)}));
   const product={code:d.code,name:d.name,brandName:d.brandName.trim(),materialName:d.materialName.trim(),categoryId:d.categoryId?Number(d.categoryId):null,gender:Number(d.gender),status:Number(d.status),description:d.description,thumbnailUrl:d.thumbnailUrl||null,variants};
   if(photos.pending.length){const body=new FormData();body.append('product',new Blob([JSON.stringify(product)],{type:'application/json'}),'product.json');body.append('images',new Blob([JSON.stringify(photos.metadata())],{type:'application/json'}),'images.json');photos.pending.forEach(p=>body.append('files',p.file));await api('/api/admin/products'+(id?'/'+id:'')+'/with-images',id?'PUT':'POST',body);}
   else await api('/api/admin/products'+(id?'/'+id:''),id?'PUT':'POST',product);
  });
  const addVariant=v=>{const row=document.createElement('div');row.className='variant';row.dataset.id=v.ID||'';row.dataset.stockQuantity=String(v.QUANTITY??0);row.innerHTML=select('colorId','Màu',lookups.colors,v.COLOR_ID,true)+input('size','Size',v.SIZE_NAME||'M')+variantSkuInput(v)+input('price','Giá bán',v.PRICE??0,'number',true,'min="0" step="0.01"')+select('vstatus','Bán',statusOptions,v.STATUS??1)+(v.ID?'':button('×','remove-variant',''));row.querySelector('[name=colorId]').required=true;row.addEventListener('input',()=>refreshVariantSku(row,p.PRODUCT_CODE));row.addEventListener('change',()=>refreshVariantSku(row,p.PRODUCT_CODE));$('#variants').append(row);refreshVariantSku(row,p.PRODUCT_CODE);};
  photos=bindProductPhotos(p);bindCategorySearch();p.variants.forEach(addVariant);if(!p.variants.length)addVariant({});
  $('#fields').onclick=async e=>{const b=e.target.closest('[data-action]');if(b?.dataset.action==='add-variant')addVariant({});if(b?.dataset.action==='remove-variant')b.closest('.variant').remove();
   if(b?.dataset.action==='create-color'){
    const name=$('#editor [name=newColorName]'),hex=$('#editor [name=newColorHex]'),message=$('#color-message');
    if(!name.value.trim()){message.textContent='Nhập tên màu mới.';name.focus();return;}
    if(hex.value.trim()&&!/^#[0-9a-f]{6}$/i.test(hex.value.trim())){message.textContent='Mã màu có dạng #1A237E hoặc để trống.';return;}
    b.disabled=true;message.textContent='Đang lưu màu…';
    try{const color=await api('/api/admin/colors','POST',{name:name.value.trim(),hexCode:hex.value.trim()||null});
     if(!lookups.colors.some(c=>String(c.ID)===String(color.ID)))lookups.colors.push(color);lookups.colors.sort((a,b)=>a.NAME.localeCompare(b.NAME,'vi'));
     const selects=[...document.querySelectorAll('#variants [name=colorId]')],target=selects.filter(s=>!s.value).pop();
     selects.forEach(s=>{const previous=s.value;s.innerHTML=options(lookups.colors,s===target?color.ID:previous,true);});
     document.querySelectorAll('#product-photos [name=photoColor]').forEach(s=>{const previous=s.value;s.innerHTML=options(lookups.colors,previous,true);});
     document.querySelectorAll('#variants .variant').forEach(row=>refreshVariantSku(row,p.PRODUCT_CODE));
     name.value='';hex.value='';message.textContent='Màu '+color.NAME+' đã có trong danh sách chọn.';
    }catch(error){message.textContent=error.message;}finally{b.disabled=false;}
   }
  };
 } else if(section==='vouchers'){
  const r=window.vouchers.find(v=>v.ID===id)||{};
  modal(id?'Sửa voucher':'Thêm voucher',`<div class="grid">${input('code','Mã voucher',r.CODE)}${select('type','Loại giảm',[{ID:'PERCENT',NAME:'Phần trăm'},{ID:'FIXED',NAME:'Số tiền'}],r.DISCOUNT_TYPE||'PERCENT')}${input('value','Mức giảm',r.DISCOUNT_VALUE??10,'number',true,'min="0.01" step="0.01"')}${input('minOrder','Đơn tối thiểu',r.MIN_ORDER??0,'number',true,'min="0"')}${input('maxDiscount','Giảm tối đa (bắt buộc cho %)',r.MAX_DISCOUNT??50000,'number',true,'min="0"')}${input('usageLimit','Giới hạn lượt',r.USAGE_LIMIT??100,'number',true,'min="0"')}${input('startsAt','Có hiệu lực từ',r.STARTS_AT?.slice(0,16)||'','datetime-local')}${input('endsAt','Hết hạn',r.ENDS_AT?.slice(0,16)||'','datetime-local')}${select('status','Trạng thái',statusOptions,r.STATUS??1)}</div>`,d=>api('/api/admin/vouchers'+(id?'/'+id:''),id?'PUT':'POST',{...d,value:Number(d.value),minOrder:Number(d.minOrder),maxDiscount:Number(d.maxDiscount),usageLimit:Number(d.usageLimit),status:Number(d.status)}));
 } else if(section==='accounts'){
  if(a==='detail'){
   const d=await api('/api/admin/customers/'+id);modal('Chi tiết khách hàng',`<h3>${esc(d.customer.FULL_NAME)}</h3><p>${esc(d.customer.EMAIL)} · ${esc(d.customer.PHONE)}</p><h3>Lịch sử đơn hàng</h3>`+table(d.orders,[['BILL_CODE','Mã đơn'],['TOTAL_AMOUNT','Tổng',money],['STATUS','Trạng thái',v=>esc(statusNames[v])],['CREATED_AT','Ngày',date]]));return;
  }
  const roles=await api('/api/admin/roles'),r=window.managers?.find(m=>m.ID===id)||{};
  modal(id?'Quyền truy cập nhân viên':'Tạo nhân viên',`<div class="grid">${id?'':input('username','Tài khoản')+input('password','Mật khẩu (12–72 ký tự)','','password',true,'minlength="12" maxlength="72" autocomplete="new-password"')+input('fullName','Họ tên')+input('email','Email','','email')}${select('roleId','Vai trò',roles,r.MANAGER_ROLE_ID)}${id?select('status','Trạng thái',statusOptions,r.STATUS):''}</div>`,d=>api('/api/admin/managers'+(id?'/'+id+'/access':''),id?'PUT':'POST',{...d,roleId:Number(d.roleId),status:Number(d.status??1)}));
 } else if(section==='stock'){
  modal('Điều chỉnh tồn kho',`<p>Nhập số dương để nhập hàng; số âm để giảm/kiểm kê. Mọi điều chỉnh được lưu lịch sử.</p>${input('delta','Số lượng thay đổi','','number')}${input('reason','Lý do')}`,d=>api('/api/admin/stock/'+id,'PUT',{delta:Number(d.delta),reason:d.reason}));
 } else if(section==='orders')return orderEditor(id);
}
async function imageEditor(id){
 const p=await api('/api/admin/products/'+id);
 modal('Ảnh · '+p.PRODUCT_NAME,`<p>JPEG, PNG, WebP · tối đa 5 MB/ảnh. Chọn màu để ảnh dùng chung cho các size cùng màu.</p><div class="toolbar">${input('files','Tải nhiều ảnh','','file',false,'multiple accept="image/jpeg,image/png,image/webp"')}${select('uploadColor','Màu',lookups.colors,null,true)}${button('Tải lên','upload','')}</div><div id="previews"></div><div id="image-list">${p.images.map(r=>`<div class="image-row" data-image="${r.ID}"><img src="${esc(r.IMAGE_URL.startsWith('assets/')?'/store/'+r.IMAGE_URL:r.IMAGE_URL)}" alt="Ảnh sản phẩm">${select('color','Màu',lookups.colors,r.COLOR_ID,true)}${input('order','Thứ tự',r.SORT_ORDER,'number')}${button(r.IMAGE_URL===p.THUMBNAIL_URL?'★ Đại diện':'Chọn đại diện','cover',r.ID)}${button('Lưu màu / thứ tự','save-image',r.ID)}${button('Gỡ','remove-image',r.ID)}</div>`).join('')||'<p>Chưa có ảnh.</p>'}</div>`);
 $('#fields input[type=file]').onchange=e=>{const dest=$('#previews');dest.replaceChildren();for(const f of e.target.files){const img=document.createElement('img'),url=URL.createObjectURL(f);img.src=url;img.width=100;img.onload=()=>URL.revokeObjectURL(url);dest.append(img);}};
 $('#fields').onclick=async e=>{const b=e.target.closest('[data-action]');if(!b)return;b.disabled=true;try{
  if(b.dataset.action==='upload'){
   const files=$('#fields input[type=file]').files;if(!files.length)throw Error('Chọn ít nhất một ảnh.');
   for(const file of files){if(file.size>5*1024*1024)throw Error('Ảnh vượt quá 5 MB.');const data=new FormData();data.append('file',file);const color=$('[name=uploadColor]').value;await api(`/api/admin/products/${id}/images`+(color?'?colorId='+color:''),'POST',data);}
  }else{
   const row=b.closest('.image-row'),color=row.querySelector('[name=color]').value;
   if(b.dataset.action==='remove-image')await api(`/api/admin/products/${id}/images/${b.dataset.id}`,'DELETE');
   else await api(`/api/admin/products/${id}/images/${b.dataset.id}?`+new URLSearchParams({order:row.querySelector('[name=order]').value,cover:b.dataset.action==='cover',...(color?{colorId:color}:{})}),'PUT');
  }
  $('#dialog').close();await imageEditor(id);
 }catch(e){$('#form-error').textContent=e.message;b.disabled=false;}};
}
function orderReturnPanel(order,id){const labels={NONE:'Chưa nhận hoàn',REQUESTED:'Đang yêu cầu trả',PARTIAL:'Đã nhận hoàn một phần',FULL:'Đã nhận hoàn toàn bộ'};return `<p class="report-note">Đặt đơn chỉ giữ hàng. ADMIN duyệt xuất giao mới giảm tồn tại kho. Hủy trước xuất chỉ giải phóng giữ. Hàng khách trả phải lập phiếu nhận hoàn theo đơn gốc và kiểm tra tình trạng trước khi nhập lại hàng bán.</p><p><b>Hàng hoàn:</b> ${esc(labels[order.RETURN_STATUS]||order.RETURN_STATUS||'Chưa nhận')} · Giá trị hàng hoàn: ${money(order.RETURN_VALUE)}</p><div class="toolbar">${[3,5,6].includes(Number(order.STATUS))&&order.RETURN_STATUS!=='FULL'?button('Lập phiếu nhận hàng hoàn','receive-return',id):''}</div>`;}
function orderInventoryPanel(data){return '<h3>Chứng từ xuất / hoàn theo đơn</h3>'+table(data.inventoryDocuments||[],[['DOC_CODE','Phiếu'],['DOC_TYPE','Nghiệp vụ',v=>esc(movementName(v))],['STATUS','Trạng thái'],['POSTED_AT','Ghi tồn',date]],r=>button('Xem phiếu','view-inventory',r.ID));}
async function orderEditor(id){
 const d=await api('/api/admin/orders/'+id),r=d.order,refundHtml=window.AdminV8?await window.AdminV8.refundPanel(id):'';
 const transitions={0:[1,4],1:[4],2:[3,6],3:[5],4:[],5:[],6:[]};
 modal('Đơn '+r.BILL_CODE,`<div class="grid"><section><h3>${esc(r.RECEIVER_NAME)}</h3><p>${esc(r.RECEIVER_PHONE)}<br>${esc(r.SHIPPING_ADDRESS)}</p><p>Trạng thái: <b>${esc(statusNames[r.STATUS])}</b></p><p>Ghi chú: ${esc(r.NOTE)}</p></section><section><p>Tiền hàng: ${money(r.SUB_TOTAL)}<br>Voucher: ${esc(r.VOUCHER_CODE||'—')}<br>Giảm giá: ${money(r.DISCOUNT_AMOUNT)}<br>Phí giao: ${money(r.SHIPPING_FEE)}<br><b>Phải trả: ${money(r.TOTAL_AMOUNT)}</b></p></section></div>${table(d.lines.map(x=>({NAME:x.productName,COLOR:x.colorName,SIZE:x.sizeName,QTY:x.quantity,PRICE:x.unitPrice,TOTAL:x.totalPrice})),[['NAME','Sản phẩm'],['COLOR','Màu'],['SIZE','Size'],['QTY','SL'],['PRICE','Đơn giá',money],['TOTAL','Thành tiền',money]])}<h3>Thanh toán và giao hàng</h3><div class="grid">${select('paymentMethod','Phương thức',[{ID:'COD',NAME:'COD'},{ID:'BANK',NAME:'Chuyển khoản'}],r.PAYMENT_METHOD)}${select('paymentStatus','Thanh toán',[{ID:0,NAME:'Chưa nhận tiền'},{ID:1,NAME:'Đã nhận tiền'}],r.PAYMENT_STATUS)}${input('carrier','Đơn vị vận chuyển',r.CARRIER,'text',false)}${input('trackingCode','Mã theo dõi',r.TRACKING_CODE,'text',false)}</div><div class="toolbar">${button('Lưu giao hàng / thanh toán','save-shipping',id)}</div>${Number(r.STATUS)===1?`<div class="toolbar">${button('Lập phiếu xuất giao (ADMIN duyệt)','prepare-dispatch',id)}</div>`:''}<h3>Chuyển trạng thái</h3><div class="toolbar">${select('nextStatus','Trạng thái mới',(transitions[r.STATUS]||[]).map(ID=>({ID,NAME:statusNames[ID]})),null,true)}${input('note','Ghi chú xử lý','','text',false)}${button('Chuyển trạng thái','transition',id)}</div>${orderReturnPanel(r,id)}${orderInventoryPanel(d)}${refundHtml}<h3>Lịch sử</h3>${table(d.history,[['NEW_STATUS','Trạng thái',v=>esc(statusNames[v])],['NOTE','Ghi chú'],['USERNAME','Người xử lý'],['CREATED_AT','Thời điểm',date]])}`);
 $('#fields').onclick=async e=>{const b=e.target.closest('[data-action]');if(!b)return;b.disabled=true;try{
  if(b.dataset.action==='record-refund'||b.dataset.action==='edit-refund'){await window.AdminV8.refundForm(id,b.dataset.action==='edit-refund'?Number(b.dataset.id):null);return;}
  if(b.dataset.action==='prepare-dispatch'){const doc=await api('/api/admin/orders/'+id+'/dispatch','POST');await window.AdminModules.documents.detail(Number(doc));return;}
  if(b.dataset.action==='view-inventory'){$('#dialog').close();await window.AdminModules.documents.detail(Number(b.dataset.id));return;}
  if(b.dataset.action==='receive-return'){$('#dialog').close();await window.AdminModules.documents.receive(id);return;}
  if(b.dataset.action==='save-shipping')await api(`/api/admin/orders/${id}/shipping`,'PUT',{paymentMethod:$('[name=paymentMethod]').value,paymentStatus:Number($('[name=paymentStatus]').value),carrier:$('[name=carrier]').value,trackingCode:$('[name=trackingCode]').value});
  else {if($('[name=nextStatus]').value==='')throw Error('Chọn trạng thái mới.');await api(`/api/admin/orders/${id}/status`,'PUT',{status:Number($('[name=nextStatus]').value),note:$('[name=note]').value});}
  $('#dialog').close();await load();await orderEditor(id);
 }catch(e){$('#form-error').textContent=e.message;b.disabled=false;}};
}
(async()=>{try{
 csrf=await api('/api/csrf');me=await api('/api/admin/me');lookups=await api('/api/admin/lookups');
 const admin=me.roles.some(r=>r.authority==='ROLE_ADMIN');$('#user').textContent=me.username+(admin?' · Quản trị viên':' · Nhân viên');
 $('#nav').innerHTML=Object.entries(titles).filter(([k])=>admin||!['accounts','audit'].includes(k)).map(([k,v])=>`<button data-section="${k}">${v}</button>`).join('');await load();
}catch(e){notice(e.message,true);}})();
