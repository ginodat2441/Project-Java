package com.example.clothingshop.admin;
import com.example.clothingshop.bill.*;
import com.example.clothingshop.common.BusinessException;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.*;
import static com.example.clothingshop.admin.AdminRepository.number;

@RestController
public class StoreController {
    final AdminRepository db; final PasswordEncoder encoder;final BillService bills;
    public StoreController(AdminRepository db,PasswordEncoder encoder,BillService bills){this.db=db;this.encoder=encoder;this.bills=bills;}
    public record Login(@NotBlank @Email String email,@NotBlank @Size(max=72) String password){}
    public record Register(@NotBlank @Email String email,@NotBlank @Size(min=12,max=72) String password,@NotBlank @Size(max=150) String name,@NotBlank @Size(max=20) String phone){}
    public record Checkout(@NotBlank @Size(max=150) String name,@NotBlank @Size(max=20) String phone,
        @NotBlank @Size(max=100) String province,@NotBlank @Size(max=100) String ward,@NotBlank @Size(max=300) String address,
        @Pattern(regexp="COD|BANK") @NotNull String paymentMethod,@Size(max=50) String voucherCode,@Size(max=500) String note,
        @NotEmpty @Size(max=100) @Valid List<BillRequest.Item> items){}
    private final Map<String,long[]> attempts=new java.util.concurrent.ConcurrentHashMap<>();
    @PostMapping("/api/customer/login") public Object login(@Valid @RequestBody Login r,HttpSession session,jakarta.servlet.http.HttpServletRequest request){
        String ip=request.getRemoteAddr();long now=System.currentTimeMillis();
        var count=attempts.compute(ip,(k,v)->v==null||v[1]<now?new long[]{1,now+600000}:new long[]{v[0]+1,v[1]});
        if(count[0]>5)throw new BusinessException("Thử lại sau 10 phút");
        var found=db.rows("SELECT c.ID,u.PASSWORD_HASH,u.AUTH_VERSION FROM USERS u JOIN CUSTOMER c ON c.USER_ID=u.ID WHERE LOWER(u.EMAIL)=:e AND u.STATUS=1 AND u.DELETE_FLAG=0 AND c.DELETE_FLAG=0","e",r.email().toLowerCase(Locale.ROOT));
        String hash=found.isEmpty()?"$2a$12$KIXxMcj5pXmZKL4yhu.9C.5DU8HiK7jZ1FkC3PzJK0jD9x8mIhEha":found.get(0).get("PASSWORD_HASH").toString();
        if(!encoder.matches(r.password(),hash)||found.isEmpty())throw new BusinessException("Email hoặc mật khẩu không đúng");
        request.changeSessionId();session.setAttribute("customer",number(found.get(0),"ID"));session.setAttribute("customer-access-version",number(found.get(0),"AUTH_VERSION"));attempts.remove(ip);return Map.of("success",true);
    }
    @Transactional @PostMapping("/api/customer/register") public Object register(@Valid @RequestBody Register r,HttpSession session,jakarta.servlet.http.HttpServletRequest request){
        long role=number(db.one("SELECT ID FROM ROLES WHERE ROLE_CODE='CUSTOMER' AND DELETE_FLAG=0"),"ID");
        long user=db.insert("INSERT INTO USERS(ROLE_ID,USERNAME,EMAIL,PASSWORD_HASH) VALUES(:r,:u,:e,:p)","r",role,"u","customer-"+UUID.randomUUID(),"e",r.email().toLowerCase(Locale.ROOT),"p",encoder.encode(r.password()));
        long id=db.insert("INSERT INTO CUSTOMER(USER_ID,FULL_NAME,PHONE) VALUES(:u,:n,:p)","u",user,"n",r.name(),"p",r.phone());
        request.changeSessionId();session.setAttribute("customer",id);session.setAttribute("customer-access-version",0L);return Map.of("success",true);
    }
    long customer(HttpSession session){Object id=session.getAttribute("customer");if(id==null)throw new BusinessException("Đăng nhập tài khoản khách hàng trước khi đặt hàng");
        var found=db.rows("SELECT c.ID,u.AUTH_VERSION FROM CUSTOMER c JOIN USERS u ON u.ID=c.USER_ID WHERE c.ID=:i AND c.DELETE_FLAG=0 AND u.DELETE_FLAG=0 AND u.STATUS=1","i",id);
        if(found.isEmpty()||!Long.valueOf(number(found.get(0),"AUTH_VERSION")).equals(session.getAttribute("customer-access-version"))){session.removeAttribute("customer");throw new BusinessException("Tài khoản hoặc quyền truy cập đã thay đổi. Hãy đăng nhập lại.");}
        return number(found.get(0),"ID");}
    @GetMapping("/api/customer/me") public Object me(HttpSession session){long id=customer(session);return db.one("SELECT c.FULL_NAME,c.PHONE,u.EMAIL FROM CUSTOMER c JOIN USERS u ON u.ID=c.USER_ID WHERE c.ID=:i","i",id);}
    @PostMapping("/api/customer/logout") public void logout(HttpSession s){s.removeAttribute("customer");}
    @Transactional @PostMapping("/api/checkout") public Object checkout(@Valid @RequestBody Checkout r,HttpSession session){
        long customer=customer(session);
        long address=db.insert("INSERT INTO ADDRESS(CUSTOMER_ID,RECEIVER_NAME,RECEIVER_PHONE,PROVINCE,WARD,DETAIL_ADDRESS) VALUES(:c,:n,:p,:pr,:w,:d)","c",customer,"n",r.name(),"p",r.phone(),"pr",r.province(),"w",r.ward(),"d",r.address());
        return bills.create(new BillRequest(customer,address,r.paymentMethod(),null,r.note(),r.voucherCode(),r.items()));
    }
    @GetMapping("/api/catalog") public Object catalog(@RequestParam(defaultValue="") String q,@RequestParam(required=false) Long categoryId,@RequestParam(defaultValue="1") int page){
        return db.page("SELECT p.ID,p.PRODUCT_NAME,p.THUMBNAIL_URL,c.CATEGORY_NAME,(SELECT MIN(PRICE) FROM PRODUCT_DETAIL WHERE PRODUCT_ID=p.ID AND STATUS=1 AND DELETE_FLAG=0) PRICE FROM PRODUCT p LEFT JOIN CATEGORY c ON c.ID=p.CATEGORY_ID WHERE p.STATUS=1 AND p.DELETE_FLAG=0 "+CategoryVisibility.SQL+" AND LOWER(p.PRODUCT_NAME) LIKE :q AND (:c IS NULL OR p.CATEGORY_ID=:c) ORDER BY p.ID DESC",page,20,"q","%"+q.toLowerCase(Locale.ROOT)+"%","c",categoryId);
    }
    @GetMapping("/api/catalog/categories") public Object categories(){return db.rows("SELECT ID,CATEGORY_NAME,PARENT_ID FROM CATEGORY WHERE STATUS=1 AND ID NOT IN (SELECT ID FROM CATEGORY START WITH STATUS=0 CONNECT BY NOCYCLE PRIOR ID=PARENT_ID) ORDER BY CATEGORY_NAME");}
    @GetMapping("/api/catalog/{id}") public Object product(@PathVariable long id){return Map.of("product",db.one("SELECT p.ID,p.PRODUCT_NAME,p.THUMBNAIL_URL,DBMS_LOB.SUBSTR(p.DESCRIPTION,4000,1) DESCRIPTION FROM PRODUCT p WHERE p.ID=:i AND p.STATUS=1 AND p.DELETE_FLAG=0"+CategoryVisibility.SQL,"i",id),
        "variants",db.rows("SELECT pd.ID,pd.COLOR_ID,c.COLOR_NAME,pd.SIZE_NAME,pd.PRICE,pd.QUANTITY FROM PRODUCT_DETAIL pd JOIN COLOR c ON c.ID=pd.COLOR_ID WHERE pd.PRODUCT_ID=:i AND pd.STATUS=1 AND pd.DELETE_FLAG=0 ORDER BY c.COLOR_NAME,pd.SIZE_NAME","i",id),
        "images",db.rows("SELECT IMAGE_URL,COLOR_ID,SORT_ORDER FROM PRODUCT_IMAGE WHERE PRODUCT_ID=:i ORDER BY SORT_ORDER,ID","i",id));}
    public record Quote(@Size(max=50) String code,@NotEmpty @Size(max=100) @Valid List<BillRequest.Item> items){}
    @PostMapping("/api/catalog/quote") public Object quote(@Valid @RequestBody Quote r){
        BigDecimal subtotal=BigDecimal.ZERO;
        for(var i:r.items()) {var p=db.one("SELECT pd.PRICE FROM PRODUCT_DETAIL pd JOIN PRODUCT p ON p.ID=pd.PRODUCT_ID WHERE pd.ID=:i AND pd.STATUS=1 AND pd.DELETE_FLAG=0 AND p.STATUS=1 AND p.DELETE_FLAG=0 AND pd.QUANTITY>=:q"+CategoryVisibility.SQL,"i",i.productDetailId(),"q",i.quantity());subtotal=subtotal.add(((BigDecimal)p.get("PRICE")).multiply(BigDecimal.valueOf(i.quantity())));}
        BigDecimal discount=bills.discount(r.code(),subtotal,false);return Map.of("subtotal",subtotal,"discount",discount,"shippingFee",30000,"total",subtotal.subtract(discount).add(BigDecimal.valueOf(30000)));
    }
}
