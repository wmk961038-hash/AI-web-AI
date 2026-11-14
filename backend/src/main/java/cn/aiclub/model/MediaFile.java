package cn.aiclub.model;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class MediaFile {
  @TableId(type = IdType.AUTO)
  private Long id;

  @JsonIgnore private Long uploadedBy;

  @JsonIgnore private String storedName;
  private String originalName;
  private String mimeType;
  private Long byteSize;
  private LocalDateTime createdAt;

  public String getUrl() {
    return "/api/media/" + id;
  }
}
