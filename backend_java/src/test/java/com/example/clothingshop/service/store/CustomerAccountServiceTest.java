package com.example.clothingshop.service.store;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

import com.example.clothingshop.common.BusinessException;
import com.example.clothingshop.dto.store.CustomerRequests.*;
import com.example.clothingshop.dto.store.StoreRequests;
import com.example.clothingshop.repository.admin.AdminRepository;
import com.example.clothingshop.repository.store.*;
import com.example.clothingshop.service.order.BillService;
import com.example.clothingshop.service.shipping.ShippingQuoteService;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.mock.web.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

class CustomerAccountServiceTest {
  final StoreService store=mock(StoreService.class);
  final CustomerAccountRepository repo=mock(CustomerAccountRepository.class);
  final CustomerCartRepository locks=mock(CustomerCartRepository.class);
  final PasswordEncoder encoder=new BCryptPasswordEncoder(4);
  final BillService bills=mock(BillService.class);
  final CustomerAccountService service=new CustomerAccountService(store,repo,locks,encoder,bills);
  final MockHttpSession session=new MockHttpSession();

  @BeforeEach void setup(){when(store.customer(session)).thenReturn(9L);}

  @Test void profileUsesSessionIdentityAndNormalizesEmail(){
    when(repo.user(9L)).thenReturn(Map.of("USER_ID",7L));
    var request=new Profile("Khách", " NEW@Example.com ","0900000000",null,null);
    service.profile(session,request);
    verify(repo).profile(9L,7L,request,"new@example.com");
    verify(store).me(session);
  }

  @Test void duplicateEmailDoesNotWriteProfile(){
    when(repo.user(9L)).thenReturn(Map.of("USER_ID",7L));
    when(repo.emailUsed("used@example.com",7)).thenReturn(true);
    assertThrows(BusinessException.class,()->service.profile(session,new Profile("Khách","used@example.com","0900000000",null,null)));
    verify(repo,never()).profile(anyLong(),anyLong(),any(),anyString());
  }

  @Test void wrongCurrentPasswordCannotChangePassword(){
    when(repo.user(9L)).thenReturn(Map.of("USER_ID",7L,"PASSWORD_HASH",encoder.encode("CurrentPass@123")));
    assertThrows(BusinessException.class,()->service.password(session,new Password("wrong","NewPassword@123")));
    verify(repo,never()).password(anyLong(),anyString());
  }

  @Test void passwordChangeHashesAndRevokesSession(){
    when(repo.user(9L)).thenReturn(Map.of("USER_ID",7L,"PASSWORD_HASH",encoder.encode("CurrentPass@123")));
    session.setAttribute("customer",9L);session.setAttribute("customer-access-version",0L);
    service.password(session,new Password("CurrentPass@123","NewPassword@123"));
    var hash=org.mockito.ArgumentCaptor.forClass(String.class);
    verify(repo).password(eq(7L),hash.capture());assertTrue(encoder.matches("NewPassword@123",hash.getValue()));
    assertNull(session.getAttribute("customer"));assertNull(session.getAttribute("customer-access-version"));
  }

  @Test void utf8PasswordsRespectBcryptByteLimit(){
    assertDoesNotThrow(()->CustomerAccountService.validPassword("á".repeat(36),true));
    assertThrows(BusinessException.class,()->CustomerAccountService.validPassword("á".repeat(37),true));
    assertThrows(BusinessException.class,()->CustomerAccountService.validPassword("short",true));
  }

  @Test void firstAddressIsDefaultAndForeignAddressCannotBeEdited(){
    var request=new Address("Khách","0900000000","Hà Nội","Cầu Giấy","12 A",false);
    when(repo.addresses(9L)).thenReturn(List.of());
    service.address(session,null,request);verify(repo).address(9L,null,request,true);
    when(repo.ownedAddress(9L,55L)).thenThrow(new BusinessException("Không tìm thấy"));
    assertThrows(BusinessException.class,()->service.address(session,55L,request));
    verify(repo,never()).address(eq(9L),eq(55L),any(),anyBoolean());
  }

  @Test void cannotReadAnotherCustomersOrder(){
    doThrow(new BusinessException("Không tìm thấy")).when(repo).ownedOrder(9L,55L);
    assertThrows(BusinessException.class,()->service.order(session,55));verifyNoInteractions(bills);
  }

  @Test void loginCreatesSessionButRegistrationRequiresSeparateLogin(){
    var queries=mock(StoreOperationsRepository.class);
    var real=new StoreService(mock(AdminRepository.class),encoder,bills,mock(ShippingQuoteService.class),queries);
    when(queries.listUsers(any(Object[].class))).thenReturn(List.of(Map.of("ID",9L,"AUTH_VERSION",2L,"PASSWORD_HASH",encoder.encode("CurrentPass@123"))));
    var request=new MockHttpServletRequest();request.setSession(session);
    real.login(new StoreRequests.Login("ME@EXAMPLE.COM","CurrentPass@123"),session,request);
    assertEquals(9L,session.getAttribute("customer"));assertEquals(2L,session.getAttribute("customer-access-version"));
    when(queries.findRoles(any(Object[].class))).thenReturn(Map.of("ID",1L));
    when(queries.insertUsers(any(Object[].class))).thenReturn(18L);
    when(queries.insertCustomer(any(Object[].class))).thenReturn(19L);
    real.register(new StoreRequests.Register("new@example.com","NewPassword@123","Khách","0900000000"),session,request);
    assertNull(session.getAttribute("customer"));assertNull(session.getAttribute("customer-access-version"));
    when(queries.emailExists("new@example.com")).thenReturn(true);
    assertThrows(BusinessException.class,()->real.register(new StoreRequests.Register("new@example.com","NewPassword@123","Khách","0900000000"),session,request));
  }
}
