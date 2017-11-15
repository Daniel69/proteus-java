package io.netifi.testing.protobuf;

import io.rsocket.Frame;
import io.rsocket.RSocket;
import io.rsocket.RSocketFactory;
import io.rsocket.transport.netty.client.TcpClientTransport;
import org.HdrHistogram.Histogram;
import reactor.core.publisher.Flux;

public class Ping {
  public static void main(String... args) {
    RSocket rSocket =
        RSocketFactory.connect()
            .frameDecoder(Frame::retain)
            .transport(TcpClientTransport.create(8801))
            .start()
            .block();
    
    ping(10_000, rSocket);
    ping(1_000_000, rSocket);
    ping(100_000_000, rSocket);
  }

  private static void ping(int count, RSocket rSocket) {
    System.out.println("starting -> " + count);
    SimpleServiceClient client = new SimpleServiceClient(rSocket);
    Histogram histogram = new Histogram(3600000000000L, 3);
    long start = System.nanoTime();
    SimpleRequest request = SimpleRequest.newBuilder().setRequestMessage("hello").build();
    Flux.range(1, count)
        .flatMap(
            i -> {
              long s = System.nanoTime();
              return client
                  .requestReply(request)
                  .doOnNext(
                      r -> {
                        histogram.recordValue(System.nanoTime() - s);
                      });
            }, 16)
        .blockLast();
    histogram.outputPercentileDistribution(System.out, 1000.0d);
    double completedMillis = (System.nanoTime() - start) / 1_000_000d;
    double rps = count / ((System.nanoTime() - start) / 1_000_000_000d);
    System.out.println("test complete in " + completedMillis + "ms");
    System.out.println("test rps " + rps);
    System.out.println();
    System.out.println();
  }
}
