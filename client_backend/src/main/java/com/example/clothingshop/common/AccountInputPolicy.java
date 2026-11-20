package com.example.clothingshop.common;

import com.example.clothingshop.dto.request.*;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.*;

public final class AccountInputPolicy {
    private AccountInputPolicy() {}
    public static class Invalid extends BusinessException {
        private final Map<String,String> fields;
        public Invalid(Map<String,String> fields) { super("Vui lòng kiểm tra các thông tin được đánh dấu.");this.fields=Map.copyOf(fields); }
        public Map<String,String> fields() { return fields; }
    }
    public static String text(String value) { return value==null?"":Normalizer.normalize(value.strip(),Normalizer.Form.NFC); }
    public static String email(String value) { return text(value).toLowerCase(Locale.ROOT); }
    public static String phone(String value) {
        String result=text(value).replaceAll("[() .-]","");
        return result.startsWith("+84") ? "0"+result.substring(3) : result;
    }
    public static boolean validEmail(String value) {
        if(value==null||value.length()>255||!value.matches("^[A-Za-z0-9.!#$%&'*+/=?^_`{|}~-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,63}$"))return false;
        String[] parts=value.split("@",-1);
        if(parts.length!=2||parts[0].length()>64||parts[0].startsWith(".")||parts[0].endsWith(".")||value.contains(".."))return false;
        for(String label:parts[1].split("\\.",-1))if(label.isEmpty()||label.length()>63||label.startsWith("-")||label.endsWith("-"))return false;
        return true;
    }
    public static void register(RegisterRequest r) {
        Map<String,String> errors=new LinkedHashMap<>();
        if(!r.username().matches("^[A-Za-z0-9][A-Za-z0-9_.-]{2,29}$"))errors.put("username","Tên đăng nhập gồm 3–30 chữ không dấu, số, dấu chấm, gạch dưới hoặc gạch ngang; bắt đầu bằng chữ hoặc số.");
        if(!validEmail(r.email()))errors.put("email","Nhập email đúng định dạng, ví dụ ten@gmail.com.");
        if(!r.fullName().matches("^[\\p{L}][\\p{L}\\p{M} '\\-]{1,149}$")||r.fullName().codePoints().filter(Character::isLetter).count()<2)
            errors.put("fullName","Họ tên phải có 2–150 ký tự, chỉ gồm chữ, khoảng trắng, dấu nháy hoặc gạch ngang.");
        if(!r.phone().matches("^0[35789][0-9]{8}$"))errors.put("phone","Nhập số di động Việt Nam hợp lệ (10 số, bắt đầu bằng 03, 05, 07, 08 hoặc 09).");
        String password=r.password();
        if(password==null||password.length()<12||password.getBytes(StandardCharsets.UTF_8).length>72
            ||!password.equals(password.strip())||password.codePoints().anyMatch(Character::isISOControl)
            ||!password.codePoints().anyMatch(Character::isLetter)||!password.chars().anyMatch(c->c>='0'&&c<='9'))
            errors.put("password","Mật khẩu từ 12 ký tự, có chữ và số, tối đa 72 byte; không có khoảng trắng ở đầu/cuối.");
        else if(Set.of("password1234","password12345","password123456","qwerty123456","admin12345678","123456789abc").contains(password.toLowerCase(Locale.ROOT)))
            errors.put("password","Mật khẩu quá phổ biến. Hãy chọn một mật khẩu riêng khó đoán hơn.");
        if(!errors.isEmpty())throw new Invalid(errors);
    }
    public static void login(LoginRequest r) {
        Map<String,String> errors=new LinkedHashMap<>();
        if(!validEmail(r.email()))errors.put("email","Nhập email đúng định dạng.");
        if(r.password()==null||r.password().isBlank()||r.password().getBytes(StandardCharsets.UTF_8).length>72)
            errors.put("password","Nhập mật khẩu của tài khoản (tối đa 72 byte).");
        if(!errors.isEmpty())throw new Invalid(errors);
    }
}
