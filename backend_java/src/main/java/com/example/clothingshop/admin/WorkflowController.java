package com.example.clothingshop.admin;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;
import jakarta.validation.Valid;
@RestController
public class WorkflowController {
 private final InventoryService inventory;private final AdminService admin;private final RefundService refunds;private final AdminRepository db;
 public WorkflowController(InventoryService i,AdminService a,RefundService r,AdminRepository db){inventory=i;admin=a;refunds=r;this.db=db;}
 @PostMapping("/api/admin/orders/{id}/dispatch") public long dispatch(@PathVariable long id,Authentication a){return inventory.prepareDispatch(id,admin.actor(a.getName()));}
 @GetMapping("/api/admin/orders/{id}/refunds") public Object refunds(@PathVariable long id){return db.rows("SELECT r.ID,r.RETURN_DOCUMENT_ID,h.DOC_CODE,r.AMOUNT,r.STATUS,r.REFERENCE_CODE,r.REASON,r.CREATED_AT,r.PAID_AT,m.USERNAME FROM BILL_REFUND r JOIN INVENTORY_DOCUMENT h ON h.ID=r.RETURN_DOCUMENT_ID JOIN MANAGER m ON m.ID=r.CREATED_BY WHERE r.BILL_ID=:b ORDER BY r.ID","b",id);}
 @PostMapping("/api/admin/orders/{id}/refunds") public long refund(@PathVariable long id,@Valid @RequestBody RefundService.Request r,Authentication a){return refunds.save(id,null,r,admin.actor(a.getName()));}
 @PutMapping("/api/admin/orders/{id}/refunds/{refund}") public long update(@PathVariable long id,@PathVariable long refund,@Valid @RequestBody RefundService.Request r,Authentication a){return refunds.save(id,refund,r,admin.actor(a.getName()));}
}
