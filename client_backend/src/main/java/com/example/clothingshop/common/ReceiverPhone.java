package com.example.clothingshop.common;

public final class ReceiverPhone {
    private ReceiverPhone() {}

    public static String normalize(String value) {
        if (value == null || !value.matches("[0-9+(). -]{1,25}")) throw invalid();
        String number = value.replaceAll("[(). -]", "");
        if (number.startsWith("+84")) number = "0" + number.substring(3);
        else if (number.startsWith("84")) number = "0" + number.substring(2);
        if (!number.matches("0(?:[35789][0-9]{8}|2[0-9]{9})")) throw invalid();
        return number;
    }

    private static BusinessException invalid() {
        return new BusinessException("Số điện thoại người nhận không hợp lệ. Nhập số Việt Nam, ví dụ 0912345678 hoặc +84912345678.");
    }
}
