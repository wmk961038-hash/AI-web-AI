package cn.aiclub.web;

import cn.aiclub.mapper.MediaMapper;
import cn.aiclub.model.MediaFile;
import cn.aiclub.security.ClubPrincipal;
import cn.aiclub.service.AuditService;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.util.UUID;
import javax.imageio.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class MediaController {
  private final MediaMapper media;
  private final JdbcTemplate db;
  private final AuditService audit;
  private final Path root;

  public MediaController(
      MediaMapper m, JdbcTemplate d, AuditService a, @Value("${club.uploads}") String dir) {
    media = m;
    db = d;
    audit = a;
    root = Path.of(dir).toAbsolutePath().normalize();
  }

  @PostMapping("/api/admin/media")
  @Transactional
  public Object upload(@RequestParam("file") MultipartFile file) throws Exception {
    if (file.isEmpty() || file.getSize() > 10 * 1024 * 1024)
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "图片需在 10MB 以内");
    BufferedImage image;
    try (var input = ImageIO.createImageInputStream(file.getInputStream())) {
      var readers = ImageIO.getImageReaders(input);
      if (!readers.hasNext())
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "仅支持有效的 JPEG/PNG 图片");
      var reader = readers.next();
      try {
        reader.setInput(input);
        String format = reader.getFormatName();
        if (!format.equalsIgnoreCase("JPEG") && !format.equalsIgnoreCase("PNG"))
          throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "仅支持 JPEG/PNG 图片");
        int w = reader.getWidth(0), h = reader.getHeight(0);
        if (w < 1 || h < 1 || (long) w * h > 24000000)
          throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "图片最多 2400 万像素");
        image = reader.read(0);
      } finally {
        reader.dispose();
      }
    }
    int w = image.getWidth(), h = image.getHeight();
    double scale = Math.min(1, 2400.0 / Math.max(w, h));

    var resized =
        new BufferedImage(
            Math.max(1, (int) (w * scale)),
            Math.max(1, (int) (h * scale)),
            BufferedImage.TYPE_INT_RGB);
    var g = resized.createGraphics();
    g.setColor(Color.WHITE);
    g.fillRect(0, 0, resized.getWidth(), resized.getHeight());
    g.setRenderingHint(
        RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
    g.drawImage(image, 0, 0, resized.getWidth(), resized.getHeight(), null);
    g.dispose();

    Files.createDirectories(root);
    String name = UUID.randomUUID() + ".jpg";
    Path path = root.resolve(name);
    ImageIO.write(resized, "JPEG", path.toFile());
    var item = new MediaFile();
    item.setUploadedBy(cn.aiclub.web.AuthController.principal().id());
    item.setStoredName(name);
    String original = file.getOriginalFilename();
    item.setOriginalName(
        original == null ? "image" : original.substring(0, Math.min(255, original.length())));
    item.setMimeType("image/jpeg");
    item.setByteSize(Files.size(path));
    try {
      media.insert(item);
      audit.record("UPLOAD_MEDIA", item.getId());
    } catch (Exception ex) {
      Files.deleteIfExists(path);
      throw ex;
    }
    return item;
  }

  @GetMapping("/api/media/{id}")
  public ResponseEntity<?> get(@PathVariable long id) {
    var auth = SecurityContextHolder.getContext().getAuthentication();
    var item = media.selectById(id);
    if (item == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "图片不存在");
    boolean permitted = auth != null && auth.getPrincipal() instanceof ClubPrincipal;
    if (!permitted
        && db.queryForObject(
                "SELECT COUNT(*) FROM content_media cm JOIN content c ON c.id=cm.content_id WHERE"
                    + " cm.media_id=? AND c.status='PUBLISHED'",
                Long.class,
                id)
            == 0) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "图片不存在");
    Path path = root.resolve(item.getStoredName()).normalize();
    if (!path.startsWith(root) || !Files.isRegularFile(path))
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "图片不存在");
    return ResponseEntity.ok()
        .contentType(MediaType.IMAGE_JPEG)
        .cacheControl(CacheControl.noStore())
        .header("X-Content-Type-Options", "nosniff")
        .body(new FileSystemResource(path));
  }
}
