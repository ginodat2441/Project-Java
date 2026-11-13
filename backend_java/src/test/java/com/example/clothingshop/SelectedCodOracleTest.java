package com.example.clothingshop;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import com.example.clothingshop.repository.admin.AdminRepository;
import com.example.clothingshop.dto.order.*;
import com.example.clothingshop.dto.store.StoreRequests;
import com.example.clothingshop.service.store.CustomerCartService;
import com.example.clothingshop.service.shipping.ShippingQuoteService;
import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.transaction.annotation.Transactional;

/** Real Oracle transaction, rolled back by Spring. Only carrier quoting is mocked. */
@SpringBootTest
@Transactional
@EnabledIfEnvironmentVariable(named="RUN_COD_ORACLE_TEST",matches="true")
class SelectedCodOracleTest {
  @Autowired AdminRepository db;
  @Autowired CustomerCartService carts;
  @MockBean ShippingQuoteService shipping;
  @Test void selectedItemsBecomePendingCodOrderAndOtherItemsRemainInCart() {
    long role=AdminRepository.number(db.one("SELECT ID FROM ROLES WHERE ROLE_CODE='CUSTOMER' AND DELETE_FLAG=0"),"ID");
    String key="cod-test-"+UUID.randomUUID();
    long user=db.insert("INSERT INTO USERS(ROLE_ID,USERNAME,EMAIL,PASSWORD_HASH) VALUES(:r,:u,:e,:p)","r",role,"u",key,"e",key+"@example.test","p","test-only-unused-hash");
    long customer=db.insert("INSERT INTO CUSTOMER(USER_ID,FULL_NAME,PHONE) VALUES(:u,'COD transaction test','0901234567')","u",user);
    var session=new MockHttpSession();session.setAttribute("customer",customer);session.setAttribute("customer-access-version",0L);
    var variants=db.rows("SELECT pd.ID,pd.PRICE FROM PRODUCT_DETAIL pd JOIN PRODUCT p ON p.ID=pd.PRODUCT_ID WHERE pd.STATUS=1 AND pd.DELETE_FLAG=0 AND p.STATUS=1 AND p.DELETE_FLAG=0 AND pd.QUANTITY>=2 ORDER BY pd.ID FETCH FIRST 2 ROWS ONLY");
    assertEquals(2,variants.size());long first=AdminRepository.number(variants.get(0),"ID"),second=AdminRepository.number(variants.get(1),"ID");
    carts.add(session,first,1);carts.add(session,second,1);
    when(shipping.quote(anyString(),anyString(),anyString(),eq("COD"),anyList(),any(),any()))
      .thenReturn(List.of(new ShippingQuoteService.Offer("TEST","Test quote",BigDecimal.valueOf(30000),"test")));
    var request=new StoreRequests.Checkout("COD test","0901234567","Hà Nội","Phường kiểm thử","Địa chỉ kiểm thử","COD",null,null,List.of(new BillRequest.Item(first,1)),"TEST",BigDecimal.valueOf(30000));
    var order=(BillView)carts.checkout(session,request);
    assertEquals("COD",order.paymentMethod());assertEquals(0,order.paymentStatus());assertEquals(0,order.status());
    assertEquals(0,order.subTotal().compareTo((BigDecimal)variants.get(0).get("PRICE")));
    assertEquals(0,order.totalAmount().compareTo(order.subTotal().add(BigDecimal.valueOf(30000))));
    var remaining=carts.get(session);assertEquals(1,remaining.items().size());assertEquals(second,remaining.items().get(0).productDetailId());
    assertEquals(1,db.rows("SELECT ID FROM BILL_DETAIL WHERE BILL_ID=:b","b",order.id()).size());
    assertTrue(db.rows("SELECT ID FROM PAYMENT_HISTORY WHERE BILL_ID=:b AND STATUS=1","b",order.id()).isEmpty());
  }
}
