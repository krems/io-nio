package com.db.train.atm.server;

import com.db.train.atm.ATMData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;

class ATMDataParser {
    private static final Logger log = LoggerFactory.getLogger(ATMDataParser.class);
    private final BlockingQueue<ATMData> resultQueue;
    private final ExecutorService executor;

    public ATMDataParser(BlockingQueue<ATMData> resultQueue, ExecutorService executor) {
        this.resultQueue = resultQueue;
        this.executor = executor;
    }

    public void start() {
        executor.submit(this::parse);
        log.info("ATMData parser started");
    }

    private void parse() {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                processAtmData();
            }
        } catch (InterruptedException e) {
            log.error("Interrupted parsing data", e);
            log.info("ATMData parser stopped");
        }
    }

    private void processAtmData() throws InterruptedException {
        ATMData take = resultQueue.take();
        log.trace("Parsed: ask: {}, bid: ", take.getAsk(), take.getBid());
    }
}
