package cn.aiclub;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import cn.aiclub.mapper.*;
import cn.aiclub.model.*;
import com.fasterxml.jackson.databind.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class WorkflowTest {
  @Autowired MockMvc mvc;
  @Autowired ObjectMapper json;
  @Autowired AccountMapper accounts;
  @Autowired ContentMapper contents;
  @Autowired PasswordEncoder encoder;
  static final String PASSWORD = "a-test-password-12345";

  String account(String role) {
    String name = "u" + UUID.randomUUID().toString().replace("-", "");
    var a = new Account();
    a.setUsername(name);
    a.setPasswordHash(encoder.encode(PASSWORD));
    a.setRole(role);
    a.setEnabled(true);
    a.setAuthVersion(0);
    accounts.insert(a);
    return name;
  }

  MockHttpSession login(String username) throws Exception {
    return login(username, PASSWORD);
  }

  MockHttpSession login(String username, String password) throws Exception {
    return (MockHttpSession)
        mvc.perform(
                post("/api/auth/login")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        json.writeValueAsString(
                            Map.of("username", username, "password", password))))
            .andExpect(status().isOk())
            .andReturn()
            .getRequest()
            .getSession(false);
  }

  Map<String, Object> input(String type) {
    var in = new HashMap<String, Object>();
    in.put("type", type);
    in.put("title", "第一版回归内容");
    in.put("category", "测试分类");
    in.put("summary", "回归测试");
    in.put(
        "body",
        "<h2>正文</h2><p>测试<script>alert(1)</script><a href='javascript:alert(1)'>链接</a></p>");
    in.put("metadata", Map.of("gallery", List.of()));
    in.put("sortOrder", 0);
    in.put("featured", false);
    in.put("pinned", false);
    in.put("archived", false);
    in.put("version", 0);
    return in;
  }

  JsonNode create(MockHttpSession session, Map<String, Object> in) throws Exception {
    return json.readTree(
        mvc.perform(
                post("/api/admin/contents")
                    .session(session)
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json.writeValueAsString(in)))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString());
  }

  JsonNode change(MockHttpSession session, long id, Map<String, Object> in) throws Exception {
    return json.readTree(
        mvc.perform(
                put("/api/admin/contents/" + id)
                    .session(session)
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json.writeValueAsString(in)))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString());
  }

  JsonNode transition(MockHttpSession session, long id, String action, int version)
      throws Exception {
    return json.readTree(
        mvc.perform(
                post("/api/admin/contents/" + id + "/" + action)
                    .session(session)
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json.writeValueAsString(Map.of("version", version))))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString());
  }

  long upload(MockHttpSession session) throws Exception {
    var bytes = new ByteArrayOutputStream();
    ImageIO.write(new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB), "png", bytes);
    return json.readTree(
            mvc.perform(
                    multipart("/api/admin/media")
                        .file(
                            new MockMultipartFile(
                                "file", "test.png", "image/png", bytes.toByteArray()))
                        .session(session)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString())
        .get("id")
        .asLong();
  }

  @Test
  void pinnedContentComesFirstThenUploadTimeDescending() throws Exception {
    LocalDateTime now = LocalDateTime.now();
    Content pinnedOld = orderFixture("置顶旧内容", 99, true, now.minusDays(3));
    Content ordinaryNew = orderFixture("普通新内容", 0, false, now.minusDays(1));
    Content ordinaryOlder = orderFixture("普通旧内容", 1, false, now.minusDays(2));
    contents.insert(pinnedOld);
    contents.insert(ordinaryNew);
    contents.insert(ordinaryOlder);

    mvc.perform(
            get("/api/public/contents")
                .param("type", "NEWS")
                .param("category", "上传时间排序测试"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items[0].title").value("置顶旧内容"))
        .andExpect(jsonPath("$.items[1].title").value("普通新内容"))
        .andExpect(jsonPath("$.items[2].title").value("普通旧内容"));
  }

  @Test
  void adminContentListSupportsServerPaginationAndFilters() throws Exception {
    var session = login(account("ADMIN"));
    String stamp = UUID.randomUUID().toString().replace("-", "");
    String category = "分页测试-" + stamp;
    for (int i = 1; i <= 3; i++) {
      var in = input("NEWS");
      in.put("title", "分页搜索-" + stamp + "-" + i);
      in.put("category", category);
      create(session, in);
    }

    mvc.perform(
            get("/api/admin/contents")
                .session(session)
                .param("category", category)
                .param("page", "1")
                .param("size", "2"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.total").value(3))
        .andExpect(jsonPath("$.page").value(1))
        .andExpect(jsonPath("$.size").value(2))
        .andExpect(jsonPath("$.items.length()").value(2));

    mvc.perform(
            get("/api/admin/contents")
                .session(session)
                .param("category", category)
                .param("page", "2")
                .param("size", "2"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.total").value(3))
        .andExpect(jsonPath("$.items.length()").value(1));

    mvc.perform(
            get("/api/admin/contents")
                .session(session)
                .param("category", category)
                .param("keyword", stamp))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.total").value(3));

    mvc.perform(
            get("/api/admin/contents")
                .session(session)
                .param("category", category)
                .param("dateFrom", LocalDate.now().plusDays(1).toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.total").value(0));

    mvc.perform(
            get("/api/admin/contents")
                .session(session)
                .param("category", category)
                .param("dateTo", LocalDate.now().minusDays(1).toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.total").value(0));

    mvc.perform(
            get("/api/admin/contents")
                .session(session)
                .param("category", category)
                .param("dateFrom", "not-a-date"))
        .andExpect(status().isBadRequest());

    mvc.perform(get("/api/admin/contents").session(session).param("page", "0"))
        .andExpect(status().isBadRequest());
    mvc.perform(get("/api/admin/contents").session(session).param("size", "101"))
        .andExpect(status().isBadRequest());
  }

  Content orderFixture(String title, int sortOrder, boolean pinned, LocalDateTime createdAt) {
    var c = new Content();
    c.setType("NEWS");
    c.setTitle(title);
    c.setCategory("上传时间排序测试");
    c.setSummary("排序测试");
    c.setBody("<p>排序测试</p>");
    c.setMetadata("{}");
    c.setStatus("PUBLISHED");
    c.setSortOrder(sortOrder);
    c.setFeatured(false);
    c.setPinned(pinned);
    c.setArchived(false);
    c.setVersion(0);
    c.setCreatedAt(createdAt);
    c.setUpdatedAt(createdAt);
    c.setPublishedAt(createdAt);
    return c;
  }

  @Test
  void bothRolesCanEditPreviewPublishUnpublishTrashAndRestore() throws Exception {
    for (String role : List.of("ADMIN", "SUPER_ADMIN")) {
      var session = login(account(role));
      var in = input("NEWS");
      long id = create(session, in).get("id").asLong();
      mvc.perform(get("/api/admin/contents").session(session)).andExpect(status().isOk());
      mvc.perform(get("/api/admin/contents/" + id).session(session)).andExpect(status().isOk());
      mvc.perform(get("/api/public/contents/" + id)).andExpect(status().isNotFound());
      in.put("title", "修改草稿");
      var c = change(session, id, in);
      assertThat(c.get("version").asInt()).isEqualTo(1);
      transition(session, id, "publish", 1);
      mvc.perform(get("/api/public/contents/" + id)).andExpect(status().isOk());
      in.put("version", 2);
      in.put("title", "已发布内容更新");
      change(session, id, in);
      mvc.perform(get("/api/public/contents/" + id))
          .andExpect(jsonPath("$.title").value("已发布内容更新"));
      mvc.perform(
              put("/api/admin/contents/" + id)
                  .session(session)
                  .with(csrf())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(json.writeValueAsString(in)))
          .andExpect(status().isConflict());
      transition(session, id, "unpublish", 3);
      mvc.perform(get("/api/public/contents/" + id)).andExpect(status().isNotFound());
      transition(session, id, "trash", 4);
      mvc.perform(get("/api/admin/contents").session(session).param("status", "TRASH"))
          .andExpect(status().isOk());
      assertThat(transition(session, id, "restore", 5).get("status").asText()).isEqualTo("DRAFT");
      mvc.perform(get("/api/admin/audit").session(session)).andExpect(status().isOk());
    }
  }

  @Test
  void bothRolesCanMaintainFixedPagesAndDisplaySettings() throws Exception {
    var fixture = new Content();
    fixture.setType("PAGE");
    fixture.setTitle("固定测试页面");
    fixture.setCategory("test-page");
    fixture.setSummary("摘要");
    fixture.setBody("<p>原文</p>");
    fixture.setMetadata("{}");
    fixture.setStatus("PUBLISHED");
    fixture.setVersion(0);
    fixture.setSortOrder(0);
    fixture.setFeatured(false);
    fixture.setPinned(false);
    fixture.setArchived(false);
    contents.insert(fixture);
    int version = 0;
    for (String role : List.of("ADMIN", "SUPER_ADMIN")) {
      var session = login(account(role));
      long image = upload(session);
      var in = input("PAGE");
      in.put("category", "test-page");
      in.put("body", "<h2>修改后的页面</h2><p>新正文</p><img src='/api/media/" + image + "'>");
      in.put("coverId", image);
      in.put("version", version);
      in.put("metadata", Map.of("joinUrl", "https://example.org/apply"));
      var c = change(session, fixture.getId(), in);
      version = c.get("version").asInt();
      assertThat(c.get("coverId").asLong()).isEqualTo(image);
      mvc.perform(get("/api/public/pages/test-page"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.body").value(org.hamcrest.Matchers.containsString("修改后的页面")));
      mvc.perform(get("/api/media/" + image)).andExpect(status().isOk());
      mvc.perform(
              post("/api/admin/contents/" + fixture.getId() + "/trash")
                  .session(session)
                  .with(csrf())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(json.writeValueAsString(Map.of("version", version))))
          .andExpect(status().isBadRequest());
      var person = input("PERSON");
      person.put("category", "社团骨干");
      person.put("personRole", "社长 / 计算机视觉");
      person.put("body", "<p>人物简介：负责社团计算机视觉方向。</p>");
      person.put("coverId", upload(session));
      person.put("archived", true);
      person.put("sortOrder", -2);
      person.put("metadata", Map.of("cohort", "2026"));
      long personId = create(session, person).get("id").asLong();
      transition(session, personId, "publish", 0);
      mvc.perform(
              get("/api/public/contents")
                  .param("type", "PERSON")
                  .param("archived", "true")
                  .param("cohort", "2026"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.total").value(org.hamcrest.Matchers.greaterThan(0)));
      mvc.perform(get("/api/public/contents/" + personId))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.personRole").value("社长 / 计算机视觉"));
      mvc.perform(get("/api/public/people/categories"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$", org.hamcrest.Matchers.hasItem("社团骨干")));
      var project = input("PROJECT");
      project.put("featured", true);
      project.put("pinned", true);
      project.put("sortOrder", -1);
      long projectId = create(session, project).get("id").asLong();
      transition(session, projectId, "publish", 0);
      mvc.perform(get("/api/public/contents").param("type", "PROJECT").param("featured", "true"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.total").value(org.hamcrest.Matchers.greaterThan(0)));
    }
  }

  @Test
  void personRequiresFiveFieldsAndExposesPublishedCategories() throws Exception {
    var session = login(account("ADMIN"));
    long image = upload(session);
    var person = input("PERSON");
    person.put("category", "测试人物类别");
    person.put("personRole", "研究员 / 多模态");
    person.put("body", "<p>这是一段人物简介。</p>");
    person.put("coverId", image);

    var missingRole = new HashMap<>(person);
    missingRole.remove("personRole");
    mvc.perform(
            post("/api/admin/contents")
                .session(session)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(missingRole)))
        .andExpect(status().isBadRequest());

    var missingImage = new HashMap<>(person);
    missingImage.remove("coverId");
    mvc.perform(
            post("/api/admin/contents")
                .session(session)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(missingImage)))
        .andExpect(status().isBadRequest());

    var emptyBio = new HashMap<>(person);
    emptyBio.put("body", "<p> </p>");
    mvc.perform(
            post("/api/admin/contents")
                .session(session)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(emptyBio)))
        .andExpect(status().isBadRequest());

    var commaCategory = new HashMap<>(person);
    commaCategory.put("category", "甲,乙");
    mvc.perform(
            post("/api/admin/contents")
                .session(session)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(commaCategory)))
        .andExpect(status().isBadRequest());

    long id = create(session, person).get("id").asLong();
    String draftCategories =
        mvc.perform(get("/api/public/people/categories"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    assertThat(draftCategories).doesNotContain("测试人物类别");
    String adminCategories =
        mvc.perform(get("/api/admin/people/categories").session(session))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    assertThat(adminCategories).contains("测试人物类别");

    transition(session, id, "publish", 0);
    String publishedCategories =
        mvc.perform(get("/api/public/people/categories"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    assertThat(publishedCategories).contains("测试人物类别");
  }

  @Test
  void onlySuperAdminManagesAccountsAndChangesRevokeSessions() throws Exception {
    var root = login(account("SUPER_ADMIN"));
    var admin = login(account("ADMIN"));
    String name = "managed" + UUID.randomUUID().toString().replace("-", "");
    var c =
        json.readTree(
            mvc.perform(
                    post("/api/admin/accounts")
                        .session(root)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                            json.writeValueAsString(
                                Map.of("username", name, "password", PASSWORD, "role", "ADMIN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andReturn()
                .getResponse()
                .getContentAsString());
    long id = c.get("id").asLong();
    var editor = login(name);
    mvc.perform(
            get("/api/admin/accounts")
                .session(root)
                .param("page", "1")
                .param("size", "1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.page").value(1))
        .andExpect(jsonPath("$.size").value(1))
        .andExpect(jsonPath("$.items.length()").value(1));
    mvc.perform(get("/api/admin/accounts").session(root).param("size", "101"))
        .andExpect(status().isBadRequest());
    mvc.perform(get("/api/admin/accounts").session(admin)).andExpect(status().isForbidden());
    mvc.perform(
            post("/api/admin/accounts/" + id + "/password")
                .session(admin)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("password", "new-password-12345"))))
        .andExpect(status().isForbidden());
    mvc.perform(
            put("/api/admin/accounts/" + id)
                .session(root)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("enabled", true, "role", "SUPER_ADMIN"))))
        .andExpect(status().isOk());
    mvc.perform(get("/api/auth/me").session(editor)).andExpect(status().isUnauthorized());
    editor = login(name);
    mvc.perform(get("/api/admin/accounts").session(editor)).andExpect(status().isOk());
    mvc.perform(
            post("/api/admin/accounts/" + id + "/password")
                .session(root)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("password", "new-password-12345"))))
        .andExpect(status().isOk());
    mvc.perform(get("/api/auth/me").session(editor)).andExpect(status().isUnauthorized());
    editor = login(name, "new-password-12345");
    mvc.perform(
            put("/api/admin/accounts/" + id)
                .session(root)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("enabled", false, "role", "ADMIN"))))
        .andExpect(status().isOk());
    mvc.perform(get("/api/auth/me").session(editor)).andExpect(status().isUnauthorized());
  }

  @Test
  void bothRolesCanChangeOwnPasswordAndLogout() throws Exception {
    for (String role : List.of("ADMIN", "SUPER_ADMIN")) {
      String name = account(role);
      var first = login(name);
      var second = login(name);
      mvc.perform(
              post("/api/auth/password")
                  .session(first)
                  .with(csrf())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(
                      json.writeValueAsString(
                          Map.of("oldPassword", "wrong", "newPassword", "updated-password-12345"))))
          .andExpect(status().isBadRequest());
      mvc.perform(
              post("/api/auth/password")
                  .session(first)
                  .with(csrf())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(
                      json.writeValueAsString(
                          Map.of(
                              "oldPassword", PASSWORD, "newPassword", "updated-password-12345"))))
          .andExpect(status().isOk());
      mvc.perform(get("/api/auth/me").session(second)).andExpect(status().isUnauthorized());
      var fresh = login(name, "updated-password-12345");
      mvc.perform(post("/api/auth/logout").session(fresh).with(csrf())).andExpect(status().isOk());
      mvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());
    }
  }

  @Test
  void draftImagesRemainPrivateToVisitorsAndAvailableToBothAdminRoles() throws Exception {
    var uploader = login(account("ADMIN"));
    var other = login(account("ADMIN"));
    var root = login(account("SUPER_ADMIN"));
    long image = upload(uploader);
    mvc.perform(get("/api/media/" + image)).andExpect(status().isNotFound());
    for (var s : List.of(uploader, other, root))
      mvc.perform(get("/api/media/" + image).session(s)).andExpect(status().isOk());
    var in = input("ACTIVITY");
    in.put("coverId", image);
    in.put("body", "<p>照片</p><img src='/api/media/" + image + "'>");
    long id = create(other, in).get("id").asLong();
    mvc.perform(get("/api/media/" + image)).andExpect(status().isNotFound());
    transition(other, id, "publish", 0);
    mvc.perform(get("/api/media/" + image))
        .andExpect(status().isOk())
        .andExpect(header().string("Cache-Control", "no-store"));
    in.put("version", 1);
    in.put("coverId", null);
    in.put("body", "<p>移除图片引用</p>");
    change(other, id, in);
    assertThat(contents.selectById(id).getCoverId()).isNull();
    mvc.perform(get("/api/media/" + image)).andExpect(status().isNotFound());
  }

  @Test
  void csrfSanitizationAndInputValidationStillApply() throws Exception {
    var admin = login(account("ADMIN"));
    var in = input("NEWS");
    mvc.perform(
            post("/api/admin/contents")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(in)))
        .andExpect(status().isUnauthorized());
    mvc.perform(
            post("/api/admin/contents")
                .session(admin)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(in)))
        .andExpect(status().isForbidden());
    assertThat(create(admin, in).get("body").asText()).doesNotContain("<script", "javascript:");
    in.put("metadata", Map.of("repository", "javascript:alert(1)"));
    mvc.perform(
            post("/api/admin/contents")
                .session(admin)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(in)))
        .andExpect(status().isBadRequest());
    mvc.perform(
            multipart("/api/admin/media")
                .file(
                    new MockMultipartFile(
                        "file", "fake.png", "image/png", "not-an-image".getBytes()))
                .session(admin)
                .with(csrf()))
        .andExpect(status().isBadRequest());
    mvc.perform(get("/api/admin/audit").session(admin).param("page", "0"))
        .andExpect(status().isBadRequest());
    mvc.perform(
            post("/api/admin/contents")
                .session(admin)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(input("PAGE"))))
        .andExpect(status().isBadRequest());
  }
}
