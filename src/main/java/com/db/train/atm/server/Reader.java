package com.db.train.atm.server;

import com.db.train.atm.ATMData;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.Queue;

class Reader implements Runnable {
    private static final int OBJECT_SIZE;
    static {
        // todo: ugly
        ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
        try {
            ObjectOutputStream outputStream = new ObjectOutputStream(byteOut);
            outputStream.writeObject(ATMData.generate());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        OBJECT_SIZE = byteOut.toByteArray().length;
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
            if (channel.read(buf) <= 0) {
                key.cancel();
                key.selector().wakeup();
                channel.close();
                return;
            } else if (buf.remaining() == 0) {
                ATMData data = deserialize(buf.array());
                if (!resultQueue.offer(data)) {
                    System.err.println("Couldn't publish result");
                }
//            new Acknowledger(key).run();
                ThroughputCounter.ops.incrementAndGet();
                buf.clear();
            }
//            ThroughputCounter.rps.incrementAndGet();
        } catch (IOException e) {
            handleException(e);
        }
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

    private ATMData deserialize(byte[] array) {
        ByteArrayInputStream bis = new ByteArrayInputStream(array);
        try (ObjectInput in = new ObjectInputStream(bis)) {
            return (ATMData) in.readObject();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
}
