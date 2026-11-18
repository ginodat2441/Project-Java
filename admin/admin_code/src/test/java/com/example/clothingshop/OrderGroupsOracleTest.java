package com.example.clothingshop;

import static org.junit.jupiter.api.Assertions.*;
import com.example.clothingshop.service.admin.AdminSearchService;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(classes=ClothingShopApplication.class)
@Transactional(readOnly=true)
@EnabledIfEnvironmentVariable(named="RUN_ORDER_GROUPS_ORACLE_TEST",matches="true")
class OrderGroupsOracleTest {
  @Autowired AdminSearchService search;
  @SuppressWarnings("unchecked")
  @Test void groupsPartitionRealOrdersAndRespectPaymentAndShippingStates() {
    var all=(Map<String,Object>)search.orders(Map.of("orderGroup","all"));
    long sum=0;
    for(String group:List.of("payment","processing","shipping","history")) {
      var result=(Map<String,Object>)search.orders(Map.of("orderGroup",group));
      sum+=((Number)result.get("total")).longValue();
      for(var row:(List<Map<String,Object>>)result.get("items")) {
        int state=((Number)row.get("STATUS")).intValue(),pay=((Number)row.get("PAYMENT_STATUS")).intValue();
        String method=row.get("PAYMENT_METHOD").toString();
        assertTrue(switch(group){
          case "payment" -> (state==0||state==1)&&!method.equals("COD")&&pay==0;
          case "processing" -> (state==0||state==1)&&(method.equals("COD")||pay!=0);
          case "shipping" -> state==2;
          default -> Set.of(3,4,5,6).contains(state);
        });
      }
    }
    assertEquals(((Number)all.get("total")).longValue(),sum);
  }
}
