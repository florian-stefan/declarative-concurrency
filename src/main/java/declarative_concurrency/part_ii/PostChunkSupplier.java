package declarative_concurrency.part_ii;

import java.util.List;
import java.util.function.Supplier;

import static java.util.Comparator.reverseOrder;

public class PostChunkSupplier implements Supplier<List<Post>> {

  private final PostRepository postRepository;

  private long lowerBound = 0;

  public PostChunkSupplier(PostRepository postRepository) {
    this.postRepository = postRepository;
  }

  @Override
  public synchronized List<Post> get() {
    List<Post> postChunk = postRepository.loadPostChunk(lowerBound, 500);

    Long updatedLowerBound = postChunk.stream()
      .map(Post::getId)
      .sorted(reverseOrder())
      .findFirst()
      .orElse(0L);

    lowerBound = updatedLowerBound;

    return postChunk;
  }

}
