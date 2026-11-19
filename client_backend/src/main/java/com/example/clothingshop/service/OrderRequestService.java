package com.example.clothingshop.service;

import com.example.clothingshop.common.*;
import jakarta.validation.constraints.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderRequestService {
    private final SqlHelper sql;
    public OrderRequestService(SqlHelper sql) { this.sql=sql; }
    public record Request(@NotNull @Positive Long billId,
        @NotBlank @Pattern(regexp="CANCEL|RETURN") String type,
        @NotBlank @Size(min=10,max=500) String reason) {}

    private static long number(Map<String,Object> row,String key) {
        return row.get(key)==null?0:((Number)row.get(key)).longValue();
    }
    private static final String ELIGIBILITY="""
        SELECT b.ID BILL_ID, b.STATUS, b.DISPATCHED, b.RETURN_STATUS,
               b.COMPLETED_AT + NUMTODSINTERVAL(15,'DAY') RETURN_DEADLINE,
               CASE WHEN b.STATUS=3 AND b.COMPLETED_AT<=LOCALTIMESTAMP
                    AND b.COMPLETED_AT>=LOCALTIMESTAMP-NUMTODSINTERVAL(15,'DAY')
                    AND NVL(b.RETURN_STATUS,'NONE')='NONE' THEN 1 ELSE 0 END CAN_RETURN
          FROM BILL b WHERE b.CUSTOMER_ID=:customer AND b.DELETE_FLAG=0
        """;

    @Transactional(readOnly=true)
    public List<Map<String,Object>> overview(long customer) {
        var params=Map.of("customer",customer);
        var requests=sql.maps("SELECT * FROM BILL_CUSTOMER_REQUEST WHERE CUSTOMER_ID=:customer ORDER BY CREATED_AT DESC,ID DESC",params);
        var refunds=sql.maps("SELECT r.BILL_ID,r.AMOUNT,r.STATUS,r.PAID_AT FROM BILL_REFUND r JOIN BILL b ON b.ID=r.BILL_ID WHERE b.CUSTOMER_ID=:customer AND b.DELETE_FLAG=0 ORDER BY r.ID DESC",params);
        return sql.maps(ELIGIBILITY+" ORDER BY b.ID DESC",params).stream().map(b->{
            long id=number(b,"BILL_ID");
            var own=requests.stream().filter(r->number(r,"BILL_ID")==id).toList();
            boolean pending=own.stream().anyMatch(r->"PENDING".equals(r.get("STATUS")));
            boolean returned=own.stream().anyMatch(r->"RETURN".equals(r.get("REQUEST_TYPE"))&&"APPROVED".equals(r.get("STATUS")));
            Map<String,Object> result=new HashMap<>();
            result.put("billId",id);result.put("canCancel",!pending&&number(b,"STATUS")<=1&&number(b,"DISPATCHED")==0);
            result.put("canReturn",!pending&&!returned&&number(b,"CAN_RETURN")==1);
            result.put("returnDeadline",b.get("RETURN_DEADLINE"));result.put("returnStatus",b.get("RETURN_STATUS"));
            result.put("requests",own);result.put("refunds",refunds.stream().filter(r->number(r,"BILL_ID")==id).toList());return result;
        }).toList();
    }

    @Transactional
    public long create(long customer,Request request) {
        String reason=request.reason().strip();
        if(reason.length()<10||reason.length()>500||reason.codePoints().anyMatch(c->Character.isISOControl(c)&&c!='\n'))
            throw new BusinessException("Vui lòng nhập lý do rõ ràng từ 10 đến 500 ký tự.");
        var params=Map.<String,Object>of("customer",customer,"bill",request.billId());
        sql.scalar("SELECT ID FROM BILL WHERE ID=:bill AND CUSTOMER_ID=:customer AND DELETE_FLAG=0 FOR UPDATE",params,Long.class)
            .orElseThrow(()->new NotFoundException("Không tìm thấy đơn hàng của bạn."));
        var eligibility=overview(customer).stream().filter(r->((Number)r.get("billId")).longValue()==request.billId()).findFirst().orElseThrow();
        String flag="CANCEL".equals(request.type())?"canCancel":"canReturn";
        if(!Boolean.TRUE.equals(eligibility.get(flag))) throw new BusinessException("Đơn không còn đủ điều kiện hoặc đã có yêu cầu chờ duyệt. Vui lòng làm mới đơn hàng.");
        long id=sql.insert("""
            INSERT INTO BILL_CUSTOMER_REQUEST(BILL_ID,CUSTOMER_ID,REQUEST_TYPE,REASON)
            VALUES(:bill,:customer,:type,:reason)
            """,Map.of("bill",request.billId(),"customer",customer,"type",request.type(),"reason",reason));
        sql.update("INSERT INTO BILL_HISTORY(BILL_ID,OLD_STATUS,NEW_STATUS,NOTE) SELECT ID,STATUS,STATUS,:note FROM BILL WHERE ID=:bill",Map.of("bill",request.billId(),"note",("CANCEL".equals(request.type())?"Khách gửi yêu cầu hủy #":"Khách gửi yêu cầu trả hàng #")+id+". Nội dung lưu tại yêu cầu."));
        return id;
    }
}
