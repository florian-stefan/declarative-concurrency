package declarative_concurrency.talk;

import java.util.List;

public interface UserService {

  List<User> loadUsers(List<String> cwids);

}
