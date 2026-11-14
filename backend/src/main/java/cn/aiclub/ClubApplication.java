package cn.aiclub;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(
    exclude =
        org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration
            .class)
@MapperScan("cn.aiclub.mapper")
public class ClubApplication {
  public static void main(String[] args) {
    SpringApplication.run(ClubApplication.class, args);
  }
}
