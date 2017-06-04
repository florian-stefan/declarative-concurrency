package declarative_concurrency.part_1;

import javaslang.Tuple;
import javaslang.Tuple2;
import javaslang.control.Either;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import static java.util.concurrent.Executors.newFixedThreadPool;
import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.stream.Collectors.toList;
import static javaslang.control.Either.left;
import static javaslang.control.Either.right;

@Slf4j
@Component
@RequiredArgsConstructor
@SuppressWarnings("Duplicates")
public class UserService4 implements UserService {

  private final UserClient userClient;

  @Override
  public List<Either<String, User>> loadUsers(List<String> cwids) {
    ExecutorService executorService = newFixedThreadPool(10);

    log.info("Starting to schedule fetching users");
    List<Tuple2<String, Future<User>>> eventualUsers = cwids.stream()
      .map(cwid -> {
        Future<User> eventualUser = executorService.submit(() -> fetchUser(cwid));

        return Tuple.of(cwid, eventualUser);
      })
      .collect(toList());
    log.info("Finished to schedule fetching users");

    log.info("Waiting for fetching users to complete");
    List<Either<String, User>> maybeUsers = eventualUsers.stream()
      .map(cwidAndEventualUser -> {
        String cwid = cwidAndEventualUser._1;
        Future<User> eventualUser = cwidAndEventualUser._2;

        return getUserAsEither(cwid, () -> eventualUser.get(2, SECONDS));
      })
      .collect(toList());
    log.info("Fetching users completed");

    return maybeUsers;
  }

  private Either<String, User> getUserAsEither(String cwid, Callable<User> eventualUser) {
    Either<String, User> maybeUser;

    try {
      maybeUser = right(eventualUser.call());
    } catch (Exception error) {
      log.error("An error occurred while fetching user with cwid = {}", cwid);

      maybeUser = left(cwid);
    }

    return maybeUser;
  }

  private User fetchUser(String cwid) {
    log.info("Starting to fetch user with cwid = {}", cwid);
    User user = userClient.fetchUser(cwid);
    log.info("Finished to fetch user with cwid = {}", cwid);

    return user;
  }

}
