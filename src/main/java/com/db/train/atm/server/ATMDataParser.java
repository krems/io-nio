package com.db.train.atm.server;

import com.db.train.atm.ATMData;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;

class ATMDataParser {
    private final BlockingQueue<ATMData> resultQueue;
    private final ExecutorService executor;

    public ATMDataParser(BlockingQueue<ATMData> resultQueue, ExecutorService executor) {
        this.resultQueue = resultQueue;
        this.executor = executor;
    }

    public void start() {
        executor.submit(this::parse);
    }

    private void parse(){
        while(!Thread.currentThread().isInterrupted()) {
            try {
                ATMData take = resultQueue.take();
//                System.out.println("Parsed: ask:" + take.getAsk() + ", bid:" + take.getBid());
            } catch (InterruptedException e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        }
    }
}
