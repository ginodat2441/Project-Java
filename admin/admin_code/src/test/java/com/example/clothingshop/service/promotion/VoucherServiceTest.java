package com.example.clothingshop.service.promotion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.clothingshop.dto.admin.AdminRequests.Voucher;
import com.example.clothingshop.repository.admin.AdminOperationsRepository;
import com.example.clothingshop.repository.admin.AdminRepository;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

class VoucherServiceTest {
  @Test
  void savesCombinedAudienceAndStartsUnlimitedVoucherImmediately() {
    AdminRepository db = mock(AdminRepository.class);
    AdminOperationsRepository queries = mock(AdminOperationsRepository.class);
    when(queries.insertVoucher(any(Object[].class))).thenReturn(41L);
    VoucherService service = new VoucherService(db, queries, mock(PasswordEncoder.class));
    Voucher request =
        new Voucher(
            "NEW_CUSTOMER",
            "FIXED",
            BigDecimal.valueOf(10000),
            BigDecimal.ZERO,
            BigDecimal.valueOf(10000),
            null,
            null,
            20,
            1,
            List.of(),
            true,
            7,
            true,
            3);

    assertEquals(41L, service.voucher(null, request, 9L));

    var parameters = parametersFrom(queries);
    assertNotNull(parameters.get("start"));
    assertNull(parameters.get("end"));
    assertEquals(7, parameters.get("newAccountDays"));
    assertEquals(1, parameters.get("noPreviousOrders"));
    assertEquals(3, parameters.get("minimumOrders"));
  }

  private static Map<String, Object> parametersFrom(AdminOperationsRepository queries) {
    var captor = org.mockito.ArgumentCaptor.forClass(Object[].class);
    verify(queries).insertVoucher(captor.capture());
    Object[] args = captor.getValue();
    Map<String, Object> parameters = new HashMap<>();
    for (int i = 0; i < args.length; i += 2)
      parameters.put(args[i].toString(), args[i + 1]);
    return parameters;
  }
}
