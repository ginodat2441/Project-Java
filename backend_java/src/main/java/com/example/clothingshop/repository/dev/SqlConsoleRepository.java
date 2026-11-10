package com.example.clothingshop.repository.dev;

import com.example.clothingshop.common.SqlHelper;
import java.util.*;
import org.springframework.stereotype.Repository;

@Repository
public class SqlConsoleRepository {
  private final SqlHelper sql;

  public SqlConsoleRepository(SqlHelper sql) {
    this.sql = sql;
  }

  public List<Map<String, Object>> execute(String statement) {
    return sql.maps(statement, Map.of());
  }
}
