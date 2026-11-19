(() => {
  'use strict';
  const normalize=value=>value.trim().replace(/\s+/g,' ');
  const money=value=>Number(value||0).toLocaleString('vi-VN')+' ₫';
  let formIndex=0;
  document.querySelectorAll('header form.search-form').forEach(form=>{
    const input=form.querySelector('input[type=text],input[type=search]');if(!input)return;
    form.classList.add('clothing-search-form');form.action='search-results.html';form.method='get';
    input.name='q';input.maxLength=120;input.autocomplete='off';input.setAttribute('aria-label','Tìm kiếm sản phẩm hoặc thương hiệu');
    input.setAttribute('role','combobox');input.setAttribute('aria-autocomplete','list');input.setAttribute('aria-expanded','false');
    const panel=document.createElement('div'),status=document.createElement('p'),list=document.createElement('ul'),all=document.createElement('a');
    panel.className='clothing-search-panel';panel.hidden=true;
    status.className='clothing-search-status';status.setAttribute('role','status');status.setAttribute('aria-live','polite');
    list.id='clothing-search-options-'+(++formIndex);list.className='clothing-search-options';list.setAttribute('role','listbox');list.setAttribute('aria-label','Sản phẩm gợi ý');input.setAttribute('aria-controls',list.id);
    all.className='clothing-search-all';all.hidden=true;panel.append(status,list,all);form.append(panel);
    const clear=document.createElement('button');clear.type='button';clear.className='clothing-search-clear';clear.setAttribute('aria-label','Xóa từ khóa tìm kiếm');clear.innerHTML='<i class="bi bi-x-lg" aria-hidden="true"></i>';clear.hidden=!input.value;input.parentElement.append(clear);
    const submit=form.querySelector('[type=submit]');submit?.setAttribute('aria-label','Tìm kiếm');
    let timer,controller,revision=0,active=-1,composing=false;
    const options=()=>[...list.querySelectorAll('[role=option]')];
    const open=()=>{panel.hidden=false;input.setAttribute('aria-expanded','true');};
    const close=()=>{revision++;clearTimeout(timer);controller?.abort();panel.hidden=true;panel.removeAttribute('aria-busy');input.setAttribute('aria-expanded','false');input.removeAttribute('aria-activedescendant');active=-1;};
    const activate=index=>{const nodes=options();if(!nodes.length)return;active=(index+nodes.length)%nodes.length;nodes.forEach((node,i)=>node.setAttribute('aria-selected',String(i===active)));input.setAttribute('aria-activedescendant',nodes[active].id);nodes[active].scrollIntoView?.({block:'nearest'});};
    async function search(){
      const keyword=normalize(input.value);clear.hidden=!input.value;active=-1;input.removeAttribute('aria-activedescendant');
      const ticket=++revision;controller?.abort();list.replaceChildren();all.hidden=true;
      if(!keyword){close();return;}
      open();if(keyword.length<2){status.textContent='Nhập ít nhất 2 ký tự để xem gợi ý.';return;}
      status.textContent='Đang tìm sản phẩm…';panel.setAttribute('aria-busy','true');controller=new AbortController();const signal=controller.signal;
      try{
        const result=await window.ClientStore.api('/api/products?'+new URLSearchParams({keyword,size:'5',page:'1',sort:'newest'}),'GET',undefined,{signal});
        if(ticket!==revision||signal.aborted)return;
        status.textContent=result.total?`Gợi ý sản phẩm · ${result.total} kết quả`:'Chưa có sản phẩm phù hợp. Thử từ khóa ngắn hơn hoặc tên thương hiệu.';
        for(const product of result.items){
          const item=document.createElement('li'),link=document.createElement('a'),info=document.createElement('span'),title=document.createElement('span'),price=document.createElement('span');
          link.href='product-details.html?id='+encodeURIComponent(product.id);link.className='clothing-search-option';link.id=list.id+'-'+product.id;link.setAttribute('role','option');link.setAttribute('aria-selected','false');link.tabIndex=-1;
          const raw=product.thumbnailUrl||'';let src='';if(raw.startsWith('assets/'))src=raw;else if(raw.startsWith('/store/assets/'))src=raw.slice(7);else if(raw.startsWith('/uploads/'))src=new URL(raw,new URL(window.CLOTHING_SHOP_API_BASE||'http://localhost:8080/api',location.origin)).href;
          if(src){const img=document.createElement('img');img.src=src;img.alt='';img.loading='lazy';img.onerror=()=>img.hidden=true;link.append(img);}
          info.className='clothing-search-option-text';title.className='clothing-search-name';title.textContent=product.productName;price.className='clothing-search-price';price.textContent=product.minPrice==null?'Đang cập nhật giá':money(product.minPrice);info.append(title,price);
          if(Number(product.totalStock)===0){const stock=document.createElement('small');stock.textContent='Tạm hết hàng';info.append(stock);}
          link.append(info);item.append(link);list.append(item);
        }
        all.href='search-results.html?'+new URLSearchParams({q:keyword});all.textContent=result.total?`Xem tất cả ${result.total} sản phẩm`:'Xem kết quả và thay đổi bộ lọc';all.hidden=false;
      }catch(error){if(ticket!==revision||signal.aborted)return;status.textContent='Chưa tải được gợi ý. Bạn vẫn có thể nhấn Enter để tìm kiếm.';}
      finally{if(ticket===revision)panel.removeAttribute('aria-busy');}
    }
    const schedule=()=>{revision++;controller?.abort();clearTimeout(timer);active=-1;input.removeAttribute('aria-activedescendant');clear.hidden=!input.value;if(!normalize(input.value)){close();return;}timer=setTimeout(search,250);};
    input.addEventListener('input',e=>{if(!composing&&!e.isComposing)schedule();});
    input.addEventListener('compositionstart',()=>composing=true);input.addEventListener('compositionend',()=>{composing=false;schedule();});
    input.addEventListener('focus',search);
    input.addEventListener('keydown',e=>{
      if(composing||e.isComposing)return;
      if(e.key==='Escape'){e.preventDefault();close();}
      else if(e.key==='ArrowDown'||e.key==='ArrowUp'){e.preventDefault();if(panel.hidden)search();activate(e.key==='ArrowDown'?active+1:(active<0?options().length-1:active-1));}
      else if(e.key==='Enter'&&!panel.hidden&&active>=0){e.preventDefault();location.assign(options()[active].href);}
    });
    clear.onclick=()=>{input.value='';clear.hidden=true;close();input.focus();};
    form.addEventListener('submit',e=>{input.value=normalize(input.value);if(!input.value){e.preventDefault();input.focus();open();status.textContent='Bạn muốn tìm sản phẩm nào? Nhập tên sản phẩm hoặc thương hiệu.';}else close();});
    document.addEventListener('pointerdown',e=>{if(!form.contains(e.target))close();});
    form.addEventListener('focusout',()=>setTimeout(()=>{if(!form.contains(document.activeElement))close();},0));
    document.getElementById('mobileSearch')?.addEventListener('shown.bs.collapse',()=>{if(form.closest('#mobileSearch'))input.focus();});
  });
})();
