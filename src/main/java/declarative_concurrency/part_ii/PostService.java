package declarative_concurrency.part_ii;

import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

@Component
public class PostService {

  public Flux<String> detectKeyWords(Post post) {
    return Flux.empty();
  }

}
