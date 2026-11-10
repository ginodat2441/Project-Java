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
public class CategoryService {
  private final AdminRepository db;
  private final AdminOperationsRepository queries;
  private final PasswordEncoder encoder;

  public CategoryService(
      AdminRepository db, AdminOperationsRepository queries, PasswordEncoder encoder) {
    this.db = db;
    this.queries = queries;
    this.encoder = encoder;
  }

  @Transactional
  public long category(Long id, Category r, long actor) {
    queries.lockCategory();
    if (id != null) queries.findCategory2("i", id);
    Long parent = r.parentId();
    Set<Long> visited = new HashSet<>();
    while (parent != null) {
      if (parent.equals(id) || !visited.add(parent))
        throw new BusinessException("Danh mục cha tạo vòng lặp");
      Object next = queries.findCategory3("i", parent).get("PARENT_ID");
      parent = next == null ? null : ((Number) next).longValue();
    }
    if (id == null)
      id = queries.insertCategory("n", r.name().trim(), "p", r.parentId(), "s", r.status());
    else queries.updateCategory("n", r.name().trim(), "p", r.parentId(), "s", r.status(), "i", id);
    db.audit(actor, "SAVE", "CATEGORY", id);
    return id;
  }
}
