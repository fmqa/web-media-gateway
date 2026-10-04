package com.github.fmqa.spu.io;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

/**
 * Buffered, blocking audio frame writer.
 * <p></p>
 * Reads audio frames from a shared queue with given latency requirements. If frames aren't available within latency
 * constraints, inserts silent frames.
 * @see JitterBuffer
 */
@Component
public class Consumer {
    public void consume(int n, int k, OutputStream out, BlockingQueue<byte[]> buffer, Lock lock, Condition signal) {
        try {
            final double duration = (double) n * 1_000_000_000.0 / k;
            final byte[] silence = new byte[n];
            final long start = System.nanoTime();
            long frames = 0;
            for (; ; ) {
                final byte[] frame;
                try {
                    frame = buffer.poll((long) duration, TimeUnit.NANOSECONDS);
                } catch (InterruptedException e) {
                    break;
                }
                if (frame == Producer.EOF) {
                    break;
                }
                try {
                    out.write(frame == null ? silence : frame);
                    out.flush();
                } catch (IOException e) {
                    break;
                }
                frames++;
                final long next = start + (long) (frames * duration);
                long sleep = next - System.nanoTime();
                if (sleep > 0) {
                    lock.lock();
                    try {
                        while (sleep > 0 && buffer.peek() != Producer.EOF) {
                            sleep = signal.awaitNanos(sleep);
                        }
                    } catch (InterruptedException e) {
                        break;
                    } finally {
                        lock.unlock();
                    }
                }
            }
        } finally {
            // Unblock producer if blocked on buffer.put()
            buffer.clear();
        }
    }
}
