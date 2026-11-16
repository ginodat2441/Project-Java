import java.nio.file.*;
import java.sql.*;
/** JDBC runner: secrets only through environment, never command arguments or output. */
public class OracleMigration {
 public static void main(String[] args)throws Exception{
  try(var c=DriverManager.getConnection(System.getenv("SPRING_DATASOURCE_URL"),System.getenv("SPRING_DATASOURCE_USERNAME"),System.getenv("SPRING_DATASOURCE_PASSWORD"));var s=c.createStatement()){
   for(String block:Files.readString(Path.of(args[0])).split("(?m)^/\\s*$")){if(!block.isBlank())s.execute(block.trim());}
   System.out.println("Migration succeeded for "+c.getMetaData().getUserName());
   boolean physical;try(var r=s.executeQuery("SELECT COUNT(*) FROM USER_TAB_COLUMNS WHERE TABLE_NAME='PRODUCT_DETAIL' AND COLUMN_NAME='PHYSICAL_QUANTITY'")){r.next();physical=r.getInt(1)>0;}
   if(physical)try(var r=s.executeQuery("SELECT COUNT(*) SKUS,SUM(QUANTITY) AVAILABLE,SUM(RESERVED_QUANTITY) RESERVED,SUM(PHYSICAL_QUANTITY) PHYSICAL,SUM(CASE WHEN QUANTITY<>PHYSICAL_QUANTITY-RESERVED_QUANTITY THEN 1 ELSE 0 END) MISMATCH FROM PRODUCT_DETAIL WHERE DELETE_FLAG=0")){while(r.next())System.out.println("SKUs="+r.getLong(1)+" available="+r.getLong(2)+" reserved="+r.getLong(3)+" physical="+r.getLong(4)+" mismatches="+r.getLong(5));}
  }
 }
}
