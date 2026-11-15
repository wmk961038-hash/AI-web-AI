package cn.aiclub.web;

import cn.aiclub.mapper.ContentMapper;
import cn.aiclub.model.Content;
import cn.aiclub.service.ContentService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@Validated
public class ContentController {
  private final ContentService contentService;
  private final ContentMapper contentMapper;

  public ContentController(ContentService s, ContentMapper c) {
    contentService = s;
    contentMapper = c;
  }

  @GetMapping("/api/public/contents")
  Object publicList(
      @RequestParam(required = false) String type,
      @RequestParam(required = false) String category,
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(defaultValue = "12") int size,
      @RequestParam(required = false) Boolean archived,
      @RequestParam(required = false) String cohort,
      @RequestParam(required = false) Boolean featured) {
    return contentService.list(
        false, type, null, category, page, size, archived, cohort, featured, null, null, null);
  }

  @GetMapping("/api/public/people/categories")
  Object personCategories() {
    return contentService.publicPersonCategories();
  }

  @GetMapping("/api/admin/people/categories")
  Object adminPersonCategories() {
    return contentService.personCategories(true);
  }

  @GetMapping("/api/public/contents/{id}")
  Content detail(@PathVariable long id) {
    return contentService.getContent(id, false);
  }

  @GetMapping("/api/public/pages/{key}")
  Object page(@PathVariable String key) {
    var c =
        contentMapper.selectOne(
            new QueryWrapper<Content>()
                .eq("type", "PAGE")
                .eq("category", key)
                .eq("status", "PUBLISHED"));
    if (c == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "页面待补充");
    return c;
  }

  @GetMapping("/api/admin/contents")
  Object adminList(
      @RequestParam(required = false) String type,
      @RequestParam(required = false) String status,
      @RequestParam(required = false) String category,
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false) String dateFrom,
      @RequestParam(required = false) String dateTo,
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(defaultValue = "20") int size) {
    return contentService.list(
        true,
        type,
        status,
        category,
        page,
        size,
        null,
        null,
        null,
        keyword,
        dateFrom,
        dateTo);
  }

  @GetMapping("/api/admin/contents/{id}")
  Object preview(@PathVariable long id) {
    return contentService.getContent(id, true);
  }

  @PostMapping("/api/admin/contents")
  Object create(@Valid @RequestBody ContentService.Input in) {
    return contentService.save(null, in);
  }

  @PutMapping("/api/admin/contents/{id}")
  Object update(@PathVariable long id, @Valid @RequestBody ContentService.Input in) {
    return contentService.save(id, in);
  }

  public record Transition(@jakarta.validation.constraints.NotNull @Min(0) Integer version) {}

  @PostMapping("/api/admin/contents/{id}/{action}")
  Object transition(
      @PathVariable long id, @PathVariable String action, @Valid @RequestBody Transition in) {
    return contentService.transition(id, action, in.version());
  }
}
