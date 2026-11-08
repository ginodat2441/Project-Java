package com.example.clothingshop.admin;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.AccessDeniedException;
import com.example.clothingshop.common.BusinessException;
import java.math.*;
import java.util.*;
import static com.example.clothingshop.admin.AdminRepository.number;
import static com.example.clothingshop.admin.InventoryRequests.*;

@Service
public class InventoryService {
 private final AdminRepository db;private final StockCostService costs;
 public InventoryService(AdminRepository db,StockCostService costs){this.db=db;this.costs=costs;}
 public boolean isAdmin(long actor){return !db.rows("SELECT m.ID FROM MANAGER m JOIN MANAGER_ROLE r ON r.ID=m.MANAGER_ROLE_ID WHERE m.ID=:i AND m.STATUS=1 AND m.DELETE_FLAG=0 AND r.ROLE_CODE='ADMIN' AND r.DELETE_FLAG=0","i",actor).isEmpty();}
 private void approver(long actor){if(!isAdmin(actor))throw new AccessDeniedException("Chỉ ADMIN được xác nhận/đảo phiếu kho");}
 private long header(String type,String reason,Long bill,Long source,Long actor,String status){
  String prefix=switch(type){case "RECEIPT"->"NK";case "ISSUE","DISPATCH"->"XK";case "COUNT"->"KK";case "RETURN"->"HT";case "REVERSAL"->"DP";case "RESERVE"->"GH";case "RELEASE"->"HG";default->"BH";};
  String code=prefix+String.format(Locale.ROOT,"%06d",number(db.one("SELECT SHOP_INVENTORY_SEQ.NEXTVAL N FROM DUAL"),"N"));
  return db.insert("INSERT INTO INVENTORY_DOCUMENT(DOC_CODE,DOC_TYPE,REASON,BILL_ID,SOURCE_ID,CREATED_BY,STATUS,APPROVED_BY,POSTED_AT) VALUES(:c,:t,:r,:b,:s,:a,:st,CASE WHEN :st='POSTED' THEN :a END,CASE WHEN :st='POSTED' THEN LOCALTIMESTAMP END)","c",code,"t",type,"r",reason,"b",bill,"s",source,"a",actor,"st",status);
 }
 public void lockProducts(Collection<Long> variants){costs.lockVariants(variants);}
 private List<Map<String,Object>> billLines(long bill){return db.rows("SELECT PRODUCT_DETAIL_ID,SUM(QUANTITY) QTY FROM BILL_DETAIL WHERE BILL_ID=:b AND DELETE_FLAG=0 GROUP BY PRODUCT_DETAIL_ID ORDER BY PRODUCT_DETAIL_ID","b",bill);}
 private void lockBill(long bill){db.one("SELECT ID FROM BILL WHERE ID=:b FOR UPDATE","b",bill);lockProducts(billLines(bill).stream().map(l->number(l,"PRODUCT_DETAIL_ID")).toList());}
 private Map<String,Object> stock(long variant){return db.one("SELECT ID,PHYSICAL_QUANTITY,RESERVED_QUANTITY,QUANTITY,AVERAGE_COST,KNOWN_VALUE,UNKNOWN_QUANTITY,STOCK_VERSION FROM PRODUCT_DETAIL WHERE ID=:i AND DELETE_FLAG=0 FOR UPDATE","i",variant);}
 @Transactional public long save(Long id,Document r,long actor){
  if(r.type().equals("ISSUE")&&r.billId()!=null)throw new BusinessException("Dùng phiếu xuất giao theo đơn; xuất khác không được trừ lặp hàng đã giữ");
  if(!Set.of("RECEIPT","ISSUE","COUNT","RETURN").contains(r.type()))throw new BusinessException("Loại phiếu thủ công không hợp lệ");
  if(r.type().equals("RETURN")){if(r.billId()==null)throw new BusinessException("Phiếu hoàn cần đơn gốc");db.one("SELECT ID FROM BILL WHERE ID=:b AND DISPATCHED=1 AND STATUS IN (3,5,6) AND DELETE_FLAG=0","b",r.billId());}
  if(id==null)id=header(r.type(),r.reason().strip(),r.type().equals("RETURN")?r.billId():null,null,actor,"DRAFT");
  else{var old=db.one("SELECT STATUS,CREATED_BY,DOC_TYPE FROM INVENTORY_DOCUMENT WHERE ID=:i FOR UPDATE","i",id);if(!old.get("STATUS").equals("DRAFT")||Set.of("REVERSAL","DISPATCH").contains(old.get("DOC_TYPE")))throw new BusinessException("Chỉ sửa phiếu nháp thủ công");if(number(old,"CREATED_BY")!=actor&&!isAdmin(actor))throw new AccessDeniedException("Không được sửa phiếu người khác");db.update("UPDATE INVENTORY_DOCUMENT SET DOC_TYPE=:t,REASON=:r,BILL_ID=:b WHERE ID=:i","t",r.type(),"r",r.reason().strip(),"b",r.type().equals("RETURN")?r.billId():null,"i",id);db.update("DELETE FROM INVENTORY_DOCUMENT_LINE WHERE DOCUMENT_ID=:i","i",id);}
  var seen=new HashSet<Long>();
  for(var l:r.lines()){
   if(!seen.add(l.variantId()))throw new BusinessException("Một SKU chỉ được có một dòng trên phiếu");
   if(!r.type().equals("COUNT")&&l.quantity()==0)throw new BusinessException("Số lượng phải lớn hơn 0");
   if(r.type().equals("RECEIPT")&&l.unitCost()==null)throw new BusinessException("Phiếu nhập cần đơn giá, có thể bằng 0 nếu có chứng từ");
   var s=db.one("SELECT PHYSICAL_QUANTITY,RESERVED_QUANTITY,STOCK_VERSION FROM PRODUCT_DETAIL WHERE ID=:i AND DELETE_FLAG=0","i",l.variantId());
   if(r.type().equals("RETURN")&&db.rows("SELECT ID FROM BILL_DETAIL WHERE BILL_ID=:b AND PRODUCT_DETAIL_ID=:p AND DELETE_FLAG=0","b",r.billId(),"p",l.variantId()).isEmpty())throw new BusinessException("SKU không thuộc đơn gốc");
   db.update("INSERT INTO INVENTORY_DOCUMENT_LINE(DOCUMENT_ID,PRODUCT_DETAIL_ID,QUANTITY,EXPECTED_QUANTITY,EXPECTED_RESERVED,EXPECTED_VERSION,RESTOCK,CONDITION_NOTE,UNIT_COST) VALUES(:d,:p,:q,:e,:er,:version,:r,:n,:c)","d",id,"p",l.variantId(),"q",l.quantity(),"e",number(s,"PHYSICAL_QUANTITY"),"er",number(s,"RESERVED_QUANTITY"),"version",number(s,"STOCK_VERSION"),"r",l.restock()?1:0,"n",l.conditionNote(),"c",Set.of("RECEIPT","COUNT").contains(r.type())?l.unitCost():null);
  }
  db.audit(actor,"SAVE_DRAFT","INVENTORY_DOCUMENT",id);return id;
 }
 private long line(long doc,long variant,long qty){return db.insert("INSERT INTO INVENTORY_DOCUMENT_LINE(DOCUMENT_ID,PRODUCT_DETAIL_ID,QUANTITY) VALUES(:d,:p,:q)","d",doc,"p",variant,"q",qty);}
 private void record(long doc,long line,long variant,long dp,long dr,BigDecimal unit,String type,Long bill,Long actor,String reason){
  var before=stock(variant);long p=number(before,"PHYSICAL_QUANTITY"),r=number(before,"RESERVED_QUANTITY");
  if(p+dp<0||r+dr<0||r+dr>p+dp||p+dp>Integer.MAX_VALUE)throw new BusinessException("Không đủ tồn tại kho/sẵn bán hoặc số giữ không hợp lệ");
  if(dp!=0)costs.movement(variant,dp,unit);
  db.update("UPDATE PRODUCT_DETAIL SET STOCK_VERSION=STOCK_VERSION+1,PHYSICAL_QUANTITY=PHYSICAL_QUANTITY+:p,RESERVED_QUANTITY=RESERVED_QUANTITY+:r,QUANTITY=PHYSICAL_QUANTITY+:p-RESERVED_QUANTITY-:r WHERE ID=:i","p",dp,"r",dr,"i",variant);
  var after=stock(variant);BigDecimal vb=before.get("AVERAGE_COST")==null&&p>0?null:(BigDecimal)before.get("KNOWN_VALUE"),va=after.get("AVERAGE_COST")==null&&p+dp>0?null:(BigDecimal)after.get("KNOWN_VALUE");
  db.update("UPDATE INVENTORY_DOCUMENT_LINE SET COST_UNIT=:u,BEFORE_QUANTITY=:ab,AFTER_QUANTITY=:aa,STOCK_DELTA=:d,PHYSICAL_BEFORE=:pb,PHYSICAL_AFTER=:pa,RESERVED_BEFORE=:rb,RESERVED_AFTER=:ra,VALUE_BEFORE=:vb,VALUE_AFTER=:va WHERE ID=:i","u",unit,"ab",p-r,"aa",p+dp-r-dr,"d",dp-dr,"pb",p,"pa",p+dp,"rb",r,"ra",r+dr,"vb",vb,"va",va,"i",line);
  db.update("INSERT INTO INVENTORY_HISTORY(PRODUCT_DETAIL_ID,QUANTITY_CHANGE,REASON,MANAGER_ID,BILL_ID,DOCUMENT_ID,MOVEMENT_TYPE,BEFORE_QUANTITY,AFTER_QUANTITY,PHYSICAL_CHANGE,RESERVED_CHANGE,PHYSICAL_BEFORE,PHYSICAL_AFTER,RESERVED_BEFORE,RESERVED_AFTER,VALUE_BEFORE,VALUE_AFTER,HISTORY_SCOPE) VALUES(:p,:q,:reason,:actor,:b,:d,:t,:ab,:aa,:dp,:dr,:pb,:pa,:rb,:ra,:vb,:va,'PHYSICAL_V8')","p",variant,"q",dp-dr,"reason",reason,"actor",actor,"b",bill,"d",doc,"t",type,"ab",p-r,"aa",p+dp-r-dr,"dp",dp,"dr",dr,"pb",p,"pa",p+dp,"rb",r,"ra",r+dr,"vb",vb,"va",va);
 }
 /** Called after atomic reservation in BillRepository; physical stock/cost are unchanged. */
 public void sale(long bill){
  long doc=header("RESERVE","Giữ hàng khi đặt đơn",bill,null,null,"POSTED");
  for(var l:billLines(bill)){long v=number(l,"PRODUCT_DETAIL_ID"),q=number(l,"QTY");var s=stock(v);db.update("UPDATE PRODUCT_DETAIL SET RESERVED_QUANTITY=RESERVED_QUANTITY-:q,QUANTITY=QUANTITY+:q WHERE ID=:i","q",q,"i",v);record(doc,line(doc,v,q),v,0,q,null,"RESERVE",bill,null,"Đặt đơn giữ hàng");}
 }
 public void cancelSale(long bill,Long actor){lockBill(bill);long doc=header("RELEASE","Giải phóng giữ khi hủy trước xuất",bill,null,actor,"POSTED");for(var l:billLines(bill)){long v=number(l,"PRODUCT_DETAIL_ID"),q=number(l,"QTY");record(doc,line(doc,v,q),v,0,-q,null,"RELEASE",bill,actor,"Hủy đơn trước xuất");}db.update("UPDATE INVENTORY_DOCUMENT SET STATUS='CANCELLED',CANCEL_REASON='Đơn hủy trước xuất',CANCELLED_BY=:a,CANCELLED_AT=LOCALTIMESTAMP WHERE BILL_ID=:b AND DOC_TYPE='DISPATCH' AND STATUS='DRAFT'","a",actor,"b",bill);}
 @Transactional public long prepareDispatch(long bill,long actor){
  lockBill(bill);var b=db.one("SELECT STATUS,DISPATCHED,STOCK_RESERVED FROM BILL WHERE ID=:b","b",bill);
  if(number(b,"DISPATCHED")==1)throw new BusinessException("Đơn đã xuất giao, không lập xuất lần hai");
  if(number(b,"STATUS")!=1||number(b,"STOCK_RESERVED")!=1)throw new BusinessException("Đơn phải được xác nhận và giữ đủ hàng trước khi lập phiếu xuất giao");
  var existing=db.rows("SELECT ID FROM INVENTORY_DOCUMENT WHERE BILL_ID=:b AND DOC_TYPE='DISPATCH' AND STATUS='DRAFT'","b",bill);if(!existing.isEmpty())return number(existing.get(0),"ID");
  long doc=header("DISPATCH","Xuất giao theo đơn",bill,null,actor,"DRAFT");for(var l:billLines(bill))line(doc,number(l,"PRODUCT_DETAIL_ID"),number(l,"QTY"));db.audit(actor,"PREPARE_DISPATCH","BILL",bill);return doc;
 }
 @Transactional public void approve(long id,long actor){
  approver(actor);var pointer=db.one("SELECT BILL_ID FROM INVENTORY_DOCUMENT WHERE ID=:i","i",id);Long bill=pointer.get("BILL_ID")==null?null:number(pointer,"BILL_ID");if(bill!=null)lockBill(bill);
  var doc=db.one("SELECT * FROM INVENTORY_DOCUMENT WHERE ID=:i FOR UPDATE","i",id);if(!doc.get("STATUS").equals("DRAFT"))throw new BusinessException("Phiếu đã ghi sổ hoặc hủy, không duyệt trùng");String type=doc.get("DOC_TYPE").toString();
  var lines=db.rows("SELECT * FROM INVENTORY_DOCUMENT_LINE WHERE DOCUMENT_ID=:i ORDER BY PRODUCT_DETAIL_ID","i",id);lockProducts(lines.stream().map(l->number(l,"PRODUCT_DETAIL_ID")).toList());
  if(type.equals("DISPATCH")){var b=db.one("SELECT STATUS,DISPATCHED,STOCK_RESERVED FROM BILL WHERE ID=:b","b",bill);if(number(b,"STATUS")!=1||number(b,"DISPATCHED")!=0||number(b,"STOCK_RESERVED")!=1)throw new BusinessException("Đơn không còn đủ điều kiện xuất giao");costs.snapshotSale(bill);}
  BigDecimal returnedValue=BigDecimal.ZERO;
  for(var l:lines){long v=number(l,"PRODUCT_DETAIL_ID"),q=number(l,"QUANTITY");var s=stock(v);long dp=0,dr=0;BigDecimal unit=costs.current(v),ret=BigDecimal.ZERO,cogs=null;
   switch(type){
    case "RECEIPT"->{if(l.get("UNIT_COST")==null)throw new BusinessException("Thiếu đơn giá nhập");dp=q;unit=(BigDecimal)l.get("UNIT_COST");}
    case "ISSUE"->dp=-q;
    case "COUNT"->{if(l.get("EXPECTED_VERSION")==null||number(s,"STOCK_VERSION")!=number(l,"EXPECTED_VERSION")||number(s,"PHYSICAL_QUANTITY")!=number(l,"EXPECTED_QUANTITY")||number(s,"RESERVED_QUANTITY")!=number(l,"EXPECTED_RESERVED"))throw new BusinessException("Tồn tại kho hoặc lượng giữ đã thay đổi từ lúc lập kiểm kê; lập lại phiếu");dp=q-number(s,"PHYSICAL_QUANTITY");if(dp>0)unit=(BigDecimal)l.get("UNIT_COST");}
    case "DISPATCH"->{dp=-q;dr=-q;}
    case "RETURN"->{
     var b=db.one("SELECT DISPATCHED,STATUS FROM BILL WHERE ID=:b","b",bill);if(number(b,"DISPATCHED")!=1||!Set.of(3L,5L,6L).contains(number(b,"STATUS")))throw new BusinessException("Đơn chưa xuất hoặc chưa đủ điều kiện nhận hoàn");
     var sold=db.one("SELECT SUM(QUANTITY) SOLD,SUM(TOTAL_PRICE-ALLOCATED_DISCOUNT) LINE_NET FROM ("+SaleAccounting.ALL_LINES+") WHERE BILL_ID=:b AND PRODUCT_DETAIL_ID=:p GROUP BY BILL_ID,PRODUCT_DETAIL_ID","b",bill,"p",v);
     var previous=db.one("SELECT NVL(SUM(l.QUANTITY),0) N,NVL(SUM(ROUND(l.RETURN_VALUE,0)),0) V,NVL(SUM(l.COGS_VALUE),0) COGS FROM INVENTORY_DOCUMENT_LINE l JOIN INVENTORY_DOCUMENT h ON h.ID=l.DOCUMENT_ID WHERE h.DOC_TYPE='RETURN' AND h.STATUS='POSTED' AND h.BILL_ID=:b AND l.PRODUCT_DETAIL_ID=:p AND NOT EXISTS(SELECT 1 FROM INVENTORY_DOCUMENT x WHERE x.SOURCE_ID=h.ID AND x.DOC_TYPE='REVERSAL' AND x.STATUS='POSTED')","b",bill,"p",v);
     if(q>number(sold,"SOLD")-number(previous,"N"))throw new BusinessException("Hoàn vượt số đã xuất còn được hoàn");
     ret=((BigDecimal)sold.get("LINE_NET")).multiply(BigDecimal.valueOf(number(previous,"N")+q)).divide(BigDecimal.valueOf(number(sold,"SOLD")),0,RoundingMode.HALF_UP).subtract((BigDecimal)previous.get("V"));
     dp=number(l,"RESTOCK")==1?q:0;unit=costs.sold(bill,v);returnedValue=returnedValue.add(ret);
     if(unit!=null){BigDecimal total=(BigDecimal)db.one("SELECT SUM(COST_TOTAL) V FROM BILL_DETAIL WHERE BILL_ID=:b AND PRODUCT_DETAIL_ID=:v AND DELETE_FLAG=0","b",bill,"v",v).get("V");cogs=total.multiply(BigDecimal.valueOf(number(previous,"N")+q)).divide(BigDecimal.valueOf(number(sold,"SOLD")),0,RoundingMode.HALF_UP).subtract((BigDecimal)previous.get("COGS"));}
    }
    case "REVERSAL"->{
     var original=db.one("SELECT l.*,h.DOC_TYPE SOURCE_TYPE FROM INVENTORY_DOCUMENT_LINE l JOIN INVENTORY_DOCUMENT h ON h.ID=l.DOCUMENT_ID WHERE l.DOCUMENT_ID=:d AND l.PRODUCT_DETAIL_ID=:p AND h.STATUS='POSTED'","d",doc.get("SOURCE_ID"),"p",v);
     if(original.get("PHYSICAL_AFTER")==null)throw new BusinessException("Phiếu cũ chỉ ghi tồn sẵn bán; cần đối chiếu trước khi đảo");
     if(number(s,"PHYSICAL_QUANTITY")!=number(original,"PHYSICAL_AFTER")||number(s,"RESERVED_QUANTITY")!=number(original,"RESERVED_AFTER")||!db.rows("SELECT ID FROM INVENTORY_HISTORY WHERE PRODUCT_DETAIL_ID=:p AND ID>(SELECT NVL(MAX(ID),0) FROM INVENTORY_HISTORY WHERE DOCUMENT_ID=:d) AND HISTORY_SCOPE='PHYSICAL_V8'","p",v,"d",doc.get("SOURCE_ID")).isEmpty())throw new BusinessException("SKU có giao dịch sau phiếu gốc; đảo không còn an toàn. Lập chứng từ điều chỉnh có đối chiếu");
     dp=number(original,"PHYSICAL_BEFORE")-number(original,"PHYSICAL_AFTER");dr=number(original,"RESERVED_BEFORE")-number(original,"RESERVED_AFTER");unit=(BigDecimal)original.get("COST_UNIT");cogs=(BigDecimal)original.get("COGS_VALUE");
    }
    default->throw new BusinessException("Không duyệt thủ công loại phiếu này");
   }
   record(id,number(l,"ID"),v,dp,dr,unit,type,bill,actor,doc.get("REASON").toString());db.update("UPDATE INVENTORY_DOCUMENT_LINE SET RETURN_VALUE=:v,COGS_VALUE=:c WHERE ID=:i","v",ret,"c",cogs!=null?cogs:unit==null?null:unit.multiply(BigDecimal.valueOf(q)).setScale(0,RoundingMode.HALF_UP),"i",l.get("ID"));
  }
  db.update("UPDATE INVENTORY_DOCUMENT SET STATUS='POSTED',APPROVED_BY=:a,POSTED_AT=LOCALTIMESTAMP WHERE ID=:i","a",actor,"i",id);
  if(type.equals("DISPATCH")){db.update("UPDATE BILL SET STATUS=2,DISPATCHED=1,STOCK_RESERVED=0,HANDLED_BY=:a WHERE ID=:b","a",actor,"b",bill);db.update("INSERT INTO BILL_HISTORY(BILL_ID,OLD_STATUS,NEW_STATUS,NOTE,MANAGER_ID) VALUES(:b,1,2,:n,:a)","b",bill,"n","Xuất giao theo phiếu #"+id,"a",actor);}
  if(type.equals("RETURN"))refreshReturn(bill,actor);
  if(type.equals("REVERSAL")&&bill!=null){var original=db.one("SELECT DOC_TYPE FROM INVENTORY_DOCUMENT WHERE ID=:s","s",doc.get("SOURCE_ID"));if(original.get("DOC_TYPE").equals("RETURN")){var b=db.one("SELECT COMPLETED_AT FROM BILL WHERE ID=:b","b",bill);BigDecimal amount=(BigDecimal)db.one("SELECT NVL(SUM(ROUND(RETURN_VALUE,0)),0) V FROM INVENTORY_DOCUMENT_LINE WHERE DOCUMENT_ID=:d","d",doc.get("SOURCE_ID")).get("V");if(b.get("COMPLETED_AT")!=null)db.update("UPDATE INVENTORY_DOCUMENT SET REVENUE_DELTA=:v WHERE ID=:i","v",amount,"i",id);refreshReturn(bill,actor);}}
  db.auditChange(actor,"POST_"+type,"INVENTORY_DOCUMENT",id,Map.of("STATUS","DRAFT","DOC_TYPE",type),Map.of("STATUS","POSTED","lines",db.rows("SELECT PRODUCT_DETAIL_ID,PHYSICAL_BEFORE,PHYSICAL_AFTER,RESERVED_BEFORE,RESERVED_AFTER,COST_UNIT,VALUE_BEFORE,VALUE_AFTER FROM INVENTORY_DOCUMENT_LINE WHERE DOCUMENT_ID=:d","d",id)),doc.get("REASON").toString());
 }
 private void refreshReturn(long bill,long actor){
  var r=db.one("SELECT NVL(SUM(l.QUANTITY),0) Q,NVL(SUM(ROUND(l.RETURN_VALUE,0)),0) V FROM INVENTORY_DOCUMENT_LINE l JOIN INVENTORY_DOCUMENT h ON h.ID=l.DOCUMENT_ID WHERE h.BILL_ID=:b AND h.DOC_TYPE='RETURN' AND h.STATUS='POSTED' AND NOT EXISTS(SELECT 1 FROM INVENTORY_DOCUMENT x WHERE x.SOURCE_ID=h.ID AND x.STATUS='POSTED' AND x.DOC_TYPE='REVERSAL')","b",bill);long sold=number(db.one("SELECT SUM(QUANTITY) Q FROM BILL_DETAIL WHERE BILL_ID=:b AND DELETE_FLAG=0","b",bill),"Q"),qty=number(r,"Q");
  db.update("UPDATE BILL SET RETURN_STATUS=:s,RETURN_VALUE=:v,RETURNED_AT=LOCALTIMESTAMP,STATUS=CASE WHEN COMPLETED_AT IS NULL THEN STATUS WHEN :q=:sold THEN 5 ELSE 3 END,HANDLED_BY=:a WHERE ID=:b","s",qty==0?"NONE":qty==sold?"FULL":"PARTIAL","v",r.get("V"),"q",qty,"sold",sold,"a",actor,"b",bill);
  db.audit(actor,"RECEIVE_RETURN","BILL",bill);
 }
 @Transactional public void cancel(long id,String reason,long actor){var d=db.one("SELECT STATUS,CREATED_BY FROM INVENTORY_DOCUMENT WHERE ID=:i FOR UPDATE","i",id);if(!d.get("STATUS").equals("DRAFT"))throw new BusinessException("Chỉ hủy nháp; phiếu đã ghi sổ cần đảo");if(number(d,"CREATED_BY")!=actor&&!isAdmin(actor))throw new AccessDeniedException("Không được hủy phiếu người khác");db.update("UPDATE INVENTORY_DOCUMENT SET STATUS='CANCELLED',CANCELLED_BY=:a,CANCEL_REASON=:r,CANCELLED_AT=LOCALTIMESTAMP WHERE ID=:i","a",actor,"r",reason,"i",id);db.audit(actor,"CANCEL_DRAFT","INVENTORY_DOCUMENT",id);}
 @Transactional public long reverse(long source,String reason,long actor){
  approver(actor);var p=db.one("SELECT BILL_ID FROM INVENTORY_DOCUMENT WHERE ID=:i","i",source);Long bill=p.get("BILL_ID")==null?null:number(p,"BILL_ID");if(bill!=null)lockBill(bill);
  db.one("SELECT ID FROM INVENTORY_DOCUMENT WHERE ID=:i AND STATUS='POSTED' AND DOC_TYPE IN ('RECEIPT','ISSUE','COUNT','RETURN') FOR UPDATE","i",source);
  if(!db.rows("SELECT ID FROM INVENTORY_DOCUMENT WHERE SOURCE_ID=:i AND STATUS<>'CANCELLED'","i",source).isEmpty())throw new BusinessException("Phiếu đã có phiếu đảo");
  if(!db.rows("SELECT ID FROM BILL_REFUND WHERE RETURN_DOCUMENT_ID=:d AND STATUS IN ('PENDING','PAID')","d",source).isEmpty())throw new BusinessException("Phiếu đã có khoản hoàn tiền, cần đối soát tiền trước khi đảo nhận hoàn");
  long id=header("REVERSAL",reason,bill,source,actor,"DRAFT");db.update("INSERT INTO INVENTORY_DOCUMENT_LINE(DOCUMENT_ID,PRODUCT_DETAIL_ID,QUANTITY,EXPECTED_QUANTITY,EXPECTED_RESERVED,UNIT_COST) SELECT :d,l.PRODUCT_DETAIL_ID,l.QUANTITY,p.PHYSICAL_QUANTITY,p.RESERVED_QUANTITY,l.UNIT_COST FROM INVENTORY_DOCUMENT_LINE l JOIN PRODUCT_DETAIL p ON p.ID=l.PRODUCT_DETAIL_ID WHERE l.DOCUMENT_ID=:s","d",id,"s",source);db.audit(actor,"CREATE_REVERSAL","INVENTORY_DOCUMENT",id);return id;
 }
}
