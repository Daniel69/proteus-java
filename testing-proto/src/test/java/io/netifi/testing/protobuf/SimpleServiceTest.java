package io.netifi.testing.protobuf;

import io.netifi.proteus.rs.RequestHandlingRSocket;
import io.rsocket.Frame;
import io.rsocket.RSocket;
import io.rsocket.RSocketFactory;
import io.rsocket.transport.netty.client.TcpClientTransport;
import io.rsocket.transport.netty.server.TcpServerTransport;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.reactivestreams.Publisher;
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
    SimpleServiceServer serviceServer = new SimpleServiceServer(new DefaultSimpleService());

    RSocketFactory.receive()
        .frameDecoder(Frame::retain)
        .acceptor((setup, sendingSocket) -> Mono.just(new RequestHandlingRSocket(serviceServer)))
        .transport(TcpServerTransport.create(8801))
        .start()
        .block();

    rSocket =
        RSocketFactory.connect()
            .frameDecoder(Frame::retain)
            .transport(TcpClientTransport.create(8801))
            .start()
            .block();
  }

  @Test
  public void testRequestReply() {
    SimpleServiceClient client = new SimpleServiceClient(rSocket);
    SimpleResponse response =
        client
            .requestReply(SimpleRequest.newBuilder().setRequestMessage("sending a message").build())
            .block();

    String responseMessage = response.getResponseMessage();

    System.out.println(responseMessage);

    Assert.assertEquals("sending a message", responseMessage);
  }
  
  @Test(timeout = 5_000)
  public void testStreaming() {
    SimpleServiceClient client = new SimpleServiceClient(rSocket);
    SimpleResponse response =
        client
            .requestStream(
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
        .requestStream(SimpleRequest.newBuilder().setRequestMessage("sending a message").build())
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

    SimpleResponse response = client.streamingRequestSingleResponse(requests).block();

    System.out.println(response.getResponseMessage());
  }

  @Test(timeout = 15_000)
  public void testBidiStreamingRpc() {
    SimpleServiceClient client = new SimpleServiceClient(rSocket);

    Flux<SimpleRequest> requests =
        Flux.range(1, 500_000)
            .map(i -> "sending -> " + i)
            .map(s -> SimpleRequest.newBuilder().setRequestMessage(s).build());

    SimpleResponse response = client.streamingRequestAndResponse(requests).take(500_000).blockLast();

    System.out.println(response.getResponseMessage());
  }

  @Test
  public void testFireAndForget() throws Exception {
    int count = 1000;
    CountDownLatch latch = new CountDownLatch(count);
    SimpleServiceClient client = new SimpleServiceClient(rSocket);
    Flux.range(1, count)
        .flatMap(
            i ->
                client.fireAndForget(
                    SimpleRequest.newBuilder().setRequestMessage("fire -> " + i).build()))
        .blockLast();
  }

  static class DefaultSimpleService implements SimpleService {

    @Override
    public Mono<Void> fireAndForget(SimpleRequest message) {
      System.out.println("got message -> " + message.getRequestMessage());
      return Mono.empty();
    }

    @Override
    public Mono<SimpleResponse> requestReply(SimpleRequest message) {
      return Mono.fromCallable(
          () ->
              SimpleResponse.newBuilder()
                  .setResponseMessageBytes(message.getRequestMessageBytes())
                  .build());
    }

    @Override
    public Mono<SimpleResponse> streamingRequestSingleResponse(Publisher<SimpleRequest> messages) {
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
    public Flux<SimpleResponse> requestStream(SimpleRequest message) {
      String requestMessage = message.getRequestMessage();
      return Flux.interval(Duration.ofMillis(200))
          .onBackpressureDrop()
          .map(i -> i + " - got message - " + requestMessage)
          .map(s -> SimpleResponse.newBuilder().setResponseMessage(s).build());
    }

    @Override
    public Flux<SimpleResponse> streamingRequestAndResponse(Publisher<SimpleRequest> messages) {
      return Flux.from(messages).flatMap(this::requestReply);
    }
  }
}
