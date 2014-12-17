package com.db.train.atm.server;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

class Acknowledger implements Runnable {
    private final SelectionKey key;
    private final SocketChannel channel;
    private final ByteBuffer buf = ByteBuffer.allocate(1);

    public Acknowledger(SelectionKey key) {
        this.key = key;
        this.channel = (SocketChannel) key.channel();
    }

    @Override
    public void run() {
        buf.clear();
        buf.put(new byte[1]);
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
//        System.out.println("Ack sent");
    }
}
