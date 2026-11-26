package com.example.clothingshop.service;

import com.example.clothingshop.dto.request.SePayIpnRequest;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SePayServiceTest {

    private final SePayService service = new SePayService("test-api-key");

    @Test
    void acceptsOnlyConfiguredApikeyAuthorizationHeader() {
        assertTrue(service.isApiKeyConfigured());
        assertTrue(service.isValidAuthorization("Apikey test-api-key"));
        assertFalse(service.isValidAuthorization("Bearer test-api-key"));
        assertFalse(service.isValidAuthorization("Apikey wrong-key"));
        assertFalse(service.isValidAuthorization(null));
        assertFalse(new SePayService("").isApiKeyConfigured());
    }

    @Test
    void extractsBillCodeFromTransferContentAndAcceptsOnlyIncomingPayments() {
        SePayIpnRequest valid = new SePayIpnRequest(92704L, "Vietcombank",
                "2026-11-13 15:09:31", "SBSEPAYGL9TEMZRXDAK", "",
                null, "HD261113150931123 chuyen tien", "in", "",
                BigDecimal.valueOf(150_000), BigDecimal.valueOf(150_000), "FT24012345678");
        SePayIpnRequest outgoing = new SePayIpnRequest(92705L, "Vietcombank",
                "2026-11-13 15:09:31", "SBSEPAYGL9TEMZRXDAK", "",
                null, "HD261113150931123 chuyen tien", "out", "",
                BigDecimal.valueOf(150_000), BigDecimal.valueOf(0), "FT24012345679");
        SePayIpnRequest withoutBillCode = new SePayIpnRequest(92706L, "Vietcombank",
                "2026-11-13 15:09:31", "SBSEPAYGL9TEMZRXDAK", "",
                null, "chuyen tien", "in", "",
                BigDecimal.valueOf(150_000), BigDecimal.valueOf(150_000), "FT24012345680");

        assertTrue(service.isValidPaymentNotification(valid));
        assertEquals("HD261113150931123", service.extractBillCode(valid).orElseThrow());
        assertFalse(service.isValidPaymentNotification(outgoing));
        assertFalse(service.isValidPaymentNotification(withoutBillCode));
        assertFalse(service.isValidPaymentNotification(null));
    }
}
