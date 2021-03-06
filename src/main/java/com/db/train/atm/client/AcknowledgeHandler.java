package com.db.train.atm.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

class AcknowledgeHandler implements Runnable {
    private static final Logger log = LoggerFactory.getLogger(Writer.class);
    private final SelectionKey key;
    private final SocketChannel channel;
    private final ByteBuffer buf = ByteBuffer.allocate(1);
    private final long startTimestamp;

    public AcknowledgeHandler(long startTimestamp, SelectionKey key) {
        this.key = key;
        this.channel = (SocketChannel) key.channel();
        this.startTimestamp = startTimestamp;
    }

    @Override
    public void run() {
        catchAck();
        turnOnWriter();
    }

    private void catchAck() {
        try {
            readAckMessage();
        } catch (IOException e) {
            handleException(e);
        }
    }

    private void readAckMessage() throws IOException {
        if (channel.read(buf) <= 0) {
            key.cancel();
            key.selector().wakeup();
            channel.close();
        }
        log.info("Delay: {} ms", (System.nanoTime() - startTimestamp) / 1e6);
    }

    private void turnOnWriter() {
        key.interestOps(SelectionKey.OP_WRITE);
        key.attach(new Writer(key));
        key.selector().wakeup();
    }

    private void handleException(Exception e) {
        log.error("Error reading ack message", e);
        key.cancel();
        key.selector().wakeup();
        try {
            channel.close();
        } catch (IOException ex) {
            log.error("Error closing channel", ex);
        }
        throw new RuntimeException(e);
    }
}
