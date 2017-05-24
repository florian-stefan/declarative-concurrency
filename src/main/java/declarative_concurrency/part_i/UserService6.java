package declarative_concurrency.part_i;

import javaslang.control.Either;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.util.List;
import java.util.function.Function;

import static java.time.Duration.ofSeconds;
import static java.util.concurrent.Executors.newFixedThreadPool;
import static java.util.stream.Collectors.toList;
import static javaslang.control.Either.left;

@Slf4j
@Component
@RequiredArgsConstructor
@SuppressWarnings("Duplicates")
public class UserService6 implements UserService {

  private final UserClient userClient;

  @Override
  public List<Either<String, User>> loadUsers(List<String> cwids) {
    Scheduler scheduler = Schedulers.fromExecutor(newFixedThreadPool(10));

    Flux<Either<String, User>> eventualUsers = Flux.fromIterable(cwids)
      .flatMap(cwid -> Mono.fromCallable(() -> fetchUser(cwid))
        .timeout(ofSeconds(2))
        .map((Function<User, Either<String, User>>) Either::right)
        .doOnError(error -> log.error("An error occurred while fetching user with cwid = {}", cwid))
        .otherwiseReturn(left(cwid))
        .subscribeOn(scheduler));

    log.info("Starting to fetch users");
    List<Either<String, User>> maybeUsers = eventualUsers
      .toStream()
      .collect(toList());
    log.info("Finished to fetch users");

    return maybeUsers;
  }

  private User fetchUser(String cwid) {
    log.info("Starting to fetch user with cwid = {}", cwid);
    User user = userClient.fetchUser(cwid);
    log.info("Finished to fetch user with cwid = {}", cwid);

    return user;
  }

}
