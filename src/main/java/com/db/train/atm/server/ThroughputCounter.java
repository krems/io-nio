package com.db.train.atm.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicLong;

class ThroughputCounter {
    private static final Logger log = LoggerFactory.getLogger(ThroughputCounter.class);
    public static final AtomicLong ops = new AtomicLong();

    public static void run() {
        new Thread(() -> {
            long start = System.nanoTime();
            await(120_000);
            log.debug("Throughput: {} msg/sec", ops.get() * 1e9 / (System.nanoTime() - start));
        }).start();
    }

    private static void await(long timeout) {
        try {
            Thread.sleep(timeout);
        } catch (InterruptedException e) {
            log.error("ThroughputCounter interrupted", e);
        }
    }
}
