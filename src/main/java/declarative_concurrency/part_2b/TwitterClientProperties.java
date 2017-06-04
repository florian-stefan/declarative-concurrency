package declarative_concurrency.part_2b;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "twitter")
public class TwitterClientProperties {

  private String apiKey;
  private String apiSecret;
  private String token;
  private String tokenSecret;
  private String track;

}
