package com.example.clothingshop.repositories;

import static org.junit.jupiter.api.Assertions.*;
import com.example.clothingshop.common.*;
import com.example.clothingshop.service.ProductService;
import java.util.*;
import org.junit.jupiter.api.Test;

class ProductCatalogTest {
    @Test void searchMatchesVietnameseWithoutAccentsAndBindsEveryWordLiterally() {
        var sql=new CaptureSql();
        new ProductRepository(sql).searchViews("  ÁO   sơ mi 100%_  ",null,null,null,"newest",1,15);
        assertTrue(sql.query.contains("TRANSLATE(LOWER("));
        assertTrue(sql.query.contains("p.PRODUCT_CODE"));
        assertTrue(sql.query.contains("b.BRAND_NAME"));
        assertTrue(sql.query.contains("ESCAPE '\\'"));
        assertEquals("%ao%",sql.params.get("searchTerm0"));
        assertEquals("%so%",sql.params.get("searchTerm1"));
        assertEquals("%mi%",sql.params.get("searchTerm2"));
        assertEquals("%100\\%\\_%",sql.params.get("searchTerm3"));
        assertFalse(sql.query.contains("100%_"));
    }
    @Test void keywordLimitAndVietnameseFoldAreConsistent() {
        assertEquals("ao dep dang moi",ProductSearchText.fold("ÁO ĐẸP DÁNG MỚI"));
        assertEquals("Áo sơ mi",ProductSearchText.normalize("  Áo  sơ mi  "));
        assertThrows(BusinessException.class,()->ProductSearchText.normalize("a".repeat(121)));
    }
    static class CaptureSql extends SqlHelper {
        String query; Map<String,Object> params; int page, size;
        CaptureSql() { super(null); }
        @Override public <T> PageResult<T> page(String q,Map<String,Object> p,int page,int size,Class<T> type) {
            this.query=q;this.params=p;this.page=page;this.size=size;
            return new PageResult<>(List.of(),page,size,0,0);
        }
    }
    @Test void priceSortIsGlobalBeforePagingAndUsesStableTieBreak() {
        var sql=new CaptureSql();var repo=new ProductRepository(sql);
        repo.searchViews(null,null,null,null,"price_asc",2,15);
        assertTrue(sql.query.endsWith("ORDER BY MIN_PRICE ASC NULLS LAST, p.ID DESC"));
        assertEquals(2,sql.page);assertEquals(15,sql.size);
        repo.searchViews(null,null,null,null,"price_desc",1,15);
        assertTrue(sql.query.endsWith("ORDER BY MIN_PRICE DESC NULLS LAST, p.ID DESC"));
    }
    @Test void categoryFilterIsBoundAndIncludesDescendantsAndAdminVisibility() {
        var sql=new CaptureSql();new ProductRepository(sql).searchViews("Áo",null,null,7L,"newest",1,15);
        assertEquals(7L,sql.params.get("categoryId"));
        assertTrue(sql.query.contains("START WITH ID=:categoryId"));
        assertTrue(sql.query.contains("p.STATUS = 1"));
        assertTrue(sql.query.contains("START WITH STATUS=0"));
    }
    @Test void unknownSortIsRejectedBeforeSql() {
        var sql=new CaptureSql();var service=new ProductService(new ProductRepository(sql));
        assertThrows(BusinessException.class,()->service.search(null,null,null,null,"PRICE;DROP TABLE PRODUCT",1,15));
        assertNull(sql.query);
    }
}
