package com.example.clothingshop;

import com.example.clothingshop.admin.*;
import com.example.clothingshop.bill.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import javax.sql.DataSource;
import java.nio.file.*;
import java.sql.*;
import java.math.*;
import java.time.*;
import java.util.*;
import static com.example.clothingshop.admin.AdminRepository.number;
import static org.junit.jupiter.api.Assertions.*;

/** Destructive demo reset, only enabled by an explicit environment switch. Backup precedes all writes. */
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.NONE)
@EnabledIfEnvironmentVariable(named="RUN_DEMO_SEED",matches="REPLACE_WITH_FAKE_DATA")
class DemoDataSeedTest {
 @Autowired AdminRepository db; @Autowired AdminService admin; @Autowired InventoryService inventory;
 @Autowired BillService bills; @Autowired PasswordEncoder encoder; @Autowired DataSource source;
 @Autowired ObjectMapper json; @Autowired PlatformTransactionManager transactions;
 final String demoPassword="DemoShop@2026!";
 long actor,staff; Path backup;
 BigDecimal money(long n){return BigDecimal.valueOf(n);}
 @Test void replaceWithDemonstrationData()throws Exception{
  backup=Path.of(System.getenv("DEMO_BACKUP_DIR")).resolve("before-demo-"+LocalDateTime.now().toString().replace(':','-')+".json");
  // Full JDBC export includes metadata and all existing values, even account hashes. Directory is private.
  Map<String,Object> snapshot=new LinkedHashMap<>();
  try(var c=source.getConnection();var s=c.createStatement();var r=s.executeQuery("SELECT TABLE_NAME FROM USER_TABLES WHERE TABLE_NAME NOT LIKE 'BIN$%' ORDER BY TABLE_NAME")){
   var names=new ArrayList<String>();while(r.next())names.add(r.getString(1));
   for(String name:names){if(!name.matches("[A-Z0-9_]+"))throw new IllegalStateException("Unexpected table");try(var q=c.createStatement();var rows=q.executeQuery("SELECT * FROM "+name)){var meta=rows.getMetaData();var columns=new ArrayList<Object>();for(int i=1;i<=meta.getColumnCount();i++)columns.add(Map.of("name",meta.getColumnName(i),"type",meta.getColumnType(i),"typeName",meta.getColumnTypeName(i)));var values=new ArrayList<Object>();while(rows.next()){var row=new ArrayList<Object>();for(int i=1;i<=meta.getColumnCount();i++){Object v=rows.getObject(i);if(v instanceof Clob clob)v=clob.getSubString(1,Math.toIntExact(clob.length()));else if(v instanceof Blob blob)v=Base64.getEncoder().encodeToString(blob.getBytes(1,Math.toIntExact(blob.length())));else if(v!=null&&!(v instanceof Number))v=rows.getString(i);row.add(v);}values.add(row);}snapshot.put(name,Map.of("columns",columns,"rows",values));}}
  }
  Files.writeString(backup,json.writerWithDefaultPrettyPrinter().writeValueAsString(snapshot),StandardOpenOption.CREATE_NEW);
  assertTrue(Files.size(backup)>100);
  new TransactionTemplate(transactions).executeWithoutResult(tx->seed());
  var summary=new LinkedHashMap<String,Object>();summary.put("backup",backup.toAbsolutePath().toString());
  for(String table:List.of("PRODUCT","PRODUCT_DETAIL","CUSTOMER","BILL","INVENTORY_DOCUMENT","VOUCHER","PRODUCT_IMAGE"))summary.put(table,number(db.one("SELECT COUNT(*) N FROM "+table),"N"));
  summary.put("orderStates",db.rows("SELECT STATUS,COUNT(*) N FROM BILL GROUP BY STATUS ORDER BY STATUS"));
  summary.put("inventory",db.rows("SELECT DOC_TYPE,STATUS,COUNT(*) N FROM INVENTORY_DOCUMENT GROUP BY DOC_TYPE,STATUS ORDER BY DOC_TYPE,STATUS"));
  summary.put("demoPassword",demoPassword);summary.put("demoStaff","demo_staff");summary.put("demoCustomer","demo_khach01");
  Files.writeString(backup.getParent().resolve("demo-summary.json"),json.writerWithDefaultPrettyPrinter().writeValueAsString(summary));
  System.out.println("DEMO DATA COMMITTED: "+json.writeValueAsString(summary));
 }
 void seed(){
  actor=number(db.one("SELECT m.ID FROM MANAGER m JOIN MANAGER_ROLE r ON r.ID=m.MANAGER_ROLE_ID WHERE m.USERNAME='hieu' AND r.ROLE_CODE='ADMIN' AND m.DELETE_FLAG=0"),"ID");
  // FK-safe DML only: no table drops, truncates, sequence resets or constraint disabling.
  for(String table:List.of("BILL_REFUND","STOCK_COST_ADJUSTMENT","INVENTORY_HISTORY","INVENTORY_DOCUMENT_LINE","INVENTORY_COST_OPENING","PAYMENT_HISTORY","BILL_HISTORY","BILL_DETAIL","CART_DETAIL","CART","PRODUCT_IMAGE","ADMIN_AUDIT","MANAGER_PASSWORD_RESET"))db.update("DELETE FROM "+table);
  db.update("UPDATE INVENTORY_DOCUMENT SET SOURCE_ID=NULL");db.update("DELETE FROM INVENTORY_DOCUMENT");
  for(String table:List.of("BILL","ADDRESS","CUSTOMER","USERS","PRODUCT_DETAIL","PRODUCT","VOUCHER"))db.update("DELETE FROM "+table);
  db.update("UPDATE CATEGORY SET PARENT_ID=NULL");for(String table:List.of("CATEGORY","BRAND","MATERIAL","COLOR"))db.update("DELETE FROM "+table);
  db.update("DELETE FROM MANAGER WHERE ID<>:a","a",actor);
  long staffRole=number(db.one("SELECT ID FROM MANAGER_ROLE WHERE ROLE_CODE='STAFF' AND DELETE_FLAG=0"),"ID");
  staff=admin.manager(new AdminRequests.Manager("demo_staff",demoPassword,"Nhân viên demo","nhanvien@example.test",staffRole),actor);
  long locked=admin.manager(new AdminRequests.Manager("demo_locked",demoPassword,"Nhân viên đã khóa","dakhoa@example.test",staffRole),actor);
  db.update("UPDATE MANAGER SET STATUS=0 WHERE ID=:i","i",locked);
  String[] colorNames={"Đen","Trắng","Xanh navy","Be","Xanh rêu","Đỏ đô"},hex={"#111827","#FFFFFF","#16324F","#D9C5A0","#556B2F","#800020"};long[] colors=new long[6];
  for(int i=0;i<6;i++)colors[i]=number(admin.color(new AdminRequests.Color(colorNames[i],hex[i]),actor),"ID");
  long parent=admin.category(null,new AdminRequests.Category("Thời trang demo",null,1),actor);
  String[] cats={"Áo thun","Áo sơ mi","Quần jeans","Quần short","Áo khoác","Váy và phụ kiện"};long[] categories=new long[6];for(int i=0;i<6;i++)categories[i]=admin.category(null,new AdminRequests.Category(cats[i],parent,1),actor);
  admin.category(null,new AdminRequests.Category("Bộ sưu tập đã ẩn",parent,0),actor);
  String[] names={"Áo thun cotton basic","Áo polo phối cổ","Áo sơ mi Oxford","Áo sơ mi linen","Quần jeans slim","Quần jeans ống rộng","Quần short kaki","Quần jogger thể thao","Áo khoác bomber","Áo hoodie nỉ","Áo cardigan len","Váy midi xếp ly","Chân váy chữ A","Áo giữ nhiệt","Mũ lưỡi trai — sắp hết","Túi canvas — bản nháp"};
  String[] materials={"Cotton 100%","Cotton pique","Oxford","Linen","Denim","Denim","Kaki","Thun thể thao","Polyester","Nỉ bông","Len","Chiffon","Kaki","Thun lạnh","Cotton","Canvas"};
  var products=new ArrayList<Long>();var variants=new ArrayList<List<Long>>();
  for(int i=0;i<names.length;i++){
   var vs=new ArrayList<AdminRequests.Variant>();for(int color=0;color<2;color++)for(String size:List.of("M","L","XL"))vs.add(new AdminRequests.Variant(null,colors[(i+color)%6],size,null,money(179000+i*25000),0,1));
   int cat=i<2?0:i<4?1:i<6?2:i<8?3:i<11?4:5;
   String image="/store/assets/img/product/product-"+(i%12+1)+".webp";
   long p=admin.product(null,new AdminRequests.Product(null,names[i],null,null,categories[cat],2,"[DEMO] Dữ liệu giả để thử sản phẩm, hình ảnh, nhập/xuất kho, đơn hàng và thống kê.",image,i==15?0:1,vs,"Demo Wear "+(i%3+1),materials[i]),actor);products.add(p);
   db.update("INSERT INTO PRODUCT_IMAGE(PRODUCT_ID,IMAGE_URL,SORT_ORDER,IS_COVER) VALUES(:p,:u,0,1)","p",p,"u",image);
   db.update("INSERT INTO PRODUCT_IMAGE(PRODUCT_ID,IMAGE_URL,SORT_ORDER,IS_COVER) VALUES(:p,:u,1,0)","p",p,"u","/store/assets/img/product/product-"+((i+1)%12+1)+".webp");
   var ids=db.rows("SELECT ID FROM PRODUCT_DETAIL WHERE PRODUCT_ID=:p ORDER BY ID","p",p).stream().map(v->number(v,"ID")).toList();variants.add(ids);
   if(i!=15){int qty=i==14?3:80;doc("RECEIPT",null,ids,qty,70000+i*11000,true,true,"DEMO · Nhập lô đầu",LocalDate.of(2024,12,1).atTime(9,0));if(i<14)doc("RECEIPT",null,ids,20,90000+i*11000,true,true,"DEMO · Nhập lô hai, giá vốn thay đổi",LocalDate.of(2025,1,1).atTime(9,0));}
  }
  String encoded=encoder.encode(demoPassword);long role=number(db.one("SELECT MIN(ID) ID FROM ROLES"),"ID");long[] customers=new long[24],addresses=new long[24];
  String[] customerNames={"Nguyễn Minh Anh","Trần Quốc Bảo","Lê Ngọc Chi","Phạm Hải Đăng","Hoàng Thu Hà","Vũ Đức Huy","Đặng Khánh Linh","Bùi Gia Minh"};
  for(int i=0;i<24;i++){String username=String.format("demo_khach%02d",i+1);long u=db.insert("INSERT INTO USERS(ROLE_ID,USERNAME,EMAIL,PASSWORD_HASH,STATUS) VALUES(:r,:n,:e,:p,:s)","r",role,"n",username,"e",username+"@example.test","p",encoded,"s",i==23?0:1);customers[i]=db.insert("INSERT INTO CUSTOMER(USER_ID,FULL_NAME,PHONE,GENDER) VALUES(:u,:n,:p,:g)","u",u,"n",customerNames[i%8]+(i>=8?" "+(i/8+1):""),"p",String.format("090%07d",1000000+i),"g",i%3);addresses[i]=db.insert("INSERT INTO ADDRESS(CUSTOMER_ID,RECEIVER_NAME,RECEIVER_PHONE,PROVINCE,WARD,DETAIL_ADDRESS,IS_DEFAULT) VALUES(:c,:n,:p,:province,:ward,:detail,1)","c",customers[i],"n",customerNames[i%8],"p",String.format("090%07d",1000000+i),"province",i%2==0?"TP. Hồ Chí Minh":"Hà Nội","ward",i%2==0?"Phường Bến Thành":"Phường Cầu Giấy","detail",(10+i)+" Đường Demo");}
  voucher("DEMO10","PERCENT",10,100000,50000,LocalDateTime.of(2024,1,1,0,0),LocalDateTime.of(2027,12,31,23,59),1000,1);
  voucher("DEMO50K","FIXED",50000,300000,50000,LocalDateTime.of(2024,1,1,0,0),LocalDateTime.of(2027,12,31,23,59),1000,1);
  voucher("HETHAN","PERCENT",15,0,50000,LocalDateTime.of(2024,1,1,0,0),LocalDateTime.of(2025,1,1,0,0),100,1);
  voucher("SAPMO","PERCENT",20,0,100000,LocalDateTime.of(2027,1,1,0,0),LocalDateTime.of(2027,12,31,23,59),100,1);
  voucher("TATMA","FIXED",20000,0,20000,LocalDateTime.of(2024,1,1,0,0),LocalDateTime.of(2027,12,31,23,59),100,0);
  for(int i=0;i<120;i++){
   int customer=i%23;var v=variants.get(i%14);long first=v.get(i%6),second=variants.get((i+3)%14).get((i+1)%6);
   int qty=2+i%3;boolean bank=i%3==0;
   var order=bills.create(new BillRequest(customers[customer],addresses[customer],bank?"BANK":"COD",money(30000),"[DEMO] Đơn mẫu "+(i+1),i%4==0?"DEMO10":i%7==0?"DEMO50K":null,List.of(new BillRequest.Item(first,qty),new BillRequest.Item(second,1))));long id=order.id();
   int state=i%10;if(state!=0){if(state==3)bills.changeStatus(id,4,actor,"[DEMO] Khách đổi ý, hoàn tồn");else{bills.changeStatus(id,1,staff,"[DEMO] Đã gọi xác nhận");if(state!=1){bills.shipping(id,new AdminRequests.Shipping(bank?"BANK":"COD",bank?1:0,"Giao hàng demo","DEMO"+String.format("%05d",i+1)),staff);bills.changeStatus(id,2,actor,"[DEMO] Bàn giao vận chuyển");if(state!=2){bills.changeStatus(id,3,actor,"[DEMO] Giao thành công");}}}}
   LocalDate date=i<96?LocalDate.of(2025,1,2).plusDays(i*7L):LocalDate.of(2026,11,1).plusDays((i-96)/3);LocalDateTime created=date.atTime(8+i%4,15),completed=created.plusHours(4);
   if(state>=4){if(state==4)bills.changeStatus(id,5,actor,"[DEMO] Khách yêu cầu trả hàng, chờ nhận");if(state==5){doc("RETURN",id,List.of(first),qty,0,true,true,"DEMO · Hoàn toàn bộ dòng chính",completed.plusHours(1));doc("RETURN",id,List.of(second),1,0,true,true,"DEMO · Hoàn dòng còn lại",completed.plusHours(1));}if(state==6)doc("RETURN",id,List.of(first),1,0,i%20==6,true,"DEMO · Hoàn một phần, kiểm tra chất lượng",completed.plusHours(1));}
   // Dates are synthetic but maintain receipt-before-sale and sale-before-return chronology.
   db.update("UPDATE BILL SET BILL_CODE=:code,CREATED_AT=:c,UPDATED_AT=:u,COMPLETED_AT=CASE WHEN COMPLETED_AT IS NOT NULL THEN :done END,RETURNED_AT=CASE WHEN RETURNED_AT IS NOT NULL THEN :ret END WHERE ID=:i","code",String.format("DEMO-HD-%03d",i+1),"c",Timestamp.valueOf(created),"u",Timestamp.valueOf(completed),"done",Timestamp.valueOf(completed),"ret",Timestamp.valueOf(completed.plusHours(1)),"i",id);
   db.update("UPDATE BILL_DETAIL SET CREATED_AT=:d WHERE BILL_ID=:b","d",Timestamp.valueOf(created),"b",id);
   db.update("UPDATE BILL_HISTORY SET CREATED_AT=:d+NUMTODSINTERVAL(NEW_STATUS,'HOUR') WHERE BILL_ID=:b","d",Timestamp.valueOf(created),"b",id);
   db.update("UPDATE INVENTORY_DOCUMENT SET CREATED_AT=CASE WHEN DOC_TYPE='RETURN' THEN :r ELSE :d END,POSTED_AT=CASE WHEN DOC_TYPE='RETURN' THEN :r ELSE :d END WHERE BILL_ID=:b","r",Timestamp.valueOf(completed.plusHours(1)),"d",Timestamp.valueOf(created),"b",id);
   db.update("UPDATE INVENTORY_HISTORY SET CREATED_AT=CASE WHEN MOVEMENT_TYPE='RETURN' THEN :r ELSE :d END WHERE BILL_ID=:b","r",Timestamp.valueOf(completed.plusHours(1)),"d",Timestamp.valueOf(created),"b",id);
   if(order.status()!=4&&state!=0&&state!=1&&state!=2&&state!=3)db.update("INSERT INTO PAYMENT_HISTORY(BILL_ID,AMOUNT,PAYMENT_METHOD,STATUS,TRANSACTION_CODE,CREATED_AT) VALUES(:b,:v,:m,1,:code,:d)","b",id,"v",order.totalAmount(),"m",bank?"BANK":"COD","code","DEMO-PAY-"+(i+1),"d",Timestamp.valueOf(completed));
  }
  var today=LocalDate.of(2026,11,8).atTime(12,0);
  doc("ISSUE",null,List.of(variants.get(0).get(0)),4,0,true,true,"DEMO · Xuất hàng tặng sự kiện",today);
  long countVariant=variants.get(1).get(0);int actual=(int)number(db.one("SELECT QUANTITY FROM PRODUCT_DETAIL WHERE ID=:i","i",countVariant),"QUANTITY")-2;
  doc("COUNT",null,List.of(countVariant),actual,0,true,true,"DEMO · Kiểm kê thiếu 2 sản phẩm",today);
  long wrong=doc("ISSUE",null,List.of(variants.get(2).get(0)),2,0,true,true,"DEMO · Phiếu xuất nhầm để minh họa đảo phiếu",today);long reverse=inventory.reverse(wrong,"DEMO · Đảo phiếu xuất nhầm",actor);inventory.approve(reverse,actor);
  doc("RECEIPT",null,List.of(variants.get(3).get(0)),15,125000,true,false,"DEMO · Phiếu nháp chờ ADMIN xác nhận",today);
  long cancel=doc("RECEIPT",null,List.of(variants.get(4).get(0)),10,150000,true,false,"DEMO · Phiếu nhập bị hủy",today);inventory.cancel(cancel,"DEMO · Nhà cung cấp hủy giao",actor);
  long cart=db.insert("INSERT INTO CART(CUSTOMER_ID) VALUES(:c)","c",customers[0]);db.update("INSERT INTO CART_DETAIL(CART_ID,PRODUCT_DETAIL_ID,QUANTITY) VALUES(:c,:v,2)","c",cart,"v",variants.get(0).get(0));
  assertEquals(120,number(db.one("SELECT COUNT(*) N FROM BILL"),"N"));assertEquals(16,number(db.one("SELECT COUNT(*) N FROM PRODUCT"),"N"));
  assertEquals(0,number(db.one("SELECT COUNT(*) N FROM PRODUCT_DETAIL v JOIN PRODUCT p ON p.ID=v.PRODUCT_ID WHERE v.QUANTITY<0 OR (v.PHYSICAL_QUANTITY>0 AND v.AVERAGE_COST IS NULL)"),"N"));
  assertEquals(0,number(db.one("SELECT COUNT(*) N FROM BILL_DETAIL d JOIN BILL b ON b.ID=d.BILL_ID WHERE d.COST_UNIT IS NULL AND b.DISPATCHED=1"),"N"));
  db.audit(actor,"REPLACE_WITH_DEMO_DATA","DATABASE",null);
 }
 void voucher(String code,String type,long value,long min,long max,LocalDateTime from,LocalDateTime to,int limit,int status){admin.voucher(null,new AdminRequests.Voucher(code,type,money(value),money(min),money(max),from,to,limit,status),actor);}
 long doc(String type,Long bill,List<Long> variants,int quantity,long cost,boolean restock,boolean posted,String reason,LocalDateTime date){
  var lines=variants.stream().map(v->new InventoryRequests.Line(v,quantity,restock,type.equals("RETURN")?(restock?"Hàng nguyên vẹn, nhập lại bán":"Hàng lỗi, không nhập lại bán"):"Dữ liệu demo",type.equals("RECEIPT")?money(cost):null)).toList();
  long id=inventory.save(null,new InventoryRequests.Document(type,reason,bill,lines),actor);if(posted)inventory.approve(id,actor);
  db.update("UPDATE INVENTORY_DOCUMENT SET CREATED_AT=:d,POSTED_AT=CASE WHEN STATUS='POSTED' THEN :d END WHERE ID=:i","d",Timestamp.valueOf(date),"i",id);
  db.update("UPDATE INVENTORY_HISTORY SET CREATED_AT=:d WHERE DOCUMENT_ID=:i","d",Timestamp.valueOf(date),"i",id);return id;
 }
}
