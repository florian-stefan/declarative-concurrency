package declarative_concurrency;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.Iterator;
import java.util.function.Supplier;

import static java.util.Arrays.asList;

@Slf4j
public class FluxCreateExample {

  private static final String CWID = "1";

  @Test
  public void shouldEmitThreeEventsFollowedByCompletion() {
    Flux<Integer> flux = Flux.create(emitter -> {
      emitter.next(1);
      emitter.next(2);
      emitter.next(3);
      emitter.complete();
    });

    StepVerifier.create(flux)
      .expectNext(1, 2, 3)
      .expectComplete()
      .verify();
  }

  @Test
  public void shouldEmitThreeEventsCompletedByError() {
    Flux<Integer> flux = Flux.create(emitter -> {
      emitter.next(1);
      emitter.next(2);
      emitter.next(3);
      emitter.error(new RuntimeException());
    });

    StepVerifier.create(flux)
      .expectNext(1, 2, 3)
      .expectError()
      .verify();
  }

  @Test
  public void shouldCreateFluxAndSubscribe() {
    log.info("Before Flux.create()");
    Flux<Integer> flux = Flux.create(emitter -> {
      log.info("emitter.next({})", 1);
      emitter.next(1);
      log.info("emitter.complete()");
      emitter.complete();
    });
    log.info("After Flux.create()");

    log.info("Before Flux.subscribe()");
    flux.subscribe(
      next -> log.info("subscriber.onNext({})", next),
      error -> log.info("subscriber.onError({})", error),
      () -> log.info("subscriber.onComplete()")
    );
    log.info("After Flux.subscribe()");
  }

  @Test
  public void shouldCreateFluxFromIterable() {
    Flux<Integer> flux = fromIterable(asList(1, 2, 3, 4, 5));

    StepVerifier.create(flux)
      .expectNext(1, 2, 3, 4, 5)
      .expectComplete()
      .verify();
  }

  private <T> Flux<T> fromIterable(Iterable<T> iterable) {
    return Flux.create(emitter -> {
      Iterator<T> iterator = iterable.iterator();
      while (iterator.hasNext() && !emitter.isCancelled()) {
        emitter.next(iterator.next());
      }
      emitter.complete();
    });
  }

  @Test
  public void shouldCreateFluxFromInterval() {
    Supplier<Flux<Long>> fluxSupplier = () -> Flux.interval(Duration.ofMillis(200)).take(2);

    StepVerifier.withVirtualTime(fluxSupplier)
      .expectSubscription()
      .expectNoEvent(Duration.ofMillis(199))
      .thenAwait(Duration.ofMillis(1))
      .expectNext(0L)
      .expectNoEvent(Duration.ofMillis(199))
      .thenAwait(Duration.ofMillis(1))
      .expectNext(1L)
      .expectComplete()
      .verify();
  }

  @Test
  public void shouldCreateMonoFromCallable() {
    Mono<User> mono = Mono.fromCallable(() -> loadUser(CWID));

    StepVerifier.create(mono)
      .expectNext(new User(CWID, ""))
      .expectComplete()
      .verify();
  }

  private User loadUser(String cwid) {
    return new User(cwid, "");
  }

}
