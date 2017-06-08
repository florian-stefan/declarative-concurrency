package declarative_concurrency.talk.job;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

@RequiredArgsConstructor
@Getter
@ToString
public class Post {

  private final long id;
  private final String text;

}
