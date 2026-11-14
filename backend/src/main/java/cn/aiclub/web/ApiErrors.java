package cn.aiclub.web;

import java.util.Map;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class ApiErrors {
  @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
  ResponseEntity<?> denied() {
    return ResponseEntity.status(403).body(Map.of("message", "无权执行此操作"));
  }

  @ExceptionHandler(ResponseStatusException.class)
  ResponseEntity<?> status(ResponseStatusException ex) {
    return ResponseEntity.status(ex.getStatusCode())
        .body(Map.of("message", ex.getReason() == null ? "请求失败" : ex.getReason()));
  }

  @ExceptionHandler({
    MethodArgumentNotValidException.class,
    IllegalArgumentException.class,
    org.springframework.http.converter.HttpMessageNotReadableException.class,
    org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class
  })
  ResponseEntity<?> invalid(Exception ex) {
    return ResponseEntity.badRequest().body(Map.of("message", "请求参数不合法，请检查必填项、长度与格式"));
  }

  @ExceptionHandler(MaxUploadSizeExceededException.class)
  ResponseEntity<?> large() {
    return ResponseEntity.status(413).body(Map.of("message", "图片不能超过 10MB"));
  }

  @ExceptionHandler(javax.imageio.IIOException.class)
  ResponseEntity<?> brokenImage() {
    return ResponseEntity.badRequest().body(Map.of("message", "图片内容损坏，请重新导出有效的 PNG/JPEG 文件"));
  }

  @ExceptionHandler(DataIntegrityViolationException.class)
  ResponseEntity<?> conflict() {
    return ResponseEntity.status(409).body(Map.of("message", "数据冲突，请刷新后重试"));
  }

  @ExceptionHandler(Exception.class)
  ResponseEntity<?> other(Exception ex) {
    LoggerFactory.getLogger(getClass()).error("Unhandled API error", ex);
    return ResponseEntity.internalServerError().body(Map.of("message", "服务暂时不可用，请稍后重试"));
  }
}
