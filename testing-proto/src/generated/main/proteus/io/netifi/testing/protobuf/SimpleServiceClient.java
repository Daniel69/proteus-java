package io.netifi.testing.protobuf;

@javax.annotation.Generated(
    value = "by Proteus proto compiler",
    comments = "Source: io/netifi/testing/protobuf/simpleservice.proto")
public final class SimpleServiceClient implements SimpleService {
  private final io.rsocket.RSocket rSocket;

  public SimpleServiceClient(io.rsocket.RSocket rSocket) {
    this.rSocket = rSocket;
  }

  @java.lang.Override
  public reactor.core.publisher.Mono<io.netifi.testing.protobuf.SimpleResponse> unaryRpc(io.netifi.testing.protobuf.SimpleRequest message) {
    int length = io.netifi.proteus.frames.ProteusMetadata.computeLength();
    io.netty.buffer.ByteBuf metadata = io.netty.buffer.ByteBufAllocator.DEFAULT.directBuffer(length);
    io.netifi.proteus.frames.ProteusMetadata.encode(metadata, SimpleService.NAMESPACE_ID, SimpleService.SERVICE_ID, SimpleService.METHOD_UNARY_RPC);
    java.nio.ByteBuffer data = message.toByteString().asReadOnlyByteBuffer();

    return rSocket.requestResponse(new io.rsocket.util.PayloadImpl(data, metadata.nioBuffer()))
      .map(deserializer(io.netifi.testing.protobuf.SimpleResponse.parser()));
  }

  @java.lang.Override
  public reactor.core.publisher.Mono<io.netifi.testing.protobuf.SimpleResponse> clientStreamingRpc(org.reactivestreams.Publisher<io.netifi.testing.protobuf.SimpleRequest> messages) {
    int length = io.netifi.proteus.frames.ProteusMetadata.computeLength();
    final io.netty.buffer.ByteBuf metadata = io.netty.buffer.ByteBufAllocator.DEFAULT.directBuffer(length);
    io.netifi.proteus.frames.ProteusMetadata.encode(metadata, SimpleService.NAMESPACE_ID, SimpleService.SERVICE_ID, SimpleService.METHOD_CLIENT_STREAMING_RPC);

    reactor.core.publisher.Flux<io.netifi.testing.protobuf.SimpleRequest> publisher = reactor.core.publisher.Flux.from(messages);
    return rSocket.requestChannel(publisher.map(new java.util.function.Function<com.google.protobuf.MessageLite, io.rsocket.Payload>() {
      @java.lang.Override
      public io.rsocket.Payload apply(com.google.protobuf.MessageLite message) {
        java.nio.ByteBuffer data = message.toByteString().asReadOnlyByteBuffer();
        return new io.rsocket.util.PayloadImpl(data, metadata.nioBuffer());
      }
    })).map(deserializer(io.netifi.testing.protobuf.SimpleResponse.parser())).single();
  }

  @java.lang.Override
  public reactor.core.publisher.Flux<io.netifi.testing.protobuf.SimpleResponse> serverStreamingRpc(io.netifi.testing.protobuf.SimpleRequest message) {
    int length = io.netifi.proteus.frames.ProteusMetadata.computeLength();
    io.netty.buffer.ByteBuf metadata = io.netty.buffer.ByteBufAllocator.DEFAULT.directBuffer(length);
    io.netifi.proteus.frames.ProteusMetadata.encode(metadata, SimpleService.NAMESPACE_ID, SimpleService.SERVICE_ID, SimpleService.METHOD_SERVER_STREAMING_RPC);
    java.nio.ByteBuffer data = message.toByteString().asReadOnlyByteBuffer();

    return rSocket.requestStream(new io.rsocket.util.PayloadImpl(data, metadata.nioBuffer()))
      .map(deserializer(io.netifi.testing.protobuf.SimpleResponse.parser()));
  }

  @java.lang.Override
  public reactor.core.publisher.Flux<io.netifi.testing.protobuf.SimpleResponse> bidiStreamingRpc(org.reactivestreams.Publisher<io.netifi.testing.protobuf.SimpleRequest> messages) {
    int length = io.netifi.proteus.frames.ProteusMetadata.computeLength();
    final io.netty.buffer.ByteBuf metadata = io.netty.buffer.ByteBufAllocator.DEFAULT.directBuffer(length);
    io.netifi.proteus.frames.ProteusMetadata.encode(metadata, SimpleService.NAMESPACE_ID, SimpleService.SERVICE_ID, SimpleService.METHOD_BIDI_STREAMING_RPC);

    reactor.core.publisher.Flux<io.netifi.testing.protobuf.SimpleRequest> publisher = reactor.core.publisher.Flux.from(messages);
    return rSocket.requestChannel(publisher.map(new java.util.function.Function<com.google.protobuf.MessageLite, io.rsocket.Payload>() {
      @java.lang.Override
      public io.rsocket.Payload apply(com.google.protobuf.MessageLite message) {
        java.nio.ByteBuffer data = message.toByteString().asReadOnlyByteBuffer();
        return new io.rsocket.util.PayloadImpl(data, metadata.nioBuffer());
      }
    })).map(deserializer(io.netifi.testing.protobuf.SimpleResponse.parser()));
  }

  @java.lang.Override
  public double availability() {
    return rSocket.availability();
  }

  @java.lang.Override
  public reactor.core.publisher.Mono<Void> close() {
    return rSocket.close();
  }

  @java.lang.Override
  public reactor.core.publisher.Mono<Void> onClose() {
    return rSocket.onClose();
  }

  private static <T> java.util.function.Function<io.rsocket.Payload, T> deserializer(final com.google.protobuf.Parser<T> parser) {
    return new java.util.function.Function<io.rsocket.Payload, T>() {
      @java.lang.Override
      public T apply(io.rsocket.Payload payload) {
        try {
          com.google.protobuf.ByteString data = com.google.protobuf.UnsafeByteOperations.unsafeWrap(payload.getData());
          return parser.parseFrom(data);
        } catch (Throwable t) {
          throw new RuntimeException(t);
        }
      }
    };
  }
}
