package declarative_concurrency;

import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
public class ConcatMapAndFlatMapExample {

  private List<String> cwids;

  @Before
  public void setUp() throws Exception {
    cwids = Stream
      .generate(() -> UUID.randomUUID().toString())
      .limit(3)
      .collect(Collectors.toList());
  }

  @Test
  public void shouldLoadUsersUsingMap() {
    Flux<User> users = Flux
      .fromIterable(cwids)
      .map(cwid -> loadUser(cwid));

    StepVerifier.create(users)
      .expectNextMatches(hasCwid(cwids.get(0)))
      .expectNextMatches(hasCwid(cwids.get(1)))
      .expectNextMatches(hasCwid(cwids.get(2)))
      .expectComplete()
      .verify();
  }

  @Test
  public void shouldLoadUsersUsingConcatMap() {
    Flux<User> users = Flux
      .fromIterable(cwids)
      .concatMap(cwid -> loadUserAsMono(cwid));

    StepVerifier.create(users)
      .expectNextMatches(hasCwid(cwids.get(0)))
      .expectNextMatches(hasCwid(cwids.get(1)))
      .expectNextMatches(hasCwid(cwids.get(2)))
      .expectComplete()
      .verify();
  }

  @Test
  public void shouldLoadUsersUsingFlatMap() {
    Flux<User> users = Flux
      .fromIterable(cwids)
      .flatMap(cwid -> loadUserAsMono(cwid));

    StepVerifier.create(users)
      .expectNextMatches(hasCwid(cwids.get(0)))
      .expectNextMatches(hasCwid(cwids.get(1)))
      .expectNextMatches(hasCwid(cwids.get(2)))
      .verifyComplete();
  }

  private User loadUser(String cwid) {
    log.info("Loading user with cwid = {}", cwid);
    return new User(cwid, "");
  }

  private Mono<User> loadUserAsMono(String cwid) {
    return Mono.fromCallable(() -> loadUser(cwid));
  }

  private Predicate<User> hasCwid(String cwid) {
    return user -> Objects.equals(user.getCwid(), cwid);
  }

}
