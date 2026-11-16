package com.example.clothingshop.util.admin;

import java.util.*;
import org.springframework.http.*;

public final class Csv {
  private Csv() {}

  public static ResponseEntity<String> response(List<Map<String, Object>> rows, String name) {
    var columns = rows.isEmpty() ? List.<String>of() : new ArrayList<>(rows.get(0).keySet());
    var out = new StringBuilder("\uFEFF");
    out.append(String.join(",", columns)).append("\r\n");
    for (var row : rows) {
      var line = new ArrayList<String>();
      for (String col : columns) {
        Object v = row.get(col);
        String text = v == null ? "" : v.toString();
        if (!(v instanceof Number) && text.matches("^[=+@-].*")) text = "'" + text;
        line.add("\"" + text.replace("\"", "\"\"") + "\"");
      }
      out.append(String.join(",", line)).append("\r\n");
    }
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_TYPE, "text/csv;charset=UTF-8")
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + name + ".csv\"")
        .body(out.toString());
  }
}
