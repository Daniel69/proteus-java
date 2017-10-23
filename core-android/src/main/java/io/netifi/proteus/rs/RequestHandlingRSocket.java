package io.netifi.proteus.rs;

import io.netifi.proteus.ProteusService;
import io.netifi.proteus.collections.BiInt2ObjectMap;
import io.netifi.proteus.exception.ServiceNotFound;
import io.netifi.proteus.frames.ProteusMetadata;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.rsocket.Payload;
import io.rsocket.RSocket;
import io.rsocket.internal.SwitchTransform;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoProcessor;

import java.util.function.BiFunction;

public class RequestHandlingRSocket implements RSocket {
  private final BiInt2ObjectMap<ProteusService> registeredServices;
  private MonoProcessor<Void> onClose;

  public RequestHandlingRSocket(ProteusService... services) {
    this.onClose = MonoProcessor.create();
    this.registeredServices = new BiInt2ObjectMap<ProteusService>();

    for (ProteusService proteusService : services) {
      int namespaceId = proteusService.getNamespaceId();
      int serviceId = proteusService.getServiceId();
      registeredServices.put(namespaceId, serviceId, proteusService);
    }
  }

  @Override
  public Mono<Void> fireAndForget(Payload payload) {
    try {
      ByteBuf metadata = Unpooled.wrappedBuffer(payload.getMetadata());
      int namespaceId = ProteusMetadata.namespaceId(metadata);
      int serviceId = ProteusMetadata.serviceId(metadata);

      ProteusService proteusService = registeredServices.get(namespaceId, serviceId);

      if (proteusService == null) {
        return Mono.error(new ServiceNotFound(namespaceId, serviceId));
      }

      return proteusService.fireAndForget(payload);

    } catch (Throwable t) {
      return Mono.error(t);
    }
  }

  @Override
  public Mono<Payload> requestResponse(Payload payload) {
    try {
      ByteBuf metadata = Unpooled.wrappedBuffer(payload.getMetadata());
      int namespaceId = ProteusMetadata.namespaceId(metadata);
      int serviceId = ProteusMetadata.serviceId(metadata);

      ProteusService proteusService = registeredServices.get(namespaceId, serviceId);

      if (proteusService == null) {
        return Mono.error(new ServiceNotFound(namespaceId, serviceId));
      }

      return proteusService.requestResponse(payload);

    } catch (Throwable t) {
      return Mono.error(t);
    }
  }

  @Override
  public Flux<Payload> requestStream(Payload payload) {
    try {
      ByteBuf metadata = Unpooled.wrappedBuffer(payload.getMetadata());
      int namespaceId = ProteusMetadata.namespaceId(metadata);
      int serviceId = ProteusMetadata.serviceId(metadata);

      ProteusService proteusService = registeredServices.get(namespaceId, serviceId);

      if (proteusService == null) {
        return Flux.error(new ServiceNotFound(namespaceId, serviceId));
      }

      return proteusService.requestStream(payload);

    } catch (Throwable t) {
      return Flux.error(t);
    }
  }

  @Override
  public Flux<Payload> requestChannel(Publisher<Payload> payloads) {
    return new SwitchTransform<>(payloads, new BiFunction<Payload, Flux<? extends Payload>, Publisher<? extends Payload>>() {
      @Override
      public Publisher<? extends Payload> apply(Payload payload, Flux<? extends Payload> publisher) {
        ByteBuf metadata = Unpooled.wrappedBuffer(payload.getMetadata());
        int namespaceId = ProteusMetadata.namespaceId(metadata);
        int serviceId = ProteusMetadata.serviceId(metadata);
        ProteusService proteusService = registeredServices.get(namespaceId, serviceId);

        return proteusService.requestChannel((Flux<Payload>)publisher);
      }
    });
  }

  @Override
  public Mono<Void> metadataPush(Payload payload) {
    try {
      ByteBuf metadata = Unpooled.wrappedBuffer(payload.getMetadata());
      int namespaceId = ProteusMetadata.namespaceId(metadata);
      int serviceId = ProteusMetadata.serviceId(metadata);

      ProteusService proteusService = registeredServices.get(namespaceId, serviceId);

      if (proteusService == null) {
        return Mono.error(new ServiceNotFound(namespaceId, serviceId));
      }

      return proteusService.metadataPush(payload);

    } catch (Throwable t) {
      return Mono.error(t);
    }
  }

  @Override
  public Mono<Void> onClose() {
    return onClose;
  }

  @Override
  public Mono<Void> close() {
    return Mono.fromRunnable(
        new Runnable() {
          @Override
          public void run() {
            onClose.onComplete();

            registeredServices.forEach(
                new BiInt2ObjectMap.EntryConsumer<ProteusService>() {
                  @Override
                  public void accept(ProteusService service) {
                    service.close().subscribe();
                  }
                });
          }
        });
  }

  @Override
  public double availability() {
    return 1.0;
  }
}
