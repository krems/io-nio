package com.db.train.atm.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Set;

public class Client {
    private static final Logger log = LoggerFactory.getLogger(ATMSpamer.class);
    private final Set<InetSocketAddress> servers;
    private final Selector selector;
    private volatile boolean running;

    public Client(Set<InetSocketAddress> servers) {
        this.servers = servers;
        try {
            selector = Selector.open();
        } catch (IOException e) {
            log.error("Error opening selector");
            throw new RuntimeException(e);
        }
    }

    public void stop() {
        running = false;
        selector.wakeup();
    }

    public void start() {
        servers.forEach(this::createConnection);
        try {
            runReactorLoop();
        } catch (IOException e) {
            log.error("Error in reactor", e);
            throw new RuntimeException(e);
        }
    }

    private void createConnection(InetSocketAddress server) {
        try {
            createAndRegisterChannel(server);
        } catch (IOException e) {
            log.error("Error creating connection", e);
            throw new RuntimeException(e);
        }
    }

    private void createAndRegisterChannel(InetSocketAddress server) throws IOException {
        SocketChannel channel = SocketChannel.open();
        channel.configureBlocking(false);
        channel.connect(server);
        awaitConnectionEstablished(channel);
        SelectionKey key = channel.register(selector, SelectionKey.OP_WRITE | SelectionKey.OP_CONNECT);
        key.attach(new Writer(key));
    }

    private void awaitConnectionEstablished(SocketChannel channel) {
        try {
            while (!channel.finishConnect()) {
                Thread.sleep(1);
            }
        } catch (IOException | InterruptedException e) {
            log.error("Error awaiting connection established", e);
            throw new RuntimeException(e);
        }
    }

    private void runReactorLoop() throws IOException {
        running = true;
        selector.wakeup();
        while (running && !Thread.currentThread().isInterrupted()) {
            selector.select();
            Set<SelectionKey> keys = selector.selectedKeys();
            if (!keys.isEmpty()) {
                keys.forEach(key -> {
                    Runnable attachment = (Runnable) key.attachment();
                    attachment.run();
                });
                keys.clear();
            }
        }
    }
}
