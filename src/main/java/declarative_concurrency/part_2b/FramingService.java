package declarative_concurrency.part_2b;

import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.function.Function;

import static java.util.stream.Collectors.joining;

@Component
public class FramingService {

  public Flux<String> parseChunksWithDelimiter(Flux<byte[]> chunks, String delimiter) {
    String prefix = delimiter.substring(delimiter.length() - 1, delimiter.length());
    String suffix = delimiter.substring(0, delimiter.length() - 1);

    Flux<byte[]> chunksWithPrefixAndSuffix = Flux.concat(
      Mono.just(prefix.getBytes(StandardCharsets.UTF_8)),
      chunks,
      Mono.just(suffix.getBytes(StandardCharsets.UTF_8))
    );

    return chunksWithPrefixAndSuffix
      .map(decodeBytes())
      .flatMap(splitChunksIntoSigns())
      .publish(collectFrames(delimiter))
      .map(stripPrefixAndSuffix(prefix, suffix))
      .map(concatSigns());
  }

  private Function<byte[], String> decodeBytes() {
    return chunkAsByteArray -> new String(chunkAsByteArray, StandardCharsets.UTF_8);
  }

  private Function<String, Flux<String>> splitChunksIntoSigns() {
    return chunkAsString -> Flux.fromArray(chunkAsString.split(""));
  }

  private Function<Flux<String>, Flux<List<String>>> collectFrames(String delimiter) {
    return signs -> signs.buffer(signs
      .buffer(delimiter.length(), 1)
      .map(concatSigns())
      .filter(delimiter::equals));
  }

  private Function<List<String>, List<String>> stripPrefixAndSuffix(String prefix, String suffix) {
    return bufferedSigns -> bufferedSigns.subList(prefix.length(), bufferedSigns.size() - suffix.length());
  }

  private Function<List<String>, String> concatSigns() {
    return signs -> signs.stream().collect(joining());
  }

}
