package com.example.clothingshop.service.product;

import static com.example.clothingshop.dto.admin.AdminRequests.*;

import com.example.clothingshop.common.*;
import com.example.clothingshop.repository.admin.AdminOperationsRepository;
import com.example.clothingshop.repository.admin.AdminRepository;
import java.util.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ColorService {
  private final AdminRepository db;
  private final AdminOperationsRepository queries;
  private final PasswordEncoder encoder;

  public ColorService(
      AdminRepository db, AdminOperationsRepository queries, PasswordEncoder encoder) {
    this.db = db;
    this.queries = queries;
    this.encoder = encoder;
  }

  @Transactional
  public Map<String, Object> color(Color r, long actor) {
    String name = r.name().strip().replaceAll("\\s+", " ");
    if (name.isBlank()) throw new BusinessException("Nhập tên màu");
    queries.lockColor();
    var existing = queries.listColor("n", name);
    if (!existing.isEmpty()) return existing.get(0);
    long id =
        queries.insertColor(
            "n", name, "h", r.hexCode() == null ? null : r.hexCode().toUpperCase(Locale.ROOT));
    db.audit(actor, "CREATE", "COLOR", id);
    return queries.findColor("i", id);
  }
}
