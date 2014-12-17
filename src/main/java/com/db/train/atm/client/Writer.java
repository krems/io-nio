package com.db.train.atm.client;

import com.db.train.atm.ATMData;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

class Writer implements Runnable {
    private static final int BUF_SIZE = 128 * 1024;
    private final SelectionKey key;
    private final SocketChannel channel;
    private final ByteBuffer buf = ByteBuffer.allocate(BUF_SIZE);

    public Writer(SelectionKey key) {
        this.key = key;
        this.channel = (SocketChannel) key.channel();
    }

    @Override
    public void run() {
        ATMData generate = ATMData.generate();
        buf.clear();
        buf.put(serialize(generate));
        buf.flip();
        while (buf.hasRemaining()) {
            try {
                channel.write(buf);
            } catch (IOException e) {
                e.printStackTrace();
                key.cancel();
                key.selector().wakeup();
                try {
                    channel.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
                throw new RuntimeException(e);
            }
        }
//        key.interestOps(SelectionKey.OP_READ);
//        key.attach(new Acknowledger(System.nanoTime(), key));
//        key.selector().wakeup();
    }

    private byte[] serialize(ATMData generate) {
        ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
        ObjectOutputStream outputStream;
        try {
            outputStream = new ObjectOutputStream(byteOut);
            outputStream.writeObject(generate);
        } catch (IOException e) {
            e.printStackTrace();
            key.cancel();
            key.selector().wakeup();
            try {
                channel.close();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
            throw new RuntimeException(e);
        }
        return byteOut.toByteArray();
    }
}
