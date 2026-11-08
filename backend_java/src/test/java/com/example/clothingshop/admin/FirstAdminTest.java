package com.example.clothingshop.admin;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.transaction.support.TransactionTemplate;
import java.util.*;
import java.util.function.Consumer;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class FirstAdminTest {
    static class Terminal implements FirstAdmin.Input {
        final Deque<String> lines=new LinkedList<>();
        final Deque<char[]> passwords=new LinkedList<>();
        final List<String> messages=new ArrayList<>();
        public String line(String prompt) { return lines.removeFirst(); }
        public char[] password(String prompt) { return passwords.removeFirst(); }
        public void message(String message) { messages.add(message); }
    }
    @Test void trimsEmailAndRetriesShortPasswordAndMismatch() {
        var db=mock(AdminRepository.class);
        var tx=mock(TransactionTemplate.class);
        when(db.one(startsWith("SELECT COUNT(*)"))).thenReturn(Map.of("N",0));
        when(db.one(startsWith("SELECT ID FROM MANAGER_ROLE"))).thenReturn(Map.of("ID",1));
        when(db.insert(anyString(),any(Object[].class))).thenReturn(5L);
        doAnswer(invocation -> {
            Consumer<org.springframework.transaction.TransactionStatus> callback=invocation.getArgument(0);
            callback.accept(mock(org.springframework.transaction.TransactionStatus.class));
            return null;
        }).when(tx).executeWithoutResult(any());
        var terminal=new Terminal();
        terminal.lines.addAll(List.of(" hieu ","admin@example.test      "));
        char[] shortPassword="short".toCharArray();
        char[] mismatch="temporary-pass-A".toCharArray(),confirmation="temporary-pass-B".toCharArray();
        char[] password="temporary-pass-C".toCharArray(),match="temporary-pass-C".toCharArray();
        terminal.passwords.addAll(List.of(shortPassword,mismatch,confirmation,password,match));
        new FirstAdmin(db,new BCryptPasswordEncoder(4),tx).runInteractive(terminal);
        var params=org.mockito.ArgumentCaptor.forClass(Object[].class);
        verify(db,times(1)).insert(anyString(),params.capture());
        var saved=AdminRepository.params(params.getValue());
        assertEquals("hieu",saved.get("u"));
        assertEquals("admin@example.test",saved.get("e"));
        assertTrue(new BCryptPasswordEncoder().matches("temporary-pass-C",saved.get("p").toString()));
        assertTrue(terminal.messages.stream().anyMatch(s -> s.contains("12 den 72")));
        assertTrue(terminal.messages.stream().anyMatch(s -> s.contains("khong khop")));
        assertTrue(terminal.messages.stream().anyMatch(s -> s.contains("thanh cong")));
        for (char[] buffer:List.of(shortPassword,mismatch,confirmation,password,match)) assertArrayEquals(new char[buffer.length],buffer);
    }
    @Test void existingAdminRejectedBeforeCollectingCredentials() {
        var db=mock(AdminRepository.class);
        when(db.one(startsWith("SELECT COUNT(*)"))).thenReturn(Map.of("N",1));
        var terminal=new Terminal();
        new FirstAdmin(db,new BCryptPasswordEncoder(4),mock(TransactionTemplate.class)).runInteractive(terminal);
        assertTrue(terminal.messages.get(0).contains("Da co admin"));
        verify(db,never()).insert(anyString(),any(Object[].class));
    }
    @Test void endOfInputCancelsWithoutCreatingAccount() {
        var db=mock(AdminRepository.class);
        when(db.one(startsWith("SELECT COUNT(*)"))).thenReturn(Map.of("N",0));
        var terminal=new Terminal();terminal.lines.add(null);
        new FirstAdmin(db,new BCryptPasswordEncoder(4),mock(TransactionTemplate.class)).runInteractive(terminal);
        assertTrue(terminal.messages.get(0).contains("Da huy"));
        verify(db,never()).insert(anyString(),any(Object[].class));
    }
}
