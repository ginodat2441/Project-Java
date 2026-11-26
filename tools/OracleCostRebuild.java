import java.sql.*;
import java.math.*;
import java.util.*;
/** Reconstruct only documented per-SKU costs. Never copy PRODUCT.AVERAGE_COST to a SKU. */
public class OracleCostRebuild {
 record Event(long line,long doc,long bill,String type,long delta,BigDecimal receipt,Long source,Timestamp at){}
 public static void main(String[] args)throws Exception{
  try(var c=DriverManager.getConnection(System.getenv("SPRING_DATASOURCE_URL"),System.getenv("SPRING_DATASOURCE_USERNAME"),System.getenv("SPRING_DATASOURCE_PASSWORD"))){
   boolean done;try(var s=c.createStatement();var r=s.executeQuery("SELECT COUNT(*) FROM SHOP_MIGRATION WHERE VERSION='V008_COST'")){r.next();done=r.getInt(1)>0;}
   if(done){if(args.length>0&&args[0].equals("apply"))rounded(c);System.out.println("Per-SKU reconstruction already applied; no quantities rewritten");return;}
   c.setAutoCommit(false);try{
    var ids=new ArrayList<Long>();try(var s=c.createStatement();var r=s.executeQuery("SELECT ID FROM PRODUCT_DETAIL ORDER BY ID")){while(r.next())ids.add(r.getLong(1));}
    int known=0,unknownCount=0;BigDecimal totalKnown=BigDecimal.ZERO;
    for(long id:ids){
     long current;try(var s=c.prepareStatement("SELECT PHYSICAL_QUANTITY FROM PRODUCT_DETAIL WHERE ID=? FOR UPDATE")){s.setLong(1,id);try(var r=s.executeQuery()){r.next();current=r.getLong(1);}}
     var events=new ArrayList<Event>();
     String sql="SELECT l.ID,l.DOCUMENT_ID,NVL(h.BILL_ID,0),h.DOC_TYPE,CASE WHEN h.DOC_TYPE='RECEIPT' THEN l.QUANTITY WHEN h.DOC_TYPE='SALE' AND b.DISPATCHED=1 THEN -l.QUANTITY WHEN h.DOC_TYPE='RETURN' THEN CASE WHEN l.RESTOCK=1 THEN l.QUANTITY ELSE 0 END WHEN h.DOC_TYPE IN ('ISSUE','COUNT','REVERSAL') THEN l.STOCK_DELTA ELSE 0 END DP,l.UNIT_COST,h.SOURCE_ID,CASE WHEN h.DOC_TYPE='SALE' THEN COALESCE((SELECT MIN(bh.CREATED_AT) FROM BILL_HISTORY bh WHERE bh.BILL_ID=h.BILL_ID AND bh.NEW_STATUS=2),h.POSTED_AT) ELSE h.POSTED_AT END AT_TIME FROM INVENTORY_DOCUMENT_LINE l JOIN INVENTORY_DOCUMENT h ON h.ID=l.DOCUMENT_ID LEFT JOIN BILL b ON b.ID=h.BILL_ID WHERE l.PRODUCT_DETAIL_ID=? AND h.STATUS='POSTED' ORDER BY AT_TIME,h.ID";
     try(var s=c.prepareStatement(sql)){s.setLong(1,id);try(var r=s.executeQuery()){while(r.next()){Object src=r.getObject(7);events.add(new Event(r.getLong(1),r.getLong(2),r.getLong(3),r.getString(4),r.getLong(5),r.getBigDecimal(6),src==null?null:((Number)src).longValue(),r.getTimestamp(8)));}}}
     long q=current-events.stream().mapToLong(Event::delta).sum();if(q<0)throw new IllegalStateException("Cannot reconcile opening quantity for variant "+id);
     BigDecimal value=BigDecimal.ZERO,unknown=BigDecimal.valueOf(q),average=q==0?BigDecimal.ZERO:null;
     Map<Long,BigDecimal> saleCosts=new HashMap<>(),docCosts=new HashMap<>();
     for(var e:events){if(e.delta==0)continue;BigDecimal unit=average;
      if(e.type.equals("RECEIPT"))unit=e.receipt;
      if(e.type.equals("RETURN"))unit=saleCosts.get(e.bill);
      if(e.type.equals("REVERSAL"))unit=docCosts.get(e.source);
      if(e.type.equals("COUNT")&&e.delta>0)unit=e.receipt;
      if(q+e.delta<0)throw new IllegalStateException("Historical negative physical stock for variant "+id+", document "+e.doc);
      if(e.type.equals("SALE")){saleCosts.put(e.bill,unit);try(var s=c.prepareStatement("UPDATE BILL_DETAIL SET COST_UNIT=? WHERE BILL_ID=? AND PRODUCT_DETAIL_ID=?")){s.setBigDecimal(1,unit);s.setLong(2,e.bill);s.setLong(3,id);s.executeUpdate();}}
      docCosts.put(e.doc,unit);
      if(e.delta>0){if(unit==null)unknown=unknown.add(BigDecimal.valueOf(e.delta));else value=value.add(unit.multiply(BigDecimal.valueOf(e.delta)));}
      else if(q+e.delta==0){unknown=BigDecimal.ZERO;value=BigDecimal.ZERO;}
      else if(unknown.signum()>0){BigDecimal ratio=BigDecimal.valueOf(q+e.delta).divide(BigDecimal.valueOf(q),16,RoundingMode.HALF_UP);unknown=unknown.multiply(ratio).setScale(8,RoundingMode.HALF_UP);value=value.multiply(ratio).setScale(8,RoundingMode.HALF_UP);}
      else value=value.add(unit.multiply(BigDecimal.valueOf(e.delta)));
      q+=e.delta;average=q==0?BigDecimal.ZERO:unknown.signum()>0?null:value.divide(BigDecimal.valueOf(q),8,RoundingMode.HALF_UP);
      try(var s=c.prepareStatement("UPDATE INVENTORY_DOCUMENT_LINE SET COST_UNIT=? WHERE ID=?")){s.setBigDecimal(1,unit);s.setLong(2,e.line);s.executeUpdate();}
     }
     if(q!=current)throw new IllegalStateException("Quantity mismatch for variant "+id);
     try(var s=c.prepareStatement("UPDATE PRODUCT_DETAIL SET AVERAGE_COST=?,KNOWN_VALUE=?,UNKNOWN_QUANTITY=?,COST_RECONCILED_AT=CASE WHEN ?=0 THEN LOCALTIMESTAMP END WHERE ID=?")){s.setBigDecimal(1,average);s.setBigDecimal(2,value);s.setBigDecimal(3,unknown);s.setBigDecimal(4,unknown);s.setLong(5,id);s.executeUpdate();}
     if(average==null)unknownCount++;else known++;totalKnown=totalKnown.add(value);
    }
    try(var s=c.createStatement()){s.executeUpdate("INSERT INTO SHOP_MIGRATION(VERSION) SELECT 'V008_COST' FROM DUAL WHERE NOT EXISTS(SELECT 1 FROM SHOP_MIGRATION WHERE VERSION='V008_COST')");}
    if(args.length>0&&args[0].equals("apply")){c.commit();rounded(c);System.out.println("Cost reconstruction committed");}else{c.rollback();System.out.println("Dry run: no values changed");}
    System.out.println("Known SKUs="+known+"; missing-cost SKUs="+unknownCount+"; documented value="+totalKnown.setScale(0,RoundingMode.HALF_UP));
   }catch(Exception e){c.rollback();throw e;}
  }
 }
 static void rounded(Connection c)throws Exception{
  try(var s=c.createStatement();var r=s.executeQuery("SELECT COUNT(*) FROM SHOP_MIGRATION WHERE VERSION='V008_COST_ROUND'")){r.next();if(r.getInt(1)>0)return;}
  c.setAutoCommit(false);
  try(var s=c.createStatement()){
   s.executeUpdate("UPDATE BILL_DETAIL SET COST_TOTAL=ROUND(QUANTITY*COST_UNIT,0) WHERE COST_UNIT IS NOT NULL AND COST_TOTAL IS NULL");
   s.executeUpdate("UPDATE INVENTORY_DOCUMENT_LINE l SET COST_UNIT=(SELECT CASE WHEN COUNT(*)=COUNT(d.COST_UNIT) THEN SUM(d.QUANTITY*d.COST_UNIT)/SUM(d.QUANTITY) END FROM BILL_DETAIL d JOIN INVENTORY_DOCUMENT h ON h.BILL_ID=d.BILL_ID WHERE h.ID=l.DOCUMENT_ID AND d.PRODUCT_DETAIL_ID=l.PRODUCT_DETAIL_ID AND d.DELETE_FLAG=0) WHERE l.DOCUMENT_ID IN (SELECT ID FROM INVENTORY_DOCUMENT WHERE DOC_TYPE='RETURN' AND STATUS='POSTED')");
   Map<String,Long> quantities=new HashMap<>();Map<String,BigDecimal> values=new HashMap<>();
   try(var r=s.executeQuery("SELECT l.ID,h.BILL_ID,l.PRODUCT_DETAIL_ID,l.QUANTITY,l.COST_UNIT FROM INVENTORY_DOCUMENT_LINE l JOIN INVENTORY_DOCUMENT h ON h.ID=l.DOCUMENT_ID WHERE h.DOC_TYPE='RETURN' AND h.STATUS='POSTED' ORDER BY h.POSTED_AT,h.ID,l.ID")){
    while(r.next()){long bill=r.getLong(2),variant=r.getLong(3),qty=r.getLong(4);String key=bill+":"+variant;long cumulative=quantities.getOrDefault(key,0L)+qty;quantities.put(key,cumulative);BigDecimal unit=r.getBigDecimal(5),amount=null;
     if(unit!=null){try(var q=c.prepareStatement("SELECT SUM(QUANTITY),SUM(COST_TOTAL) FROM BILL_DETAIL WHERE BILL_ID=? AND PRODUCT_DETAIL_ID=? AND DELETE_FLAG=0")){q.setLong(1,bill);q.setLong(2,variant);try(var totals=q.executeQuery()){totals.next();BigDecimal newValue=totals.getBigDecimal(2).multiply(BigDecimal.valueOf(cumulative)).divide(totals.getBigDecimal(1),0,RoundingMode.HALF_UP);amount=newValue.subtract(values.getOrDefault(key,BigDecimal.ZERO));values.put(key,newValue);}}}
     try(var u=c.prepareStatement("UPDATE INVENTORY_DOCUMENT_LINE SET COGS_VALUE=? WHERE ID=? AND COGS_VALUE IS NULL")){u.setBigDecimal(1,amount);u.setLong(2,r.getLong(1));u.executeUpdate();}
    }
   }
   s.executeUpdate("INSERT INTO SHOP_MIGRATION(VERSION) VALUES('V008_COST_ROUND')");c.commit();
  }catch(Exception e){c.rollback();throw e;}
 }
}
