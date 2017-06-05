package declarative_concurrency.part_3;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.asynchttpclient.DefaultAsyncHttpClient;
import org.asynchttpclient.Request;
import org.asynchttpclient.RequestBuilder;
import org.asynchttpclient.oauth.ConsumerKey;
import org.asynchttpclient.oauth.OAuthSignatureCalculator;
import org.asynchttpclient.oauth.RequestToken;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;

import static java.lang.String.format;

@Component
@RequiredArgsConstructor
public class TwitterClient {

  private static final String DELIMITER = "\r\n";

  private final TwitterClientProperties properties;
  private final FramingService framingService;
  private final ObjectMapper objectMapper;

  public Flux<Tweet> stream() {
    String url = format("https://stream.twitter.com/1.1/statuses/filter.json?track=%s", properties.getTrack());

    ConsumerKey consumerKey = new ConsumerKey(properties.getApiKey(), properties.getApiSecret());
    RequestToken requestToken = new RequestToken(properties.getToken(), properties.getTokenSecret());

    Request request = new RequestBuilder()
      .setMethod("POST")
      .setUrl(url)
      .setSignatureCalculator(new OAuthSignatureCalculator(consumerKey, requestToken))
      .setRequestTimeout(Integer.MAX_VALUE)
      .build();

    Flux<byte[]> chunks = Flux.create(emitter -> {
      DefaultAsyncHttpClient asyncHttpClient = new DefaultAsyncHttpClient();
      asyncHttpClient.executeRequest(request, new FluxSinkAdapter(emitter));
      emitter.onCancel(asyncHttpClient::close);
    });

    return framingService
      .parseChunksWithDelimiter(chunks, DELIMITER)
      .flatMap(this::parseTweet)
      .share();
  }

  private Mono<Tweet> parseTweet(String tweetAsString) {
    try {
      return Mono.just(objectMapper.readValue(tweetAsString, Tweet.class));
    } catch (IOException e) {
      return Mono.error(e);
    }
  }

}
