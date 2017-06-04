package declarative_concurrency.part_2a;

import javaslang.Tuple;
import javaslang.Tuple2;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.util.List;
import java.util.function.Function;

import static java.util.stream.Stream.generate;

@Slf4j
@Component
@RequiredArgsConstructor
public class PostJob {

  private static final int AMOUNT_OF_REQUIRED_POSTS_WITH_KEYWORDS = 1000;

  private final PostRepository postRepository;
  private final PostService postService;

  public void run() {
    List<Tuple2<Long, String>> keyWords = Flux
      .fromStream(generate(new PostChunkSupplier(postRepository)))
      .takeWhile(posts -> posts.size() > 0)
      .concatMap(this::processNextChunk)
      .take(AMOUNT_OF_REQUIRED_POSTS_WITH_KEYWORDS)
      .collectList()
      .block();

    saveKeyWords(keyWords);
  }

  private Flux<Tuple2<Long, String>> processNextChunk(List<Post> posts) {
    // flatMap vs concatMap vs flatMapSequential
    // concatMapDelayError
    return Flux
      .fromIterable(posts)
      .flatMap(post -> postService.detectKeyWords(post)
        .onErrorResumeWith(handleError(post))
        .map(keyWord -> Tuple.of(post.getId(), keyWord))
        .subscribeOn(Schedulers.parallel()));
  }

  private Function<Throwable, Flux<String>> handleError(Post post) {
    return error -> {
      log.error("An error occurred while trying to detect keywords for {}", post);
      return Flux.empty();
    };
  }

  private void saveKeyWords(List<Tuple2<Long, String>> keyWords) {
    log.info("Saving keywords {}", keyWords);
  }

}
