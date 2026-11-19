package com.example.clothingshop.service.order;

import static com.example.clothingshop.repository.admin.AdminRepository.number;
import com.example.clothingshop.repository.admin.AdminRepository;
import com.example.clothingshop.common.BusinessException;
import jakarta.validation.constraints.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service
public class CustomerOrderRequestService {
    private final AdminRepository db;
    private final BillService bills;
    public CustomerOrderRequestService(AdminRepository db,BillService bills){this.db=db;this.bills=bills;}
    public record Review(@NotBlank @Pattern(regexp="APPROVED|REJECTED") String decision,
                         @NotBlank @Size(min=10,max=500) String note) {}
    public Object pending(){return db.page("""
        SELECT r.*,b.BILL_CODE,b.RECEIVER_NAME FROM BILL_CUSTOMER_REQUEST r
        JOIN BILL b ON b.ID=r.BILL_ID WHERE r.STATUS='PENDING' AND b.DELETE_FLAG=0
        ORDER BY r.CREATED_AT,r.ID
        """,1,100);}
    public List<Map<String,Object>> forBill(long bill){return db.rows("SELECT * FROM BILL_CUSTOMER_REQUEST WHERE BILL_ID=:id ORDER BY CREATED_AT DESC,ID DESC","id",bill);}

    @Transactional
    public void review(long id,Review review,long actor){
        if(db.rows("SELECT m.ID FROM MANAGER m JOIN MANAGER_ROLE r ON r.ID=m.MANAGER_ROLE_ID WHERE m.ID=:actor AND m.STATUS=1 AND m.DELETE_FLAG=0 AND r.DELETE_FLAG=0 AND r.ROLE_CODE='ADMIN'","actor",actor).isEmpty())
            throw new org.springframework.security.access.AccessDeniedException("Chỉ quản trị viên được duyệt yêu cầu hủy / trả hàng.");
        if(review.note().strip().length()<10)throw new BusinessException("Nhập nội dung phản hồi từ 10 đến 500 ký tự.");
        // Always lock BILL before request, matching customer creation and warehouse operations.
        var pointer=db.one("SELECT BILL_ID FROM BILL_CUSTOMER_REQUEST WHERE ID=:id","id",id);
        long bill=number(pointer,"BILL_ID");
        var order=db.one("SELECT * FROM BILL WHERE ID=:id AND DELETE_FLAG=0 FOR UPDATE","id",bill);
        var request=db.one("SELECT * FROM BILL_CUSTOMER_REQUEST WHERE ID=:id FOR UPDATE","id",id);
        if(!"PENDING".equals(request.get("STATUS")))throw new BusinessException("Yêu cầu đã được xử lý. Vui lòng tải lại.");
        if("APPROVED".equals(review.decision())){
            if("CANCEL".equals(request.get("REQUEST_TYPE"))){
                if(number(order,"STATUS")>1||number(order,"DISPATCHED")!=0)throw new BusinessException("Đơn đã xuất giao; không thể duyệt hủy.");
                bills.approveCustomerCancellation(bill,actor,"Duyệt yêu cầu hủy #"+id+". Nội dung phản hồi lưu tại yêu cầu.");
            }else{
                if(number(order,"STATUS")!=3||!"NONE".equals(Objects.toString(order.get("RETURN_STATUS"),"NONE")))throw new BusinessException("Đơn không còn đủ điều kiện tiếp nhận yêu cầu trả hàng.");
                // Deadline is checked when the customer submits, not when staff review the queue.
                db.update("UPDATE BILL SET RETURN_STATUS='REQUESTED' WHERE ID=:id","id",bill);
                db.update("INSERT INTO BILL_HISTORY(BILL_ID,OLD_STATUS,NEW_STATUS,NOTE,MANAGER_ID) VALUES(:id,3,3,:note,:actor)","id",bill,"note","Duyệt yêu cầu trả hàng #"+id+". Nội dung phản hồi lưu tại yêu cầu.","actor",actor);
            }
        }
        db.update("UPDATE BILL_CUSTOMER_REQUEST SET STATUS=:status,RESPONSE_NOTE=:note,REVIEWED_AT=LOCALTIMESTAMP,REVIEWED_BY=:actor WHERE ID=:id AND STATUS='PENDING'",
            "status",review.decision(),"note",review.note().strip(),"actor",actor,"id",id);
        db.audit(actor,"ORDER_REQUEST_"+review.decision(),"BILL_CUSTOMER_REQUEST",id);
    }
}
