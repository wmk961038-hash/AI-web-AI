package cn.aiclub;

import org.springframework.boot.SpringApplication;

/** Local UI smoke testing only. This class and H2 are excluded from the production jar. */
public class PreviewApplication {
  public static void main(String[] args) {
    var app = new SpringApplication(ClubApplication.class);
    app.setAdditionalProfiles("test");
    app.run(
        "--spring.datasource.url=jdbc:h2:mem:preview;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "--spring.datasource.username=sa",
        "--spring.datasource.password=",
        "--club.bootstrap-user=test-super",
        "--club.bootstrap-password=a-test-password-12345");
  }
}
