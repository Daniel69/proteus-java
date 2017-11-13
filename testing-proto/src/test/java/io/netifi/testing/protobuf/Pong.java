package io.netifi.testing.protobuf;

import io.netifi.proteus.rs.RequestHandlingRSocket;
import io.rsocket.Frame;
import io.rsocket.RSocketFactory;
import io.rsocket.transport.netty.server.TcpServerTransport;
import reactor.core.publisher.Mono;

public class Pong {
  public static void main(String... args) {
    SimpleServiceServer serviceServer =
        new SimpleServiceServer(new SimpleServiceTest.DefaultSimpleService());

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
