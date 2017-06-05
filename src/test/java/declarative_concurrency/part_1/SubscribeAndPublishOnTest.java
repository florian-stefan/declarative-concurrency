package declarative_concurrency.part_1;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;
import reactor.test.StepVerifier;

@Slf4j
public class SubscribeAndPublishOnTest {

  @Test
  public void shouldSubscribeAndPublishOnMainThread() {
    StepVerifier.create(fluxFrom("a", "b", "c", "d", "e")
      .doOnNext(value -> log.info("Received {}", value)))
      .expectNextCount(5)
      .expectComplete()
      .verify();
  }

  @Test
  public void shouldSubscribeAndPublishOnCustomThread() {
    StepVerifier.create(fluxFrom("a", "b", "c", "d", "e")
      .doOnNext(value -> log.info("Received {}", value))
      .subscribeOn(Schedulers.parallel()))
      .expectNextCount(5)
      .expectComplete()
      .verify();
  }

  @Test
  public void shouldSubscribeOnMainThreadAndPublishOnCustomThread() {
    StepVerifier.create(fluxFrom("a", "b", "c", "d", "e")
      .publishOn(Schedulers.parallel())
      .doOnNext(value -> log.info("Received {}", value)))
      .expectNextCount(5)
      .expectComplete()
      .verify();
  }

  private Flux<String> fluxFrom(String... values) {
    return Flux.create(emitter -> {
      log.info("onSubscribed()");
      for (String value : values) {
        if (emitter.isCancelled()) {
          return;
        }
        log.info("next({})", value);
        emitter.next(value);
      }
      log.info("complete()");
      emitter.complete();
    });
  }

}
