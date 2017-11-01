package io.netifi.testing.protobuf;

@javax.annotation.Generated(
    value = "by Proteus proto compiler",
    comments = "Source: io/netifi/testing/protobuf/simpleservice.proto")
public final class SimpleServiceServer implements io.netifi.proteus.ProteusService {
  private final SimpleService service;

  public SimpleServiceServer(SimpleService service) {
    this.service = service;
  }

  @java.lang.Override
  public int getNamespaceId() {
    return SimpleService.NAMESPACE_ID;
  }

  @java.lang.Override
  public int getServiceId() {
    return SimpleService.SERVICE_ID;
  }

  @java.lang.Override
  public reactor.core.publisher.Mono<Void> fireAndForget(io.rsocket.Payload payload) {
    try {
      io.netty.buffer.ByteBuf metadata = io.netty.buffer.Unpooled.wrappedBuffer(payload.getMetadata());
      switch(io.netifi.proteus.frames.ProteusMetadata.methodId(metadata)) {
        case SimpleService.METHOD_FIRE_AND_FORGET: {
          com.google.protobuf.ByteString data = com.google.protobuf.UnsafeByteOperations.unsafeWrap(payload.getData());
          return service.fireAndForget(io.netifi.testing.protobuf.SimpleRequest.parseFrom(data));
        }
        default: {
          return reactor.core.publisher.Mono.error(new UnsupportedOperationException());
        }
      }
    } catch (Throwable t) {
      return reactor.core.publisher.Mono.error(t);
    }
  }

  @java.lang.Override
  public reactor.core.publisher.Mono<io.rsocket.Payload> requestResponse(io.rsocket.Payload payload) {
    try {
      io.netty.buffer.ByteBuf metadata = io.netty.buffer.Unpooled.wrappedBuffer(payload.getMetadata());
      switch(io.netifi.proteus.frames.ProteusMetadata.methodId(metadata)) {
        case SimpleService.METHOD_UNARY_RPC: {
          com.google.protobuf.ByteString data = com.google.protobuf.UnsafeByteOperations.unsafeWrap(payload.getData());
          return service.unaryRpc(io.netifi.testing.protobuf.SimpleRequest.parseFrom(data)).map(serializer);
        }
        default: {
          return reactor.core.publisher.Mono.error(new UnsupportedOperationException());
        }
      }
    } catch (Throwable t) {
      return reactor.core.publisher.Mono.error(t);
    }
  }

  @java.lang.Override
  public reactor.core.publisher.Flux<io.rsocket.Payload> requestStream(io.rsocket.Payload payload) {
    try {
      io.netty.buffer.ByteBuf metadata = io.netty.buffer.Unpooled.wrappedBuffer(payload.getMetadata());
      switch(io.netifi.proteus.frames.ProteusMetadata.methodId(metadata)) {
        case SimpleService.METHOD_STREAM_ON_FIRE_AND_FORGET: {
          com.google.protobuf.ByteString data = com.google.protobuf.UnsafeByteOperations.unsafeWrap(payload.getData());
          return service.streamOnFireAndForget(com.google.protobuf.Empty.parseFrom(data)).map(serializer);
        }
        case SimpleService.METHOD_SERVER_STREAMING_RPC: {
          com.google.protobuf.ByteString data = com.google.protobuf.UnsafeByteOperations.unsafeWrap(payload.getData());
          return service.serverStreamingRpc(io.netifi.testing.protobuf.SimpleRequest.parseFrom(data)).map(serializer);
        }
        default: {
          return reactor.core.publisher.Flux.error(new UnsupportedOperationException());
        }
      }
    } catch (Throwable t) {
      return reactor.core.publisher.Flux.error(t);
    }
  }

  @java.lang.Override
  public reactor.core.publisher.Flux<io.rsocket.Payload> requestChannel(org.reactivestreams.Publisher<io.rsocket.Payload> payloads) {
    return new io.rsocket.internal.SwitchTransform<io.rsocket.Payload, com.google.protobuf.MessageLite>(payloads, new java.util.function.BiFunction<io.rsocket.Payload, reactor.core.publisher.Flux<io.rsocket.Payload>, org.reactivestreams.Publisher<? extends com.google.protobuf.MessageLite>>() {
      @java.lang.Override
      public org.reactivestreams.Publisher<? extends com.google.protobuf.MessageLite> apply(io.rsocket.Payload payload, reactor.core.publisher.Flux<io.rsocket.Payload> publisher) {
        try {
          io.netty.buffer.ByteBuf metadata = io.netty.buffer.Unpooled.wrappedBuffer(payload.getMetadata());
          switch(io.netifi.proteus.frames.ProteusMetadata.methodId(metadata)) {
            case SimpleService.METHOD_CLIENT_STREAMING_RPC: {
              reactor.core.publisher.Flux<io.netifi.testing.protobuf.SimpleRequest> messages =
                publisher.map(deserializer(io.netifi.testing.protobuf.SimpleRequest.parser()));
              return service.clientStreamingRpc(messages);
            }
            case SimpleService.METHOD_BIDI_STREAMING_RPC: {
              reactor.core.publisher.Flux<io.netifi.testing.protobuf.SimpleRequest> messages =
                publisher.map(deserializer(io.netifi.testing.protobuf.SimpleRequest.parser()));
              return service.bidiStreamingRpc(messages);
            }
            default: {
              return reactor.core.publisher.Flux.error(new UnsupportedOperationException());
            }
          }
        } catch (Throwable t) {
          return reactor.core.publisher.Flux.error(t);
        }
      }
    }).map(serializer);
  }

  @java.lang.Override
  public reactor.core.publisher.Mono<Void> metadataPush(io.rsocket.Payload payload) {
    return reactor.core.publisher.Mono.error(new UnsupportedOperationException("Metadata-Push not implemented."));
  }

  @java.lang.Override
  public double availability() {
    return service.availability();
  }

  @java.lang.Override
  public reactor.core.publisher.Mono<Void> close() {
    return service.close();
  }

  @java.lang.Override
  public reactor.core.publisher.Mono<Void> onClose() {
    return service.onClose();
  }

  private static final java.util.function.Function<com.google.protobuf.MessageLite, io.rsocket.Payload> serializer =
    new java.util.function.Function<com.google.protobuf.MessageLite, io.rsocket.Payload>() {
      @java.lang.Override
      public io.rsocket.Payload apply(com.google.protobuf.MessageLite message) {
        return new io.rsocket.util.PayloadImpl(message.toByteString().asReadOnlyByteBuffer());
      }
    };

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
