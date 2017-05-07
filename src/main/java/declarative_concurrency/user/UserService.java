package declarative_concurrency.user;

import javaslang.control.Either;

import java.util.List;

public interface UserService {

  List<Either<String, User>> loadUsers(List<String> cwids);

}
