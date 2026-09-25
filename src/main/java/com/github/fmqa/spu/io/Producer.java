package com.github.fmqa.spu.io;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

/**
 * Buffered, blocking audio frame reader.
 * <p></p>
 * Fills a shared queue with audio frames read from an {@link InputStream}.
 * @see JitterBuffer
 */
@Component
public class Producer {
    static final byte[] EOF = new byte[0];

    @Async
    public void produce(int n, InputStream in, BlockingQueue<byte[]> buffer, Lock lock, Condition signal) {
        try {
            for (;;) {
                byte[] window = new byte[n];
                final int nread;
                try {
                    nread = in.readNBytes(window, 0, n);
                } catch (IOException e) {
                    return;
                }
                if (nread == 0) {
                    return;
                }
                try {
                    buffer.put(window);
                } catch (InterruptedException e) {
                    return;
                }
            }
        } finally {
            try {
                buffer.put(EOF);
            } catch (InterruptedException _) {
            }
            lock.lock();
            try {
                signal.signalAll();
            } finally {
                lock.unlock();
            }
        }
    }
}
