package com.example.clothingshop.admin;
import jakarta.mail.AuthenticationFailedException;
import jakarta.mail.MessagingException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.javamail.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

class SmtpCheckRunnerTest {
    @SuppressWarnings("unchecked")
    private SmtpCheckRunner runner(JavaMailSender sender){var provider=mock(ObjectProvider.class);when(provider.getIfAvailable()).thenReturn(sender);if(sender instanceof JavaMailSenderImpl implementation){when(implementation.getUsername()).thenReturn("sender@example.test");when(implementation.getPassword()).thenReturn("X".repeat(16));}return new SmtpCheckRunner(provider);}
    @Test void verifiesConnectionWithoutSendingMessages()throws Exception{
        var sender=mock(JavaMailSenderImpl.class);when(sender.getHost()).thenReturn("smtp.example.test");
        var result=runner(sender).check();assertTrue(result.success());assertEquals("SMTP_CHECK_OK",result.code());verify(sender).testConnection();verify(sender,never()).send(any(org.springframework.mail.SimpleMailMessage.class));
    }
    @Test void authenticationErrorsAreSanitized()throws Exception{
        var sender=mock(JavaMailSenderImpl.class);when(sender.getHost()).thenReturn("smtp.example.test");doThrow(new AuthenticationFailedException("provider-secret-detail")).when(sender).testConnection();
        var result=runner(sender).check();assertFalse(result.success());assertEquals("SMTP_CHECK_AUTH_FAILED",result.code());assertFalse(result.toString().contains("provider-secret-detail"));
    }
    @Test void networkErrorsAreSanitized()throws Exception{
        var sender=mock(JavaMailSenderImpl.class);when(sender.getHost()).thenReturn("smtp.example.test");doThrow(new MessagingException("private-server-detail")).when(sender).testConnection();assertEquals("SMTP_CHECK_CONNECTION_FAILED",runner(sender).check().code());
    }
    @Test void missingConfigurationDoesNotAttemptConnection(){assertEquals("SMTP_CHECK_NOT_CONFIGURED",runner(null).check().code());}
}
