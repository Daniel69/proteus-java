package io.netifi.testing.protobuf;

import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.rsocket.Frame;
import io.rsocket.RSocket;
import io.rsocket.RSocketFactory;
import io.rsocket.transport.netty.client.TcpClientTransport;
import reactor.core.publisher.Flux;

import java.util.concurrent.TimeUnit;

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
    SimpleMeterRegistry registry = new SimpleMeterRegistry();
    SimpleServiceClient client = new SimpleServiceClient(rSocket, registry);
    long start = System.nanoTime();
    SimpleRequest request = SimpleRequest.newBuilder().setRequestMessage("hello").build();
    Flux.range(1, count)
        .flatMap(
            i -> client.requestReply(request), 16)
        .blockLast();
    double completedMillis = (System.nanoTime() - start) / 1_000_000d;
    double rps = count / ((System.nanoTime() - start) / 1_000_000_000d);
    System.out.println("test complete in " + completedMillis + "ms");
    System.out.println("test rps " + rps);
    Timer timer = registry.find("proteus.client.latency").tags("method", "requestReply").timer().get();
    System.out.println("p50: " + timer.percentile(0.50, TimeUnit.MILLISECONDS) + "ms");
    System.out.println("p90: " + timer.percentile(0.90, TimeUnit.MILLISECONDS) + "ms");
    System.out.println("p99: " + timer.percentile(0.99, TimeUnit.MILLISECONDS) + "ms");
    System.out.println("p99.9: " + timer.percentile(0.999, TimeUnit.MILLISECONDS) + "ms");
    System.out.println("max: " + timer.max(TimeUnit.MILLISECONDS) + "ms");
    System.out.println();
  }
}
