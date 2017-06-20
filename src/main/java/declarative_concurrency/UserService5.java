package declarative_concurrency;

import javaslang.control.Either;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.function.Function;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.concurrent.CompletableFuture.supplyAsync;
import static java.util.concurrent.Executors.newFixedThreadPool;
import static javaslang.control.Either.left;

@Slf4j
@Component
@RequiredArgsConstructor
@SuppressWarnings("Duplicates")
public class UserService5 implements UserService {

  private final UserClient userClient;

  @Override
  public List<Either<String, User>> loadUsers(List<String> cwids) {
    ExecutorService executorService = newFixedThreadPool(10);

    log.info("Starting to schedule fetching users");
    Future<List<Either<String, User>>> eventualUsers = cwids.stream()
      .map(cwid -> supplyAsync(() -> fetchUser(cwid), executorService)
        .thenApply((Function<User, Either<String, User>>) Either::right)
        .exceptionally(error -> {
          log.error("An error occurred while fetching user with cwid = {}", cwid);

          return left(cwid);
        }))
      .reduce(
        completedFuture(new ArrayList<>()),
        this::combine,
        this::combineAll
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

  private <T> CompletableFuture<List<T>> combine(CompletableFuture<List<T>> xs,
                                                 CompletableFuture<T> x) {
    return xs.thenCombine(x, (as, a) -> {
      as.add(a);
      return as;
    });
  }

  private <T> CompletableFuture<List<T>> combineAll(CompletableFuture<List<T>> xs,
                                                    CompletableFuture<List<T>> ys) {
    return xs.thenCombine(ys, (as, bs) -> {
      as.addAll(bs);
      return as;
    });
  }

  private User fetchUser(String cwid) {
    log.info("Starting to fetch user with cwid = {}", cwid);
    User user = userClient.fetchUser(cwid);
    log.info("Finished to fetch user with cwid = {}", cwid);

    return user;
  }

}
