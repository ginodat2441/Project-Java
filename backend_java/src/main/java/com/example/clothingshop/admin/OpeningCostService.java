package com.example.clothingshop.admin;
import org.springframework.stereotype.Service;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.*;
/** V007 compatibility DTO only. Shared product cost must never be written after V008. */
@Deprecated
@Service
public class OpeningCostService {
 public record Receipt(@NotNull Long lineId,@NotNull @DecimalMin("0") @Digits(integer=12,fraction=2) BigDecimal unitCost){}
 public record Request(@DecimalMin("0") @Digits(integer=12,fraction=2) BigDecimal openingUnitCost,@NotBlank @Size(max=500) String reason,@NotNull @Size(max=1000) List<@NotNull @Valid Receipt> receipts){}
 public Map<String,Object> preview(long id){throw new IllegalArgumentException("Đối chiếu theo ID biến thể tại /api/admin/sku-cost/{id}");}
 public void save(long id,Request request,long actor){throw new IllegalArgumentException("Không ghi giá vốn chung của mã sản phẩm. Dùng đối chiếu giá vốn SKU");}
}
