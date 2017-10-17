package io.netifi.proteus.util;

import java.security.SecureRandom;
import java.util.Random;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;

public class TimebasedIdGenerator {
    private static final AtomicIntegerFieldUpdater<TimebasedIdGenerator> COUNTER =
        AtomicIntegerFieldUpdater.newUpdater(TimebasedIdGenerator.class, "counter");
    private final int id;

    @SuppressWarnings("unused")
    private volatile int counter = 0;

    public TimebasedIdGenerator(int id) {
        this.id = Math.abs(id) << 16;

        Random rnd = new SecureRandom();
        counter = rnd.nextInt(65535);
    }

    public long nextId() {
        int count = COUNTER.getAndIncrement(this) & 65535;
        long time = System.currentTimeMillis() << 32;
        return time | count | id;
    }
}