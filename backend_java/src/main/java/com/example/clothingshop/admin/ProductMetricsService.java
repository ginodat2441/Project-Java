package com.example.clothingshop.admin;

import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.util.*;

@Service
public class ProductMetricsService {
 private final AdminRepository db;
 public ProductMetricsService(AdminRepository db){this.db=db;}
 public Map<String,Object> stock(String q,Long productId,int threshold,int page,java.math.BigDecimal minValue,java.math.BigDecimal maxValue){
  return stock(q,productId,threshold,page,minValue,maxValue,null);
 }
 public Map<String,Object> stock(String q,Long productId,int threshold,int page,java.math.BigDecimal minValue,java.math.BigDecimal maxValue,String stockState){
  if(threshold<0||minValue!=null&&minValue.signum()<0||maxValue!=null&&maxValue.signum()<0||minValue!=null&&maxValue!=null&&minValue.compareTo(maxValue)>0)throw new IllegalArgumentException("Khoảng lọc tồn kho không hợp lệ");
  String source="""
   SELECT s.*,
    CASE WHEN COUNT(*) OVER(PARTITION BY s.PRODUCT_ID)=COUNT(s.INVENTORY_VALUE) OVER(PARTITION BY s.PRODUCT_ID)
     THEN SUM(s.INVENTORY_VALUE) OVER(PARTITION BY s.PRODUCT_ID) END PRODUCT_INVENTORY_VALUE
   FROM (SELECT pd.ID,p.ID PRODUCT_ID,p.PRODUCT_CODE,p.PRODUCT_NAME,c.COLOR_NAME,pd.SIZE_NAME,pd.SKU,pd.QUANTITY,pd.AVERAGE_COST,ROUND(pd.KNOWN_VALUE,0) KNOWN_VALUE,pd.UNKNOWN_QUANTITY,
    pd.PHYSICAL_QUANTITY PHYSICAL,pd.RESERVED_QUANTITY RESERVED,
    CASE WHEN pd.PHYSICAL_QUANTITY=0 THEN 0 WHEN pd.AVERAGE_COST IS NOT NULL THEN ROUND(pd.KNOWN_VALUE,0) END INVENTORY_VALUE,
    CASE WHEN pd.QUANTITY=0 THEN 0 ELSE ROUND(pd.QUANTITY*pd.AVERAGE_COST,0) END AVAILABLE_VALUE
   FROM PRODUCT_DETAIL pd JOIN PRODUCT p ON p.ID=pd.PRODUCT_ID JOIN COLOR c ON c.ID=pd.COLOR_ID WHERE pd.DELETE_FLAG=0 AND p.DELETE_FLAG=0) s
   """;
  String filtered="SELECT * FROM ("+source+") WHERE QUANTITY<=:t AND (:p IS NULL OR PRODUCT_ID=:p) AND (LOWER(PRODUCT_NAME) LIKE :q OR LOWER(PRODUCT_CODE) LIKE :q OR LOWER(SKU) LIKE :q) AND (:min IS NULL OR PRODUCT_INVENTORY_VALUE>=:min) AND (:max IS NULL OR PRODUCT_INVENTORY_VALUE<=:max)";
  filtered+=" AND (:state IS NULL OR (:state='ZERO' AND QUANTITY=0) OR (:state='LOW' AND QUANTITY<=5) OR (:state='HELD' AND RESERVED>0) OR (:state='MISSING' AND PHYSICAL>0 AND AVERAGE_COST IS NULL))";
  Object[] args={"t",threshold,"p",productId,"q","%"+q.toLowerCase(Locale.ROOT)+"%","min",minValue,"max",maxValue,"state",stockState};
  String sums="SELECT CASE WHEN COUNT(*)=COUNT(INVENTORY_VALUE) THEN NVL(SUM(INVENTORY_VALUE),0) END INVENTORY_VALUE,ROUND(NVL(SUM(KNOWN_VALUE),0),0) KNOWN_VALUE,COUNT(CASE WHEN INVENTORY_VALUE IS NULL THEN 1 END) UNKNOWN_COST_SKUS,COUNT(DISTINCT CASE WHEN INVENTORY_VALUE IS NULL THEN PRODUCT_ID END) UNKNOWN_COST_PRODUCTS,NVL(SUM(QUANTITY),0) QUANTITY,NVL(SUM(PHYSICAL),0) PHYSICAL,NVL(SUM(RESERVED),0) RESERVED FROM (";
  var result=new LinkedHashMap<String,Object>(db.page(filtered+" ORDER BY QUANTITY,ID",page,30,args));
  result.put("totals",db.one(sums+filtered+")",args));
  result.put("globalTotals",db.one(sums+source+")"));
  result.put("products",db.rows("SELECT ID,PRODUCT_CODE||' · '||PRODUCT_NAME NAME FROM PRODUCT WHERE DELETE_FLAG=0 ORDER BY PRODUCT_CODE"));
  return result;
 }
 public Map<String,Object> list(String q,Long category,Integer status,int page,LocalDate from,LocalDate to){
  if(from!=null&&to!=null&&to.isBefore(from))throw new IllegalArgumentException("Ngày kết thúc phải sau ngày bắt đầu");
  var args=new Object[]{"q","%"+q.toLowerCase(Locale.ROOT)+"%","c",category,"s",status,"f",from==null?null:java.sql.Date.valueOf(from),"t",to==null?null:java.sql.Date.valueOf(to.plusDays(1))};
  String source="""
   SELECT p.ID,p.PRODUCT_CODE,p.PRODUCT_NAME,p.STATUS,p.THUMBNAIL_URL,c.CATEGORY_NAME,
   NVL(s.Q,0) STOCK,NVL(s.PHYSICAL,0) PHYSICAL,NVL(s.RESERVED,0) RESERVED,s.KNOWN_VALUE,NVL(s.MISSING,0) UNKNOWN_COST_SKUS,
   CASE WHEN NVL(s.MISSING,0)=0 THEN NVL(s.V,0) END INVENTORY_VALUE,
   ROUND(NVL(r.V,0),0) TOTAL_REVENUE
   FROM PRODUCT p LEFT JOIN CATEGORY c ON c.ID=p.CATEGORY_ID
   LEFT JOIN (SELECT PRODUCT_ID,SUM(QUANTITY) Q,SUM(PHYSICAL_QUANTITY) PHYSICAL,SUM(RESERVED_QUANTITY) RESERVED,SUM(ROUND(KNOWN_VALUE,0)) KNOWN_VALUE,SUM(CASE WHEN PHYSICAL_QUANTITY>0 AND AVERAGE_COST IS NULL THEN 1 ELSE 0 END) MISSING,SUM(CASE WHEN PHYSICAL_QUANTITY=0 THEN 0 ELSE ROUND(KNOWN_VALUE,0) END) V FROM PRODUCT_DETAIL WHERE DELETE_FLAG=0 GROUP BY PRODUCT_ID) s ON s.PRODUCT_ID=p.ID
   LEFT JOIN (SELECT v.PRODUCT_ID,SUM(e.NET-e.RETURN_VALUE) V FROM (
   """+ReportService.EVENTS+"""
   ) e JOIN PRODUCT_DETAIL v ON v.ID=e.PRODUCT_DETAIL_ID WHERE (:f IS NULL OR e.EVENT_AT>=:f) AND (:t IS NULL OR e.EVENT_AT<:t) GROUP BY v.PRODUCT_ID) r ON r.PRODUCT_ID=p.ID
   WHERE p.DELETE_FLAG=0 AND (LOWER(p.PRODUCT_NAME) LIKE :q OR LOWER(p.PRODUCT_CODE) LIKE :q OR EXISTS(SELECT 1 FROM PRODUCT_DETAIL d WHERE d.PRODUCT_ID=p.ID AND LOWER(d.SKU) LIKE :q))
   AND (:c IS NULL OR p.CATEGORY_ID=:c) AND (:s IS NULL OR p.STATUS=:s)
   """;
  var result=new LinkedHashMap<String,Object>(db.page("SELECT * FROM ("+source+") ORDER BY ID DESC",page,20,args));
  result.put("totals",db.one("SELECT CASE WHEN COUNT(*)=COUNT(INVENTORY_VALUE) THEN NVL(SUM(INVENTORY_VALUE),0) END INVENTORY_VALUE,NVL(SUM(KNOWN_VALUE),0) KNOWN_VALUE,NVL(SUM(TOTAL_REVENUE),0) TOTAL_REVENUE,NVL(SUM(UNKNOWN_COST_SKUS),0) UNKNOWN_COST_SKUS,COUNT(*)-COUNT(INVENTORY_VALUE) UNKNOWN_COST_PRODUCTS FROM ("+source+")",args));
  return result;
 }
}
