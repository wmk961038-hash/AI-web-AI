package cn.aiclub.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class AuditService {
  private final JdbcTemplate db;

  public AuditService(JdbcTemplate db) {
    this.db = db;
  }

  public void record(String action, Object target) {
    var auth = SecurityContextHolder.getContext().getAuthentication();
    db.update(
        "INSERT INTO audit_log(actor,action,target) VALUES(?,?,?)",
        auth == null ? "system" : auth.getName(),
        action,
        String.valueOf(target));
  }
}
