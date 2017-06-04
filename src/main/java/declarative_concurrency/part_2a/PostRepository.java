package declarative_concurrency.part_2a;

import org.springframework.stereotype.Component;

import java.util.List;

import static java.util.Collections.emptyList;

@Component
public class PostRepository {

  public List<Post> loadPostChunk(long lowerBound, int chunkSize) {
    return emptyList();
  }

}
