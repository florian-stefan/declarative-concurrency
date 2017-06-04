package declarative_concurrency.part_2b;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

import static java.util.stream.Collectors.joining;

public class FramingUtils {

  public static Flux<Message> extractMessagesByDelimiter(Flux<String> chunks, String delimiter) {
    int delimiterLength = delimiter.length();

    String prefix = delimiter.substring(delimiterLength - 1, delimiterLength);
    String suffix = delimiter.substring(0, delimiterLength - 1);

    return Flux.concat(Mono.just(prefix), chunks, Mono.just(suffix))
      .flatMap(chunk -> Flux.fromArray(chunk.split("")))
      .publish(signs -> signs.buffer(signs
        .buffer(delimiterLength, 1)
        .map(FramingUtils::concatStrings)
        .filter(delimiter::equals)))
      .map(bufferedSigns -> bufferedSigns.subList(1, bufferedSigns.size() - suffix.length()))
      .map(FramingUtils::concatStrings)
      .map(Message::new);
  }

  private static String concatStrings(List<String> strings) {
    return strings.stream().collect(joining());
  }

}
