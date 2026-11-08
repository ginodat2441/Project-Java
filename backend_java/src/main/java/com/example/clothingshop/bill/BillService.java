package com.example.clothingshop.bill;

import com.example.clothingshop.common.BusinessException;
import com.example.clothingshop.common.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.example.clothingshop.admin.AdminRepository;
import com.example.clothingshop.admin.AdminService;
import com.example.clothingshop.admin.AdminRequests;
import static com.example.clothingshop.admin.AdminRepository.number;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class BillService {

    private final BillRepository repo;
    private final AdminRepository db;
    private final AdminService admin;
    private final com.example.clothingshop.admin.InventoryService inventory;

    public BillService(BillRepository repo,AdminRepository db,AdminService admin,com.example.clothingshop.admin.InventoryService inventory) { this.repo = repo; this.db=db;this.admin=admin;this.inventory=inventory; }

    /**
     * Đặt hàng. @Transactional: có lỗi (hết hàng, sai địa chỉ...) -> ROLLBACK toàn bộ,
     * kể cả các lần trừ tồn kho trước đó.
     */
    @Transactional
    public BillView create(BillRequest req) {
        if(req.items()==null||req.items().isEmpty()||req.items().size()>100)throw new BusinessException("Giỏ hàng không hợp lệ");
        db.one("SELECT c.ID FROM CUSTOMER c JOIN USERS u ON u.ID=c.USER_ID WHERE c.ID=:i AND c.DELETE_FLAG=0 AND u.DELETE_FLAG=0 AND u.STATUS=1","i",req.customerId());
        if(!java.util.Set.of("COD","BANK").contains(req.paymentMethod()))throw new BusinessException("Chỉ hỗ trợ COD hoặc chuyển khoản thủ công");
        var addr = repo.findAddress(req.addressId(), req.customerId())
                .orElseThrow(() -> new BusinessException("Địa chỉ không hợp lệ"));

        if(req.items().stream().map(BillRequest.Item::productDetailId).distinct().count()!=req.items().size())throw new BusinessException("Gop cac dong trung SKU truoc khi dat don");
        // 1. Trừ kho + tính tiền
        BigDecimal subTotal = BigDecimal.ZERO;
        Map<Long, BigDecimal> priceMap = new HashMap<>();
        var sorted=req.items().stream().sorted(java.util.Comparator.comparing(BillRequest.Item::productDetailId)).toList();
        inventory.lockProducts(sorted.stream().map(BillRequest.Item::productDetailId).toList());
        for (var item : sorted) {
            if(item.quantity()==null||item.quantity()<1||item.quantity()>100000)throw new BusinessException("Số lượng không hợp lệ");
            BigDecimal price = repo.findPrice(item.productDetailId())
                    .orElseThrow(() -> new BusinessException("Sản phẩm chi tiết không tồn tại: " + item.productDetailId()));
            if (repo.decreaseStock(item.productDetailId(), item.quantity()) == 0) {
                throw new BusinessException("Không đủ hàng cho sản phẩm chi tiết id=" + item.productDetailId());
            }
            priceMap.put(item.productDetailId(), price);
            subTotal = subTotal.add(price.multiply(BigDecimal.valueOf(item.quantity())));
        }
        BigDecimal ship = BigDecimal.valueOf(30000); // Shop policy; never accept browser totals.
        BigDecimal discount=discount(req.voucherCode(),subTotal,true);

        // 2. Insert BILL
        Map<String, Object> p = new HashMap<>();
        p.put("code", "HD" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyMMddHHmmss"))
                + java.util.UUID.randomUUID().toString().substring(0,8));
        p.put("customerId", req.customerId());
        p.put("addressId", req.addressId());
        p.put("receiverName", addr.receiverName());
        p.put("receiverPhone", addr.receiverPhone());
        p.put("shippingAddress", addr.fullAddress());
        p.put("subTotal", subTotal);
        p.put("shippingFee", ship);
        p.put("discount", discount);
        p.put("total", subTotal.subtract(discount).add(ship));
        p.put("paymentMethod", req.paymentMethod());
        p.put("note", req.note());
        long billId = repo.insertBill(p);
        db.update("UPDATE BILL SET STOCK_RESERVED=1,VOUCHER_CODE=:v WHERE ID=:i","v",req.voucherCode()==null?null:req.voucherCode().trim().toUpperCase(java.util.Locale.ROOT),"i",billId);

        // 3. Insert BILL_DETAIL + BILL_HISTORY
        for (var item : req.items()) {
            repo.insertBillDetail(billId, item.productDetailId(), item.quantity(), priceMap.get(item.productDetailId()));
        }
        inventory.sale(billId);
        repo.insertHistory(billId, null, 0, "Khách đặt hàng", null);

        return get(billId);
    }

    public BillView get(long id) {
        return repo.findById(id).orElseThrow(() -> new NotFoundException("Không tìm thấy hóa đơn id=" + id));
    }

    public List<BillLine> lines(long id) { return repo.findLines(id); }

    public List<BillView> byCustomer(long customerId) { return repo.findByCustomer(customerId); }

    @Transactional
    public BillView changeStatus(long billId, int newStatus, Long managerId, String note) {
        db.one("SELECT ID FROM BILL WHERE ID=:i AND DELETE_FLAG=0 FOR UPDATE","i",billId);
        BillView bill = get(billId);
        int old=bill.status();
        boolean valid=(old==0&&(newStatus==1||newStatus==4))||(old==1&&(newStatus==2||newStatus==4))||(old==2&&(newStatus==3||newStatus==6))||(old==3&&newStatus==5);
        if(!valid)throw new BusinessException("Không thể chuyển trạng thái này");
        if(newStatus==3&&!bill.paymentMethod().equals("COD")&&bill.paymentStatus()!=1)throw new BusinessException("Ghi nhận đã thanh toán trước khi hoàn thành đơn chuyển khoản");
        if(newStatus==2){long dispatch=inventory.prepareDispatch(billId,managerId);inventory.approve(dispatch,managerId);return get(billId);}
        if (repo.updateStatus(billId, bill.status(), newStatus, managerId) == 0) {
            throw new BusinessException("Trạng thái đơn đã bị thay đổi, vui lòng tải lại");
        }
        repo.insertHistory(billId, bill.status(), newStatus, note, managerId);
        if(newStatus==4&&db.update("UPDATE BILL SET STOCK_RESERVED=0 WHERE ID=:i AND STOCK_RESERVED=1","i",billId)==1){
            inventory.cancelSale(billId,managerId);
        }
        if(newStatus==3)db.update("UPDATE BILL SET COMPLETED_AT=COALESCE(COMPLETED_AT,LOCALTIMESTAMP) WHERE ID=:i","i",billId);
        if(newStatus==5)db.update("UPDATE BILL SET RETURN_STATUS=CASE WHEN RETURN_STATUS='NONE' THEN 'REQUESTED' ELSE RETURN_STATUS END WHERE ID=:i","i",billId);
        if(newStatus==3&&bill.paymentMethod().equals("COD"))db.update("UPDATE BILL SET PAYMENT_STATUS=1 WHERE ID=:i","i",billId);
        db.auditChange(managerId,"STATUS_"+newStatus,"BILL",billId,Map.of("status",old),Map.of("status",newStatus),note);
        return get(billId);
    }

    public BigDecimal discount(String code,BigDecimal subtotal,boolean consume){
        if(code==null||code.isBlank())return BigDecimal.ZERO;
        var rows=db.rows("SELECT ID,DISCOUNT_TYPE,DISCOUNT_VALUE,MAX_DISCOUNT FROM VOUCHER WHERE CODE=:c AND STATUS=1 AND STARTS_AT<=LOCALTIMESTAMP AND ENDS_AT>LOCALTIMESTAMP AND USED_COUNT<USAGE_LIMIT AND MIN_ORDER<=:s"+(consume?" FOR UPDATE":""),"c",code.trim().toUpperCase(java.util.Locale.ROOT),"s",subtotal);
        if(rows.isEmpty())throw new BusinessException("Voucher không hợp lệ, chưa có hiệu lực, hết hạn, hết lượt hoặc đơn chưa đạt tối thiểu");
        var v=rows.get(0);
        if(consume&&db.update("UPDATE VOUCHER SET USED_COUNT=USED_COUNT+1 WHERE ID=:i AND USED_COUNT<USAGE_LIMIT AND STATUS=1 AND STARTS_AT<=LOCALTIMESTAMP AND ENDS_AT>LOCALTIMESTAMP AND MIN_ORDER<=:s","i",number(v,"ID"),"s",subtotal)==0)throw new BusinessException("Voucher đã hết lượt hoặc hết hiệu lực");
        BigDecimal value=(BigDecimal)v.get("DISCOUNT_VALUE");
        if(v.get("DISCOUNT_TYPE").equals("PERCENT"))value=subtotal.multiply(value).divide(BigDecimal.valueOf(100),0,java.math.RoundingMode.HALF_UP).min((BigDecimal)v.get("MAX_DISCOUNT"));
        return value.min(subtotal).setScale(0,java.math.RoundingMode.HALF_UP);
    }
    @Transactional public void shipping(long id,AdminRequests.Shipping r,long actor){
        var order=db.one("SELECT STATUS,PAYMENT_STATUS FROM BILL WHERE ID=:i AND DELETE_FLAG=0 FOR UPDATE","i",id);
        if(number(order,"STATUS")>=3)throw new BusinessException("Đơn đã kết thúc; không thể sửa thông tin thanh toán/giao hàng");
        if(number(order,"PAYMENT_STATUS")==1&&r.paymentStatus()==0)throw new BusinessException("Không thể bỏ ghi nhận thanh toán đã nhận");
        db.update("UPDATE BILL SET PAYMENT_METHOD=:p,PAYMENT_STATUS=:s,CARRIER=:c,TRACKING_CODE=:t,HANDLED_BY=:a WHERE ID=:i","p",r.paymentMethod(),"s",r.paymentStatus(),"c",r.carrier(),"t",r.trackingCode(),"a",actor,"i",id);
        db.audit(actor,"SHIPPING_PAYMENT","BILL",id);
    }
}
