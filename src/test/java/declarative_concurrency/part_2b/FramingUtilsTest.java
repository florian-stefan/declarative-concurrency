package declarative_concurrency.part_2b;

import org.junit.Test;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import static declarative_concurrency.part_2b.FramingUtils.extractMessagesByDelimiter;

public class FramingUtilsTest {

  @Test
  public void shouldExtractMessagesUsingDelimiterWithOnlyOneSign() {
    Flux<String> chunks = Flux.just(
      "Old John of Gaunt, time-honour'd Lancaster,\n",
      "Hast thou, according to thy oath and band,",
      "\nBrought hither Henry Hereford thy bold ",
      "son,\nHere to make good the boisterous late ",
      "appeal,\nWhich then our leisure would not let ",
      "us hear,\nAgainst the Duke of Norfolk, Thomas ",
      "Mowbray?"
    );

    StepVerifier.create(extractMessagesByDelimiter(chunks, "\n"))
      .expectNext(new Message("Old John of Gaunt, time-honour'd Lancaster,"))
      .expectNext(new Message("Hast thou, according to thy oath and band,"))
      .expectNext(new Message("Brought hither Henry Hereford thy bold son,"))
      .expectNext(new Message("Here to make good the boisterous late appeal,"))
      .expectNext(new Message("Which then our leisure would not let us hear,"))
      .expectNext(new Message("Against the Duke of Norfolk, Thomas Mowbray?"))
      .expectComplete()
      .verify();
  }

  @Test
  public void shouldExtractMessagesUsingDelimiterWithMultipleSigns() {
    Flux<String> chunks = Flux.just(
      "Old John of Gaunt, time-honour'd Lancaster,<br>",
      "Hast thou, according to thy oath and band,",
      "<br>Brought hither Henry Hereford thy bold ",
      "son,<br>Here to make good the boisterous late ",
      "appeal,<br>Which then our leisure would not let ",
      "us hear,<br>Against the Duke of Norfolk, Thomas ",
      "Mowbray?"
    );

    StepVerifier.create(extractMessagesByDelimiter(chunks, "<br>"))
      .expectNext(new Message("Old John of Gaunt, time-honour'd Lancaster,"))
      .expectNext(new Message("Hast thou, according to thy oath and band,"))
      .expectNext(new Message("Brought hither Henry Hereford thy bold son,"))
      .expectNext(new Message("Here to make good the boisterous late appeal,"))
      .expectNext(new Message("Which then our leisure would not let us hear,"))
      .expectNext(new Message("Against the Duke of Norfolk, Thomas Mowbray?"))
      .expectComplete()
      .verify();
  }

}
