package io.netifi.testing.protobuf;

@javax.annotation.Generated(
    value = "by Proteus proto compiler",
    comments = "Source: io/netifi/testing/protobuf/simpleservice.proto")
public final class SimpleServiceClient implements SimpleService {
  private final io.rsocket.RSocket rSocket;
  private final long accountId, fromAccountId;
  private final String group, fromGroup;
  private final String destination, fromDestination;

  private final io.netifi.proteus.util.TimebasedIdGenerator generator;
  private final io.netifi.proteus.auth.SessionUtil sessionUtil;
  private final java.util.concurrent.atomic.AtomicLong currentSessionCounter;
  private final java.util.concurrent.atomic.AtomicReference<byte[]> currentSessionToken;

  public SimpleServiceClient(
      io.rsocket.RSocket rSocket,
      long accountId,
      String group,
      String destination,
      long fromAccountId,
      String fromGroup,
      String fromDestination,
      io.netifi.proteus.util.TimebasedIdGenerator generator,
      io.netifi.proteus.auth.SessionUtil sessionUtil,
      java.util.concurrent.atomic.AtomicLong currentSessionCounter,
      java.util.concurrent.atomic.AtomicReference<byte[]> currentSessionToken) {
    this.rSocket = rSocket;
    this.accountId = accountId;
    this.group = group;
    this.destination = destination;
    this.fromAccountId = fromAccountId;
    this.fromGroup = fromGroup;
    this.fromDestination = fromDestination;
    this.generator = generator;
    this.sessionUtil = sessionUtil;
    this.currentSessionCounter = currentSessionCounter;
    this.currentSessionToken = currentSessionToken;
  }

  @java.lang.Override
  public reactor.core.publisher.Mono<io.netifi.testing.protobuf.SimpleResponse> unaryRpc(io.netifi.testing.protobuf.SimpleRequest message) {
    io.netty.buffer.ByteBuf route;
    if (destination != null && !destination.isEmpty()) {
      int length = io.netifi.proteus.frames.RouteDestinationFlyweight.computeLength(io.netifi.proteus.frames.RouteType.STREAM_ID_ROUTE, destination, group);
      route = io.netty.buffer.PooledByteBufAllocator.DEFAULT.directBuffer(length);
      io.netifi.proteus.frames.RouteDestinationFlyweight.encodeRouteByDestination(route, io.netifi.proteus.frames.RouteType.STREAM_ID_ROUTE, accountId, destination, group);
    } else {
      int length = io.netifi.proteus.frames.RouteDestinationFlyweight.computeLength(io.netifi.proteus.frames.RouteType.STREAM_GROUP_ROUTE, group);
      route = io.netty.buffer.PooledByteBufAllocator.DEFAULT.directBuffer(length);
      io.netifi.proteus.frames.RouteDestinationFlyweight.encodeRouteByGroup(route, io.netifi.proteus.frames.RouteType.STREAM_GROUP_ROUTE, accountId, group);
    }

    int length = io.netifi.proteus.frames.RoutingFlyweight.computeLength(true, false, true, fromDestination, route);
    io.netty.buffer.ByteBuf metadata = io.netty.buffer.PooledByteBufAllocator.DEFAULT.directBuffer(length);
    java.nio.ByteBuffer data = message.toByteString().asReadOnlyByteBuffer();
    int requestToken = sessionUtil.generateRequestToken(currentSessionToken.get(), data, currentSessionCounter.incrementAndGet());
    io.netifi.proteus.frames.RoutingFlyweight.encode(
        metadata,
        true,
        true,
        false,
        requestToken,
        fromAccountId,
        fromDestination,
        0,
        SimpleService.PACKAGE_ID,
        SimpleService.SERVICE_ID,
        SimpleService.METHOD_UNARY_RPC,
        generator.nextId(),
        route);

    return rSocket.requestResponse(new io.rsocket.util.PayloadImpl(data, metadata.nioBuffer()))
      .map(deserializer(io.netifi.testing.protobuf.SimpleResponse.parser()));
  }

  @java.lang.Override
  public reactor.core.publisher.Mono<io.netifi.testing.protobuf.SimpleResponse> clientStreamingRpc(org.reactivestreams.Publisher<io.netifi.testing.protobuf.SimpleRequest> messages) {
    final io.netty.buffer.ByteBuf route;
    if (destination != null && !destination.isEmpty()) {
      int length = io.netifi.proteus.frames.RouteDestinationFlyweight.computeLength(io.netifi.proteus.frames.RouteType.STREAM_ID_ROUTE, destination, group);
      route = io.netty.buffer.PooledByteBufAllocator.DEFAULT.directBuffer(length);
      io.netifi.proteus.frames.RouteDestinationFlyweight.encodeRouteByDestination(route, io.netifi.proteus.frames.RouteType.STREAM_ID_ROUTE, accountId, destination, group);
    } else {
      int length = io.netifi.proteus.frames.RouteDestinationFlyweight.computeLength(io.netifi.proteus.frames.RouteType.STREAM_GROUP_ROUTE, group);
      route = io.netty.buffer.PooledByteBufAllocator.DEFAULT.directBuffer(length);
      io.netifi.proteus.frames.RouteDestinationFlyweight.encodeRouteByGroup(route, io.netifi.proteus.frames.RouteType.STREAM_GROUP_ROUTE, accountId, group);
    }

    int length = io.netifi.proteus.frames.RoutingFlyweight.computeLength(true, false, true, fromDestination, route);
    final io.netty.buffer.ByteBuf metadata = io.netty.buffer.PooledByteBufAllocator.DEFAULT.directBuffer(length);
    reactor.core.publisher.Flux<io.netifi.testing.protobuf.SimpleRequest> publisher = reactor.core.publisher.Flux.from(messages);
    return rSocket.requestChannel(publisher.map(new java.util.function.Function<com.google.protobuf.MessageLite, io.rsocket.Payload>() {
      @java.lang.Override
      public io.rsocket.Payload apply(com.google.protobuf.MessageLite message) {
        java.nio.ByteBuffer data = message.toByteString().asReadOnlyByteBuffer();
        int requestToken = sessionUtil.generateRequestToken(currentSessionToken.get(), data, currentSessionCounter.incrementAndGet());
        io.netifi.proteus.frames.RoutingFlyweight.encode(
            metadata,
            true,
            true,
            false,
            requestToken,
            fromAccountId,
            fromDestination,
            0,
            SimpleService.PACKAGE_ID,
            SimpleService.SERVICE_ID,
            SimpleService.METHOD_CLIENT_STREAMING_RPC,
            generator.nextId(),
            route);

        return new io.rsocket.util.PayloadImpl(data, metadata.nioBuffer());
      }
    })).map(deserializer(io.netifi.testing.protobuf.SimpleResponse.parser())).single();
  }

  @java.lang.Override
  public reactor.core.publisher.Flux<io.netifi.testing.protobuf.SimpleResponse> serverStreamingRpc(io.netifi.testing.protobuf.SimpleRequest message) {
    io.netty.buffer.ByteBuf route;
    if (destination != null && !destination.isEmpty()) {
      int length = io.netifi.proteus.frames.RouteDestinationFlyweight.computeLength(io.netifi.proteus.frames.RouteType.STREAM_ID_ROUTE, destination, group);
      route = io.netty.buffer.PooledByteBufAllocator.DEFAULT.directBuffer(length);
      io.netifi.proteus.frames.RouteDestinationFlyweight.encodeRouteByDestination(route, io.netifi.proteus.frames.RouteType.STREAM_ID_ROUTE, accountId, destination, group);
    } else {
      int length = io.netifi.proteus.frames.RouteDestinationFlyweight.computeLength(io.netifi.proteus.frames.RouteType.STREAM_GROUP_ROUTE, group);
      route = io.netty.buffer.PooledByteBufAllocator.DEFAULT.directBuffer(length);
      io.netifi.proteus.frames.RouteDestinationFlyweight.encodeRouteByGroup(route, io.netifi.proteus.frames.RouteType.STREAM_GROUP_ROUTE, accountId, group);
    }

    int length = io.netifi.proteus.frames.RoutingFlyweight.computeLength(true, false, true, fromDestination, route);
    io.netty.buffer.ByteBuf metadata = io.netty.buffer.PooledByteBufAllocator.DEFAULT.directBuffer(length);
    java.nio.ByteBuffer data = message.toByteString().asReadOnlyByteBuffer();
    int requestToken = sessionUtil.generateRequestToken(currentSessionToken.get(), data, currentSessionCounter.incrementAndGet());
    io.netifi.proteus.frames.RoutingFlyweight.encode(
        metadata,
        true,
        true,
        false,
        requestToken,
        fromAccountId,
        fromDestination,
        0,
        SimpleService.PACKAGE_ID,
        SimpleService.SERVICE_ID,
        SimpleService.METHOD_SERVER_STREAMING_RPC,
        generator.nextId(),
        route);

    return rSocket.requestStream(new io.rsocket.util.PayloadImpl(data, metadata.nioBuffer()))
      .map(deserializer(io.netifi.testing.protobuf.SimpleResponse.parser()));
  }

  @java.lang.Override
  public reactor.core.publisher.Flux<io.netifi.testing.protobuf.SimpleResponse> bidiStreamingRpc(org.reactivestreams.Publisher<io.netifi.testing.protobuf.SimpleRequest> messages) {
    final io.netty.buffer.ByteBuf route;
    if (destination != null && !destination.isEmpty()) {
      int length = io.netifi.proteus.frames.RouteDestinationFlyweight.computeLength(io.netifi.proteus.frames.RouteType.STREAM_ID_ROUTE, destination, group);
      route = io.netty.buffer.PooledByteBufAllocator.DEFAULT.directBuffer(length);
      io.netifi.proteus.frames.RouteDestinationFlyweight.encodeRouteByDestination(route, io.netifi.proteus.frames.RouteType.STREAM_ID_ROUTE, accountId, destination, group);
    } else {
      int length = io.netifi.proteus.frames.RouteDestinationFlyweight.computeLength(io.netifi.proteus.frames.RouteType.STREAM_GROUP_ROUTE, group);
      route = io.netty.buffer.PooledByteBufAllocator.DEFAULT.directBuffer(length);
      io.netifi.proteus.frames.RouteDestinationFlyweight.encodeRouteByGroup(route, io.netifi.proteus.frames.RouteType.STREAM_GROUP_ROUTE, accountId, group);
    }

    int length = io.netifi.proteus.frames.RoutingFlyweight.computeLength(true, false, true, fromDestination, route);
    final io.netty.buffer.ByteBuf metadata = io.netty.buffer.PooledByteBufAllocator.DEFAULT.directBuffer(length);
    reactor.core.publisher.Flux<io.netifi.testing.protobuf.SimpleRequest> publisher = reactor.core.publisher.Flux.from(messages);
    return rSocket.requestChannel(publisher.map(new java.util.function.Function<com.google.protobuf.MessageLite, io.rsocket.Payload>() {
      @java.lang.Override
      public io.rsocket.Payload apply(com.google.protobuf.MessageLite message) {
        java.nio.ByteBuffer data = message.toByteString().asReadOnlyByteBuffer();
        int requestToken = sessionUtil.generateRequestToken(currentSessionToken.get(), data, currentSessionCounter.incrementAndGet());
        io.netifi.proteus.frames.RoutingFlyweight.encode(
            metadata,
            true,
            true,
            false,
            requestToken,
            fromAccountId,
            fromDestination,
            0,
            SimpleService.PACKAGE_ID,
            SimpleService.SERVICE_ID,
            SimpleService.METHOD_BIDI_STREAMING_RPC,
            generator.nextId(),
            route);

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
