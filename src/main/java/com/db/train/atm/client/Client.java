package com.db.train.atm.client;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Set;

public class Client {
    private final Set<InetSocketAddress> servers;
    private volatile boolean running;
    private final Selector selector;

    public Client(Set<InetSocketAddress> servers) {
        this.servers = servers;
        try {
            selector = Selector.open();
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public void start() {
        servers.forEach(this::connectAndSend);
        try {
            runReactorLoop();
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
    public void stop() {
        running = false;
        selector.wakeup();
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

    private void connectAndSend(InetSocketAddress server) {
        try {
            SocketChannel channel = SocketChannel.open();
            channel.configureBlocking(false);
            channel.connect(server);
            try {
                if (!channel.finishConnect()) {
                    System.err.println("Not yet connected");
                    return;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            SelectionKey key = channel.register(selector, SelectionKey.OP_WRITE | SelectionKey.OP_CONNECT);
            key.attach(new Writer(key));
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
}
