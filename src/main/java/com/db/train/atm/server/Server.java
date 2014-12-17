package com.db.train.atm.server;

import com.db.train.atm.ATMData;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

// 41_751.107629655206msg/sec | 12_593.748157790473msg/sec
public class Server {
    static final int CLIENT_SELECTORS_NUMBER = Runtime.getRuntime().availableProcessors();
    private final ExecutorService executor = Executors.newFixedThreadPool(CLIENT_SELECTORS_NUMBER + 2);
    private final BlockingQueue<ATMData> resultQueue = new LinkedBlockingQueue<>(200_000);
    private final Selector acceptorSelector;
    private final int port;
    private volatile boolean running;

    public Server(int port) {
        this.port = port;
        try {
            this.acceptorSelector = Selector.open();
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public void start() {
        ATMDataParser dataParser = new ATMDataParser(resultQueue, executor);
        dataParser.start();
        prepareServerSocket();
        runAcceptorReactorLoop();
    }

    public void stop() {
        running = false;
        acceptorSelector.wakeup();
    }

    private void runAcceptorReactorLoop() {
        running = true;
        while (running && !Thread.currentThread().isInterrupted()) {
            try {
                acceptorSelector.select();
                Set<SelectionKey> keys = acceptorSelector.selectedKeys();
                if (!keys.isEmpty()) {
                    keys.forEach(key -> {
                        Runnable attachment = (Runnable) key.attachment();
                        attachment.run();
                    });
                    keys.clear();
                }
            } catch (IOException e) {
                e.printStackTrace();
                running = false;
                executor.shutdownNow();
                throw new RuntimeException(e);
            }
        }
        executor.shutdownNow();
    }

    private void prepareServerSocket() {
        try {
            ServerSocketChannel srvSocket = ServerSocketChannel.open();
            srvSocket.socket().bind(new InetSocketAddress(port));
            srvSocket.configureBlocking(false);
            SelectionKey key = srvSocket.register(acceptorSelector, SelectionKey.OP_ACCEPT);
            key.attach(new Acceptor(executor, key, resultQueue));
            acceptorSelector.wakeup();
        } catch (IOException e) {
            e.printStackTrace();
            running = false;
            throw new RuntimeException(e);
        }
    }
}
