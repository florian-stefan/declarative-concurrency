package declarative_concurrency.part_i;

import javaslang.control.Either;

import java.util.List;

public interface UserService {

  List<Either<String, User>> loadUsers(List<String> cwids);

}
