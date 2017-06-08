package declarative_concurrency.talk;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

@RequiredArgsConstructor
@Getter
@ToString
public class User {

  private final String cwid;
  private final String name;

}
