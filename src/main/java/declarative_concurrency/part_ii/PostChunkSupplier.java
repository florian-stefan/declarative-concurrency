package declarative_concurrency.part_ii;

import reactor.core.publisher.Mono;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

import static java.util.Comparator.reverseOrder;

public class PostChunkSupplier implements Supplier<Mono<List<Post>>> {

  private final PostRepository postRepository;
  private final AtomicLong currentLowerBound;

  public PostChunkSupplier(PostRepository postRepository) {
    this.postRepository = postRepository;
    this.currentLowerBound = new AtomicLong();
  }

  @Override
  public Mono<List<Post>> get() {
    long lowerBound = currentLowerBound.get();

    List<Post> postChunk = postRepository.loadPostChunk(lowerBound, 500);

    Long updatedLowerBound = postChunk.stream()
      .map(Post::getId)
      .sorted(reverseOrder())
      .findFirst()
      .orElse(0L);

    boolean compareAndSetResult = currentLowerBound.compareAndSet(lowerBound, updatedLowerBound);

    return compareAndSetResult ? Mono.just(postChunk) : Mono.empty();
  }

}
