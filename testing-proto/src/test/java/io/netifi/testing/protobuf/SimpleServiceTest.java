package io.netifi.testing.protobuf;

import com.google.protobuf.Empty;
import io.rsocket.RSocket;
import io.rsocket.RSocketFactory;
import io.rsocket.transport.netty.client.TcpClientTransport;
import io.rsocket.transport.netty.server.TcpServerTransport;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.reactivestreams.Publisher;
import reactor.core.publisher.EmitterProcessor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

public class SimpleServiceTest {

  private static RSocket rSocket;

  @BeforeClass
  public static void setup() {
    RSocketFactory.receive()
        .acceptor(
            (setup, sendingSocket) ->
                Mono.just(new SimpleServiceServer(new DefaultSimpleService())))
        .transport(TcpServerTransport.create(8801))
        .start()
        .block();

    rSocket = RSocketFactory.connect().transport(TcpClientTransport.create(8801)).start().block();
  }

  @Test
  public void testUnaryRpc() {
    SimpleServiceClient client = new SimpleServiceClient(rSocket);
    SimpleResponse response =
        client
            .unaryRpc(SimpleRequest.newBuilder().setRequestMessage("sending a message").build())
            .block();

    String responseMessage = response.getResponseMessage();

    System.out.println(responseMessage);

    Assert.assertEquals("we got the message -> sending a message", responseMessage);
  }

  @Test(timeout = 5_000)
  public void testStreaming() {
    SimpleServiceClient client = new SimpleServiceClient(rSocket);
    SimpleResponse response =
        client
            .serverStreamingRpc(
                SimpleRequest.newBuilder().setRequestMessage("sending a message").build())
            .take(5)
            .blockLast();

    String responseMessage = response.getResponseMessage();
    System.out.println(responseMessage);
  }

  @Test(timeout = 5_000)
  public void testStreamingPrintEach() {
    SimpleServiceClient client = new SimpleServiceClient(rSocket);
    client
        .serverStreamingRpc(
            SimpleRequest.newBuilder().setRequestMessage("sending a message").build())
        .take(5)
        .toStream()
        .forEach(simpleResponse -> System.out.println(simpleResponse.getResponseMessage()));
  }

  @Test(timeout = 3_000)
  public void testClientStreamingRpc() {
    SimpleServiceClient client = new SimpleServiceClient(rSocket);

    Flux<SimpleRequest> requests =
        Flux.range(1, 11)
            .map(i -> "sending -> " + i)
            .map(s -> SimpleRequest.newBuilder().setRequestMessage(s).build());

    SimpleResponse response = client.clientStreamingRpc(requests).block();

    System.out.println(response.getResponseMessage());
  }

  @Test(timeout = 3_000)
  public void testBidiStreamingRpc() {
    SimpleServiceClient client = new SimpleServiceClient(rSocket);

    Flux<SimpleRequest> requests =
        Flux.range(1, 11)
            .map(i -> "sending -> " + i)
            .map(s -> SimpleRequest.newBuilder().setRequestMessage(s).build());

    SimpleResponse response = client.bidiStreamingRpc(requests).take(10).blockLast();

    System.out.println(response.getResponseMessage());
  }

  @Test
  public void testFireAndForget() throws Exception {
    int count = 1000;
    CountDownLatch latch = new CountDownLatch(count);
    SimpleServiceClient client = new SimpleServiceClient(rSocket);
    client
        .streamOnFireAndForget(Empty.getDefaultInstance())
        .subscribe(simpleResponse -> latch.countDown());
    Flux.range(1, count)
        .flatMap(
            i ->
                client.fireAndForget(
                    SimpleRequest.newBuilder().setRequestMessage("fire -> " + i).build()))
        .subscribe();
    latch.await();
  }

  static class DefaultSimpleService implements SimpleService {
    EmitterProcessor<SimpleRequest> messages = EmitterProcessor.create();

    @Override
    public Mono<Void> fireAndForget(SimpleRequest message) {
      messages.onNext(message);
      return Mono.empty();
    }

    @Override
    public Flux<SimpleResponse> streamOnFireAndForget(Empty message) {
      return messages.map(
          simpleRequest ->
              SimpleResponse.newBuilder()
                  .setResponseMessage("got fire and forget -> " + simpleRequest.getRequestMessage())
                  .build());
    }

    @Override
    public Mono<SimpleResponse> unaryRpc(SimpleRequest message) {
      return Mono.fromCallable(
          () ->
              SimpleResponse.newBuilder()
                  .setResponseMessage("we got the message -> " + message.getRequestMessage())
                  .build());
    }

    @Override
    public Mono<SimpleResponse> clientStreamingRpc(Publisher<SimpleRequest> messages) {
      return Flux.from(messages)
          .windowTimeout(10, Duration.ofSeconds(500))
          .take(1)
          .flatMap(Function.identity())
          .reduce(
              new ConcurrentHashMap<Character, AtomicInteger>(),
              (map, s) -> {
                char[] chars = s.getRequestMessage().toCharArray();
                for (char c : chars) {
                  map.computeIfAbsent(c, _c -> new AtomicInteger()).incrementAndGet();
                }

                return map;
              })
          .map(
              map -> {
                StringBuilder builder = new StringBuilder();

                map.forEach(
                    (character, atomicInteger) -> {
                      builder
                          .append("character -> ")
                          .append(character)
                          .append(", count -> ")
                          .append(atomicInteger.get())
                          .append("\n");
                    });

                String s = builder.toString();

                return SimpleResponse.newBuilder().setResponseMessage(s).build();
              });
    }

    @Override
    public Flux<SimpleResponse> serverStreamingRpc(SimpleRequest message) {
      String requestMessage = message.getRequestMessage();
      return Flux.interval(Duration.ofMillis(200))
          .onBackpressureDrop()
          .map(i -> i + " - got message - " + requestMessage)
          .map(s -> SimpleResponse.newBuilder().setResponseMessage(s).build());
    }

    @Override
    public Flux<SimpleResponse> bidiStreamingRpc(Publisher<SimpleRequest> messages) {
      return Flux.from(messages).flatMap(this::unaryRpc);
    }

    @Override
    public double availability() {
      return 1.0;
    }

    @Override
    public Mono<Void> close() {
      return Mono.empty();
    }

    @Override
    public Mono<Void> onClose() {
      return Mono.empty();
    }
  }
}
