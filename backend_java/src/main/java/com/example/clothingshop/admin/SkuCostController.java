package com.example.clothingshop.admin;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.*;
import java.util.*;
import static com.example.clothingshop.admin.AdminRepository.number;
@RestController
@RequestMapping("/api/admin/sku-cost")
public class SkuCostController {
 private final AdminRepository db;private final AdminService admin;private final StockCostService costs;
 public SkuCostController(AdminRepository d,AdminService a,StockCostService c){db=d;admin=a;costs=c;}
 public record Snapshot(@NotNull @PositiveOrZero Long expectedQuantity,@NotNull @DecimalMin("0") @Digits(integer=12,fraction=8) BigDecimal unitCost,@NotBlank @Size(max=500) String reason,@NotBlank @Size(max=500) String evidence){}
 @GetMapping("/{id}") public Object preview(@PathVariable long id){return Map.of("stock",db.one("SELECT v.ID,v.SKU,v.COLOR_ID,c.COLOR_NAME,v.SIZE_NAME,v.PHYSICAL_QUANTITY,v.RESERVED_QUANTITY,v.QUANTITY,v.AVERAGE_COST,v.KNOWN_VALUE,v.UNKNOWN_QUANTITY FROM PRODUCT_DETAIL v JOIN COLOR c ON c.ID=v.COLOR_ID WHERE v.ID=:i","i",id),"receipts",db.rows("SELECT h.DOC_CODE,h.ID DOCUMENT_ID,l.ID,l.QUANTITY,l.UNIT_COST,l.COST_UNIT,h.POSTED_AT FROM INVENTORY_DOCUMENT_LINE l JOIN INVENTORY_DOCUMENT h ON h.ID=l.DOCUMENT_ID WHERE l.PRODUCT_DETAIL_ID=:i AND h.DOC_TYPE='RECEIPT' AND h.STATUS='POSTED' ORDER BY h.ID","i",id),"impact",Map.of("quantityChanged",false,"scope","CURRENT_BALANCE_AND_FUTURE_DISPATCHES","historicalSalesChanged",false));}
 @PostMapping("/{id}") @Transactional public void snapshot(@PathVariable long id,@Valid @RequestBody Snapshot r,Authentication a){
  long actor=admin.actor(a.getName());costs.lockVariants(List.of(id));var before=db.one("SELECT PHYSICAL_QUANTITY,AVERAGE_COST,KNOWN_VALUE,UNKNOWN_QUANTITY FROM PRODUCT_DETAIL WHERE ID=:i FOR UPDATE","i",id);long q=number(before,"PHYSICAL_QUANTITY");if(q!=r.expectedQuantity())throw new IllegalArgumentException("Số dư đã thay đổi; mở lại xem trước");BigDecimal effective=q==0?BigDecimal.ZERO:r.unitCost(),value=effective.multiply(BigDecimal.valueOf(q)).setScale(8,RoundingMode.HALF_UP);
  db.update("INSERT INTO STOCK_COST_ADJUSTMENT(PRODUCT_DETAIL_ID,PHYSICAL_QUANTITY,BEFORE_COST,AFTER_COST,BEFORE_KNOWN_VALUE,AFTER_VALUE,REASON,EVIDENCE,CREATED_BY) VALUES(:i,:q,:before,:after,:bv,:av,:r,:e,:a)","i",id,"q",q,"before",before.get("AVERAGE_COST"),"after",effective,"bv",before.get("KNOWN_VALUE"),"av",value,"r",r.reason(),"e",r.evidence(),"a",actor);
  db.update("UPDATE PRODUCT_DETAIL SET STOCK_VERSION=STOCK_VERSION+1,AVERAGE_COST=:c,KNOWN_VALUE=:v,UNKNOWN_QUANTITY=0,COST_RECONCILED_AT=LOCALTIMESTAMP WHERE ID=:i","c",q==0?BigDecimal.ZERO:r.unitCost(),"v",value,"i",id);
  db.auditChange(actor,"COST_SNAPSHOT","PRODUCT_DETAIL",id,before,Map.of("quantity",q,"unitCost",effective,"value",value,"evidence",r.evidence()),r.reason());
 }
}
