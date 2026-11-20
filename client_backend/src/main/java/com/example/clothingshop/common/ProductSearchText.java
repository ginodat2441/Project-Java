package com.example.clothingshop.common;

import java.text.Normalizer;
import java.util.Locale;

public final class ProductSearchText {
    private ProductSearchText() {}
    public static final String ACCENTS="àáạảãâầấậẩẫăằắặẳẵèéẹẻẽêềếệểễìíịỉĩòóọỏõôồốộổỗơờớợởỡùúụủũưừứựửữỳýỵỷỹđ";
    public static String fold(String value) {
        return Normalizer.normalize(value.toLowerCase(Locale.ROOT),Normalizer.Form.NFD)
            .replaceAll("\\p{M}+","").replace('đ','d');
    }
    public static String normalize(String value) {
        if(value==null)return null;
        String clean=Normalizer.normalize(value.strip(),Normalizer.Form.NFC).replaceAll("\\s+"," ");
        if(clean.length()>120)throw new BusinessException("Từ khóa tìm kiếm tối đa 120 ký tự.");
        if(clean.codePoints().anyMatch(Character::isISOControl))throw new BusinessException("Từ khóa tìm kiếm chứa ký tự không hợp lệ.");
        return clean;
    }
    public static String literalLike(String value) {
        return "%"+fold(value).replace("\\","\\\\").replace("%","\\%").replace("_","\\_")+"%";
    }
}
