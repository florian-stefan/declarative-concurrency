package declarative_concurrency;

import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.time.Duration.ofSeconds;
import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.stream.Collectors.toList;

@Slf4j
public class BackpressureExample {

  private List<String> cwids;

  @Before
  public void setUp() throws Exception {
    cwids = Stream
      .generate(() -> UUID.randomUUID().toString())
      .limit(5000)
      .collect(Collectors.toList());
  }

  @Test(expected = RejectedExecutionException.class)
  public void shouldLoadUsersWithoutBackpressure() {
    ExecutorService executorService = createExecutorService(10);

    log.info("Starting to schedule fetching users");
    List<Future<User>> eventualUsers = cwids.stream()
      .map(cwid -> executorService.submit(() -> fetchUser(cwid)))
      .collect(toList());
    log.info("Finished to schedule fetching users");

    log.info("Waiting for fetching users to complete");
    List<User> users = eventualUsers.stream()
      .map(eventualUser -> {
        try {
          return eventualUser.get();
        } catch (Exception e) {
          throw new RuntimeException(e);
        }
      })
      .collect(toList());
    log.info("Fetching users completed");

    users.forEach(user -> log.info("Fetched user {}", user));
  }

  @Test
  public void shouldLoadUsersByUsingBackpressure() {
    Scheduler scheduler = Schedulers.fromExecutor(createExecutorService(10));

    Flux<User> eventualUsers = Flux.fromIterable(cwids)
      .flatMap(cwid -> Mono.fromCallable(() -> fetchUser(cwid))
        .timeout(ofSeconds(2))
        .subscribeOn(scheduler), 10);

    log.info("Starting to fetch users");
    List<User> users = eventualUsers
      .collectList()
      .block();
    log.info("Finished to fetch users");

    users.forEach(user -> log.info("Fetched user {}", user));
  }

  private ExecutorService createExecutorService(int nThreads) {
    return new ThreadPoolExecutor(nThreads, nThreads,
      0L, TimeUnit.MILLISECONDS,
      new LinkedBlockingQueue<>(500));
  }

  private User fetchUser(String cwid) {
    log.info("Starting to fetch user with cwid = {}", cwid);
    sleepForOneSecond();
    log.info("Finished to fetch user with cwid = {}", cwid);

    return new User(cwid, "");
  }

  private void sleepForOneSecond() {
    try {
      SECONDS.sleep(1);
    } catch (InterruptedException ignored) {
    }
  }

}
