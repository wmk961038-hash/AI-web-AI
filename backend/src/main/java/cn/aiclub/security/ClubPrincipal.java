package cn.aiclub.security;

import java.io.Serializable;
import java.security.Principal;

public record ClubPrincipal(Long id, String username, int version)
    implements Principal, Serializable {
  public String getName() {
    return username;
  }
}
