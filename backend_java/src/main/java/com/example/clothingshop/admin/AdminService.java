package com.example.clothingshop.admin;

import com.example.clothingshop.common.*;
import static com.example.clothingshop.admin.AdminRepository.number;
import static com.example.clothingshop.admin.AdminRequests.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.util.*;

@Service
public class AdminService {
    final AdminRepository db; final PasswordEncoder encoder;
    public AdminService(AdminRepository db,PasswordEncoder encoder){this.db=db;this.encoder=encoder;}
    public long actor(String username){return number(db.one("SELECT ID FROM MANAGER WHERE USERNAME=:u AND STATUS=1 AND DELETE_FLAG=0","u",username),"ID");}
    @Transactional public Map<String,Object> color(Color r,long actor){
        String name=r.name().strip().replaceAll("\\s+"," ");
        if(name.isBlank())throw new BusinessException("Nhập tên màu");
        db.update("LOCK TABLE COLOR IN EXCLUSIVE MODE");
        var existing=db.rows("SELECT ID,COLOR_NAME NAME,HEX_CODE FROM COLOR WHERE LOWER(TRIM(COLOR_NAME))=LOWER(:n) AND DELETE_FLAG=0 ORDER BY ID","n",name);
        if(!existing.isEmpty())return existing.get(0);
        long id=db.insert("INSERT INTO COLOR(COLOR_NAME,HEX_CODE) VALUES(:n,:h)","n",name,"h",r.hexCode()==null?null:r.hexCode().toUpperCase(Locale.ROOT));
        db.audit(actor,"CREATE","COLOR",id);
        return db.one("SELECT ID,COLOR_NAME NAME,HEX_CODE FROM COLOR WHERE ID=:i","i",id);
    }
    @Transactional public long category(Long id,Category r,long actor){
        db.update("LOCK TABLE CATEGORY IN EXCLUSIVE MODE");
        if(id!=null)db.one("SELECT ID FROM CATEGORY WHERE ID=:i","i",id);
        Long parent=r.parentId(); Set<Long> visited=new HashSet<>();
        while(parent!=null){
            if(parent.equals(id)||!visited.add(parent))throw new BusinessException("Danh mục cha tạo vòng lặp");
            Object next=db.one("SELECT PARENT_ID FROM CATEGORY WHERE ID=:i","i",parent).get("PARENT_ID");
            parent=next==null?null:((Number)next).longValue();
        }
        if(id==null) id=db.insert("INSERT INTO CATEGORY(CATEGORY_NAME,PARENT_ID,STATUS) VALUES(:n,:p,:s)","n",r.name().trim(),"p",r.parentId(),"s",r.status());
        else db.update("UPDATE CATEGORY SET CATEGORY_NAME=:n,PARENT_ID=:p,STATUS=:s WHERE ID=:i","n",r.name().trim(),"p",r.parentId(),"s",r.status(),"i",id);
        db.audit(actor,"SAVE","CATEGORY",id);return id;
    }
    @Transactional public long manager(Manager r,long actor){
        validRole(r.roleId());
        long id=db.insert("INSERT INTO MANAGER(MANAGER_ROLE_ID,USERNAME,PASSWORD_HASH,FULL_NAME,EMAIL) VALUES(:r,:u,:p,:n,:e)","r",r.roleId(),"u",r.username(),"p",encoder.encode(r.password()),"n",r.fullName(),"e",r.email());
        db.audit(actor,"CREATE","MANAGER",id);return id;
    }
    String validRole(long id){String code=db.one("SELECT ROLE_CODE FROM MANAGER_ROLE WHERE ID=:i AND DELETE_FLAG=0","i",id).get("ROLE_CODE").toString();
        if(!Set.of("ADMIN","STAFF").contains(code))throw new BusinessException("Vai trò không được hỗ trợ");return code;}
    @Transactional public void access(long id,Access r,long actor){
        db.update("LOCK TABLE MANAGER IN EXCLUSIVE MODE");
        var old=db.one("SELECT ID,MANAGER_ROLE_ID,STATUS FROM MANAGER WHERE ID=:i AND DELETE_FLAG=0","i",id);
        String role=validRole(r.roleId());
        if(id==actor&&(r.status()==0||!role.equals("ADMIN")))throw new BusinessException("Không thể tự khóa hoặc thu hồi quyền của mình");
        long active=number(db.one("SELECT COUNT(*) N FROM MANAGER m JOIN MANAGER_ROLE r ON r.ID=m.MANAGER_ROLE_ID WHERE m.STATUS=1 AND m.DELETE_FLAG=0 AND r.ROLE_CODE='ADMIN' AND r.DELETE_FLAG=0 AND m.ID<>:i","i",id),"N");
        if(active==0&&(r.status()==0||!role.equals("ADMIN")))throw new BusinessException("Phải giữ ít nhất một quản trị viên hoạt động");
        db.update("UPDATE MANAGER SET AUTH_VERSION=AUTH_VERSION+1,MANAGER_ROLE_ID=:r,STATUS=:s WHERE ID=:i","r",r.roleId(),"s",r.status(),"i",id);
        db.auditChange(actor,"ACCESS","MANAGER",id,old,Map.of("MANAGER_ROLE_ID",r.roleId(),"STATUS",r.status()),"Đổi quyền hoặc trạng thái tài khoản");
    }
    @Transactional public void customer(long id,int status,long actor){
        if(status!=0&&status!=1)throw new BusinessException("Trạng thái không hợp lệ");
        if(db.update("UPDATE USERS SET AUTH_VERSION=AUTH_VERSION+1,STATUS=:s WHERE ID=(SELECT USER_ID FROM CUSTOMER WHERE ID=:i AND DELETE_FLAG=0)","s",status,"i",id)==0)throw new NotFoundException("Không tìm thấy khách hàng");
        db.audit(actor,"STATUS","CUSTOMER",id);
    }
    public Map<String,Object> product(long id){
        var product=db.one("SELECT ID,PRODUCT_CODE,PRODUCT_NAME,BRAND_ID,MATERIAL_ID,CATEGORY_ID,TARGET_GENDER,DBMS_LOB.SUBSTR(DESCRIPTION,4000,1) DESCRIPTION,THUMBNAIL_URL,STATUS FROM PRODUCT WHERE ID=:i AND DELETE_FLAG=0","i",id);
        product.put("variants",db.rows("SELECT ID,COLOR_ID,SIZE_NAME,SKU,PRICE,QUANTITY,STATUS FROM PRODUCT_DETAIL WHERE PRODUCT_ID=:i AND DELETE_FLAG=0 ORDER BY ID","i",id));
        product.put("images",db.rows("SELECT ID,IMAGE_URL,COLOR_ID,SORT_ORDER,IS_COVER FROM PRODUCT_IMAGE WHERE PRODUCT_ID=:i ORDER BY SORT_ORDER,ID","i",id));return product;
    }
    @Transactional public long product(Long id,Product r,long actor){
        // Consistent lookup lock order also applies when a product creates both values.
        long brandId=lookup("BRAND",r.brandId(),r.brandName(),actor);
        long materialId=lookup("MATERIAL",r.materialId(),r.materialName(),actor);
        String code=id==null?nextProductCode():db.one("SELECT PRODUCT_CODE FROM PRODUCT WHERE ID=:i AND DELETE_FLAG=0 FOR UPDATE","i",id).get("PRODUCT_CODE").toString();
        var pairs=new HashSet<String>();var ids=new HashSet<Long>();boolean selling=false;
        Map<Long,String> colorNames=new HashMap<>();
        for(var v:r.variants()){
            if(v.id()==null&&v.quantity()!=0)throw new BusinessException("Biến thể mới bắt đầu tồn 0. Lập phiếu nhập kho sau khi tạo sản phẩm.");
            if(!pairs.add(v.colorId()+":"+v.size().trim().toUpperCase(Locale.ROOT)))throw new BusinessException("Trùng cặp màu–size");
            if(v.id()!=null&&!ids.add(v.id()))throw new BusinessException("Biến thể bị lặp");
            selling|=v.status()==1;
            colorNames.put(v.colorId(),db.one("SELECT COLOR_NAME FROM COLOR WHERE ID=:i AND DELETE_FLAG=0","i",v.colorId()).get("COLOR_NAME").toString());
        }
        String cover=r.thumbnailUrl();
        if(cover!=null&&!cover.isBlank()&&!(cover.startsWith("/uploads/")||cover.startsWith("/store/assets/img/")||cover.startsWith("assets/img/")))throw new BusinessException("Chọn ảnh đã tải lên hoặc ảnh cửa hàng");
        if(cover!=null&&cover.startsWith("/uploads/")&&db.rows("SELECT ID FROM PRODUCT_IMAGE WHERE IMAGE_URL=:u AND PRODUCT_ID=:i","u",cover,"i",id).isEmpty())throw new BusinessException("Ảnh không thuộc sản phẩm");
        if(r.status()==1&&(!selling||cover==null||cover.isBlank()))throw new BusinessException("Sản phẩm đang bán cần ảnh đại diện và ít nhất một biến thể đang bán");
        if(r.categoryId()!=null)db.one("SELECT ID FROM CATEGORY WHERE ID=:i","i",r.categoryId());
        Object[] params={"c",code,"n",r.name().trim(),"b",brandId,"m",materialId,"cat",r.categoryId(),"g",r.gender(),"d",r.description(),"t",cover,"s",r.status(),"i",id};
        if(id==null)id=db.insert("INSERT INTO PRODUCT(PRODUCT_CODE,PRODUCT_NAME,BRAND_ID,MATERIAL_ID,CATEGORY_ID,TARGET_GENDER,DESCRIPTION,THUMBNAIL_URL,STATUS) VALUES(:c,:n,:b,:m,:cat,:g,:d,:t,:s)",params);
        else db.update("UPDATE PRODUCT SET PRODUCT_CODE=:c,PRODUCT_NAME=:n,BRAND_ID=:b,MATERIAL_ID=:m,CATEGORY_ID=:cat,TARGET_GENDER=:g,DESCRIPTION=:d,THUMBNAIL_URL=:t,STATUS=:s WHERE ID=:i",params);
        var existing=db.rows("SELECT ID FROM PRODUCT_DETAIL WHERE PRODUCT_ID=:i AND DELETE_FLAG=0","i",id);
        for(var e:existing)if(!ids.contains(number(e,"ID")))throw new BusinessException("Giữ biến thể cũ trong form; dùng trạng thái ngừng bán thay vì xóa");
        for(var v:r.variants()){
            String sku=v.id()==null?nextSku(code,colorNames.get(v.colorId()),v.colorId(),v.size()):null;
            Object[] vp={"p",id,"c",v.colorId(),"s",v.size().trim().toUpperCase(Locale.ROOT),"sku",sku,"price",v.price().setScale(0,java.math.RoundingMode.HALF_UP),"q",v.quantity(),"st",v.status(),"i",v.id()};
            if(v.id()==null){db.insert("INSERT INTO PRODUCT_DETAIL(PRODUCT_ID,COLOR_ID,SIZE_NAME,SKU,PRICE,QUANTITY,STATUS) VALUES(:p,:c,:s,:sku,:price,:q,:st)",vp);}
            else{
                var old=db.one("SELECT QUANTITY,COLOR_ID,SIZE_NAME FROM PRODUCT_DETAIL WHERE ID=:i AND PRODUCT_ID=:p FOR UPDATE","i",v.id(),"p",id);
                if(number(old,"COLOR_ID")!=v.colorId()||!old.get("SIZE_NAME").toString().equalsIgnoreCase(v.size().trim()))throw new BusinessException("Không đổi màu–size của biến thể cũ; tạo biến thể mới để giữ lịch sử");
                if(number(old,"QUANTITY")!=v.quantity())throw new BusinessException("Điều chỉnh tồn qua trang Tồn kho và nhập lý do");
                db.update("UPDATE PRODUCT_DETAIL SET PRICE=:price,STATUS=:st WHERE ID=:i AND PRODUCT_ID=:p",vp);
            }
        }
        db.audit(actor,"SAVE_PRICE_VARIANTS","PRODUCT",id);return id;
    }
    private String nextProductCode(){
        for(int i=0;i<1000;i++){
            String value=new java.math.BigDecimal(db.one("SELECT SHOP_PRODUCT_CODE_SEQ.NEXTVAL N FROM DUAL").get("N").toString()).toBigIntegerExact().toString();
            String code="SP"+"0".repeat(Math.max(0,3-value.length()))+value;
            if(db.rows("SELECT ID FROM PRODUCT WHERE PRODUCT_CODE=:c","c",code).isEmpty())return code;
        }
        throw new BusinessException("Không cấp được mã sản phẩm. Kiểm tra sequence mã sản phẩm.");
    }
    static String skuPart(String value){
        String part=java.text.Normalizer.normalize(value,java.text.Normalizer.Form.NFD).replaceAll("\\p{M}+","").replace('đ','d').replace('Đ','D').toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]+","-").replaceAll("^-|-$","");
        return part.length()>12?part.substring(0,12):part;
    }
    private String nextSku(String code,String color,long colorId,String size){
        String c=skuPart(color),s=skuPart(size);
        String base=code+"-"+(c.isBlank()?"C"+colorId:c)+"-"+(s.isBlank()?"SIZE":s);
        for(int i=1;i<10000;i++){
            String suffix=i==1?"":"-"+i;String sku=base.substring(0,Math.min(base.length(),80-suffix.length()))+suffix;
            if(db.rows("SELECT ID FROM PRODUCT_DETAIL WHERE UPPER(SKU)=:s","s",sku).isEmpty())return sku;
        }
        throw new BusinessException("Không thể tạo SKU duy nhất");
    }
    private long lookup(String kind,Long id,String name,long actor){
        String column=switch(kind){case "BRAND"->"BRAND_NAME";case "MATERIAL"->"MATERIAL_NAME";default->throw new IllegalArgumentException("Lookup không hợp lệ");};
        if(id!=null)return number(db.one("SELECT ID FROM "+kind+" WHERE ID=:i AND DELETE_FLAG=0","i",id),"ID");
        if(name==null||name.isBlank())throw new BusinessException("Nhập "+(kind.equals("BRAND")?"thương hiệu":"chất liệu"));
        String normalized=name.strip().replaceAll("\\s+"," ");
        db.update("LOCK TABLE "+kind+" IN EXCLUSIVE MODE");
        var existing=db.rows("SELECT ID FROM "+kind+" WHERE LOWER(TRIM("+column+"))=LOWER(:n) AND DELETE_FLAG=0 ORDER BY ID","n",normalized);
        if(!existing.isEmpty())return number(existing.get(0),"ID");
        long created=db.insert("INSERT INTO "+kind+"("+column+") VALUES(:n)","n",normalized);db.audit(actor,"CREATE",kind,created);return created;
    }
    public void movement(long variant,int delta,String reason,Long actor,Long bill){
        db.update("INSERT INTO INVENTORY_HISTORY(PRODUCT_DETAIL_ID,QUANTITY_CHANGE,REASON,MANAGER_ID,BILL_ID) VALUES(:p,:q,:r,:a,:b)","p",variant,"q",delta,"r",reason,"a",actor,"b",bill);
    }
    @Transactional public void stock(long id,Stock r,long actor){
        throw new BusinessException("Không sửa tồn trực tiếp. Lập phiếu nhập/xuất/kiểm kê và chờ ADMIN xác nhận.");
    }
    @Transactional public long voucher(Long id,Voucher r,long actor){
        if(!r.endsAt().isAfter(r.startsAt())||(r.type().equals("PERCENT")&&(r.value().compareTo(java.math.BigDecimal.valueOf(100))>0||r.maxDiscount().signum()==0)))throw new BusinessException("Thời gian hoặc mức giảm không hợp lệ");
        if(id!=null){var old=db.one("SELECT USED_COUNT FROM VOUCHER WHERE ID=:i FOR UPDATE","i",id);if(number(old,"USED_COUNT")>r.usageLimit())throw new BusinessException("Giới hạn thấp hơn lượt đã dùng");}
        Object[] p={"c",r.code().toUpperCase(Locale.ROOT),"t",r.type(),"v",r.value(),"min",r.minOrder(),"max",r.maxDiscount(),"start",java.sql.Timestamp.valueOf(r.startsAt()),"end",java.sql.Timestamp.valueOf(r.endsAt()),"lim",r.usageLimit(),"s",r.status(),"i",id};
        if(id==null)id=db.insert("INSERT INTO VOUCHER(CODE,DISCOUNT_TYPE,DISCOUNT_VALUE,MIN_ORDER,MAX_DISCOUNT,STARTS_AT,ENDS_AT,USAGE_LIMIT,STATUS) VALUES(:c,:t,:v,:min,:max,:start,:end,:lim,:s)",p);
        else db.update("UPDATE VOUCHER SET CODE=:c,DISCOUNT_TYPE=:t,DISCOUNT_VALUE=:v,MIN_ORDER=:min,MAX_DISCOUNT=:max,STARTS_AT=:start,ENDS_AT=:end,USAGE_LIMIT=:lim,STATUS=:s WHERE ID=:i",p);
        db.audit(actor,"SAVE","VOUCHER",id);return id;
    }
}
