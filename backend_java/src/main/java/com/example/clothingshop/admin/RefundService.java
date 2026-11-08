package com.example.clothingshop.admin;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import static com.example.clothingshop.admin.AdminRepository.number;
@Service
public class RefundService {
 private final AdminRepository db;private final InventoryService inventory;
 public RefundService(AdminRepository db,InventoryService inventory){this.db=db;this.inventory=inventory;}
 public record Request(@NotNull @Positive Long documentId,@NotNull @DecimalMin("1") @Digits(integer=15,fraction=0) BigDecimal amount,@NotBlank @Pattern(regexp="PENDING|PAID|FAILED") String status,@NotBlank @Size(max=150) String reference,@NotBlank @Size(max=500) String reason){}
 @Transactional public long save(long bill,Long id,Request r,long actor){
  if(!inventory.isAdmin(actor))throw new org.springframework.security.access.AccessDeniedException("Chỉ ADMIN xác nhận hoàn tiền");
  var b=db.one("SELECT PAYMENT_STATUS FROM BILL WHERE ID=:b FOR UPDATE","b",bill);if(number(b,"PAYMENT_STATUS")!=1&&number(b,"PAYMENT_STATUS")!=2)throw new IllegalArgumentException("Chưa ghi nhận đã thu tiền, không thể hoàn tiền");
  db.one("SELECT ID FROM INVENTORY_DOCUMENT h WHERE ID=:d AND BILL_ID=:b AND DOC_TYPE='RETURN' AND STATUS='POSTED' AND NOT EXISTS(SELECT 1 FROM INVENTORY_DOCUMENT x WHERE x.SOURCE_ID=h.ID AND x.STATUS='POSTED' AND x.DOC_TYPE='REVERSAL') FOR UPDATE","d",r.documentId(),"b",bill);
  if(id!=null){var old=db.one("SELECT STATUS FROM BILL_REFUND WHERE ID=:i AND BILL_ID=:b FOR UPDATE","i",id,"b",bill);if(!old.get("STATUS").equals("PENDING"))throw new IllegalArgumentException("Khoản hoàn đã kết thúc, không ghi nhận lại");}
  var a=db.one("SELECT (SELECT NVL(SUM(ROUND(RETURN_VALUE,0)),0) FROM INVENTORY_DOCUMENT_LINE WHERE DOCUMENT_ID=:d) CAP,(SELECT NVL(SUM(CASE WHEN STATUS IN ('PAID','PENDING') AND (:i IS NULL OR ID<>:i) THEN AMOUNT ELSE 0 END),0) FROM BILL_REFUND WHERE RETURN_DOCUMENT_ID=:d) USED FROM DUAL","d",r.documentId(),"i",id);
  if(!r.status().equals("FAILED")&&r.amount().compareTo(((BigDecimal)a.get("CAP")).subtract((BigDecimal)a.get("USED")))>0)throw new IllegalArgumentException("Số tiền hoàn vượt giá trị hàng nhận hoàn còn được hoàn tiền");
  Object[] p={"b",bill,"d",r.documentId(),"v",r.amount(),"s",r.status(),"ref",r.reference(),"r",r.reason(),"a",actor,"i",id};
  if(id==null)id=db.insert("INSERT INTO BILL_REFUND(BILL_ID,RETURN_DOCUMENT_ID,AMOUNT,STATUS,REFERENCE_CODE,REASON,CREATED_BY,PAID_AT) VALUES(:b,:d,:v,:s,:ref,:r,:a,CASE WHEN :s='PAID' THEN LOCALTIMESTAMP END)",p);
  else db.update("UPDATE BILL_REFUND SET RETURN_DOCUMENT_ID=:d,AMOUNT=:v,STATUS=:s,REFERENCE_CODE=:ref,REASON=:r,CREATED_BY=:a,PAID_AT=CASE WHEN :s='PAID' THEN LOCALTIMESTAMP END WHERE ID=:i",p);
  db.audit(actor,"REFUND_"+r.status(),"BILL_REFUND",id);return id;
 }
}
