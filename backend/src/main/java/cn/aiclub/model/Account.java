package cn.aiclub.model;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("admin_account")
public class Account {
  @TableId(type = IdType.AUTO)
  private Long id;

  private String username;
  @JsonIgnore private String passwordHash;
  private String role;
  private Boolean enabled;
  @JsonIgnore private Integer authVersion;
  private LocalDateTime createdAt;
}
