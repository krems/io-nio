package com.db.train.atm.server;

import com.db.train.atm.ATMData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

// 41_751.107629655206 msg/sec (multiple clients) | 12_593.748157790473 msg/sec (single client)
public class Server {
    static final int CLIENT_SELECTORS_NUMBER = Runtime.getRuntime().availableProcessors();
    private static final Logger log = LoggerFactory.getLogger(Server.class);
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
            log.error("Error opening selector", e);
            throw new RuntimeException(e);
        }
    }

    public void stop() {
        running = false;
        acceptorSelector.wakeup();
        log.info("Server stopped");
    }

    public void start() {
        startATMDataParser();
        prepareServerSocket();
        executor.submit(this::runAcceptorReactorLoop);
    }

    private void startATMDataParser() {
        ATMDataParser dataParser = new ATMDataParser(resultQueue, executor);
        dataParser.start();
    }

    private void prepareServerSocket() {
        try {
            doPrepareServerSocket();
        } catch (Exception e) {
            log.error("Error preparing server socket channel", e);
            running = false;
            throw new RuntimeException(e);
        }
    }

    private void doPrepareServerSocket() throws IOException {
        ServerSocketChannel srvSocket = ServerSocketChannel.open();
        srvSocket.socket().bind(new InetSocketAddress(port));
        srvSocket.configureBlocking(false);
        SelectionKey key = srvSocket.register(acceptorSelector, SelectionKey.OP_ACCEPT);
        key.attach(new Acceptor(executor, key, resultQueue));
        acceptorSelector.wakeup();
    }

    private void runAcceptorReactorLoop() {
        running = true;
        try {
            doRunReactorLoop();
        } catch (Exception e) {
            log.error("Error in server reactor", e);
            running = false;
            executor.shutdownNow();
            throw new RuntimeException(e);
        }
        executor.shutdownNow();
    }

    private void doRunReactorLoop() throws IOException {
        while (running && !Thread.currentThread().isInterrupted()) {
            acceptorSelector.select();
            processSelected();
        }
    }

    private void processSelected() {
        Set<SelectionKey> keys = acceptorSelector.selectedKeys();
        if (!keys.isEmpty()) {
            keys.forEach(key -> {
                Runnable attachment = (Runnable) key.attachment();
                attachment.run();
            });
            keys.clear();
        }
    }
}
