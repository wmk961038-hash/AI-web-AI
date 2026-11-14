package cn.aiclub.web;

import cn.aiclub.mapper.AccountMapper;
import cn.aiclub.model.Account;
import cn.aiclub.service.AuditService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/admin/accounts")
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class AccountController {
  private final AccountMapper accounts;
  private final PasswordEncoder encoder;
  private final AuditService audit;

  public AccountController(AccountMapper accounts, PasswordEncoder encoder, AuditService audit) {
    this.accounts = accounts;
    this.encoder = encoder;
    this.audit = audit;
  }

  public record Create(
      @NotNull @Pattern(regexp = "[a-zA-Z0-9_.-]{3,64}") String username,
      @NotBlank @Size(min = 12, max = 72) String password,
      @NotNull @Pattern(regexp = "ADMIN|SUPER_ADMIN") String role) {}

  public record Change(
      @NotNull Boolean enabled, @NotNull @Pattern(regexp = "ADMIN|SUPER_ADMIN") String role) {}

  public record Reset(@NotBlank @Size(min = 12, max = 72) String password) {}

  @GetMapping
  public Object list() {
    return accounts.selectList(new QueryWrapper<Account>().orderByAsc("id"));
  }

  @PostMapping
  @Transactional
  public Account create(@Valid @RequestBody Create in) {
    var a = new Account();
    a.setUsername(in.username());
    a.setPasswordHash(encoder.encode(in.password()));
    a.setEnabled(true);
    a.setRole(in.role());
    a.setAuthVersion(0);
    accounts.insert(a);
    audit.record("CREATE_ACCOUNT", a.getId());
    return a;
  }

  @PutMapping("/{id}")
  @Transactional
  public void change(@PathVariable long id, @Valid @RequestBody Change in) {
    var supers =
        accounts.selectList(
            new QueryWrapper<Account>()
                .eq("role", "SUPER_ADMIN")
                .orderByAsc("id")
                .last("FOR UPDATE"));
    var a = required(id);
    if (id == AuthController.principal().id()
        && (!in.enabled() || !"SUPER_ADMIN".equals(in.role())))
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "不能停用或降级自己的账号");
    if (a.getEnabled()
        && "SUPER_ADMIN".equals(a.getRole())
        && (!in.enabled() || !"SUPER_ADMIN".equals(in.role()))
        && supers.stream().filter(Account::getEnabled).count() <= 1)
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "必须保留一个可用的超级管理员");
    a.setEnabled(in.enabled());
    a.setRole(in.role());
    a.setAuthVersion(a.getAuthVersion() + 1);
    accounts.updateById(a);
    audit.record("CHANGE_ACCOUNT", id);
  }

  @PostMapping("/{id}/password")
  @Transactional
  public void reset(@PathVariable long id, @Valid @RequestBody Reset in) {
    var a = required(id);
    a.setPasswordHash(encoder.encode(in.password()));
    a.setAuthVersion(a.getAuthVersion() + 1);
    accounts.updateById(a);
    audit.record("RESET_PASSWORD", id);
  }

  private Account required(long id) {
    var a = accounts.selectOne(new QueryWrapper<Account>().eq("id", id).last("FOR UPDATE"));
    if (a == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "账号不存在");
    return a;
  }
}
