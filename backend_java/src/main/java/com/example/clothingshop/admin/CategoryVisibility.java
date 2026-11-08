package com.example.clothingshop.admin;
public final class CategoryVisibility {
 private CategoryVisibility(){}
 public static final String SQL=" AND (p.CATEGORY_ID IS NULL OR p.CATEGORY_ID NOT IN (SELECT ID FROM CATEGORY START WITH STATUS=0 CONNECT BY NOCYCLE PRIOR ID=PARENT_ID)) ";
}
