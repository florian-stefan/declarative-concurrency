package declarative_concurrency.talk.framing;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@ToString
@EqualsAndHashCode
@JsonIgnoreProperties(ignoreUnknown = true)
public class Tweet {

  public final String id;
  public final String text;

  @JsonCreator
  public Tweet(@JsonProperty("id_str") String id,
               @JsonProperty("text") String text) {
    this.id = id;
    this.text = text;
  }

}
