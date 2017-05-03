package declarative_concurrency.user;

import javaslang.Tuple;
import javaslang.Tuple2;
import javaslang.control.Either;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.function.Function;

import static java.time.Duration.ofSeconds;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.concurrent.CompletableFuture.supplyAsync;
import static java.util.concurrent.Executors.newFixedThreadPool;
import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.stream.Collectors.toList;
import static javaslang.control.Either.left;
import static javaslang.control.Either.right;

@SuppressWarnings("Duplicates")
@Slf4j
@Component
@RequiredArgsConstructor
public class UserService {

  private final UserClient userClient;

  private User fetchUser(String cwid) {
    log.info("Starting to fetch user with cwid = {}", cwid);
    User user = userClient.fetchUser(cwid);
    log.info("Finished to fetch user with cwid = {}", cwid);

    return user;
  }

  public List<Either<String, User>> loadUsersSequential(List<String> cwids) {
    log.info("Starting to fetch users");
    List<Either<String, User>> maybeUsers = cwids.stream()
      .map(cwid -> getUserAsEither(cwid, () -> fetchUser(cwid)))
      .collect(toList());
    log.info("Finished to fetch users");

    return maybeUsers;
  }

  // no control over thread pool
  public List<Either<String, User>> loadUsersParallelStream(List<String> cwids) {
    log.info("Starting to fetch users");
    List<Either<String, User>> maybeUsers = cwids.parallelStream()
      .map(cwid -> getUserAsEither(cwid, () -> fetchUser(cwid)))
      .collect(toList());
    log.info("Finished to fetch users");

    return maybeUsers;
  }

  // no control over individual tasks
  public List<Either<String, User>> loadUsersForkJoinPool(List<String> cwids) {
    ForkJoinPool forkJoinPool = new ForkJoinPool(10);

    log.info("Starting to schedule fetching users");
    Future<List<Either<String, User>>> eventualUsers = forkJoinPool.submit(() -> cwids.parallelStream()
      .map(cwid -> getUserAsEither(cwid, () -> fetchUser(cwid)))
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

  public List<Either<String, User>> loadUsersExecutorService(List<String> cwids) {
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

  public List<Either<String, User>> loadUsersCompletableFuture(List<String> cwids) {
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
        this::combineListAndElement,
        this::combineListAndElements
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

  public List<Either<String, User>> loadUsersFlux(List<String> cwids) {
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

  private <T> CompletableFuture<List<T>> combineListAndElement(CompletableFuture<List<T>> eventualList,
                                                               CompletableFuture<T> eventualElement) {
    return eventualList.thenCombine(eventualElement, (list, element) -> {
      list.add(element);
      return list;
    });
  }

  private <T> CompletableFuture<List<T>> combineListAndElements(CompletableFuture<List<T>> eventualList,
                                                                CompletableFuture<List<T>> eventualElements) {
    return eventualList.thenCombine(eventualElements, (list, elements) -> {
      list.addAll(elements);
      return list;
    });
  }

}
