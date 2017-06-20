package declarative_concurrency;

import javaslang.control.Either;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Future;

import static java.util.stream.Collectors.toList;
import static javaslang.control.Either.left;
import static javaslang.control.Either.right;

@Slf4j
@Component
@RequiredArgsConstructor
@SuppressWarnings("Duplicates")
public class UserService3 implements UserService {

  private final UserClient userClient;

  @Override
  public List<Either<String, User>> loadUsers(List<String> cwids) {
    ForkJoinPool forkJoinPool = new ForkJoinPool(10);

    log.info("Starting to schedule fetching users");
    Future<List<Either<String, User>>> eventualUsers = forkJoinPool.submit(() -> cwids.parallelStream()
      .map(this::fetchUserAsEither)
      .collect(toList())
    );
    log.info("Finished to schedule fetching users");

    log.info("Waiting for fetching users to complete");
    try {
      List<Either<String, User>> maybeUsers = eventualUsers.get();

      log.info("Fetching users completed");
      return maybeUsers;
    } catch (Exception error) {
      log.info("An error occurred while waiting for fetching users to complete", error);

      throw new RuntimeException(error);
    }
  }

  private Either<String, User> fetchUserAsEither(String cwid) {
    Either<String, User> maybeUser;

    try {
      maybeUser = right(fetchUser(cwid));
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
