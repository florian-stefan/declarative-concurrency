package declarative_concurrency;

import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

@Slf4j
public class SubscribeOnExample {

  private List<String> cwids;

  @Before
  public void setUp() throws Exception {
    cwids = Stream
      .generate(() -> UUID.randomUUID().toString())
      .limit(3)
      .collect(Collectors.toList());
  }

  @Test
  public void shouldCreateFluxAndSubscribe() throws InterruptedException {
    CountDownLatch countDownLatch = new CountDownLatch(1);

    log.info("Before Flux.create()");
    Flux<Integer> flux = Flux.<Integer>create(emitter -> {
      log.info("emitter.next({})", 1);
      emitter.next(1);
      log.info("emitter.complete()");
      emitter.complete();
    }).subscribeOn(Schedulers.parallel());
    log.info("After Flux.create()");

    log.info("Before Flux.subscribe()");
    flux.subscribe(
      next -> log.info("subscriber.onNext({})", next),
      error -> log.info("subscriber.onError({})", error),
      countDownLatch::countDown
    );
    log.info("After Flux.subscribe()");

    countDownLatch.await();
  }

  @Test
  public void shouldLoadUsersUsingFlatMapAndSubscribeOn() {
    log.info("Before Flux.fromIterable()");
    Flux<User> users = Flux
      .fromIterable(cwids)
      .flatMap(cwid -> Mono
        .fromCallable(() -> loadUser(cwid)))
      .subscribeOn(Schedulers.parallel());

    StepVerifier.create(users)
      .expectNextMatches(hasCwid(cwids.get(0)))
      .expectNextMatches(hasCwid(cwids.get(1)))
      .expectNextMatches(hasCwid(cwids.get(2)))
      .verifyComplete();
  }

  @Test
  public void shouldLoadUsersConcurrently() {
    log.info("Before Flux.fromIterable()");
    Flux<User> users = Flux
      .fromIterable(cwids)
      .flatMap(cwid -> Mono
        .fromCallable(() -> loadUser(cwid))
        .subscribeOn(Schedulers.parallel()));

    StepVerifier.create(users)
      .expectNextMatches(hasCwid(cwids.get(0)))
      .expectNextMatches(hasCwid(cwids.get(1)))
      .expectNextMatches(hasCwid(cwids.get(2)))
      .verifyComplete();
  }

  @Test
  public void shouldLoadUsersWithVirtualTime() {
    log.info("Before Flux.fromIterable()");
    Supplier<Flux<User>> users = () -> Flux
      .fromIterable(cwids)
      .flatMap(cwid -> Mono
        .fromCallable(() -> loadUser(cwid))
        .subscribeOn(Schedulers.parallel()));

    StepVerifier.withVirtualTime(users)
      .thenAwait()
      .expectNextMatches(hasCwid(cwids.get(0)))
      .expectNextMatches(hasCwid(cwids.get(1)))
      .expectNextMatches(hasCwid(cwids.get(2)))
      .verifyComplete();
  }

  @Test
  public void shouldLoadUsersUsingFlatMapSequentialAndSubscribeOn() {
    Flux<User> users = Flux
      .fromIterable(cwids)
      .flatMapSequential(cwid -> Mono.fromCallable(() -> loadUser(cwid))
        .subscribeOn(Schedulers.parallel()));

    StepVerifier.create(users)
      .expectNextMatches(hasCwid(cwids.get(0)))
      .expectNextMatches(hasCwid(cwids.get(1)))
      .expectNextMatches(hasCwid(cwids.get(2)))
      .expectComplete()
      .verify();
  }

  @Test
  public void shouldCreateWorker() throws InterruptedException {
    CountDownLatch countDownLatch = new CountDownLatch(2);

    Scheduler.Worker worker = Schedulers.parallel().createWorker();

    worker.schedule(() -> {
      log.info("Starting to execute first task");

      worker.schedule(() -> {
        log.info("Starting to execute second task");
        executeTask(countDownLatch);
        log.info("Finished to execute second task");
      });

      executeTask(countDownLatch);
      log.info("Finished to execute first task");
    });

    countDownLatch.await();
  }

  private User loadUser(String cwid) {
    log.info("loadUser({})", cwid);

    return new User(cwid, "");
  }

  private Predicate<User> hasCwid(String cwid) {
    return user -> Objects.equals(user.getCwid(), cwid);
  }

  private void executeTask(CountDownLatch countDownLatch) {
    try {
      MILLISECONDS.sleep(200);
      countDownLatch.countDown();
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

}
