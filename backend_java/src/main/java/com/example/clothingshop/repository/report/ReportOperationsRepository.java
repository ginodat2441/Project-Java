package com.example.clothingshop.repository.report;

import com.example.clothingshop.repository.admin.AdminRepository;
import java.util.*;
import org.springframework.stereotype.Repository;

@Repository
public class ReportOperationsRepository {
  private final AdminRepository db;

  public ReportOperationsRepository(AdminRepository db) {
    this.db = db;
  }

  public Map<String, Object> findProductDetail(Object... args) {
    return db.one(
        "SELECT (SELECT COUNT(*) FROM PRODUCT_DETAIL d JOIN PRODUCT p ON p.ID=d.PRODUCT_ID WHERE"
            + " d.DELETE_FLAG=0 AND p.DELETE_FLAG=0 AND d.QUANTITY<=:t) LOW_STOCK,(SELECT COUNT(*)"
            + " FROM BILL WHERE STATUS=0 AND DELETE_FLAG=0) WAIT_CONFIRM,(SELECT COUNT(*) FROM BILL"
            + " WHERE STATUS=1 AND DELETE_FLAG=0) WAIT_DISPATCH,(SELECT COUNT(*) FROM BILL WHERE"
            + " STATUS=6 AND DELETE_FLAG=0) FAILED,(SELECT COUNT(*) FROM INVENTORY_DOCUMENT WHERE"
            + " STATUS='DRAFT') WAIT_DOCUMENT FROM DUAL",
        args);
  }

  public List<Map<String, Object>> listBill(Object... args) {
    return db.rows(
        "SELECT STATUS,COUNT(*) TOTAL FROM BILL WHERE DELETE_FLAG=0 GROUP BY STATUS ORDER BY"
            + " STATUS",
        args);
  }

  public List<Map<String, Object>> listProductDetail(Object... args) {
    return db.rows(
        "SELECT pd.SKU,p.PRODUCT_NAME,pd.SIZE_NAME,pd.QUANTITY FROM PRODUCT_DETAIL pd JOIN PRODUCT"
            + " p ON p.ID=pd.PRODUCT_ID WHERE pd.DELETE_FLAG=0 AND p.DELETE_FLAG=0 AND"
            + " pd.QUANTITY<=:t ORDER BY pd.QUANTITY FETCH FIRST 50 ROWS ONLY",
        args);
  }
}
