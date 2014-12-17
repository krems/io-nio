package com.db.train.atm.server;

import com.db.train.atm.ATMData;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

class SelectorReactor implements Runnable {
    private final Queue<SocketChannel> registerQueue = new ConcurrentLinkedQueue<>();
    private final Queue<ATMData> resultQueue;
    private final Selector selector;

    public SelectorReactor(Queue<ATMData> resultQueue, Selector selector) {
        this.resultQueue = resultQueue;
        this.selector = selector;
    }

    public void enqueueForRegister(SocketChannel channel) {
        if (!registerQueue.offer(channel)) {
            System.err.println("Failed to enqueue for register");
        }
    }

    public void wakeUpSelector() {
        selector.wakeup();
    }

    @Override
    public void run() {
        try {
            runReactorLoop();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void runReactorLoop() throws IOException {
        while (!Thread.currentThread().isInterrupted()) {
            selector.select();
            Set<SelectionKey> keys = selector.selectedKeys();
            if (!keys.isEmpty()) {
                keys.forEach(key -> {
                    Runnable attachment = (Runnable) key.attachment();
                    attachment.run();
                });
                keys.clear();
            }
            SocketChannel channel = registerQueue.poll();
            if (channel != null) {
                SelectionKey key = channel.register(selector, SelectionKey.OP_READ);
                key.attach(new Reader(key, resultQueue));
            }
        }
    }
}
