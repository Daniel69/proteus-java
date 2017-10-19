package io.netifi.testing.protobuf;

/**
 * <pre>
 * A simple service for test.
 * </pre>
 */
@javax.annotation.Generated(
    value = "by Proteus proto compiler",
    comments = "Source: io/netifi/testing/protobuf/simpleservice.proto")
public interface SimpleService extends io.rsocket.Availability, io.rsocket.Closeable {
  int NAMESPACE_ID = 298608432;
  int SERVICE_ID = -1305494814;
  int METHOD_UNARY_RPC = -1434830019;
  int METHOD_CLIENT_STREAMING_RPC = 356703499;
  int METHOD_SERVER_STREAMING_RPC = -803409785;
  int METHOD_BIDI_STREAMING_RPC = -1207876110;

  /**
   * <pre>
   * Simple unary RPC.
   * </pre>
   */
  reactor.core.publisher.Mono<io.netifi.testing.protobuf.SimpleResponse> unaryRpc(io.netifi.testing.protobuf.SimpleRequest message);

  /**
   * <pre>
   * Simple client-to-server streaming RPC.
   * </pre>
   */
  reactor.core.publisher.Mono<io.netifi.testing.protobuf.SimpleResponse> clientStreamingRpc(org.reactivestreams.Publisher<io.netifi.testing.protobuf.SimpleRequest> messages);

  /**
   * <pre>
   * Simple server-to-client streaming RPC.
   * </pre>
   */
  reactor.core.publisher.Flux<io.netifi.testing.protobuf.SimpleResponse> serverStreamingRpc(io.netifi.testing.protobuf.SimpleRequest message);

  /**
   * <pre>
   * Simple bidirectional streaming RPC.
   * </pre>
   */
  reactor.core.publisher.Flux<io.netifi.testing.protobuf.SimpleResponse> bidiStreamingRpc(org.reactivestreams.Publisher<io.netifi.testing.protobuf.SimpleRequest> messages);
}
