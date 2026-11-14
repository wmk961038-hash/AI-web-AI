package cn.aiclub.web;

import cn.aiclub.mapper.AccountMapper;
import cn.aiclub.model.Account;
import cn.aiclub.security.*;
import cn.aiclub.service.AuditService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import jakarta.servlet.http.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
  private final AccountMapper accountMapper;
  private final PasswordEncoder passwordEncoder;
  private final LoginLimiter loginLimiter;
  private final AuditService auditService;

  public AuthController(AccountMapper a, PasswordEncoder e, LoginLimiter l, AuditService log) {
    accountMapper = a;
    passwordEncoder = e;
    loginLimiter = l;
    auditService = log;
  }

  public record Login(
      @NotBlank @Size(max = 64) String username, @NotBlank @Size(max = 72) String password) {}

  @GetMapping("/csrf")
  public Object csrf(CsrfToken token) {
    return Map.of("token", token.getToken(), "headerName", token.getHeaderName());
  }

  @PostMapping("/login")
  public Object login(@Valid @RequestBody Login in, HttpServletRequest req) {
    loginLimiter.attempt(req.getRemoteAddr(), in.username().strip().toLowerCase(Locale.ROOT));
    var a = accountMapper.selectOne(new QueryWrapper<Account>().eq("username", in.username()));
    String dummy = "$2a$12$EphWqK2FKd59iMTtWYV6/eZVbryWh2dGp0FJQaht7iL/l3hRhI9iS";
    boolean matches =
        passwordEncoder.matches(in.password(), a == null ? dummy : a.getPasswordHash());
    if (a == null || !matches || !a.getEnabled())
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "账号或密码错误");
    var old = req.getSession(false);
    if (old != null) old.invalidate();
    var context = SecurityContextHolder.createEmptyContext();
    context.setAuthentication(
        UsernamePasswordAuthenticationToken.authenticated(
            new ClubPrincipal(a.getId(), a.getUsername(), a.getAuthVersion()),
            null,
            List.of(new SimpleGrantedAuthority("ROLE_" + a.getRole()))));
    SecurityContextHolder.setContext(context);
    req.getSession(true)
        .setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);
    auditService.record("LOGIN", a.getId());
    return a;
  }

  @GetMapping("/me")
  public Object me() {
    return accountMapper.selectById(principal().id());
  }

  @PostMapping("/logout")
  public void logout(HttpServletRequest req) {
    auditService.record("LOGOUT", principal().id());
    req.getSession().invalidate();
    SecurityContextHolder.clearContext();
  }

  public record Password(
      @NotBlank @Size(max = 72) String oldPassword,
      @NotBlank @Size(min = 12, max = 72) String newPassword) {}

  @PostMapping("/password")
  @org.springframework.transaction.annotation.Transactional
  public void password(@Valid @RequestBody Password in, HttpServletRequest req) {
    var a =
        accountMapper.selectOne(
            new QueryWrapper<Account>().eq("id", principal().id()).last("FOR UPDATE"));
    if (!passwordEncoder.matches(in.oldPassword(), a.getPasswordHash()))
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "原密码错误");
    a.setPasswordHash(passwordEncoder.encode(in.newPassword()));
    a.setAuthVersion(a.getAuthVersion() + 1);
    accountMapper.updateById(a);
    auditService.record("CHANGE_PASSWORD", a.getId());
    req.getSession().invalidate();
    SecurityContextHolder.clearContext();
  }

  public static ClubPrincipal principal() {
    return (ClubPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
  }
}
