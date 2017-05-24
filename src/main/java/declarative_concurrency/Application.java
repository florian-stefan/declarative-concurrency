package declarative_concurrency;

import declarative_concurrency.part_i.User;
import declarative_concurrency.part_i.UserService;
import javaslang.control.Either;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.List;
import java.util.function.Consumer;

import static java.util.Arrays.asList;

@Slf4j
@SpringBootApplication
@RequiredArgsConstructor
public class Application implements CommandLineRunner {

  private static final List<String> CWIDS = asList("1", "2", "3", "4", "5", "6", "7", "8", "9");
  private static final Consumer<Either<String, User>> PRINT_RESULT = maybeUser -> log.info("result = {}", maybeUser);

  private final List<UserService> userServices;

  public static void main(String[] args) {
    SpringApplication.run(Application.class, args);
  }

  @Override
  public void run(String... strings) throws Exception {
    userServices.forEach(userService -> {
      log.info("Starting to execute {}", userService.getClass().getCanonicalName());
      userService.loadUsers(CWIDS).forEach(PRINT_RESULT);
      log.info("Finished to execute {}", userService.getClass().getCanonicalName());
    });
  }

}
