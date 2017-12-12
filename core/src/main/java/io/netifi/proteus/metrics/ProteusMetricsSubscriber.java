package io.netifi.proteus.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Timer;
import org.reactivestreams.Subscription;
import reactor.core.CoreSubscriber;
import reactor.core.publisher.Operators;
import reactor.util.context.Context;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class ProteusMetricsSubscriber<T> extends AtomicBoolean implements Subscription, CoreSubscriber<T> {
  private final CoreSubscriber<? super T> actual;
  private final Counter success, error, cancelled;
  private final Timer timer;

  private Subscription s;
  private long start;

  ProteusMetricsSubscriber(CoreSubscriber<? super T> actual, Counter success, Counter error, Counter cancelled, Timer timer) {
    this.actual = actual;
    this.success = success;
    this.error = error;
    this.cancelled = cancelled;
    this.timer = timer;
  }

  @Override
  public void onSubscribe(Subscription s) {
    if (Operators.validate(this.s, s)) {
      this.s = s;
      this.start = System.nanoTime();

      actual.onSubscribe(this);
    }
  }

  @Override
  public void onNext(T t) {
    actual.onNext(t);
  }

  @Override
  public void onError(Throwable t) {
    if (compareAndSet(false, true)) {
      error.increment();
      timer.record(System.nanoTime() - start, TimeUnit.NANOSECONDS);
    }
    actual.onError(t);
  }

  @Override
  public void onComplete() {
    if (compareAndSet(false, true)) {
      success.increment();
      timer.record(System.nanoTime() - start, TimeUnit.NANOSECONDS);
    }
    actual.onComplete();
  }

  @Override
  public void request(long n) {
    s.request(n);
  }

  @Override
  public void cancel() {
    if (compareAndSet(false, true)) {
      cancelled.increment();
      timer.record(System.nanoTime() - start, TimeUnit.NANOSECONDS);
    }
    s.cancel();
  }

  @Override
  public Context currentContext() {
    return actual.currentContext();
  }
}
