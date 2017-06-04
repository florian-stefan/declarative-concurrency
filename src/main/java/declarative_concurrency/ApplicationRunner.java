package declarative_concurrency;

import declarative_concurrency.part_2b.TwitterClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ApplicationRunner implements CommandLineRunner {

  private final TwitterClient twitterClient;

  @Override
  public void run(String... strings) throws Exception {
    twitterClient.stream().subscribe(
      tweet -> log.info("Received tweet: {}", tweet),
      error -> log.error("An error occurred.", error)
    );

  }

}
