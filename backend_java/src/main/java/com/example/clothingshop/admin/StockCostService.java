package com.example.clothingshop.admin;

import org.springframework.stereotype.Service;
import java.math.*;
import java.util.*;
import static com.example.clothingshop.admin.AdminRepository.number;

/** Moving average of physical stock for each immutable variant ID. */
@Service
public class StockCostService {
 private final AdminRepository db;
 public StockCostService(AdminRepository db){this.db=db;}
 public void lockVariants(Collection<Long> variants){
  var products=new TreeSet<Long>();
  for(long id:variants)products.add(number(db.one("SELECT PRODUCT_ID FROM PRODUCT_DETAIL WHERE ID=:i","i",id),"PRODUCT_ID"));
  for(long id:products)db.one("SELECT ID FROM PRODUCT WHERE ID=:i FOR UPDATE","i",id);
  for(long id:new TreeSet<>(variants))db.one("SELECT ID FROM PRODUCT_DETAIL WHERE ID=:i FOR UPDATE","i",id);
 }
 public BigDecimal current(long variant){return (BigDecimal)db.one("SELECT AVERAGE_COST FROM PRODUCT_DETAIL WHERE ID=:i","i",variant).get("AVERAGE_COST");}
 public BigDecimal sold(long bill,long variant){
  var r=db.one("SELECT SUM(QUANTITY) Q,SUM(QUANTITY*COST_UNIT) V,COUNT(*) N,COUNT(COST_UNIT) KNOWN FROM BILL_DETAIL WHERE BILL_ID=:b AND PRODUCT_DETAIL_ID=:v AND DELETE_FLAG=0","b",bill,"v",variant);
  return number(r,"N")==0||number(r,"N")!=number(r,"KNOWN")?null:((BigDecimal)r.get("V")).divide((BigDecimal)r.get("Q"),8,RoundingMode.HALF_UP);
 }
 public static BigDecimal adjusted(long before,BigDecimal average,long delta,BigDecimal unit){
  if(before+delta==0)return BigDecimal.ZERO;
  if((before>0&&average==null)||(delta!=0&&unit==null))return null;
  BigDecimal value=(before==0?BigDecimal.ZERO:average.multiply(BigDecimal.valueOf(before))).add(unit==null?BigDecimal.ZERO:unit.multiply(BigDecimal.valueOf(delta)));
  if(value.signum()<0)throw new IllegalArgumentException("Giá trị tồn âm; kiểm tra phiếu đảo và các lần nhập/xuất sau phiếu gốc");
  return value.divide(BigDecimal.valueOf(before+delta),8,RoundingMode.HALF_UP);
 }
 public void movement(long variant,long delta,BigDecimal unit){
  var r=db.one("SELECT PHYSICAL_QUANTITY,AVERAGE_COST,KNOWN_VALUE,UNKNOWN_QUANTITY FROM PRODUCT_DETAIL WHERE ID=:i FOR UPDATE","i",variant);
  long before=number(r,"PHYSICAL_QUANTITY");BigDecimal known=(BigDecimal)r.get("KNOWN_VALUE"),unknown=(BigDecimal)r.get("UNKNOWN_QUANTITY");
  BigDecimal next=adjusted(before,(BigDecimal)r.get("AVERAGE_COST"),delta,unit);
  if(before+delta==0){known=BigDecimal.ZERO;unknown=BigDecimal.ZERO;}
  else if(delta>0){if(unit==null)unknown=unknown.add(BigDecimal.valueOf(delta));else known=known.add(unit.multiply(BigDecimal.valueOf(delta)));}
  else if(delta<0){if(next!=null){known=next.multiply(BigDecimal.valueOf(before+delta));unknown=BigDecimal.ZERO;}else if(before>0){BigDecimal ratio=BigDecimal.valueOf(before+delta).divide(BigDecimal.valueOf(before),16,RoundingMode.HALF_UP);known=known.multiply(ratio).setScale(8,RoundingMode.HALF_UP);unknown=unknown.multiply(ratio).setScale(8,RoundingMode.HALF_UP);}}
  if(unknown.signum()==0&&before+delta>0)next=known.divide(BigDecimal.valueOf(before+delta),8,RoundingMode.HALF_UP);
  db.update("UPDATE PRODUCT_DETAIL SET AVERAGE_COST=:a,KNOWN_VALUE=:v,UNKNOWN_QUANTITY=:u WHERE ID=:i","a",next,"v",known,"u",unknown,"i",variant);
 }
 public void snapshotSale(long bill){db.update("UPDATE BILL_DETAIL d SET COST_UNIT=(SELECT v.AVERAGE_COST FROM PRODUCT_DETAIL v WHERE v.ID=d.PRODUCT_DETAIL_ID),COST_TOTAL=(SELECT ROUND(d.QUANTITY*v.AVERAGE_COST,0) FROM PRODUCT_DETAIL v WHERE v.ID=d.PRODUCT_DETAIL_ID) WHERE d.BILL_ID=:b","b",bill);}
}
