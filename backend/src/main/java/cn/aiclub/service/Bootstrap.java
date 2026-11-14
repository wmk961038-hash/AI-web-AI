package cn.aiclub.service;

import cn.aiclub.mapper.AccountMapper;
import cn.aiclub.model.Account;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class Bootstrap implements ApplicationRunner {
  private final AccountMapper accounts;
  private final PasswordEncoder encoder;

  @Value("${club.bootstrap-user}")
  private String username;

  @Value("${club.bootstrap-password}")
  private String password;

  public Bootstrap(AccountMapper a, PasswordEncoder e) {
    accounts = a;
    encoder = e;
  }

  public void run(ApplicationArguments args) {
    if (accounts.selectCount(null) > 0) return;
    if (!username.matches("[a-zA-Z0-9_.-]{3,64}")
        || password.length() < 12
        || password.length() > 72)
      throw new IllegalStateException(
          "首次启动必须通过 BOOTSTRAP_USER 与 BOOTSTRAP_PASSWORD 设置超级管理员（密码 12–72 字符）");
    var a = new Account();
    a.setUsername(username);
    a.setPasswordHash(encoder.encode(password));
    a.setRole("SUPER_ADMIN");
    a.setEnabled(true);
    a.setAuthVersion(0);
    accounts.insert(a);
  }
}
