package com.example.clothingshop;

import com.example.clothingshop.admin.*;
import com.fasterxml.jackson.databind.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.net.*;
import java.net.http.*;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

/** Explicit opt-in: exercises a running HTTP server against real Oracle, then removes only its own fixtures. */
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT)
@EnabledIfEnvironmentVariable(named="RUN_ORACLE_TESTS",matches="true")
class OracleAcceptanceTest {
 @LocalServerPort int port;
 @Autowired AdminRepository db;
 @Autowired PasswordEncoder encoder;
 @Autowired ObjectMapper json;
 @Autowired RecoveryDelivery recoveryDelivery;
 List<java.nio.file.Path> recoveryFiles=new ArrayList<>();
 String prefix="IT"+UUID.randomUUID().toString().substring(0,8),password=UUID.randomUUID()+"xA1!";
 List<Long> managers=new ArrayList<>(),products=new ArrayList<>(),categories=new ArrayList<>(),vouchers=new ArrayList<>();
 long customer,user,address;
 class Browser {
  HttpClient client=HttpClient.newBuilder().cookieHandler(new CookieManager(null,CookiePolicy.ACCEPT_ALL)).followRedirects(HttpClient.Redirect.NEVER).build();
  String token;
  HttpResponse<String> request(String path,String method,Object body)throws Exception{
   var b=HttpRequest.newBuilder(URI.create("http://localhost:"+port+path));
   if(!method.equals("GET")&&token!=null)b.header("X-XSRF-TOKEN",token);
   if(body!=null)b.header("Content-Type","application/json");
   return client.send(b.method(method,body==null?HttpRequest.BodyPublishers.noBody():HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body))).build(),HttpResponse.BodyHandlers.ofString());
  }
  void csrf()throws Exception{token=json.readTree(request("/api/csrf","GET",null).body()).get("token").asText();}
  int login(String username,String pw)throws Exception{
   csrf();var r=client.send(HttpRequest.newBuilder(URI.create("http://localhost:"+port+"/login")).header("Content-Type","application/x-www-form-urlencoded").header("X-XSRF-TOKEN",token).POST(HttpRequest.BodyPublishers.ofString("username="+URLEncoder.encode(username,java.nio.charset.StandardCharsets.UTF_8)+"&password="+URLEncoder.encode(pw,java.nio.charset.StandardCharsets.UTF_8))).build(),HttpResponse.BodyHandlers.ofString());
   csrf();assertEquals(302,r.statusCode());return r.headers().firstValue("location").orElse("").contains("error")?0:1;
  }
  JsonNode get(String path)throws Exception{var r=request(path,"GET",null);assertEquals(200,r.statusCode(),r.body());return json.readTree(r.body());}
  JsonNode save(String path,String method,Object body)throws Exception{var r=request(path,method,body);assertEquals(200,r.statusCode(),r.body());return r.body().isBlank()?json.nullNode():json.readTree(r.body());}
  HttpResponse<String> upload(long product,byte[] bytes)throws Exception{
   String boundary="ShopBoundary"+UUID.randomUUID();var out=new java.io.ByteArrayOutputStream();
   out.write(("--"+boundary+"\r\nContent-Disposition: form-data; name=\"file\"; filename=\"../../unsafe.png\"\r\nContent-Type: image/png\r\n\r\n").getBytes());out.write(bytes);out.write(("\r\n--"+boundary+"--\r\n").getBytes());
   return client.send(HttpRequest.newBuilder(URI.create("http://localhost:"+port+"/api/admin/products/"+product+"/images")).header("X-XSRF-TOKEN",token).header("Content-Type","multipart/form-data; boundary="+boundary).POST(HttpRequest.BodyPublishers.ofByteArray(out.toByteArray())).build(),HttpResponse.BodyHandlers.ofString());
  }
  HttpResponse<String> productWithImages(Map<String,Object> product,List<byte[]> files,List<Map<String,Object>> images)throws Exception{
   String boundary="ProductBoundary"+UUID.randomUUID();var out=new java.io.ByteArrayOutputStream();
   for(var part:Map.of("product",json.writeValueAsBytes(product),"images",json.writeValueAsBytes(images)).entrySet()){
    out.write(("--"+boundary+"\r\nContent-Disposition: form-data; name=\""+part.getKey()+"\"\r\nContent-Type: application/json\r\n\r\n").getBytes());out.write(part.getValue());out.write("\r\n".getBytes());
   }
   for(byte[] file:files){out.write(("--"+boundary+"\r\nContent-Disposition: form-data; name=\"files\"; filename=\"photo.png\"\r\nContent-Type: image/png\r\n\r\n").getBytes());out.write(file);out.write("\r\n".getBytes());}
   out.write(("--"+boundary+"--\r\n").getBytes());
   return client.send(HttpRequest.newBuilder(URI.create("http://localhost:"+port+"/api/admin/products/with-images")).header("X-XSRF-TOKEN",token).header("Content-Type","multipart/form-data; boundary="+boundary).POST(HttpRequest.BodyPublishers.ofByteArray(out.toByteArray())).build(),HttpResponse.BodyHandlers.ofString());
  }
 }
 long num(Map<String,Object> m,String k){return AdminRepository.number(m,k);}
 long fixtureManager(String role)throws Exception{
  long r=num(db.one("SELECT ID FROM MANAGER_ROLE WHERE ROLE_CODE=:r","r",role),"ID");
  long id=db.insert("INSERT INTO MANAGER(MANAGER_ROLE_ID,USERNAME,PASSWORD_HASH,FULL_NAME,EMAIL) VALUES(:r,:u,:p,:u,:e)","r",r,"u",prefix+role,"p",encoder.encode(password),"e",prefix+role+"@example.test");managers.add(id);return id;
 }
 Map<String,Object> variant(Long id,long color,String size,String sku,int qty){var v=new HashMap<String,Object>();if(id!=null)v.put("id",id);v.putAll(Map.of("colorId",color,"size",size,"sku",prefix+sku,"price",199000,"quantity",qty,"status",1));return v;}
 Map<String,Object> productBody(List<Map<String,Object>> variants){return new HashMap<>(Map.of("code",prefix+"BASIC","name",prefix+" Áo thun basic","brandId",num(db.one("SELECT MIN(ID) ID FROM BRAND WHERE DELETE_FLAG=0"),"ID"),"materialId",num(db.one("SELECT MIN(ID) ID FROM MATERIAL WHERE DELETE_FLAG=0"),"ID"),"status",1,"thumbnailUrl","/store/assets/img/product/product-1.webp","variants",variants));}
 Map<String,Object> bill(long variant,int qty,String voucher){return Map.of("customerId",customer,"addressId",address,"paymentMethod","COD","shippingFee",-100000,"voucherCode",voucher,"items",List.of(Map.of("productDetailId",variant,"quantity",qty)));}
 Map<String,Object> voucher(String code,String starts,String ends,int limit){return Map.of("code",prefix+code,"type","PERCENT","value",10,"minOrder",100000,"maxDiscount",50000,"startsAt",starts,"endsAt",ends,"usageLimit",limit,"status",1);}
 long document(Browser browser,String type,Long bill,Map<Long,Integer> quantities,boolean restock)throws Exception{
  var lines=quantities.entrySet().stream().map(e->Map.of("variantId",e.getKey(),"quantity",e.getValue(),"restock",restock,"unitCost",125000.50,"conditionNote","Nghiệm thu")).toList();
  var body=new HashMap<String,Object>();body.put("type",type);body.put("reason",prefix+" - "+type);body.put("billId",bill);body.put("lines",lines);return browser.save("/api/admin/inventory/documents","POST",body).asLong();
 }
 void postDocument(Browser browser,String type,Long bill,Map<Long,Integer> quantities,boolean restock)throws Exception{long id=document(browser,type,bill,quantities,restock);browser.save("/api/admin/inventory/documents/"+id+"/approve","POST",null);}
 String recoveryToken(Browser browser,String identifier)throws Exception{
  Set<java.nio.file.Path> before=new HashSet<>();if(java.nio.file.Files.isDirectory(recoveryDelivery.directory()))try(var paths=java.nio.file.Files.list(recoveryDelivery.directory())){before.addAll(paths.toList());}
  var result=browser.save("/api/auth/forgot-password","POST",Map.of("identifier",identifier));assertFalse(result.has("token"));
  java.nio.file.Path created;try(var paths=java.nio.file.Files.list(recoveryDelivery.directory())){created=paths.filter(p->!before.contains(p)).findFirst().orElseThrow();}recoveryFiles.add(created);
  String content=java.nio.file.Files.readString(created);var matcher=java.util.regex.Pattern.compile("#token=([A-Za-z0-9_-]+)").matcher(content);assertTrue(matcher.find());return matcher.group(1);
 }
 @Test void completeShopFlows()throws Exception{
  var anonymous=new Browser();assertEquals(302,anonymous.request("/admin","GET",null).statusCode());assertEquals(401,anonymous.request("/api/admin/products","GET",null).statusCode());
  assertEquals(403,anonymous.request("/api/brands","POST",Map.of("brandName","bad")).statusCode());
  assertEquals(200,anonymous.request("/store/index.html","GET",null).statusCode());
  fixtureManager("ADMIN");long staffId=fixtureManager("STAFF");
  var admin=new Browser();assertEquals(0,admin.login(prefix+"ADMIN","incorrect-password"));assertEquals(1,admin.login(prefix+"ADMIN",password));
  assertEquals(200,admin.request("/admin","GET",null).statusCode());
  var noCsrf=new Browser();assertEquals(1,noCsrf.login(prefix+"ADMIN",password));noCsrf.token=null;assertEquals(403,noCsrf.request("/api/admin/categories","POST",Map.of("name","bad","status",1)).statusCode());
  assertEquals(403,admin.request("/api/dev/sql","POST",Map.of("sql","select * from manager")).statusCode());
  var staff=new Browser();assertEquals(1,staff.login(prefix+"STAFF",password));assertEquals(403,staff.request("/api/admin/managers","GET",null).statusCode());
  long staffRole=num(db.one("SELECT MANAGER_ROLE_ID FROM MANAGER WHERE ID=:i","i",staffId),"MANAGER_ROLE_ID");
  long targetAdminRole=num(db.one("SELECT ID FROM MANAGER_ROLE WHERE ROLE_CODE='ADMIN'"),"ID");
  admin.save("/api/admin/managers/"+staffId+"/access","PUT",Map.of("roleId",targetAdminRole,"status",1));assertEquals(401,staff.request("/api/admin/products","GET",null).statusCode());staff.login(prefix+"STAFF",password);
  admin.save("/api/admin/managers/"+staffId+"/access","PUT",Map.of("roleId",staffRole,"status",1));assertEquals(401,staff.request("/api/admin/products","GET",null).statusCode());staff.login(prefix+"STAFF",password);
  admin.save("/api/admin/managers/"+staffId+"/access","PUT",Map.of("roleId",staffRole,"status",0));
  admin.save("/api/admin/managers/"+staffId+"/access","PUT",Map.of("roleId",staffRole,"status",1));assertEquals(401,staff.request("/api/admin/products","GET",null).statusCode());staff.login(prefix+"STAFF",password);
  admin.save("/api/admin/managers/"+staffId+"/access","PUT",Map.of("roleId",staffRole,"status",0));assertEquals(401,staff.request("/api/admin/products","GET",null).statusCode());
  long adminId=managers.get(0),adminRole=num(db.one("SELECT MANAGER_ROLE_ID FROM MANAGER WHERE ID=:i","i",adminId),"MANAGER_ROLE_ID");
  assertEquals(400,admin.request("/api/admin/managers/"+adminId+"/access","PUT",Map.of("roleId",adminRole,"status",0)).statusCode());
  assertFalse(admin.get("/api/admin/managers").toString().toLowerCase().contains("password"));
  long newColor=admin.save("/api/admin/colors","POST",Map.of("name",prefix+" Xanh navy","hexCode","#123456")).get("ID").asLong();
  assertEquals(newColor,admin.save("/api/admin/colors","POST",Map.of("name","  "+prefix+" Xanh navy  ")).get("ID").asLong());
  assertEquals(400,admin.request("/api/admin/colors","POST",Map.of("name",prefix+" Invalid","hexCode","bad")).statusCode());
  assertTrue(admin.get("/api/admin/lookups").get("colors").toString().contains(prefix+" Xanh navy"));
  long parent=admin.save("/api/admin/categories","POST",Map.of("name",prefix+" Áo","status",1)).asLong();categories.add(parent);
  long child=admin.save("/api/admin/categories","POST",Map.of("name",prefix+" Áo thun","parentId",parent,"status",1)).asLong();categories.add(child);
  assertEquals(400,admin.request("/api/admin/categories/"+parent,"PUT",Map.of("name","cycle","parentId",child,"status",1)).statusCode());
  var colors=db.rows("SELECT ID FROM COLOR WHERE DELETE_FLAG=0 ORDER BY ID FETCH FIRST 2 ROWS ONLY");assertEquals(2,colors.size());long black=num(colors.get(0),"ID"),white=num(colors.get(1),"ID");
  var variants=List.of(variant(null,black,"M","-BLACK-M",0),variant(null,black,"L","-BLACK-L",0),variant(null,white,"M","-WHITE-M",0),variant(null,newColor,"XL","-NAVY-XL",0));
  assertEquals(400,admin.request("/api/admin/products","POST",productBody(List.of(variant(null,black,"XS","-FORGED-STOCK",10)))).statusCode());
  var body=productBody(variants);body.put("categoryId",child);long product=admin.save("/api/admin/products","POST",body).asLong();products.add(product);
  var initial=admin.get("/api/admin/products/"+product).get("variants");Map<Long,Integer> receipt=new LinkedHashMap<>();for(int i=0;i<initial.size();i++)receipt.put(initial.get(i).get("ID").asLong(),i==0?3:5);postDocument(admin,"RECEIPT",null,receipt,true);
  assertEquals(400,admin.upload(product,"not a real image".getBytes()).statusCode());
  var png=new java.io.ByteArrayOutputStream();javax.imageio.ImageIO.write(new java.awt.image.BufferedImage(2,2,java.awt.image.BufferedImage.TYPE_INT_RGB),"png",png);
  var uploaded=admin.upload(product,png.toByteArray());assertEquals(200,uploaded.statusCode(),uploaded.body());long image=Long.parseLong(uploaded.body());
  admin.save("/api/admin/products/"+product+"/images/"+image+"?colorId="+black+"&order=2&cover=true","PUT",null);
  var imageRow=db.one("SELECT IMAGE_URL FROM PRODUCT_IMAGE WHERE ID=:i","i",image);String imageUrl=imageRow.get("IMAGE_URL").toString();assertTrue(imageUrl.matches("/uploads/[0-9a-f-]{36}\\.png"));
  assertEquals(200,anonymous.request(imageUrl,"GET",null).statusCode());assertEquals(400,admin.request("/api/admin/products/"+product+"/images/"+image,"DELETE",null).statusCode());
  assertEquals(black,anonymous.get("/api/catalog/"+product).get("images").get(0).get("COLOR_ID").asLong());
  assertEquals(413,admin.upload(product,new byte[5*1024*1024+1]).statusCode());
  var jpeg=new java.io.ByteArrayOutputStream();javax.imageio.ImageIO.write(new java.awt.image.BufferedImage(2,2,java.awt.image.BufferedImage.TYPE_INT_RGB),"jpg",jpeg);assertEquals(200,admin.upload(product,jpeg.toByteArray()).statusCode());
  assertEquals(200,admin.upload(product,java.nio.file.Files.readAllBytes(java.nio.file.Path.of("../giao dien/assets/img/product/product-1.webp"))).statusCode());
  var duplicate=productBody(List.of(variant(null,black,"M","-DUP1",0),variant(null,black,"M","-DUP2",0)));duplicate.put("code",prefix+"DUP");assertEquals(400,admin.request("/api/admin/products","POST",duplicate).statusCode());
  String generatedCode=admin.get("/api/admin/products/"+product).get("PRODUCT_CODE").asText();assertTrue(generatedCode.matches("SP[0-9]+"));assertNotEquals(body.get("code"),generatedCode);
  var skuDup=productBody(List.of(variant(null,black,"S","-BLACK-M",0)));skuDup.put("code",generatedCode);long secondProduct=admin.save("/api/admin/products","POST",skuDup).asLong();products.add(secondProduct);
  var secondSaved=admin.get("/api/admin/products/"+secondProduct);assertTrue(Long.parseLong(secondSaved.get("PRODUCT_CODE").asText().substring(2))>Long.parseLong(generatedCode.substring(2)));assertTrue(secondSaved.get("variants").get(0).get("SKU").asText().startsWith(secondSaved.get("PRODUCT_CODE").asText()+"-"));
  var negative=variant(null,black,"XL","-NEG",-1);assertEquals(400,admin.request("/api/admin/products","POST",productBody(List.of(negative))).statusCode());negative.put("quantity",1);negative.put("price",-1);assertEquals(400,admin.request("/api/admin/products","POST",productBody(List.of(negative))).statusCode());
  assertEquals(400,admin.request("/api/admin/products","POST",productBody(List.of())).statusCode());
  assertEquals(2,num(db.one("SELECT COUNT(*) N FROM PRODUCT WHERE PRODUCT_NAME LIKE :p","p",prefix+"%"),"N"),"Invalid forms must not leave products");
  var details=admin.get("/api/admin/products/"+product).get("variants");long chosen=details.get(0).get("ID").asLong(),other=details.get(1).get("ID").asLong();
  assertTrue(details.get(0).get("SKU").asText().startsWith(generatedCode+"-"));assertNotEquals(prefix+"-BLACK-M",details.get(0).get("SKU").asText());
  long role=num(db.one("SELECT ID FROM ROLES WHERE ROLE_CODE='CUSTOMER'"),"ID");
  user=db.insert("INSERT INTO USERS(ROLE_ID,USERNAME,EMAIL,PASSWORD_HASH) VALUES(:r,:u,:e,:p)","r",role,"u",prefix,"e",prefix+"@example.test","p",encoder.encode(password));
  customer=db.insert("INSERT INTO CUSTOMER(USER_ID,FULL_NAME,PHONE) VALUES(:u,:n,'0900000000')","u",user,"n",prefix);
  address=db.insert("INSERT INTO ADDRESS(CUSTOMER_ID,RECEIVER_NAME,RECEIVER_PHONE,PROVINCE,WARD,DETAIL_ADDRESS) VALUES(:c,:n,'0900000000',:n,:n,:n)","c",customer,"n",prefix);
  var client=new Browser();client.csrf();client.save("/api/customer/login","POST",Map.of("email",prefix+"@example.test","password",password));
  admin.save("/api/admin/customers/"+customer+"/status?status=0","PUT",null);assertEquals(400,client.request("/api/customer/me","GET",null).statusCode());
  admin.save("/api/admin/customers/"+customer+"/status?status=1","PUT",null);assertEquals(400,client.request("/api/customer/me","GET",null).statusCode());
  client.save("/api/customer/login","POST",Map.of("email",prefix+"@example.test","password",password));assertEquals(200,client.request("/api/customer/me","GET",null).statusCode());
  for(var v:List.of(voucher("OK","2020-01-01T00:00:00","2099-01-01T00:00:00",1),voucher("OLD","2020-01-01T00:00:00","2021-01-01T00:00:00",1),voucher("FUTURE","2098-01-01T00:00:00","2099-01-01T00:00:00",1)))vouchers.add(admin.save("/api/admin/vouchers","POST",v).asLong());
  assertEquals(400,admin.request("/api/bills","POST",bill(chosen,1,prefix+"OLD")).statusCode());
  assertEquals(400,admin.request("/api/bills","POST",bill(chosen,1,prefix+"FUTURE")).statusCode());
  var minimum=new HashMap<String,Object>(voucher("MIN","2020-01-01T00:00:00","2099-01-01T00:00:00",10));minimum.put("minOrder",99999999);vouchers.add(admin.save("/api/admin/vouchers","POST",minimum).asLong());
  assertEquals(400,admin.request("/api/bills","POST",bill(chosen,1,prefix+"MIN")).statusCode());
  assertEquals(3,num(db.one("SELECT QUANTITY FROM PRODUCT_DETAIL WHERE ID=:i","i",chosen),"QUANTITY"));
  long order=admin.save("/api/bills","POST",bill(chosen,1,prefix+"OK")).get("data").get("id").asLong();
  assertEquals(2,num(db.one("SELECT QUANTITY FROM PRODUCT_DETAIL WHERE ID=:i","i",chosen),"QUANTITY"));assertEquals(5,num(db.one("SELECT QUANTITY FROM PRODUCT_DETAIL WHERE ID=:i","i",other),"QUANTITY"));
  assertEquals(209100,num(db.one("SELECT TOTAL_AMOUNT FROM BILL WHERE ID=:i","i",order),"TOTAL_AMOUNT"),"Server totals ignore negative browser shipping");
  assertEquals(400,admin.request("/api/bills","POST",bill(chosen,1,prefix+"OK")).statusCode());
  admin.save("/api/admin/orders/"+order+"/status","PUT",Map.of("status",1,"note","accept"));
  assertEquals(400,admin.request("/api/admin/orders/"+order+"/status","PUT",Map.of("status",3)).statusCode());
  admin.save("/api/admin/orders/"+order+"/status","PUT",Map.of("status",4));assertEquals(3,num(db.one("SELECT QUANTITY FROM PRODUCT_DETAIL WHERE ID=:i","i",chosen),"QUANTITY"));
  assertEquals(400,admin.request("/api/admin/orders/"+order+"/status","PUT",Map.of("status",4)).statusCode());assertEquals(3,num(db.one("SELECT QUANTITY FROM PRODUCT_DETAIL WHERE ID=:i","i",chosen),"QUANTITY"));
  assertEquals(3,num(db.one("SELECT COUNT(*) N FROM BILL_HISTORY WHERE BILL_ID=:i","i",order),"N"));
  var concurrent1=new Browser();concurrent1.login(prefix+"ADMIN",password);var concurrent2=new Browser();concurrent2.login(prefix+"ADMIN",password);
  var pool=Executors.newFixedThreadPool(2);try{
   var f1=pool.submit(()->concurrent1.request("/api/bills","POST",bill(chosen,2,"")));var f2=pool.submit(()->concurrent2.request("/api/bills","POST",bill(chosen,2,"")));
   var results=List.of(f1.get(),f2.get());assertEquals(1,results.stream().filter(r->r.statusCode()==200).count());assertEquals(1,results.stream().filter(r->r.statusCode()==400).count());
  }finally{pool.shutdownNow();}
  assertEquals(1,num(db.one("SELECT QUANTITY FROM PRODUCT_DETAIL WHERE ID=:i","i",chosen),"QUANTITY"));
  assertEquals(400,admin.request("/api/admin/stock/"+chosen,"PUT",Map.of("delta",4,"reason","Không được sửa trực tiếp")).statusCode());postDocument(admin,"RECEIPT",null,Map.of(chosen,4),true);assertEquals(5,num(db.one("SELECT QUANTITY FROM PRODUCT_DETAIL WHERE ID=:i","i",chosen),"QUANTITY"));
  assertEquals(400,admin.request("/api/admin/stock/"+chosen,"PUT",Map.of("delta",-6,"reason","Invalid")).statusCode());
  long singleVoucher=admin.save("/api/admin/vouchers","POST",voucher("RACE","2020-01-01T00:00:00","2099-01-01T00:00:00",1)).asLong();vouchers.add(singleVoucher);
  var voucherPool=Executors.newFixedThreadPool(2);try{
   var f1=voucherPool.submit(()->concurrent1.request("/api/bills","POST",bill(chosen,1,prefix+"RACE")));var f2=voucherPool.submit(()->concurrent2.request("/api/bills","POST",bill(other,1,prefix+"RACE")));
   var results=List.of(f1.get(),f2.get());assertEquals(1,results.stream().filter(r->r.statusCode()==200).count());assertEquals(1,results.stream().filter(r->r.statusCode()==400).count());
  }finally{voucherPool.shutdownNow();}
  assertEquals(1,num(db.one("SELECT USED_COUNT FROM VOUCHER WHERE ID=:i","i",singleVoucher),"USED_COUNT"));
  // Public checkout requires the customer session, and derives customer identity server-side.
  var checkout=Map.of("name",prefix,"phone","0900000000","province","HCM","ward","Ward","address","Test","paymentMethod","COD","items",List.of(Map.of("productDetailId",other,"quantity",1)));
  anonymous.csrf();assertEquals(400,anonymous.request("/api/checkout","POST",checkout).statusCode());
  var publicOrder=client.save("/api/checkout","POST",checkout);assertEquals(customer,publicOrder.get("customerId").asLong());
  admin.save("/api/admin/orders/"+publicOrder.get("id").asLong()+"/payment-failed","POST",null);
  assertEquals(400,admin.request("/api/admin/orders/"+publicOrder.get("id").asLong()+"/payment-failed","POST",null).statusCode());
  var formProduct=productBody(List.of(variant(null,black,"S","-FORM-S",0)));formProduct.put("code",prefix+"FORM");formProduct.remove("brandId");formProduct.remove("materialId");formProduct.remove("thumbnailUrl");formProduct.put("brandName",prefix+" Brand");formProduct.put("materialName",prefix+" Linen");
  var formResult=admin.productWithImages(formProduct,List.of(png.toByteArray()),List.of(Map.of("colorId",black,"order",0,"cover",true)));assertEquals(200,formResult.statusCode(),formResult.body());long formId=Long.parseLong(formResult.body());products.add(formId);
  var formSaved=db.one("SELECT p.STATUS,p.THUMBNAIL_URL,b.BRAND_NAME,m.MATERIAL_NAME FROM PRODUCT p JOIN BRAND b ON b.ID=p.BRAND_ID JOIN MATERIAL m ON m.ID=p.MATERIAL_ID WHERE p.ID=:i","i",formId);
  assertEquals(1,num(formSaved,"STATUS"));assertTrue(formSaved.get("THUMBNAIL_URL").toString().startsWith("/uploads/"));assertEquals(prefix+" Brand",formSaved.get("BRAND_NAME"));assertEquals(prefix+" Linen",formSaved.get("MATERIAL_NAME"));
  var invalidForm=new HashMap<String,Object>(formProduct);invalidForm.put("name",prefix+"BADFORM");invalidForm.put("code",prefix+"BADFORM");invalidForm.put("brandName",prefix+" RollbackBrand");invalidForm.put("materialName",prefix+" RollbackMaterial");invalidForm.put("variants",List.of(variant(null,black,"S","-BADFORM-S",0)));
  assertEquals(400,admin.productWithImages(invalidForm,List.of(png.toByteArray(),"invalid image".getBytes()),List.of(Map.of("order",0,"cover",true),Map.of("order",1,"cover",false))).statusCode());
  assertEquals(0,num(db.one("SELECT COUNT(*) N FROM PRODUCT WHERE PRODUCT_NAME=:c","c",prefix+"BADFORM"),"N"));assertEquals(0,num(db.one("SELECT COUNT(*) N FROM BRAND WHERE BRAND_NAME=:n","n",prefix+" RollbackBrand"),"N"));assertEquals(0,num(db.one("SELECT COUNT(*) N FROM MATERIAL WHERE MATERIAL_NAME=:n","n",prefix+" RollbackMaterial"),"N"));
  var current=admin.get("/api/admin/products/"+product);String originalSku=current.get("variants").get(0).get("SKU").asText();List<Map<String,Object>> edits=new ArrayList<>();
  for(var v:current.get("variants")){var edit=variant(v.get("ID").asLong(),v.get("COLOR_ID").asLong(),v.get("SIZE_NAME").asText(),"FORGED",v.get("QUANTITY").asInt());edits.add(edit);}
  var update=productBody(edits);update.put("code","FORGED-CODE");update.put("thumbnailUrl",current.get("THUMBNAIL_URL").asText());admin.save("/api/admin/products/"+product,"PUT",update);
  var preserved=admin.get("/api/admin/products/"+product);assertEquals(generatedCode,preserved.get("PRODUCT_CODE").asText());assertEquals(originalSku,preserved.get("variants").get(0).get("SKU").asText());
  var noSku=variant(null,black,"XS","unused",0);noSku.remove("sku");var autoA=productBody(List.of(noSku));autoA.remove("code");autoA.put("name",prefix+"AUTO-A");var autoB=new HashMap<>(autoA);autoB.put("name",prefix+"AUTO-B");
  var codePool=Executors.newFixedThreadPool(2);try{
   var a=codePool.submit(()->concurrent1.request("/api/admin/products","POST",autoA));var b=codePool.submit(()->concurrent2.request("/api/admin/products","POST",autoB));var ar=a.get();var br=b.get();assertEquals(200,ar.statusCode(),ar.body());assertEquals(200,br.statusCode(),br.body());
   String ac=admin.get("/api/admin/products/"+Long.parseLong(ar.body())).get("PRODUCT_CODE").asText(),bc=admin.get("/api/admin/products/"+Long.parseLong(br.body())).get("PRODUCT_CODE").asText();assertNotEquals(ac,bc);
  }finally{codePool.shutdownNow();}
  // Drafts do not change stock; staff can prepare them but only ADMIN can post.
  admin.save("/api/admin/managers/"+staffId+"/access","PUT",Map.of("roleId",staffRole,"status",1));staff.login(prefix+"STAFF",password);
  long beforeDraft=num(db.one("SELECT QUANTITY FROM PRODUCT_DETAIL WHERE ID=:i","i",chosen),"QUANTITY");
  long receiptDoc=document(staff,"RECEIPT",null,Map.of(chosen,2),true);assertEquals(beforeDraft,num(db.one("SELECT QUANTITY FROM PRODUCT_DETAIL WHERE ID=:i","i",chosen),"QUANTITY"));
  assertEquals(403,staff.request("/api/admin/inventory/documents/"+receiptDoc+"/approve","POST",null).statusCode());admin.save("/api/admin/inventory/documents/"+receiptDoc+"/approve","POST",null);
  assertEquals(250001.00,((Number)db.one("SELECT SUM(QUANTITY*UNIT_COST) V FROM INVENTORY_DOCUMENT_LINE WHERE DOCUMENT_ID=:i","i",receiptDoc).get("V")).doubleValue());assertEquals(beforeDraft+2,num(db.one("SELECT QUANTITY FROM PRODUCT_DETAIL WHERE ID=:i","i",chosen),"QUANTITY"));assertEquals(400,admin.request("/api/admin/inventory/documents/"+receiptDoc+"/approve","POST",null).statusCode());
  long badIssue=document(admin,"ISSUE",null,Map.of(chosen,100000),true);assertEquals(400,admin.request("/api/admin/inventory/documents/"+badIssue+"/approve","POST",null).statusCode());admin.save("/api/admin/inventory/documents/"+badIssue+"/cancel","POST",Map.of("reason",prefix+" - cancelled"));
  long issueBefore=num(db.one("SELECT QUANTITY FROM PRODUCT_DETAIL WHERE ID=:i","i",chosen),"QUANTITY"),validIssue=document(admin,"ISSUE",null,Map.of(chosen,2),true);
  admin.save("/api/admin/inventory/documents/"+validIssue,"PUT",Map.of("type","ISSUE","reason",prefix+" - corrected draft","lines",List.of(Map.of("variantId",chosen,"quantity",1,"restock",true))));admin.save("/api/admin/inventory/documents/"+validIssue+"/approve","POST",null);assertEquals(issueBefore-1,num(db.one("SELECT QUANTITY FROM PRODUCT_DETAIL WHERE ID=:i","i",chosen),"QUANTITY"));
  long reverseIssue=admin.save("/api/admin/inventory/documents/"+validIssue+"/reverse","POST",Map.of("reason",prefix+" - undo issue")).asLong();admin.save("/api/admin/inventory/documents/"+reverseIssue+"/approve","POST",null);assertEquals(issueBefore,num(db.one("SELECT QUANTITY FROM PRODUCT_DETAIL WHERE ID=:i","i",chosen),"QUANTITY"));
  long staleCount=document(admin,"COUNT",null,Map.of(chosen,10),true);postDocument(admin,"RECEIPT",null,Map.of(chosen,1),true);assertEquals(400,admin.request("/api/admin/inventory/documents/"+staleCount+"/approve","POST",null).statusCode());admin.save("/api/admin/inventory/documents/"+staleCount+"/cancel","POST",Map.of("reason",prefix+" - recount"));
  int countTarget=(int)num(db.one("SELECT QUANTITY FROM PRODUCT_DETAIL WHERE ID=:i","i",chosen),"QUANTITY")+1;long countDoc=document(admin,"COUNT",null,Map.of(chosen,countTarget),true);admin.save("/api/admin/inventory/documents/"+countDoc+"/approve","POST",null);
  long reverse=admin.save("/api/admin/inventory/documents/"+countDoc+"/reverse","POST",Map.of("reason",prefix+" - correction")).asLong();admin.save("/api/admin/inventory/documents/"+reverse+"/approve","POST",null);assertEquals(countTarget-1,num(db.one("SELECT QUANTITY FROM PRODUCT_DETAIL WHERE ID=:i","i",chosen),"QUANTITY"));assertEquals(400,admin.request("/api/admin/inventory/documents/"+countDoc+"/reverse","POST",Map.of("reason","duplicate")).statusCode());
  // Returns cap quantities against the original order and distinguish sellable/damaged goods.
  long completedOrder=admin.save("/api/bills","POST",bill(other,2,"")).get("data").get("id").asLong();
  for(int status:List.of(1,2,3))admin.save("/api/admin/orders/"+completedOrder+"/status","PUT",Map.of("status",status));
  String linkedCode=db.one("SELECT BILL_CODE FROM BILL WHERE ID=:i","i",completedOrder).get("BILL_CODE").toString();
  long stockBeforeLink=num(db.one("SELECT QUANTITY FROM PRODUCT_DETAIL WHERE ID=:i","i",other),"QUANTITY");
  var linkedIssues=admin.get("/api/admin/inventory/order-issues?q="+linkedCode);assertEquals(1,linkedIssues.size());assertEquals(completedOrder,linkedIssues.get(0).get("BILL_ID").asLong());
  var linkedDocument=admin.get("/api/admin/inventory/documents/"+linkedIssues.get(0).get("DOCUMENT_ID").asLong());assertEquals(linkedCode,linkedDocument.get("document").get("BILL_CODE").asText());assertFalse(linkedDocument.get("document").get("RECEIVER_NAME").asText().isBlank());
  assertFalse(admin.get("/api/admin/inventory/documents?q="+linkedCode).get("items").isEmpty());
  assertEquals(400,admin.request("/api/admin/inventory/documents","POST",Map.of("type","ISSUE","billId",completedOrder,"reason",prefix+" duplicate sale issue","lines",List.of(Map.of("variantId",other,"quantity",1,"restock",true)))).statusCode());
  assertEquals(stockBeforeLink,num(db.one("SELECT QUANTITY FROM PRODUCT_DETAIL WHERE ID=:i","i",other),"QUANTITY"));
  double originalCost=((Number)db.one("SELECT COST_UNIT FROM BILL_DETAIL WHERE BILL_ID=:b AND PRODUCT_DETAIL_ID=:v","b",completedOrder,"v",other).get("COST_UNIT")).doubleValue();
  long pooledQty=num(db.one("SELECT SUM(QUANTITY) Q FROM PRODUCT_DETAIL WHERE PRODUCT_ID=:p AND DELETE_FLAG=0","p",product),"Q");
  long variedReceipt=admin.save("/api/admin/inventory/documents","POST",Map.of("type","RECEIPT","reason",prefix+" varied cost","lines",List.of(Map.of("variantId",other,"quantity",2,"unitCost",300000,"restock",true)))).asLong();
  admin.save("/api/admin/inventory/documents/"+variedReceipt+"/approve","POST",null);
  double averageAfter=((Number)db.one("SELECT AVERAGE_COST FROM PRODUCT WHERE ID=:p","p",product).get("AVERAGE_COST")).doubleValue();
  assertEquals((pooledQty*originalCost+600000)/(pooledQty+2),averageAfter,0.000001);
  assertEquals(originalCost,((Number)db.one("SELECT COST_UNIT FROM BILL_DETAIL WHERE BILL_ID=:b AND PRODUCT_DETAIL_ID=:v","b",completedOrder,"v",other).get("COST_UNIT")).doubleValue());
  long beforeReturn=num(db.one("SELECT QUANTITY FROM PRODUCT_DETAIL WHERE ID=:i","i",other),"QUANTITY");postDocument(admin,"RETURN",completedOrder,Map.of(other,1),true);
  assertEquals(beforeReturn+1,num(db.one("SELECT QUANTITY FROM PRODUCT_DETAIL WHERE ID=:i","i",other),"QUANTITY"));assertEquals("PARTIAL",db.one("SELECT RETURN_STATUS FROM BILL WHERE ID=:i","i",completedOrder).get("RETURN_STATUS"));
  for(String period:List.of("DAY","MONTH","QUARTER","YEAR")){
   var report=admin.get("/api/admin/reports?period="+period+"&productId="+product);assertEquals(2,report.get("totals").get("SOLD").asInt());assertEquals(1,report.get("totals").get("RETURNED").asInt());assertEquals(199000,report.get("totals").get("NET_AFTER_RETURN").asInt());assertEquals(30000,report.get("totals").get("SHIPPING").asInt());assertFalse(report.get("series").isEmpty());assertFalse(report.get("variants").isEmpty());
  }
  var productRows=admin.get("/api/admin/products?q="+generatedCode);
  var productRow=productRows.get("items").get(0);assertEquals(product,productRow.get("ID").asLong());assertEquals(199000,productRow.get("TOTAL_REVENUE").asInt());
  double expectedValue=((Number)db.one("SELECT ROUND(SUM(d.QUANTITY)*p.AVERAGE_COST,2) V FROM PRODUCT p JOIN PRODUCT_DETAIL d ON d.PRODUCT_ID=p.ID WHERE p.ID=:p AND d.DELETE_FLAG=0 GROUP BY p.AVERAGE_COST","p",product).get("V")).doubleValue();
  assertEquals(expectedValue,productRow.get("INVENTORY_VALUE").asDouble(),0.001);assertEquals(expectedValue,productRows.get("totals").get("INVENTORY_VALUE").asDouble(),0.001);
  assertEquals(199000,productRows.get("totals").get("TOTAL_REVENUE").asInt());
  var yesterday=java.time.LocalDate.now().minusDays(1);var past=admin.get("/api/admin/products?q="+generatedCode+"&from="+yesterday+"&to="+yesterday);
  assertEquals(0,past.get("totals").get("TOTAL_REVENUE").asInt());assertEquals(expectedValue,past.get("totals").get("INVENTORY_VALUE").asDouble(),0.001);
  var emptyPage=admin.get("/api/admin/products?q="+generatedCode+"&page=2");assertTrue(emptyPage.get("items").isEmpty());assertEquals(199000,emptyPage.get("totals").get("TOTAL_REVENUE").asInt());
  var monthReport=admin.get("/api/admin/reports?period=DAY&month="+java.time.LocalDate.now().getMonthValue()+"&productId="+product);assertEquals(java.time.YearMonth.now().lengthOfMonth(),monthReport.get("series").size());
  String day=java.time.LocalDate.now().toString();var daily=admin.get("/api/admin/reports?period=DAY&from="+day+"&to="+day+"&productId="+product);assertEquals(1,daily.get("series").size());assertEquals(199000,daily.get("totals").get("NET_AFTER_RETURN").asInt());
  int otherQuarter=java.time.LocalDate.now().getMonthValue()>3?1:4;assertEquals(0,admin.get("/api/admin/reports?quarter="+otherQuarter+"&productId="+product).get("totals").get("NET_AFTER_RETURN").asInt());assertEquals(0,admin.get("/api/admin/reports?year="+(java.time.LocalDate.now().getYear()-1)+"&productId="+product).get("totals").get("NET_AFTER_RETURN").asInt());
  long excess=document(admin,"RETURN",completedOrder,Map.of(other,2),true);assertEquals(400,admin.request("/api/admin/inventory/documents/"+excess+"/approve","POST",null).statusCode());
  long returnA=document(admin,"RETURN",completedOrder,Map.of(other,1),false),returnB=document(admin,"RETURN",completedOrder,Map.of(other,1),false);var returnPool=Executors.newFixedThreadPool(2);try{var a=returnPool.submit(()->concurrent1.request("/api/admin/inventory/documents/"+returnA+"/approve","POST",null));var b=returnPool.submit(()->concurrent2.request("/api/admin/inventory/documents/"+returnB+"/approve","POST",null));var results=List.of(a.get(),b.get());assertEquals(1,results.stream().filter(r->r.statusCode()==200).count());assertEquals(1,results.stream().filter(r->r.statusCode()==400).count());}finally{returnPool.shutdownNow();}
  assertEquals(beforeReturn+1,num(db.one("SELECT QUANTITY FROM PRODUCT_DETAIL WHERE ID=:i","i",other),"QUANTITY"));assertEquals("FULL",db.one("SELECT RETURN_STATUS FROM BILL WHERE ID=:i","i",completedOrder).get("RETURN_STATUS"));
  assertEquals(0,admin.get("/api/admin/reports?productId="+product).get("totals").get("NET_AFTER_RETURN").asInt());
  long netVoucher=admin.save("/api/admin/vouchers","POST",voucher("NET","2020-01-01T00:00:00","2099-01-01T00:00:00",10)).asLong();vouchers.add(netVoucher);
  long netOrder=admin.save("/api/bills","POST",bill(other,2,prefix+"NET")).get("data").get("id").asLong();
  assertEquals(0,admin.get("/api/admin/products?q="+generatedCode).get("totals").get("TOTAL_REVENUE").asInt(),"Pending orders are not revenue");
  for(int state:List.of(1,2,3))admin.save("/api/admin/orders/"+netOrder+"/status","PUT",Map.of("status",state));
  assertEquals(358200,admin.get("/api/admin/products?q="+generatedCode).get("totals").get("TOTAL_REVENUE").asInt(),"Apply voucher once and exclude shipping");
  postDocument(admin,"RETURN",netOrder,Map.of(other,1),true);
  assertEquals(179100,admin.get("/api/admin/products?q="+generatedCode).get("totals").get("TOTAL_REVENUE").asInt(),"Return deducts the discounted amount actually paid");
  var stockValue=admin.get("/api/admin/stock?productId="+product);double stockTotal=stockValue.get("totals").get("INVENTORY_VALUE").asDouble();
  assertEquals(admin.get("/api/admin/products?q="+generatedCode).get("totals").get("INVENTORY_VALUE").asDouble(),stockTotal,0.001);
  assertEquals(stockTotal,stockValue.get("items").get(0).get("PRODUCT_INVENTORY_VALUE").asDouble(),0.001);
  assertTrue(admin.get("/api/admin/stock?productId="+product+"&minValue="+(stockTotal+1)).get("items").isEmpty());
  assertTrue(admin.get("/api/admin/stock?productId="+product+"&maxValue="+(stockTotal-1)).get("items").isEmpty());
  var stockPage=admin.get("/api/admin/stock?productId="+product+"&page=2");assertTrue(stockPage.get("items").isEmpty());assertEquals(stockTotal,stockPage.get("totals").get("INVENTORY_VALUE").asDouble(),0.001);
  assertEquals(stockValue.get("globalTotals"),admin.get("/api/admin/stock?q=NO_MATCH_VALUE_TEST").get("globalTotals"));
  long legacyColor=num(db.one("SELECT COLOR_ID FROM PRODUCT_DETAIL WHERE ID=:i","i",chosen),"COLOR_ID");
  long legacyProduct=admin.save("/api/admin/products","POST",productBody(List.of(variant(null,legacyColor,"M","OLD-COST",0)))).asLong();
  long legacyVariant=admin.get("/api/admin/products/"+legacyProduct).get("variants").get(0).get("ID").asLong();
  db.update("UPDATE PRODUCT_DETAIL SET QUANTITY=90 WHERE ID=:i","i",legacyVariant);
  long legacyReceipt=0;
  for(var entry:Map.of(10,1222,100,100000).entrySet()){long doc=admin.save("/api/admin/inventory/documents","POST",Map.of("type","RECEIPT","reason",prefix+" historic receipt","lines",List.of(Map.of("variantId",legacyVariant,"quantity",entry.getKey(),"unitCost",entry.getValue(),"restock",true)))).asLong();admin.save("/api/admin/inventory/documents/"+doc+"/approve","POST",null);if(entry.getKey()==10)legacyReceipt=doc;}
  long oldLine=num(db.one("SELECT ID FROM INVENTORY_DOCUMENT_LINE WHERE DOCUMENT_ID=:d","d",legacyReceipt),"ID");db.update("UPDATE INVENTORY_DOCUMENT_LINE SET UNIT_COST=NULL,COST_UNIT=NULL WHERE ID=:i","i",oldLine);
  var opening=admin.get("/api/admin/opening-cost/"+legacyProduct);assertEquals(90,opening.get("openingQuantity").asInt());assertEquals(200,opening.get("quantity").asInt());
  var initialize=Map.of("openingUnitCost",50000,"reason",prefix+" documented opening cost","receipts",List.of(Map.of("lineId",oldLine,"unitCost",1222)));
  assertEquals(403,staff.request("/api/admin/opening-cost/"+legacyProduct,"POST",initialize).statusCode());
  assertEquals(400,admin.request("/api/admin/opening-cost/"+legacyProduct,"POST",Map.of("reason",prefix,"receipts",List.of())).statusCode());
  admin.save("/api/admin/opening-cost/"+legacyProduct,"POST",initialize);
  assertEquals(200,num(db.one("SELECT QUANTITY FROM PRODUCT_DETAIL WHERE ID=:i","i",legacyVariant),"QUANTITY"));
  var legacyCode=admin.get("/api/admin/products/"+legacyProduct).get("PRODUCT_CODE").asText();assertEquals(14512220,admin.get("/api/admin/products?q="+legacyCode).get("totals").get("INVENTORY_VALUE").asLong());
  assertEquals(400,admin.request("/api/admin/opening-cost/"+legacyProduct,"POST",initialize).statusCode());
  var balance=admin.get("/api/admin/inventory/summary?q="+URLEncoder.encode(originalSku,java.nio.charset.StandardCharsets.UTF_8)).get("items").get(0);
  long expectedClosing=balance.get("OPENING").asLong()+balance.get("RECEIPT").asLong()-balance.get("ISSUE").asLong()-balance.get("SALE").asLong()+balance.get("CANCELLED").asLong()+balance.get("RETURNED").asLong()+balance.get("ADJUSTMENT").asLong();assertEquals(expectedClosing,balance.get("CLOSING").asLong());
  for(var s:admin.get("/api/admin/stock").get("items"))assertEquals(s.get("QUANTITY").asLong()+s.get("RESERVED").asLong(),s.get("PHYSICAL").asLong());assertEquals(200,admin.request("/api/stats/revenue-by-month","GET",null).statusCode());assertEquals(200,admin.request("/api/stats/top-products","GET",null).statusCode());
  // One-use recovery links never appear in the API, and password change revokes sessions.
  String recoveryName=prefix+"RECOVERY";long recoveryManager=admin.save("/api/admin/managers","POST",Map.of("username",recoveryName,"password",password,"fullName",prefix+" Recovery","email",recoveryName+"@example.test","roleId",adminRole)).asLong();
  var oldSession=new Browser();oldSession.login(recoveryName,password);var recovery=new Browser();recovery.csrf();
  String expired=recoveryToken(recovery,recoveryName);db.update("UPDATE MANAGER_PASSWORD_RESET SET EXPIRES_AT=LOCALTIMESTAMP-INTERVAL '1' MINUTE WHERE MANAGER_ID=:i","i",recoveryManager);
  assertEquals(400,recovery.request("/api/auth/reset-password","POST",Map.of("token",expired,"password",password+"Z")).statusCode());
  String token=recoveryToken(recovery,recoveryName),newPassword=UUID.randomUUID()+"newA1!";
  recovery.save("/api/auth/reset-password","POST",Map.of("token",token,"password",newPassword));assertEquals(400,recovery.request("/api/auth/reset-password","POST",Map.of("token",token,"password",newPassword)).statusCode());assertEquals(401,oldSession.request("/api/admin/me","GET",null).statusCode());
  var newSession=new Browser();assertEquals(1,newSession.login(recoveryName,newPassword));assertTrue(encoder.matches(newPassword,db.one("SELECT PASSWORD_HASH FROM MANAGER WHERE ID=:i","i",recoveryManager).get("PASSWORD_HASH").toString()));
  String lockedToken=recoveryToken(recovery,recoveryName);admin.save("/api/admin/managers/"+recoveryManager+"/access","PUT",Map.of("roleId",adminRole,"status",0));assertEquals(400,recovery.request("/api/auth/reset-password","POST",Map.of("token",lockedToken,"password",newPassword)).statusCode());
  assertTrue(admin.get("/api/admin/audit").get("total").asInt()>0);
  var dash=admin.get("/api/admin/dashboard");assertEquals(num(db.one("SELECT COUNT(*) N FROM BILL WHERE DELETE_FLAG=0"),"N"),java.util.stream.StreamSupport.stream(dash.get("statuses").spliterator(),false).mapToLong(n->n.get("TOTAL").asLong()).sum());
  var limited=new Browser();
  for(int i=0;i<5;i++)assertEquals(0,limited.login(prefix+"NONEXISTENT","invalid-password"));
  var blocked=limited.client.send(HttpRequest.newBuilder(URI.create("http://localhost:"+port+"/login")).header("Content-Type","application/x-www-form-urlencoded").header("X-XSRF-TOKEN",limited.token).POST(HttpRequest.BodyPublishers.ofString("username="+prefix+"NONEXISTENT&password=invalid-password")).build(),HttpResponse.BodyHandlers.ofString());
  assertEquals(302,blocked.statusCode());var redirect=URI.create(blocked.headers().firstValue("location").orElseThrow());String destination=redirect.getRawPath()+"?"+redirect.getRawQuery();assertTrue(destination.startsWith("/login?lockedUntil="),destination);assertTrue(Long.parseLong(blocked.headers().firstValue("Retry-After").orElseThrow())>0);
  var loginPage=limited.request(destination,"GET",null);assertEquals(200,loginPage.statusCode());assertFalse(loginPage.body().contains("Whitelabel"));
 }
 @AfterEach void cleanup(){
  // IDs and prefix belong solely to this test. Never touch pre-existing shop data.
  db.update("DELETE FROM INVENTORY_HISTORY WHERE BILL_ID IN (SELECT ID FROM BILL WHERE BILL_CODE LIKE 'HD%' AND CUSTOMER_ID IN (SELECT ID FROM CUSTOMER WHERE USER_ID IN (SELECT ID FROM USERS WHERE USERNAME=:p))) OR MANAGER_ID IN (SELECT ID FROM MANAGER WHERE USERNAME LIKE :pfx)","p",prefix,"pfx",prefix+"%");
  String docs="SELECT ID FROM INVENTORY_DOCUMENT WHERE CREATED_BY IN (SELECT ID FROM MANAGER WHERE USERNAME LIKE :pfx) OR BILL_ID IN (SELECT ID FROM BILL WHERE CUSTOMER_ID IN (SELECT ID FROM CUSTOMER WHERE USER_ID IN (SELECT ID FROM USERS WHERE USERNAME=:p)))";
  db.update("DELETE FROM INVENTORY_DOCUMENT_LINE WHERE DOCUMENT_ID IN ("+docs+")","p",prefix,"pfx",prefix+"%");db.update("DELETE FROM INVENTORY_DOCUMENT WHERE ID IN ("+docs+")","p",prefix,"pfx",prefix+"%");
  for(String table:List.of("BILL_HISTORY","BILL_DETAIL","PAYMENT_HISTORY"))db.update("DELETE FROM "+table+" WHERE BILL_ID IN (SELECT ID FROM BILL WHERE CUSTOMER_ID IN (SELECT ID FROM CUSTOMER WHERE USER_ID IN (SELECT ID FROM USERS WHERE USERNAME=:p)))","p",prefix);
  db.update("DELETE FROM BILL WHERE CUSTOMER_ID IN (SELECT ID FROM CUSTOMER WHERE USER_ID IN (SELECT ID FROM USERS WHERE USERNAME=:p))","p",prefix);
  db.update("DELETE FROM ADDRESS WHERE CUSTOMER_ID IN (SELECT ID FROM CUSTOMER WHERE USER_ID IN (SELECT ID FROM USERS WHERE USERNAME=:p))","p",prefix);
  db.update("DELETE FROM CUSTOMER WHERE USER_ID IN (SELECT ID FROM USERS WHERE USERNAME=:p)","p",prefix);db.update("DELETE FROM USERS WHERE USERNAME=:p","p",prefix);
  var imageRows=db.rows("SELECT IMAGE_URL FROM PRODUCT_IMAGE WHERE PRODUCT_ID IN (SELECT ID FROM PRODUCT WHERE PRODUCT_NAME LIKE :p)","p",prefix+"%");
  for(var image:imageRows)try{String name=java.nio.file.Path.of(image.get("IMAGE_URL").toString()).getFileName().toString();if(name.matches("[0-9a-f-]{36}\\.(png|jpg|webp)"))java.nio.file.Files.deleteIfExists(java.nio.file.Path.of(System.getenv("SHOP_UPLOAD_DIR")).resolve(name));}catch(java.io.IOException ignored){}
  db.update("DELETE FROM PRODUCT_IMAGE WHERE PRODUCT_ID IN (SELECT ID FROM PRODUCT WHERE PRODUCT_NAME LIKE :p)","p",prefix+"%");
  db.update("DELETE FROM INVENTORY_COST_OPENING WHERE PRODUCT_ID IN (SELECT ID FROM PRODUCT WHERE PRODUCT_NAME LIKE :p)","p",prefix+"%");
  db.update("DELETE FROM PRODUCT_DETAIL WHERE PRODUCT_ID IN (SELECT ID FROM PRODUCT WHERE PRODUCT_NAME LIKE :p)","p",prefix+"%");db.update("DELETE FROM PRODUCT WHERE PRODUCT_NAME LIKE :p","p",prefix+"%");
  db.update("DELETE FROM COLOR WHERE COLOR_NAME LIKE :p","p",prefix+"%");
  db.update("DELETE FROM BRAND WHERE BRAND_NAME LIKE :p","p",prefix+"%");db.update("DELETE FROM MATERIAL WHERE MATERIAL_NAME LIKE :p","p",prefix+"%");
  db.update("UPDATE CATEGORY SET PARENT_ID=NULL WHERE CATEGORY_NAME LIKE :p","p",prefix+"%");db.update("DELETE FROM CATEGORY WHERE CATEGORY_NAME LIKE :p","p",prefix+"%");
  db.update("DELETE FROM VOUCHER WHERE CODE LIKE :p","p",prefix.toUpperCase(Locale.ROOT)+"%");
  db.update("DELETE FROM MANAGER_PASSWORD_RESET WHERE MANAGER_ID IN (SELECT ID FROM MANAGER WHERE USERNAME LIKE :p)","p",prefix+"%");
  for(var file:recoveryFiles)try{java.nio.file.Files.deleteIfExists(file);}catch(java.io.IOException ignored){}
  db.update("DELETE FROM ADMIN_AUDIT WHERE MANAGER_ID IN (SELECT ID FROM MANAGER WHERE USERNAME LIKE :p)","p",prefix+"%");db.update("DELETE FROM MANAGER WHERE USERNAME LIKE :p","p",prefix+"%");
 }
}
