package cn.aiclub.model;

import com.baomidou.mybatisplus.annotation.*;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class Content {
  @TableId(type = IdType.AUTO)
  private Long id;

  private String createdBy;

  private String type;
  private String title;
  private String category;
  private String personRole;
  private String summary;
  private String body;
  private Long coverId;
  private String metadata;
  private String status;
  private Integer sortOrder;
  private Boolean featured;
  private Boolean pinned;
  private Boolean archived;
  private Integer version;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
  private LocalDateTime publishedAt;
}
