package cn.aiclub.security;

import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class LoginLimiter {
  private record Window(int attempts, long until) {}

  private final Map<String, Window> windows = new HashMap<>();

  public synchronized void attempt(String ip, String user) {
    long now = System.currentTimeMillis();
    windows.entrySet().removeIf(e -> e.getValue().until < now); // 遍历判断: 如果过期时间早于当前时间就删除window记录
    if (windows.size() > 20000)
      throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "登录繁忙，请稍后重试");
    check("ip:" + ip, 40, now);
    check("user:" + user, 8, now);
  }

  private void check(String key, int limit, long now) {
    var w = windows.getOrDefault(key, new Window(0, now + 15 * 60 * 1000));
    if (w.attempts >= limit)
      throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "登录尝试过多，请在 15 分钟后重试");
    windows.put(key, new Window(w.attempts + 1, w.until));
  }
}
