package declarative_concurrency.part_i;

import javaslang.control.Either;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

import static java.util.stream.Collectors.toList;
import static javaslang.control.Either.left;
import static javaslang.control.Either.right;

@Slf4j
@Component
@RequiredArgsConstructor
@SuppressWarnings("Duplicates")
public class UserService1 implements UserService {

  private final UserClient userClient;

  @Override
  public List<Either<String, User>> loadUsers(List<String> cwids) {
    log.info("Starting to fetch users");
    List<Either<String, User>> maybeUsers = cwids.stream()
      .map(this::fetchUserAsEither)
      .collect(toList());
    log.info("Finished to fetch users");

    return maybeUsers;
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
