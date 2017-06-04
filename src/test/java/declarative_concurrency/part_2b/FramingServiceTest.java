package declarative_concurrency.part_2b;

import org.junit.Before;
import org.junit.Test;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.nio.charset.StandardCharsets;

public class FramingServiceTest {

  private FramingService sut;

  @Before
  public void setUp() {
    sut = new FramingService();
  }

  @Test
  public void shouldParseChunksWithDelimiterUsingOnlyOneSign() {
    Flux<byte[]> chunks = Flux.just(
      "Old John of Gaunt, time-honour'd Lancaster,\n",
      "Hast thou, according to thy oath and band,",
      "\nBrought hither Henry Hereford thy bold ",
      "son,\nHere to make good the boisterous late ",
      "appeal,\nWhich then our leisure would not let ",
      "us hear,\nAgainst the Duke of Norfolk, Thomas ",
      "Mowbray?"
    ).map(chunk -> chunk.getBytes(StandardCharsets.UTF_8));

    StepVerifier.create(sut.parseChunksWithDelimiter(chunks, "\n"))
      .expectNext("Old John of Gaunt, time-honour'd Lancaster,")
      .expectNext("Hast thou, according to thy oath and band,")
      .expectNext("Brought hither Henry Hereford thy bold son,")
      .expectNext("Here to make good the boisterous late appeal,")
      .expectNext("Which then our leisure would not let us hear,")
      .expectNext("Against the Duke of Norfolk, Thomas Mowbray?")
      .expectComplete()
      .verify();
  }

  @Test
  public void shouldParseChunksWithDelimiterUsingMultipleSigns() {
    Flux<byte[]> chunks = Flux.just(
      "Old John of Gaunt, time-honour'd Lancaster,<br>",
      "Hast thou, according to thy oath and band,",
      "<br>Brought hither Henry Hereford thy bold ",
      "son,<br>Here to make good the boisterous late ",
      "appeal,<br>Which then our leisure would not let ",
      "us hear,<br>Against the Duke of Norfolk, Thomas ",
      "Mowbray?"
    ).map(chunk -> chunk.getBytes(StandardCharsets.UTF_8));

    StepVerifier.create(sut.parseChunksWithDelimiter(chunks, "<br>"))
      .expectNext("Old John of Gaunt, time-honour'd Lancaster,")
      .expectNext("Hast thou, according to thy oath and band,")
      .expectNext("Brought hither Henry Hereford thy bold son,")
      .expectNext("Here to make good the boisterous late appeal,")
      .expectNext("Which then our leisure would not let us hear,")
      .expectNext("Against the Duke of Norfolk, Thomas Mowbray?")
      .expectComplete()
      .verify();
  }

}
