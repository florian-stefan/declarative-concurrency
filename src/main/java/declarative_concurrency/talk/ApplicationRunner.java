package declarative_concurrency.talk;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Component
@RequiredArgsConstructor
public class ApplicationRunner implements CommandLineRunner {

  private final UserService1 userService;

  @Override
  public void run(String... strings) throws Exception {
    List<String> cwids = Stream
      .generate(() -> UUID.randomUUID().toString())
      .limit(5000)
      .collect(Collectors.toList());

    List<User> users = userService.loadUsers(cwids);

    users.forEach(user -> log.info("Got {}", user));
  }

}
