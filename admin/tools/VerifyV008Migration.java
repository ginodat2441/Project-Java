import java.sql.*;
import java.nio.file.*;
import java.util.*;
/** Run only on an isolated *_STAGE copy. Checks retry, rollback guard, restoration and re-apply. */
public class VerifyV008Migration {
 static void run(Connection c,String file)throws Exception{try(var s=c.createStatement()){for(String block:Files.readString(Path.of(file)).split("(?m)^/\\s*$"))if(!block.isBlank())s.execute(block.trim());}}
 static Map<Long,String> rows(Connection c)throws Exception{var out=new TreeMap<Long,String>();try(var s=c.createStatement();var r=s.executeQuery("SELECT ID,SKU,QUANTITY FROM PRODUCT_DETAIL ORDER BY ID")){while(r.next())out.put(r.getLong(1),r.getString(2)+"|"+r.getLong(3));}return out;}
 public static void main(String[] args)throws Exception{
  String user=System.getenv("SPRING_DATASOURCE_USERNAME");if(user==null||!user.endsWith("_STAGE"))throw new IllegalArgumentException("Isolated *_STAGE schema required");
  try(var c=DriverManager.getConnection(System.getenv("SPRING_DATASOURCE_URL"),user,System.getenv("SPRING_DATASOURCE_PASSWORD"))){
   String migration="backend_java/src/main/resources/db/migrations/V008__sku_physical_inventory.sql",rollback="backend_java/src/main/resources/db/migrations/V008__rollback_before_new_writes.sql";var before=rows(c);run(c,"backend_java/src/main/resources/db/migrations/V008__preflight.sql");run(c,migration);
   String tag="VERIFY_ROLLBACK_"+UUID.randomUUID().toString().substring(0,8);
   try(var s=c.prepareStatement("INSERT INTO ADMIN_AUDIT(ACTION,OBJECT_TYPE) VALUES(?,'MIGRATION_TEST')")){s.setString(1,tag);s.executeUpdate();}
   try{run(c,rollback);throw new IllegalStateException("Rollback should reject post-upgrade writes");}catch(SQLException expected){if(!expected.getMessage().contains("20081"))throw expected;System.out.println("PASS: rollback rejects post-upgrade audited writes");}
   try(var s=c.prepareStatement("DELETE FROM ADMIN_AUDIT WHERE ACTION=? AND OBJECT_TYPE='MIGRATION_TEST'")){s.setString(1,tag);s.executeUpdate();}
   run(c,rollback);if(!before.equals(rows(c)))throw new IllegalStateException("Rollback changed available quantities or SKU identities");System.out.println("PASS: rollback restored original SKU identities and available balances");
   run(c,migration);run(c,migration);if(!before.equals(rows(c)))throw new IllegalStateException("Retry changed available quantities or SKU identities");
   try(var s=c.createStatement();var r=s.executeQuery("SELECT COUNT(*) FROM PRODUCT_DETAIL WHERE QUANTITY<>PHYSICAL_QUANTITY-RESERVED_QUANTITY")){r.next();if(r.getInt(1)!=0)throw new IllegalStateException("Stock balance mismatch");}
   System.out.println("PASS: migration applies and retries without changing "+before.size()+" SKU identities/available balances; reconstruct costs next");
  }
 }
}
