package com.example.clothingshop.service;

import com.example.clothingshop.dto.request.SePayIpnRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class SePayService {

    private static final Pattern BILL_CODE_PATTERN =
            Pattern.compile("(?<![A-Z0-9])HD\\d{15}(?![A-Z0-9])", Pattern.CASE_INSENSITIVE);

    private final String apiKey;

    @Value("${app.sepay.bank:}")
    private String bank = "";
    @Value("${app.sepay.account-number:}")
    private String account = "";

    public SePayService(@Value("${app.sepay.secret-key:}") String apiKey) {
        this.apiKey = apiKey;
    }

    public void requireCheckoutConfiguration() {
        if (!isApiKeyConfigured() || !bank.matches("[A-Za-z0-9_-]{1,50}")
                || !account.matches("[A-Za-z0-9]{1,50}")) {
            throw new com.example.clothingshop.common.BusinessException(
                    "Phương thức thanh toán SePay chưa sẵn sàng. Vui lòng chọn COD.");
        }
    }

    public String qrUrl(java.math.BigDecimal amount, String billCode) {
        requireCheckoutConfiguration();
        if (amount.signum() <= 0 || amount.stripTrailingZeros().scale() > 0)
            throw new com.example.clothingshop.common.BusinessException("Số tiền thanh toán SePay không hợp lệ.");
        return org.springframework.web.util.UriComponentsBuilder.fromUriString("https://vietqr.app/img")
                .queryParam("bank", bank).queryParam("acc", account)
                .queryParam("amount", amount.toBigIntegerExact()).queryParam("des", billCode)
                .queryParam("template", "compact").build().encode().toUriString();
    }

    public boolean isApiKeyConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }

    public boolean isValidAuthorization(String authorization) {
        if (!isApiKeyConfigured() || authorization == null) {
            return false;
        }
        String expected = "Apikey " + apiKey;
        return MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8),
                authorization.getBytes(StandardCharsets.UTF_8));
    }

    public boolean isValidPaymentNotification(SePayIpnRequest notification) {
        return notification != null
                && notification.id() != null
                && notification.id() > 0
                && notification.transferType() != null
                && "in".equalsIgnoreCase(notification.transferType())
                && notification.transferAmount() != null
                && notification.transferAmount().signum() > 0
                && extractBillCode(notification).isPresent();
    }

    public Optional<String> extractBillCode(SePayIpnRequest notification) {
        if (notification == null) {
            return Optional.empty();
        }
        Optional<String> contentCode = extractBillCode(notification.content());
        return contentCode.isPresent() ? contentCode : extractBillCode(notification.code());
    }

    private Optional<String> extractBillCode(String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        Matcher matcher = BILL_CODE_PATTERN.matcher(value);
        return matcher.find()
                ? Optional.of(matcher.group().toUpperCase(Locale.ROOT))
                : Optional.empty();
    }
}
