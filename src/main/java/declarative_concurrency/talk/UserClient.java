package declarative_concurrency.talk;


import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Random;

import static java.util.concurrent.TimeUnit.SECONDS;

@Slf4j
@Component
public class UserClient {

  private final static Random RANDOM = new Random();

  private final static String[] NAMES = new String[]{
    "SMITH",
    "JOHNSON",
    "WILLIAMS",
    "JONES",
    "BROWN",
    "DAVIS",
    "MILLER",
    "WILSON",
    "MOORE",
    "TAYLOR"
  };

  public User fetchUser(String cwid) {
    sleepForOneSecond();

    return new User(cwid, getNextName());
  }

  private void sleepForOneSecond() {
    try {
      SECONDS.sleep(1);
    } catch (InterruptedException ignored) {
    }
  }

  private String getNextName() {
    return NAMES[RANDOM.nextInt(NAMES.length)];
  }

}
