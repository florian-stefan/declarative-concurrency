package declarative_concurrency;

import javaslang.control.Either;
import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import static java.lang.Integer.MAX_VALUE;
import static java.time.Duration.ofMillis;
import static java.util.Arrays.asList;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;
import static javaslang.control.Either.left;
import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
public class TimeoutAndRetryExample {

  private static final List<String> CWIDS = asList("1", "2", "3");
  private static final String CWID = CWIDS.get(0);

  private Map<String, AtomicInteger> atomicIntegers;

  @Before
  public void setUp() throws Exception {
    atomicIntegers = CWIDS.stream().collect(toMap(cwid -> cwid, cwid -> new AtomicInteger()));
  }

  private Mono<User> fetchUserWithRetry(int numRetries) {
    return Mono.fromCallable(() -> loadUser(CWID))
      .timeout(ofMillis(250))
      .doOnError(error -> log.error("Failed to load user", error))
      .retry(numRetries);
  }

  @Test
  public void shouldEmitUserApiNotAvailableException() {
    Mono<User> maybeUser = fetchUserWithRetry(1);

    StepVerifier.create(maybeUser)
      .expectError(UserApiNotAvailableException.class)
      .verify();
  }

  @Test
  public void shouldEmitTimeoutException() {
    Mono<User> maybeUser = fetchUserWithRetry(2);

    StepVerifier.create(maybeUser)
      .expectError(TimeoutException.class)
      .verify();
  }

  @Test
  public void shouldEmitUser() {
    Mono<User> maybeUser = fetchUserWithRetry(4);

    StepVerifier.create(maybeUser)
      .expectNextCount(1)
      .expectComplete()
      .verify();
  }

  private Mono<User> fetchUserWithRetryAndExponentialBackoff(int numRetries) {
    return Mono.fromCallable(() -> loadUser(CWID))
      .timeout(ofMillis(250))
      .retryWhen(errors -> errors.zipWith(Flux.range(0, MAX_VALUE), (error, index) -> {
        if (index < numRetries) {
          long timeToWait = (long) Math.pow(10, index);
          return Mono.just(error).delaySubscription(ofMillis(timeToWait));
        } else {
          return Mono.error(error);
        }
      }).flatMap(identity()));
  }

  @Test
  public void shouldEmitUserApiNotAvailableExceptionWithExponentialBackoff() {
    Mono<User> maybeUser = fetchUserWithRetryAndExponentialBackoff(1);

    StepVerifier.create(maybeUser)
      .expectError(UserApiNotAvailableException.class)
      .verify();
  }

  @Test
  public void shouldEmitTimeoutExceptionWithExponentialBackoff() {
    Mono<User> maybeUser = fetchUserWithRetryAndExponentialBackoff(2);

    StepVerifier.create(maybeUser)
      .expectError(TimeoutException.class)
      .verify();
  }

  @Test
  public void shouldEmitUserWithExponentialBackoff() {
    Mono<User> maybeUser = fetchUserWithRetryAndExponentialBackoff(4);

    StepVerifier.create(maybeUser)
      .expectNextCount(1)
      .expectComplete()
      .verify();
  }

  @Test
  public void shouldEmitUsersWithExponentialBackoff() {
    Flux<Either<String, User>> users = Flux.fromIterable(CWIDS)
      .flatMap(cwid -> Mono.fromCallable(() -> loadUser(cwid))
        .timeout(ofMillis(250), Schedulers.single())
        .retryWhen(errors -> errors.zipWith(Flux.range(0, MAX_VALUE), (error, index) -> {
          if (index < 3) {
            return Mono.just(error).delayElement(withExponentialBackoff(index));
          } else {
            return Mono.error(error);
          }
        }).flatMap(identity()))
        .map(Either::<String, User>right)
        .otherwiseReturn(left(cwid))
        .subscribeOn(Schedulers.parallel()));

    assertThat(users.collectList().block()).containsExactlyInAnyOrder(left("1"), left("2"), left("3"));
  }

  private Duration withExponentialBackoff(int index) {
    long millis = (long) Math.pow(10, index);
    return ofMillis(millis);
  }

  private User loadUser(String cwid) {
    log.info("loadUser({})", cwid);
    switch (atomicIntegers.get(cwid).getAndIncrement()) {
      case 0:
      case 1:
        sleep(200);
        throw new UserApiNotAvailableException();
      case 2:
      case 3:
        sleep(500);
        return new User(cwid, "Tim Timeout");
      default:
        sleep(200);
        return new User(cwid, "Sam Success");
    }
  }

  private void sleep(int timeout) {
    try {
      TimeUnit.MILLISECONDS.sleep(timeout);
    } catch (InterruptedException e) {
      log.error("Failed to sleep.", e);
    }
  }

  private static class UserApiNotAvailableException extends RuntimeException {
  }

}
