package com.db.train.atm.server;

import com.db.train.atm.ATMData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.Queue;

class Reader implements Runnable {
    private static final Logger log = LoggerFactory.getLogger(Reader.class);
    private static final int OBJECT_SIZE;
    static {
        OBJECT_SIZE = calculateObjectSize();
    }
    private final Queue<ATMData> resultQueue;
    private final SelectionKey key;
    private final SocketChannel channel;
    private final ByteBuffer buf = ByteBuffer.allocate(OBJECT_SIZE);

    public Reader(SelectionKey key, Queue<ATMData> resultQueue) {
        this.key = key;
        this.channel = (SocketChannel) key.channel();
        this.resultQueue = resultQueue;
    }

    @Override
    public void run() {
        try {
            receive();
        } catch (Exception e) {
            handleException(e);
        }
    }

    private void receive() throws IOException {
        if (channel.read(buf) <= 0) {
            key.cancel();
            key.selector().wakeup();
            channel.close();
        } else if (buf.remaining() == 0) {
            processReceivedData();
        }
    }

    private void processReceivedData() {
        ATMData data = deserialize(buf.array());
        if (!resultQueue.offer(data)) {
            System.err.println("Couldn't publish result");
        }
        new Acknowledger(key).run();
        ThroughputCounter.ops.incrementAndGet();
        buf.clear();
    }

    private void handleException(Exception e) {
        log.error("Error occurred", e);
        key.cancel();
        key.selector().wakeup();
        try {
            channel.close();
        } catch (IOException ex) {
            log.error("Error closing channel", ex);
        }
        throw new RuntimeException(e);
    }

    private ATMData deserialize(byte[] array) {
        ByteArrayInputStream bis = new ByteArrayInputStream(array);
        try (ObjectInput in = new ObjectInputStream(bis)) {
            return (ATMData) in.readObject();
        } catch (IOException | ClassNotFoundException e) {
            log.error("Error during deserialization", e);
            throw new RuntimeException(e);
        }
    }

    private static int calculateObjectSize() {
        // todo: ugly
        ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
        try {
            ObjectOutputStream outputStream = new ObjectOutputStream(byteOut);
            outputStream.writeObject(ATMData.generate());
        } catch (Exception e) {
            log.error("Error calculating object size", e);
            throw new RuntimeException(e);
        }
        return byteOut.toByteArray().length;
    }
}
