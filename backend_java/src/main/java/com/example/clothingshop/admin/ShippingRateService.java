package com.example.clothingshop.admin;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.*;
import static com.example.clothingshop.admin.AdminRepository.number;
@Service
public class ShippingRateService {
 private final AdminRepository db;
 public ShippingRateService(AdminRepository db){this.db=db;}
 public record Update(@NotNull @DecimalMin("0") @Digits(integer=9,fraction=0) BigDecimal fee,@Min(0) @Max(1) int status,@NotNull @PositiveOrZero Long version,@NotBlank @Size(max=500) String reason){}
 public List<Map<String,Object>> list(){return db.rows("SELECT PROVINCE_CODE CODE,PROVINCE_NAME NAME,FEE,STATUS,VERSION,UPDATED_AT FROM SHIPPING_RATE ORDER BY PROVINCE_NAME");}
 public BigDecimal fee(String province){var p=ProvinceCatalog.resolve(province);var rate=db.one("SELECT FEE,STATUS FROM SHIPPING_RATE WHERE PROVINCE_CODE=:c","c",p.code());if(number(rate,"STATUS")!=1)throw new IllegalArgumentException("Shop tạm ngưng giao đến "+p.name());return (BigDecimal)rate.get("FEE");}
 @Transactional public void update(String code,Update r,long actor){
  ProvinceCatalog.resolve(code);var before=db.one("SELECT FEE,STATUS,VERSION FROM SHIPPING_RATE WHERE PROVINCE_CODE=:c FOR UPDATE","c",code);if(number(before,"VERSION")!=r.version())throw new IllegalArgumentException("Bảng phí đã được cập nhật; tải lại trước khi lưu");
  db.update("UPDATE SHIPPING_RATE SET FEE=:f,STATUS=:s,VERSION=VERSION+1,UPDATED_BY=:a,UPDATED_AT=LOCALTIMESTAMP WHERE PROVINCE_CODE=:c","f",r.fee(),"s",r.status(),"a",actor,"c",code);
  db.auditChange(actor,"SHIPPING_RATE","SHIPPING_RATE",null,before,Map.of("provinceCode",code,"fee",r.fee(),"status",r.status()),r.reason());
 }
}
