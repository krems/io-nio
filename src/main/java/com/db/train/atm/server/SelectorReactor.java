package com.db.train.atm.server;

import com.db.train.atm.ATMData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

class SelectorReactor implements Runnable {
    private static final Logger log = LoggerFactory.getLogger(SelectorReactor.class);
    private final Queue<SocketChannel> registerQueue = new ConcurrentLinkedQueue<>();
    private final Queue<ATMData> resultQueue;
    private final Selector selector;

    public SelectorReactor(Queue<ATMData> resultQueue, Selector selector) {
        this.resultQueue = resultQueue;
        this.selector = selector;
    }

    public void enqueueForRegister(SocketChannel channel) {
        if (!registerQueue.offer(channel)) {
            log.warn("Failed to enqueue for register");
            try {
                channel.close();
            } catch (IOException e) {
                log.error("Error closing channel", e);
            }
        }
    }

    public void wakeUpSelector() {
        selector.wakeup();
    }

    @Override
    public void run() {
        try {
            runReactorLoop();
        } catch (Exception e) {
            log.error("Error in selector reactor", e);
            throw new RuntimeException(e);
        }
    }

    private void runReactorLoop() throws IOException {
        while (!Thread.currentThread().isInterrupted()) {
            selector.select();
            processSelected();
            registerNewChannels();
        }
    }

    private void processSelected() {
        Set<SelectionKey> keys = selector.selectedKeys();
        if (!keys.isEmpty()) {
            keys.forEach(key -> {
                Runnable attachment = (Runnable) key.attachment();
                attachment.run();
            });
            keys.clear();
        }
    }

    private void registerNewChannels() throws ClosedChannelException {
        SocketChannel channel = registerQueue.poll();
        if (channel != null) {
            SelectionKey key = channel.register(selector, SelectionKey.OP_READ);
            key.attach(new Reader(key, resultQueue));
        }
    }
}
