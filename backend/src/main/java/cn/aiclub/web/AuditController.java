package cn.aiclub.web;

import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
public class AuditController {
  private final JdbcTemplate db;

  public AuditController(JdbcTemplate db) {
    this.db = db;
  }

  @GetMapping("/api/admin/audit")
  @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
  public Object list(
      @RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "20") int size) {
    if (page < 1 || page > 100000 || size < 1 || size > 100) throw new IllegalArgumentException();
    return Map.of(
        "total",
        db.queryForObject("SELECT COUNT(*) FROM audit_log", Long.class),
        "items",
        db.queryForList(
            "SELECT id,actor,action,target,created_at AS createdAt FROM audit_log ORDER BY id DESC"
                + " LIMIT ? OFFSET ?",
            size,
            (page - 1) * size));
  }
}
