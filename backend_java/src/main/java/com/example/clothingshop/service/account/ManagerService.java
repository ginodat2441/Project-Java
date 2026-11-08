package com.example.clothingshop.service.account;

import static com.example.clothingshop.dto.admin.AdminRequests.*;
import static com.example.clothingshop.repository.admin.AdminRepository.number;

import com.example.clothingshop.common.*;
import com.example.clothingshop.repository.admin.AdminOperationsRepository;
import com.example.clothingshop.repository.admin.AdminRepository;
import java.util.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ManagerService {
  private String validRole(long id) {
    String code = queries.findManagerRole("i", id).get("ROLE_CODE").toString();
    if (!Set.of("ADMIN", "STAFF").contains(code))
      throw new BusinessException("Vai trò không được hỗ trợ");
    return code;
  }

  private final AdminRepository db;
  private final AdminOperationsRepository queries;
  private final PasswordEncoder encoder;

  public ManagerService(
      AdminRepository db, AdminOperationsRepository queries, PasswordEncoder encoder) {
    this.db = db;
    this.queries = queries;
    this.encoder = encoder;
  }

  @Transactional
  public long manager(Manager r, long actor) {
    validRole(r.roleId());
    long id =
        queries.insertManager(
            "r",
            r.roleId(),
            "u",
            r.username(),
            "p",
            encoder.encode(r.password()),
            "n",
            r.fullName(),
            "e",
            r.email());
    db.audit(actor, "CREATE", "MANAGER", id);
    return id;
  }

  @Transactional
  public void access(long id, Access r, long actor) {
    queries.lockManager();
    var old = queries.findManager2("i", id);
    String role = validRole(r.roleId());
    if (id == actor && (r.status() == 0 || !role.equals("ADMIN")))
      throw new BusinessException("Không thể tự khóa hoặc thu hồi quyền của mình");
    long active = number(queries.findManager3("i", id), "N");
    if (active == 0 && (r.status() == 0 || !role.equals("ADMIN")))
      throw new BusinessException("Phải giữ ít nhất một quản trị viên hoạt động");
    queries.updateManager("r", r.roleId(), "s", r.status(), "i", id);
    db.auditChange(
        actor,
        "ACCESS",
        "MANAGER",
        id,
        old,
        Map.of("MANAGER_ROLE_ID", r.roleId(), "STATUS", r.status()),
        "Đổi quyền hoặc trạng thái tài khoản");
  }
}
