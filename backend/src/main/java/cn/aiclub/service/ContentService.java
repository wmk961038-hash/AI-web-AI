package cn.aiclub.service;

import cn.aiclub.mapper.*;
import cn.aiclub.model.Content;
import cn.aiclub.web.AuthController;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.fasterxml.jackson.databind.*;
import jakarta.validation.constraints.*;
import java.time.*;
import java.util.*;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ContentService {
  public record Input(
      @NotNull @Pattern(regexp = "NEWS|ACTIVITY|PROJECT|PERSON|PAGE") String type,
      @NotBlank @Size(max = 180) String title,
      @NotNull @Size(max = 80) String category,
      @Size(max = 100) String personRole,
      @NotNull @Size(max = 600) String summary,
      @NotNull @Size(max = 200000) String body,
      @Positive Long coverId,
      @NotNull Map<String, Object> metadata,
      @NotNull @Min(-100000) @Max(100000) Integer sortOrder,
      @NotNull Boolean featured,
      @NotNull Boolean pinned,
      @NotNull Boolean archived,
      @NotNull @Min(0) Integer version) {}

  private final ContentMapper contents;
  private final MediaMapper media;
  private final JdbcTemplate db;
  private final ObjectMapper json;
  private final AuditService audit;

  public ContentService(
      ContentMapper c, MediaMapper m, JdbcTemplate d, ObjectMapper j, AuditService a) {
    contents = c;
    media = m;
    db = d;
    json = j;
    audit = a;
  }

  public Content getContent(long id, boolean admin) {
    var c = contents.selectById(id);
    if (c == null || (!admin && !"PUBLISHED".equals(c.getStatus())))
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "内容不存在或尚未发布");
    return c;
  }

  public Object list(
      boolean admin,
      String type,
      String status,
      String category,
      int page,
      int size,
      Boolean archived,
      String cohort,
      Boolean featured) {
    if (page < 1 || page > 100000 || size < 1 || size > 100) throw new IllegalArgumentException();
    var q = new QueryWrapper<Content>();
    if (type != null && !type.isBlank()) q.in("type", Arrays.asList(type.split(",")));
    if (category != null && !category.isBlank())
      q.in("category", Arrays.asList(category.split(",")));
    if (archived != null) q.eq("archived", archived);
    if (featured != null) q.eq("featured", featured);
    if (cohort != null && !cohort.isBlank())
      q.apply("LOCATE({0}, metadata) > 0", "\"cohort\":" + json.valueToTree(cohort).toString());
    if (!admin) q.eq("status", "PUBLISHED");
    else if (status != null && !status.isBlank()) q.eq("status", status);
    else q.ne("status", "TRASH");
    long total = contents.selectCount(q);
    q.orderByDesc("pinned")
        .orderByDesc("created_at")
        .orderByDesc("id")
        .last("LIMIT " + size + " OFFSET " + ((page - 1) * size));
    return Map.of("items", contents.selectList(q), "total", total, "page", page, "size", size);
  }

  public List<String> publicPersonCategories() {
    return db.queryForList(
        "SELECT DISTINCT category FROM content WHERE type='PERSON' AND status='PUBLISHED'"
            + " AND category <> '' ORDER BY category",
        String.class);
  }

  @Transactional
  @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
  public Content save(Long id, Input in) {
    Content c = id == null ? new Content() : getContent(id, true);
    if (id == null && in.type().equals("PAGE")) bad("固定页面请通过页面设置修改");
    if (id != null && (!in.type().equals(c.getType()) || "TRASH".equals(c.getStatus())))
      bad("内容类型不可修改；回收站内容请先恢复");
    if ("PAGE".equals(c.getType()) && !Objects.equals(c.getCategory(), in.category()))
      bad("固定页面标识不可修改");
    var meta = json.valueToTree(in.metadata());
    if (meta.toString().length() > 20000) bad("扩展信息过长");
    var allowed =
        Set.of(
            "startAt",
            "endAt",
            "location",
            "activityStatus",
            "members",
            "repository",
            "demo",
            "sourceName",
            "sourceUrl",
            "credit",
            "cohort",
            "position",
            "gallery",
            "joinUrl");
    meta.fieldNames()
        .forEachRemaining(
            k -> {
              if (!allowed.contains(k)) bad("不支持的扩展字段：" + k);
            });
    for (String key : allowed) {
      if (!key.equals("gallery") && meta.has(key) && !meta.get(key).isTextual()) bad("扩展字段应为文本");
    }
    for (String k : List.of("repository", "demo", "sourceUrl", "joinUrl"))
      if (meta.hasNonNull(k) && !meta.get(k).asText().isBlank()) {
        try {
          var uri = java.net.URI.create(meta.get(k).asText());
          if (!Set.of("http", "https").contains(uri.getScheme()) || uri.getHost() == null)
            bad("链接必须是完整的 http/https 地址");
        } catch (IllegalArgumentException ex) {
          bad("链接格式不合法");
        }
      }
    for (String k : List.of("startAt", "endAt"))
      if (meta.hasNonNull(k) && !meta.get(k).asText().isBlank())
        try {
          LocalDateTime.parse(meta.get(k).asText());
        } catch (Exception ex) {
          bad("活动时间格式不合法");
        }
    if (meta.path("startAt").asText().length() > 0
        && meta.path("endAt").asText().length() > 0
        && LocalDateTime.parse(meta.get("endAt").asText())
            .isBefore(LocalDateTime.parse(meta.get("startAt").asText()))) bad("结束时间不能早于开始时间");
    String body =
        Jsoup.clean(
            in.body(),
            "https://club.invalid",
            Safelist.relaxed().preserveRelativeLinks(true).addAttributes("*", "class"));
    var doc = Jsoup.parseBodyFragment(body);
    boolean person = "PERSON".equals(in.type());
    String category = in.category();
    String personRole = null;
    if (person) {
      category = in.category().trim();
      personRole = in.personRole() == null ? "" : in.personRole().trim();
      if (category.isBlank()) bad("请填写师生类别");
      if (category.contains(",")) bad("类别不能包含英文逗号");
      if (personRole.isBlank()) bad("请填写职务方向");
      if (in.coverId() == null) bad("请上传相关图片");
      if (doc.text().isBlank()) bad("请填写简介");
    }
    Set<Long> refs = new HashSet<>();
    for (var img : doc.select("img")) {
      String src = img.attr("src");
      if (!src.matches("/api/media/[1-9][0-9]*")) img.remove();
      else refs.add(Long.parseLong(src.substring(11)));
    }
    if (in.coverId() != null) refs.add(in.coverId());
    if (meta.has("gallery")) {
      if (!meta.get("gallery").isArray() || meta.get("gallery").size() > 40) bad("相册最多 40 张图片");
      for (var img : meta.get("gallery")) {
        if (!img.canConvertToLong() || !img.isIntegralNumber() || img.asLong() < 1) bad("相册图片编号无效");
        refs.add(img.asLong());
      }
    }
    for (Long ref : refs) if (media.selectById(ref) == null) bad("引用的图片不存在");
    c.setType(in.type());
    c.setTitle(in.title().trim());
    c.setCategory(category);
    c.setPersonRole(person ? personRole : null);
    c.setSummary(in.summary());
    c.setBody(doc.body().html());
    c.setMetadata(meta.toString());
    c.setCoverId(in.coverId());
    c.setSortOrder(in.sortOrder());
    c.setFeatured(in.featured());
    c.setPinned(in.pinned());
    c.setArchived(in.archived());
    c.setUpdatedAt(LocalDateTime.now());
    if (id == null) {
      c.setCreatedBy(AuthController.principal().username());
      c.setVersion(0);
      c.setStatus("DRAFT");
      contents.insert(c);
    } else {
      c.setVersion(null);
      c.setCoverId(null);
      int n =
          contents.update(
              c,
              new UpdateWrapper<Content>()
                  .eq("id", id)
                  .eq("version", in.version())
                  .set("cover_id", in.coverId())
                  .set("version", in.version() + 1));
      if (n != 1) throw new ResponseStatusException(HttpStatus.CONFLICT, "内容已被其他管理员修改，请刷新后再编辑");
    }
    db.update("DELETE FROM content_media WHERE content_id=?", c.getId());
    refs.forEach(
        ref ->
            db.update(
                "INSERT INTO content_media(content_id,media_id) VALUES(?,?)", c.getId(), ref));
    audit.record(id == null ? "CREATE_CONTENT" : "UPDATE_CONTENT", c.getId());
    return getContent(c.getId(), true);
  }

  @Transactional
  @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
  public Content transition(long id, String action, int version) {
    var c = getContent(id, true);
    String next =
        switch (action) {
          case "publish" -> "PUBLISHED";
          case "unpublish", "restore" -> "DRAFT";
          case "trash" -> "TRASH";
          default -> throw new IllegalArgumentException();
        };
    if (("TRASH".equals(c.getStatus()) && !action.equals("restore"))
        || (action.equals("restore") && !c.getStatus().equals("TRASH"))) bad("当前状态不支持此操作");
    if (c.getType().equals("PAGE") && action.equals("trash")) bad("固定页面不能移入回收站");
    var update =
        new UpdateWrapper<Content>()
            .eq("id", id)
            .eq("version", version)
            .set("status", next)
            .set("version", version + 1)
            .set("updated_at", LocalDateTime.now());
    if (next.equals("PUBLISHED")) update.set("published_at", LocalDateTime.now());
    if (contents.update(null, update) != 1)
      throw new ResponseStatusException(HttpStatus.CONFLICT, "内容已变化，请刷新重试");
    audit.record(action.toUpperCase() + "_CONTENT", id);
    return getContent(id, true);
  }

  public static void bad(String message) {
    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
  }
}
