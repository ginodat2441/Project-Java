package com.example.clothingshop;

import static org.junit.jupiter.api.Assertions.*;
import com.example.clothingshop.common.SqlHelper;
import com.example.clothingshop.dto.request.BillRequest;
import com.example.clothingshop.service.BillService;
import com.example.clothingshop.repositories.ProductRepository;
import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
@EnabledIfEnvironmentVariable(named="RUN_PURCHASE_ORACLE_TEST",matches="true")
class PurchaseOracleTest {
    @Autowired SqlHelper sql;
    @Autowired BillService service;
    @Autowired ProductRepository products;
    @Test void directOrderPersistsAdminCompatibleBillAndStockReservationWithoutCartMutation() {
        var addr=sql.maps("SELECT ID,CUSTOMER_ID FROM ADDRESS WHERE DELETE_FLAG=0 ORDER BY ID FETCH FIRST 1 ROWS ONLY",Map.of()).get(0);
        long address=((Number)addr.get("ID")).longValue(),customer=((Number)addr.get("CUSTOMER_ID")).longValue();
        var all=products.searchViews(null,null,null,null,"price_asc",1,100).items();
        BigDecimal previous=null;
        for(var product:all)if(product.minPrice()!=null){if(previous!=null)assertTrue(previous.compareTo(product.minPrice())<=0);previous=product.minPrice();}
        var pd=all.stream().flatMap(p->products.findDetailViews(p.id()).stream()).filter(v->v.quantity()>0).findFirst().orElseThrow();
        var params=Map.<String,Object>of("cid",customer,"id",pd.id());
        long cartBefore=sql.scalar("SELECT COUNT(*) FROM CART_DETAIL cd JOIN CART c ON c.ID=cd.CART_ID WHERE c.CUSTOMER_ID=:cid",params,Long.class).orElseThrow();
        var stockBefore=sql.scalar("SELECT RESERVED_QUANTITY FROM PRODUCT_DETAIL WHERE ID=:id",params,Integer.class).orElseThrow();
        var items=List.of(new BillRequest.Item(pd.id(),1));
        var quote=service.quote(items);
        var request = new BillRequest(address, "COD", "Rollback purchase integration test", items);
        var key = UUID.randomUUID().toString();
        var order = service.create(customer, request, key);
        assertEquals(order.id(), service.create(customer, request, key).id());
        assertThrows(com.example.clothingshop.common.BusinessException.class,
                () -> service.updateManagedStatus(order.id(), 1, "Không được bỏ qua phiếu xuất", 1L, true));
        assertEquals(0,quote.get("totalAmount").compareTo(order.totalAmount()));
        assertEquals(0,order.status());assertEquals(0,order.paymentStatus());
        var billParams=Map.<String,Object>of("bid",order.id());
        assertEquals(1L,sql.scalar("SELECT COUNT(*) FROM BILL_DETAIL WHERE BILL_ID=:bid AND QUANTITY=1",billParams,Long.class).orElseThrow());
        assertEquals(1L,sql.scalar("SELECT COUNT(*) FROM BILL WHERE ID=:bid AND DELETE_FLAG=0 AND PAYMENT_METHOD='COD' AND STATUS=0 AND PAYMENT_STATUS=0",billParams,Long.class).orElseThrow());
        assertEquals(stockBefore+1,sql.scalar("SELECT RESERVED_QUANTITY FROM PRODUCT_DETAIL WHERE ID=:id",params,Integer.class).orElseThrow());
        assertEquals(cartBefore,sql.scalar("SELECT COUNT(*) FROM CART_DETAIL cd JOIN CART c ON c.ID=cd.CART_ID WHERE c.CUSTOMER_ID=:cid",params,Long.class).orElseThrow());
        assertEquals(4, service.cancel(customer, order.id()).status());
        assertEquals(stockBefore, sql.scalar("SELECT RESERVED_QUANTITY FROM PRODUCT_DETAIL WHERE ID=:id",
                params, Integer.class).orElseThrow());
        assertEquals(1L, sql.scalar(
                "SELECT COUNT(*) FROM BILL_HISTORY WHERE BILL_ID=:bid AND NEW_STATUS=4 AND MANAGER_ID IS NULL",
                billParams, Long.class).orElseThrow());
    }
}
