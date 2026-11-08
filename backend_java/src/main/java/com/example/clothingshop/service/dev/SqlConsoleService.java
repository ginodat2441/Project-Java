package com.example.clothingshop.service.dev;

import com.example.clothingshop.common.BusinessException;
import com.example.clothingshop.dto.dev.SqlRequest;
import com.example.clothingshop.repository.dev.SqlConsoleRepository;
import java.util.*;
import org.springframework.stereotype.Service;

@Service
public class SqlConsoleService {
  private final SqlConsoleRepository repository;

  public SqlConsoleService(SqlConsoleRepository repository) {
    this.repository = repository;
  }

  public List<Map<String, Object>> execute(SqlRequest req) {
    String s = req.sql() == null ? "" : req.sql().trim();
    if (s.endsWith(";")) s = s.substring(0, s.length() - 1);
    String lower = s.toLowerCase();
    if (!(lower.startsWith("select") || lower.startsWith("with")) || s.contains(";")) {
      throw new BusinessException("Chỉ cho phép một câu SELECT");
    }
    return repository.execute(s);
  }
}
