package com.example.clothingshop;

import static org.junit.jupiter.api.Assertions.*;
import com.example.clothingshop.repositories.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/** Read-only integration check against the developer's Oracle database. */
@SpringBootTest
@EnabledIfEnvironmentVariable(named="RUN_SEARCH_ORACLE_TEST",matches="true")
class ProductSearchOracleTest {
    @Autowired ProductRepository products;
    @Test void vietnameseSearchAndLiteralWildcardsExecuteOnOracle() {
        var accented=products.searchViews("Áo sơ mi",null,null,null,"newest",1,15);
        var plain=products.searchViews("ao so mi",null,null,null,"newest",1,15);
        assertTrue(plain.total()>0,"Expected the local clothing catalog to contain shirts");
        assertEquals(accented.total(),plain.total());
        assertEquals(accented.items(),plain.items());
        assertEquals(0,products.searchViews("%_",null,null,null,"newest",1,15).total());
        products.searchViews("SP",null,null,null,"price_asc",1,5);
    }
}
