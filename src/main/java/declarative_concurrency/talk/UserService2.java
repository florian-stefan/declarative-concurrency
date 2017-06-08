package declarative_concurrency.talk;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static java.time.Duration.ofSeconds;

@Slf4j
@Component
@RequiredArgsConstructor
@SuppressWarnings("Duplicates")
public class UserService2 implements UserService {

  private final UserClient userClient;

  @Override
  public List<User> loadUsers(List<String> cwids) {
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

    return users;
  }

  private ExecutorService createExecutorService(int nThreads) {
    return new ThreadPoolExecutor(nThreads, nThreads,
      0L, TimeUnit.MILLISECONDS,
      new LinkedBlockingQueue<>(500));
  }

  private User fetchUser(String cwid) {
    log.info("Starting to fetch user with cwid = {}", cwid);
    User user = userClient.fetchUser(cwid);
    log.info("Finished to fetch user with cwid = {}", cwid);

    return user;
  }

  public <T> Flux<T> fromIterable(Iterable<T> cwids) {
    return Flux.create(emitter -> {
      Iterator<T> iterator = cwids.iterator();
      while (iterator.hasNext() && !emitter.isCancelled()) {
        emitter.next(iterator.next());
      }
      emitter.complete();
    });
  }

}
