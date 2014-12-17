package com.db.train.atm.server;

import java.util.concurrent.atomic.AtomicLong;

class ThroughputCounter {
//    public static final AtomicLong rps = new AtomicLong();
    public static final AtomicLong ops = new AtomicLong();

    public static void run() {
        new Thread(() -> {
            long start = System.nanoTime();
            try {
                Thread.sleep(120_000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            long opsOnDone = ops.get();
            System.err.println("\n\n\nThroughput: " + opsOnDone * 1e9 / (System.nanoTime() - start) + "msg/sec\n\n\n");
            System.err.println(ops.get());
//            System.err.println(rps.get());
            System.exit(1);
        }).start();
    }
}
