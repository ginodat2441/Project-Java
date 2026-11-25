"use strict";
window.AdminPages ??= {};
window.AdminOrderUI = {
  groups: [["all","Tất cả"],["payment","Chờ thanh toán"],["processing","Chờ xử lý"],["shipping","Đang vận chuyển"],["history","Lịch sử mua"]],
  payment(v) { return ({0:"Chưa thanh toán",1:"Đã thanh toán",2:"Đã hoàn tiền"})[Number(v)] || "Chưa xác định"; },
  state(v) { return `<span class="order-state order-state-${Number(v)}">${esc(statusNames[v] || "Chưa xác định")}</span>`; },
  note(group) { return ({all:"Theo dõi trạng thái xử lý và thanh toán riêng biệt. COD được thu tiền khi giao hàng.",payment:"Đơn chuyển khoản chưa thanh toán, chưa bàn giao vận chuyển. Đơn COD không nằm trong nhóm này.",processing:"Đơn COD mới hoặc đơn đã thanh toán đang chờ xác nhận / xuất giao.",shipping:"Đơn đã xuất giao và đang vận chuyển. COD có thể vẫn chưa thu tiền.",history:"Các đơn hoàn thành, đã hủy, trả hàng hoặc giao thất bại. Xem chi tiết để xử lý hàng hoàn."})[group]; }
};
window.AdminPages.orders = {
  async render() {
    const group=filter.orderGroup || "all";
    const qs=new URLSearchParams({page,q:filter.q || "",orderGroup:group});
    for(const key of ["status","from","to","paymentMethod","paymentStatus","returnStatus"])
      if(filter[key]!==undefined && filter[key]!=="")qs.set(key,filter[key]);
    const refundQuery=new URLSearchParams({page:"1",orderGroup:"history",status:"4",paymentStatus:"1"});
    const [data,refundData,requests]=await Promise.all([
      api("/api/admin/orders?"+qs),
      api("/api/admin/orders?"+refundQuery),api("/api/admin/order-requests")
    ]);
    const needsRefund=Number(refundData.total || 0);
    const pendingRequests=requests.items || [];
    const pendingCancels=pendingRequests.filter(r=>r.REQUEST_TYPE==='CANCEL');
    const pendingCancelByBill=new Map(pendingCancels.map(r=>[Number(r.BILL_ID),r]));
    const paidSepayCancels=pendingCancels.filter(r=>r.PAYMENT_METHOD==='SEPAY'&&Number(r.PAYMENT_STATUS)===1);
    const requestInbox=Number(requests.total)>0?`<section class="order-request-inbox"><h2>Yêu cầu từ khách đang chờ xử lý <span>${Number(requests.total)}</span></h2>${paidSepayCancels.length?`<div class="order-request-critical" role="alert"><strong>Ưu tiên: ${paidSepayCancels.length} đơn SePay đã thanh toán đang chờ duyệt hủy</strong><p>Nếu chấp nhận hủy, cần ghi nhận hoàn tiền thủ công sau khi đối soát giao dịch.</p></div>`:''}<p>Yêu cầu hủy đang chờ duyệt sẽ chặn xuất giao. Mở đơn để xem lý do và phản hồi khách.</p>${table(pendingRequests,[['BILL_CODE','Mã đơn'],['RECEIVER_NAME','Khách hàng'],['REQUEST_TYPE','Yêu cầu',v=>v==='CANCEL'?'Hủy đơn':'Trả hàng / hoàn tiền'],['PAYMENT_METHOD','Thanh toán',(v,r)=>r.PAYMENT_METHOD==='SEPAY'&&Number(r.PAYMENT_STATUS)===1?'<span class="order-request-payment-warning">SePay · ĐÃ THANH TOÁN</span>':`${esc(v)} · ${esc(AdminOrderUI.payment(r.PAYMENT_STATUS))}`],['REASON','Lý do'],['CREATED_AT','Ngày gửi',date]],r=>button('Xem và xử lý','detail',r.BILL_ID))}${Number(requests.total)>100?'<p>Hiển thị 100 yêu cầu cũ nhất. Xử lý để xem tiếp.</p>':''}</section>`:'';
    notice("");
    const advanced=select("status","Trạng thái đơn",statusNames.map((NAME,ID)=>({ID,NAME})),filter.status,true)
      +select("paymentMethod","Phương thức",[{ID:"COD",NAME:"COD"},{ID:"SEPAY",NAME:"SePay"},{ID:"BANK",NAME:"Chuyển khoản cũ"}],filter.paymentMethod,true)
      +select("paymentStatus","Thanh toán",[{ID:0,NAME:"Chưa thanh toán"},{ID:1,NAME:"Đã thanh toán"},{ID:2,NAME:"Đã hoàn tiền"}],filter.paymentStatus,true)
      +select("returnStatus","Hàng hoàn",[{ID:"NONE",NAME:"Chưa nhận hoàn"},{ID:"REQUESTED",NAME:"Yêu cầu trả"},{ID:"PARTIAL",NAME:"Hoàn một phần"},{ID:"FULL",NAME:"Hoàn toàn bộ"}],filter.returnStatus,true)
      +input("from","Từ ngày",filter.from || "","date",false)+input("to","Đến ngày",filter.to || "","date",false);
    const open=["status","from","to","paymentMethod","paymentStatus","returnStatus"].some(k=>filter[k]!==undefined&&filter[k]!=="");
    const refundAlert=needsRefund>0
      ? `<section class="refund-critical-alert" role="alert" aria-live="assertive"><span class="refund-critical-alert__label">MỨC ĐỘ KHẨN CẤP</span><div><strong>Cần xử lý hoàn tiền ngay</strong><p>Có ${esc(needsRefund)} đơn đã hủy nhưng đã thanh toán, đang cần hoàn tiền thủ công.</p></div>${button("Xem danh sách đơn", "refund-alert-orders", "")}</section>`
      : "";
    return `<div class="orders-workspace">${requestInbox}<div class="order-group-tabs" role="group" aria-label="Nhóm đơn hàng">${AdminOrderUI.groups.map(([key,label])=>button(label,"order-group-"+key,"",`aria-pressed="${key===group}"`)).join("")}</div>${refundAlert}<p class="order-explainer">${AdminOrderUI.note(group)}</p>
      <form id="filter" class="orders-filter"><input type="hidden" name="orderGroup" value="${esc(group)}"><div class="orders-search"><label>Tìm đơn hàng<input name="q" value="${esc(filter.q||"")}" placeholder="Mã đơn hoặc tên người nhận"></label><button type="submit">Tìm kiếm</button>${button("Xóa bộ lọc","clear-order-filter","")}${button("Làm mới","refresh-orders","")}</div><details ${open?"open":""}><summary>Bộ lọc nâng cao</summary><div class="order-advanced-grid">${advanced}</div></details></form>
      <div class="orders-result-heading"><h2>${AdminOrderUI.groups.find(([key])=>key===group)?.[1] || "Đơn hàng"}</h2><span>${Number(data.total)} đơn phù hợp</span></div>${table(data.items,[
        ["BILL_CODE","Mã đơn",v=>`<strong>${esc(v)}</strong>`],["RECEIVER_NAME","Người nhận"],
        ["RECEIVER_PHONE","Điện thoại liên hệ"],
        ["TOTAL_AMOUNT","Tổng tiền",v=>`<strong class="order-money">${money(v)}</strong>`],
        ["STATUS","Trạng thái",(v,r)=>`${AdminOrderUI.state(v)}${pendingCancelByBill.has(Number(r.ID))?'<span class="order-cancel-request-badge">KHÁCH YÊU CẦU HỦY</span>':''}`],
        ["PAYMENT_STATUS","Thanh toán",(v,r)=>`<span class="order-payment order-payment-${Number(v)}">${AdminOrderUI.payment(v)}</span><small class="order-method">${esc(r.PAYMENT_METHOD)}</small>${pendingCancelByBill.has(Number(r.ID))&&r.PAYMENT_METHOD==='SEPAY'&&Number(v)===1?'<small class="order-cancel-request-payment">Yêu cầu hủy · cần xử lý hoàn tiền nếu duyệt</small>':''}`],
        ["CREATED_AT","Ngày đặt",date]],r=>button("Xử lý đơn","detail",r.ID))}${pager(data)}</div>`;
  },
  async action(a,id) {
    if(a==="refund-alert-orders"){filter={status:4,paymentStatus:1,orderGroup:"history"};page=1;return load();}
    if(a.startsWith("order-group-")) {const group=a.slice(12);if(!AdminOrderUI.groups.some(([key])=>key===group))return;filter={...filter,orderGroup:group};delete filter.status;page=1;return load();}
    if(a==="clear-order-filter"){filter={orderGroup:filter.orderGroup||"all"};page=1;return load();}
    if(a==="refresh-orders")return load();
    if(a==="detail")return orderEditor(id);
  }
};
