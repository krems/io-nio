package com.db.train.atm.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

class Acknowledger implements Runnable {
    private static final Logger log = LoggerFactory.getLogger(Acknowledger.class);
    private final SelectionKey key;
    private final SocketChannel channel;
    private final ByteBuffer buf = ByteBuffer.allocate(1);

    public Acknowledger(SelectionKey key) {
        this.key = key;
        this.channel = (SocketChannel) key.channel();
    }

    @Override
    public void run() {
        prepareDataToSend();
        while (buf.hasRemaining()) {
            send(buf);
        }
        log.trace("Ack sent");
    }

    private void prepareDataToSend() {
        buf.clear();
        buf.put(new byte[1]);
        buf.flip();
    }

    private void send(ByteBuffer buf) {
        try {
            channel.write(buf);
        } catch (IOException e) {
            log.error("Error writing to channel", e);
            key.cancel();
            key.selector().wakeup();
            try {
                channel.close();
            } catch (IOException e1) {
                log.error("Error closing channel");
            }
            throw new RuntimeException(e);
        }
    }
}
