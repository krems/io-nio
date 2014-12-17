package com.db.train.atm.client;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

class Acknowledger implements Runnable {
    private final SelectionKey key;
    private final SocketChannel channel;
    private final ByteBuffer buf = ByteBuffer.allocate(1);
    private final long startTimestamp;

    public Acknowledger(long startTimestamp, SelectionKey key) {
        this.key = key;
        this.channel = (SocketChannel) key.channel();
        this.startTimestamp = startTimestamp;
    }

    @Override
    public void run() {
        try {
            if (channel.read(buf) <= 0) {
                key.cancel();
                key.selector().wakeup();
                channel.close();
            }
            System.out.println("Delay: " + ((System.nanoTime() - startTimestamp) / 1e6) + "ms");
        } catch (IOException e) {
            handleException(e);
        }
        key.interestOps(SelectionKey.OP_WRITE);
        key.attach(new Writer(key));
        key.selector().wakeup();
    }

    private void handleException(IOException e) {
        e.printStackTrace();
        key.cancel();
        key.selector().wakeup();
        try {
            channel.close();
        } catch (IOException e1) {
            e1.printStackTrace();
        }
    }
}
