package declarative_concurrency.talk;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.*;

import static java.util.stream.Collectors.toList;

@Slf4j
@Component
@RequiredArgsConstructor
@SuppressWarnings("Duplicates")
public class UserService1 implements UserService {

  private final UserClient userClient;

  @Override
  public List<User> loadUsers(List<String> cwids) {
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

}
