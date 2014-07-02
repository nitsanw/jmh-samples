package psy.lob.saw.conc;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLongArray;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;

public class FalseSharingBenchmarkVolatile {
    private final static int LONGS_IN_CACHELINE = Integer.getInteger("longs.in.cacheline", 8);
    private final static int NUMBER_OF_THREADS = Integer.getInteger("threads", 32);
    private final static AtomicInteger THREAD_INDEX = new AtomicInteger(0);

    @State(Scope.Benchmark)
    public static class SharedCounters {
        private final AtomicLongArray array = new AtomicLongArray(LONGS_IN_CACHELINE * (NUMBER_OF_THREADS + 2));
    }

    @State(Scope.Thread)
    public static class ThreadIndex {
        /**
         * First shared access index points to location which is buffered to the
         * left by a cacheline but all others are lined up sequentially causing
         * false sharing between them. Last index points to a location which is
         * buffered to the right.
         */
        private final int falseSharedIndex = LONGS_IN_CACHELINE + THREAD_INDEX.getAndIncrement();

        /**
         * Each index is buffered by a cacheline to it's left and the last by a
         * cacheline to the right.
         */
        private final int noSharingIndex = LONGS_IN_CACHELINE + (falseSharedIndex - LONGS_IN_CACHELINE)
                * LONGS_IN_CACHELINE;
    }

    @Benchmark
    public void measureUnshared(SharedCounters counters, ThreadIndex index) {
        long value = counters.array.get(index.noSharingIndex);
        counters.array.set(index.noSharingIndex, value + 1);
    }

    @Benchmark
    public void measureShared(SharedCounters counters, ThreadIndex index) {
        long value = counters.array.get(index.falseSharedIndex);
        counters.array.set(index.falseSharedIndex, value + 1);
    }
}
