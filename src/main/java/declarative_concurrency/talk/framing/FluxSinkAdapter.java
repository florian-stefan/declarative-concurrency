package declarative_concurrency.talk.framing;

import lombok.extern.slf4j.Slf4j;
import org.asynchttpclient.*;
import org.asynchttpclient.Response.ResponseBuilder;
import reactor.core.publisher.FluxSink;

@Slf4j
public class FluxSinkAdapter implements AsyncHandler<Response> {

  private final FluxSink<byte[]> fluxSink;
  private final ResponseBuilder builder;

  public FluxSinkAdapter(FluxSink<byte[]> fluxSink) {
    this.fluxSink = fluxSink;
    this.builder = new ResponseBuilder();
  }

  @Override
  public void onThrowable(Throwable throwable) {
    fluxSink.error(throwable);
  }

  @Override
  public State onBodyPartReceived(HttpResponseBodyPart httpResponseBodyPart) throws Exception {
    fluxSink.next(httpResponseBodyPart.getBodyPartBytes());

    return httpResponseBodyPart.isLast() ? State.ABORT : State.CONTINUE;
  }

  @Override
  public State onStatusReceived(HttpResponseStatus httpResponseStatus) throws Exception {
    log.info("Received HTTP response status: {}", httpResponseStatus.getStatusCode());
    builder.accumulate(httpResponseStatus);

    if (httpResponseStatus.getStatusCode() == 200) {
      return State.CONTINUE;
    } else {
      fluxSink.error(new Exception(httpResponseStatus.getStatusText()));

      return State.ABORT;
    }
  }

  @Override
  public State onHeadersReceived(HttpResponseHeaders httpResponseHeaders) throws Exception {
    log.info("Received HTTP response headers: {}", httpResponseHeaders.getHeaders());
    builder.accumulate(httpResponseHeaders);

    return State.CONTINUE;
  }

  @Override
  public Response onCompleted() throws Exception {
    fluxSink.complete();

    return builder.build();
  }

}
