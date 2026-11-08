package com.example.clothingshop.admin;
import java.text.Normalizer;
import java.util.*;
/** Province codes: Decision 19/2025/QD-TTg, official source recorded in SHIPPING.md. */
public final class ProvinceCatalog {
 private ProvinceCatalog(){}
 public record Province(String code,String name){}
 public static final List<Province> ALL=List.of(
  new Province("01","Thành phố Hà Nội"),new Province("04","Tỉnh Cao Bằng"),new Province("08","Tỉnh Tuyên Quang"),new Province("11","Tỉnh Điện Biên"),new Province("12","Tỉnh Lai Châu"),new Province("14","Tỉnh Sơn La"),new Province("15","Tỉnh Lào Cai"),new Province("19","Tỉnh Thái Nguyên"),new Province("20","Tỉnh Lạng Sơn"),new Province("22","Tỉnh Quảng Ninh"),new Province("24","Tỉnh Bắc Ninh"),new Province("25","Tỉnh Phú Thọ"),new Province("31","Thành phố Hải Phòng"),new Province("33","Tỉnh Hưng Yên"),new Province("37","Tỉnh Ninh Bình"),new Province("38","Tỉnh Thanh Hóa"),new Province("40","Tỉnh Nghệ An"),new Province("42","Tỉnh Hà Tĩnh"),new Province("44","Tỉnh Quảng Trị"),new Province("46","Thành phố Huế"),new Province("48","Thành phố Đà Nẵng"),new Province("51","Tỉnh Quảng Ngãi"),new Province("52","Tỉnh Gia Lai"),new Province("56","Tỉnh Khánh Hòa"),new Province("66","Tỉnh Đắk Lắk"),new Province("68","Tỉnh Lâm Đồng"),new Province("75","Tỉnh Đồng Nai"),new Province("79","Thành phố Hồ Chí Minh"),new Province("80","Tỉnh Tây Ninh"),new Province("82","Tỉnh Đồng Tháp"),new Province("86","Tỉnh Vĩnh Long"),new Province("91","Tỉnh An Giang"),new Province("92","Thành phố Cần Thơ"),new Province("96","Tỉnh Cà Mau"));
 public static String normalize(String name){return Normalizer.normalize(name.strip(),Normalizer.Form.NFD).replaceAll("\\p{M}+","").replace('đ','d').replace('Đ','D').toLowerCase(Locale.ROOT).replaceFirst("^(thanh pho|tinh|tp[.]?)\\s*","").replaceAll("[^a-z0-9]","");}
 public static Province resolve(String input){if(input==null||input.isBlank())throw new IllegalArgumentException("Chọn tỉnh/thành để tính phí giao hàng");String key=normalize(input);if(Set.of("tphcm","hcm","saigon","tp hochiminh").contains(key))key="hochiminh";final String match=key;return ALL.stream().filter(p->p.code().equals(input.trim())||normalize(p.name()).equals(match)).findFirst().orElseThrow(()->new IllegalArgumentException("Tỉnh/thành không hợp lệ. Chọn trong danh sách tỉnh/thành hiện tại"));}
}
