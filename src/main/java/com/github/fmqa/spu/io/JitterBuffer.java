package com.github.fmqa.spu.io;

import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.io.OutputStream;
import java.time.Duration;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Jitter buffered I/O provider.
 */
@Component
public record JitterBuffer(Consumer consumer, Producer producer) {
    /**
     * Perform a jitter-buffered audio sample copy from {@code in} to {@code out}.
     * @param rate audio byte rate in bytes per second (e.g. {@code 2 * channels * samplerate} for audio/l16
     * @param duration duration of a single buffer window chunk
     * @param target target buffer delay, used to compute target rate count
     * @param max maximum buffer delay capacity
     * @param in source stream from which incoming audio bytes are read
     * @param out destination stream to which timed audio frames are written
     */
    public void copy(int rate, Duration duration, Duration target, Duration max, InputStream in, OutputStream out) {
        requirePositive(duration, "duration must be >= 0");
        requirePositive(target, "target must be >= 0");
        requirePositive(max, "max must be >= 0");
        final int window = Math.toIntExact(Math.round((double) rate * duration.toNanos() / 1_000_000_000.0));
        final int ftarget = Math.max(1, (int) Math.round((double) target.toNanos() / duration.toNanos()));
        final int fmax = Math.max(ftarget, (int) Math.ceil((double) max.toNanos() / duration.toNanos()));
        final BlockingQueue<byte[]> buffer = new ArrayBlockingQueue<>(fmax);
        final ReentrantLock lock = new ReentrantLock();
        final Condition signal = lock.newCondition();
        producer.produce(window, in, buffer, lock, signal);
        consumer.consume(window, rate, out, buffer, lock, signal);
    }

    private static void requirePositive(Duration duration, String message) {
        if (!duration.isPositive()) {
            throw new IllegalArgumentException(message);
        }
    }
}
