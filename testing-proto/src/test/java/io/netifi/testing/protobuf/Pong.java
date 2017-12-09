package io.netifi.testing.protobuf;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.netifi.proteus.rs.RequestHandlingRSocket;
import io.rsocket.Frame;
import io.rsocket.RSocketFactory;
import io.rsocket.transport.netty.server.TcpServerTransport;
import reactor.core.publisher.Mono;

public class Pong {
  public static void main(String... args) {
    SimpleMeterRegistry registry = new SimpleMeterRegistry();
    SimpleServiceServer serviceServer =
        new SimpleServiceServer(new SimpleServiceTest.DefaultSimpleService(), registry);

    RSocketFactory.receive()
        .frameDecoder(Frame::retain)
        .acceptor((setup, sendingSocket) -> Mono.just(new RequestHandlingRSocket(serviceServer)))
        .transport(TcpServerTransport.create(8801))
        .start()
        .block()
        .onClose()
        .block();
  }
}
