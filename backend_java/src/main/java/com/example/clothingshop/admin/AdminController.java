package com.example.clothingshop.admin;
import static com.example.clothingshop.admin.AdminRequests.*;
import com.example.clothingshop.bill.BillService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.csrf.CsrfToken;
import java.util.*;

@RestController
public class AdminController {
    final AdminRepository db; final AdminService service; final BillService bills;final ReportService reports;final ProductMetricsService metrics;final AdminSearchService search;
    public AdminController(AdminRepository db,AdminService service,BillService bills,ReportService reports,ProductMetricsService metrics,AdminSearchService search){this.db=db;this.service=service;this.bills=bills;this.reports=reports;this.metrics=metrics;this.search=search;}
    @GetMapping("/api/csrf") public Map<String,String> csrf(CsrfToken token){return Map.of("token",token.getToken(),"headerName",token.getHeaderName());}
    @GetMapping("/api/admin/me") public Map<String,Object> me(Authentication a){return Map.of("username",a.getName(),"roles",a.getAuthorities());}
    @GetMapping("/api/admin/lookups") public Map<String,Object> lookups(){return Map.of(
        "brands",db.rows("SELECT ID,BRAND_NAME NAME FROM BRAND WHERE DELETE_FLAG=0 ORDER BY BRAND_NAME"),
        "materials",db.rows("SELECT ID,MATERIAL_NAME NAME FROM MATERIAL WHERE DELETE_FLAG=0 ORDER BY MATERIAL_NAME"),
        "colors",db.rows("SELECT ID,COLOR_NAME NAME FROM COLOR WHERE DELETE_FLAG=0 ORDER BY COLOR_NAME"),
        "categories",db.rows("SELECT ID,CATEGORY_NAME NAME,PARENT_ID,STATUS FROM CATEGORY ORDER BY CATEGORY_NAME"));}
    @GetMapping("/api/admin/categories") public Object categories(@RequestParam(defaultValue="") String q){return search.categories(q);}

    @PostMapping("/api/admin/colors") public Object createColor(@Valid @RequestBody Color r,Authentication a){return service.color(r,service.actor(a.getName()));}
    @GetMapping("/api/admin/categories/{id}/impact") public Object impact(@PathVariable long id){return search.categoryImpact(id);}
    @GetMapping("/api/admin/vouchers/{id}/orders") public Object voucherOrders(@PathVariable long id){return search.voucherOrders(id);}
    @PostMapping("/api/admin/categories") public long createCategory(@Valid @RequestBody Category r,Authentication a){return service.category(null,r,service.actor(a.getName()));}
    @PutMapping("/api/admin/categories/{id}") public long editCategory(@PathVariable long id,@Valid @RequestBody Category r,Authentication a){return service.category(id,r,service.actor(a.getName()));}
    @GetMapping("/api/admin/roles") public Object roles(){return db.rows("SELECT ID,ROLE_CODE,ROLE_NAME FROM MANAGER_ROLE WHERE DELETE_FLAG=0 AND ROLE_CODE IN ('ADMIN','STAFF')");}
    @GetMapping("/api/admin/managers") public Object managers(@RequestParam Map<String,String> filters){return search.managers(filters);}

    @PostMapping("/api/admin/managers") public long createManager(@Valid @RequestBody Manager r,Authentication a){return service.manager(r,service.actor(a.getName()));}
    @PutMapping("/api/admin/managers/{id}/access") public void access(@PathVariable long id,@Valid @RequestBody Access r,Authentication a){service.access(id,r,service.actor(a.getName()));}
    @GetMapping("/api/admin/customers") public Object customers(@RequestParam(defaultValue="") String q,@RequestParam(defaultValue="1") int page){return db.page("SELECT c.ID,c.FULL_NAME,c.PHONE,u.EMAIL,u.STATUS FROM CUSTOMER c JOIN USERS u ON u.ID=c.USER_ID WHERE c.DELETE_FLAG=0 AND (LOWER(c.FULL_NAME) LIKE :q OR LOWER(u.EMAIL) LIKE :q OR c.PHONE LIKE :q) ORDER BY c.ID DESC",page,20,"q","%"+q.toLowerCase(Locale.ROOT)+"%");}
    @GetMapping("/api/admin/customers/{id}") public Object customer(@PathVariable long id){return Map.of("customer",db.one("SELECT c.ID,c.FULL_NAME,c.PHONE,u.EMAIL,u.STATUS FROM CUSTOMER c JOIN USERS u ON u.ID=c.USER_ID WHERE c.ID=:i","i",id),"orders",db.rows("SELECT ID,BILL_CODE,TOTAL_AMOUNT,STATUS,CREATED_AT FROM BILL WHERE CUSTOMER_ID=:i AND DELETE_FLAG=0 ORDER BY ID DESC","i",id));}
    @PutMapping("/api/admin/customers/{id}/status") public void customerStatus(@PathVariable long id,@RequestParam int status,Authentication a){service.customer(id,status,service.actor(a.getName()));}
    @GetMapping("/api/admin/products") public Object products(@RequestParam(defaultValue="") String q,@RequestParam(required=false) Long categoryId,@RequestParam(required=false) Integer status,@RequestParam(defaultValue="1") int page,@RequestParam(required=false) java.time.LocalDate from,@RequestParam(required=false) java.time.LocalDate to){
        return metrics.list(q,categoryId,status,page,from,to);
    }
    @GetMapping("/api/admin/products/{id}") public Object product(@PathVariable long id){return service.product(id);}
    @PostMapping("/api/admin/products") public long createProduct(@Valid @RequestBody Product r,Authentication a){return service.product(null,r,service.actor(a.getName()));}
    @PutMapping("/api/admin/products/{id}") public long editProduct(@PathVariable long id,@Valid @RequestBody Product r,Authentication a){return service.product(id,r,service.actor(a.getName()));}
    @GetMapping("/api/admin/vouchers") public Object vouchers(@RequestParam Map<String,String> filters){return search.vouchers(filters);}

    @PostMapping("/api/admin/vouchers") public long createVoucher(@Valid @RequestBody Voucher r,Authentication a){return service.voucher(null,r,service.actor(a.getName()));}
    @PutMapping("/api/admin/vouchers/{id}") public long editVoucher(@PathVariable long id,@Valid @RequestBody Voucher r,Authentication a){return service.voucher(id,r,service.actor(a.getName()));}
    @GetMapping("/api/admin/orders") public Object orders(@RequestParam Map<String,String> filters){return search.orders(filters);}

    @GetMapping("/api/admin/orders/{id}") public Object order(@PathVariable long id){return Map.of("order",db.one("SELECT ID,BILL_CODE,CUSTOMER_ID,RECEIVER_NAME,RECEIVER_PHONE,SHIPPING_ADDRESS,SUB_TOTAL,SHIPPING_FEE,DISCOUNT_AMOUNT,TOTAL_AMOUNT,PAYMENT_METHOD,PAYMENT_STATUS,STATUS,VOUCHER_CODE,CARRIER,TRACKING_CODE,NOTE,CREATED_AT,COMPLETED_AT,RETURN_STATUS,RETURN_VALUE,RETURNED_AT FROM BILL WHERE ID=:i AND DELETE_FLAG=0","i",id),"lines",bills.lines(id),"history",db.rows("SELECT h.ID,h.OLD_STATUS,h.NEW_STATUS,h.NOTE,m.USERNAME,h.CREATED_AT FROM BILL_HISTORY h LEFT JOIN MANAGER m ON m.ID=h.MANAGER_ID WHERE BILL_ID=:i ORDER BY h.ID","i",id),"inventoryDocuments",db.rows("SELECT ID,DOC_CODE,DOC_TYPE,STATUS,POSTED_AT FROM INVENTORY_DOCUMENT WHERE BILL_ID=:i ORDER BY ID","i",id));}
    @PutMapping("/api/admin/orders/{id}/status") public Object orderStatus(@PathVariable long id,@Valid @RequestBody OrderStatus r,Authentication a){return bills.changeStatus(id,r.status(),service.actor(a.getName()),r.note());}
    @PutMapping("/api/admin/orders/{id}/shipping") public void shipping(@PathVariable long id,@Valid @RequestBody Shipping r,Authentication a){bills.shipping(id,r,service.actor(a.getName()));}
    @PostMapping("/api/admin/orders/{id}/payment-failed") public Object paymentFailed(@PathVariable long id,Authentication a){return bills.changeStatus(id,4,service.actor(a.getName()),"Thanh toán thất bại");}
    @GetMapping("/api/admin/stock") public Object stock(@RequestParam(defaultValue="2147483647") int threshold,@RequestParam(defaultValue="1") int page,@RequestParam(defaultValue="") String q,@RequestParam(required=false) Long productId,@RequestParam(required=false) java.math.BigDecimal minValue,@RequestParam(required=false) java.math.BigDecimal maxValue,@RequestParam(required=false) String stockState){return metrics.stock(q,productId,threshold,page,minValue,maxValue,stockState);}
    @PutMapping("/api/admin/stock/{id}") public void adjustStock(@PathVariable long id,@Valid @RequestBody Stock r,Authentication a){service.stock(id,r,service.actor(a.getName()));}
    @GetMapping("/api/admin/inventory-history") public Object history(@RequestParam Map<String,String> filters){return search.history(filters);}

    @GetMapping("/api/admin/audit") public Object audit(@RequestParam Map<String,String> filters){return search.audit(filters);}

    @SuppressWarnings("unchecked")
    @GetMapping("/api/admin/dashboard") public Object dashboard(@RequestParam(defaultValue="5") int threshold,@RequestParam(required=false) Integer year){
        int y=year==null?java.time.LocalDate.now().getYear():year;
        var report=reports.report("MONTH",y,null,null,null,null,null,"",1);
        var revenue=((List<Map<String,Object>>)report.get("series")).stream().map(r->Map.of("MONTH",r.get("PERIOD"),"REVENUE",r.get("NET_AFTER_RETURN"))).toList();
        var top=reports.topProducts(y,10,0);
        return Map.of(
        "period",y,"totals",report.get("totals"),"inventory",metrics.stock("",null,Integer.MAX_VALUE,1,null,null).get("globalTotals"),
        "alerts",db.one("SELECT (SELECT COUNT(*) FROM PRODUCT_DETAIL d JOIN PRODUCT p ON p.ID=d.PRODUCT_ID WHERE d.DELETE_FLAG=0 AND p.DELETE_FLAG=0 AND d.QUANTITY<=:t) LOW_STOCK,(SELECT COUNT(*) FROM BILL WHERE STATUS=0 AND DELETE_FLAG=0) WAIT_CONFIRM,(SELECT COUNT(*) FROM BILL WHERE STATUS=1 AND DELETE_FLAG=0) WAIT_DISPATCH,(SELECT COUNT(*) FROM BILL WHERE STATUS=6 AND DELETE_FLAG=0) FAILED,(SELECT COUNT(*) FROM INVENTORY_DOCUMENT WHERE STATUS='DRAFT') WAIT_DOCUMENT FROM DUAL","t",Math.max(0,threshold)),
        "revenue",revenue,
        "statuses",db.rows("SELECT STATUS,COUNT(*) TOTAL FROM BILL WHERE DELETE_FLAG=0 GROUP BY STATUS ORDER BY STATUS"),
        "topProducts",top,
        "lowStock",db.rows("SELECT pd.SKU,p.PRODUCT_NAME,pd.SIZE_NAME,pd.QUANTITY FROM PRODUCT_DETAIL pd JOIN PRODUCT p ON p.ID=pd.PRODUCT_ID WHERE pd.DELETE_FLAG=0 AND p.DELETE_FLAG=0 AND pd.QUANTITY<=:t ORDER BY pd.QUANTITY FETCH FIRST 50 ROWS ONLY","t",Math.max(0,threshold)));}
}
