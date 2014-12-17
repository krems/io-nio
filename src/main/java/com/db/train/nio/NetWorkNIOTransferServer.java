package com.db.train.nio;

import com.db.train.CommonUtils;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

// 82_962ms
public class NetWorkNIOTransferServer {
    private static final int NETWORK_BUFFER_SIZE = 128 * 1024;
    private static final ByteBuffer POISON = ByteBuffer.allocate(0);
    private static final Queue<ByteBuffer> inputQueue = new ConcurrentLinkedQueue<>();

    private static Selector srvSelector;
    private static volatile boolean readIsRunning;
    private static volatile boolean writeIsRunning;
    private static long start;

    public static void main(String[] args) {
        openSelector();
        prepareServerSocketChannel();
        Writer writer = prepareFileReaderChannel();
        runMainReactorLoop(writer);
    }

    private static void openSelector() {
        try {
            srvSelector = Selector.open();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void runMainReactorLoop(Writer writer) {
        readIsRunning = true;
        writeIsRunning = true;
        while (isRunning() && !Thread.currentThread().isInterrupted()) {
            try {
                int selected = srvSelector.select();
                if (selected > 0) {
                    Set<SelectionKey> keys = srvSelector.selectedKeys();
                    keys.forEach(key -> {
                        Runnable attachment = (Runnable) key.attachment();
                        attachment.run();
                    });
                    keys.clear();
                }
                writer.run();
            } catch (IOException e) {
                e.printStackTrace();
                readIsRunning = false;
                writeIsRunning = false;
                inputQueue.offer(POISON);
                srvSelector.wakeup();
            }
        }
        closeSelector();
        System.out.println("Copy took " + ((System.nanoTime() - start) / 1e6) + "ms");
    }

    private static void closeSelector() {
        try {
            srvSelector.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static boolean isRunning() {
        return readIsRunning || writeIsRunning;
    }

    private static void prepareServerSocketChannel() {
        try {
            ServerSocketChannel srvSocket = ServerSocketChannel.open();
            srvSocket.socket().bind(new InetSocketAddress(CommonUtils.DEFAULT_PORT));
            srvSocket.configureBlocking(false);
            SelectionKey sk = srvSocket.register(srvSelector, SelectionKey.OP_ACCEPT);
            sk.attach(new Acceptor(sk));
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
    private static Writer prepareFileReaderChannel() {
        try {
            RandomAccessFile inputFile = new RandomAccessFile(CommonUtils.DEFAULT_DEST_PATH, "rw");
            FileChannel channel = inputFile.getChannel();
            return new Writer(channel);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    private static class Acceptor implements Runnable {
        private final ServerSocketChannel srvSocket;
        private final SelectionKey sk;

        private Acceptor(SelectionKey sk) {
            this.sk = sk;
            this.srvSocket = (ServerSocketChannel) sk.channel();
        }

        @Override
        public void run() {
            try {
                SocketChannel inputSocket = srvSocket.accept();
                if (inputSocket != null) {
                    start = System.nanoTime();
                    inputSocket.configureBlocking(false);
                    SelectionKey sk = inputSocket.register(srvSelector, SelectionKey.OP_READ);
                    sk.attach(new Reader(sk));
                    this.sk.cancel();
                }
            } catch (IOException e) {
                e.printStackTrace();
                readIsRunning = false;
                writeIsRunning = false;
                inputQueue.offer(POISON);
                sk.cancel();
                srvSelector.wakeup();
            }
        }
    }

    private static class Reader implements Runnable {
        private final SocketChannel socket;
        private final SelectionKey sk;

        private Reader(SelectionKey sk) {
            this.sk = sk;
            this.socket = (SocketChannel) sk.channel();
        }

        @Override
        public void run() {
            ByteBuffer buf = ByteBuffer.allocate(NETWORK_BUFFER_SIZE);
            try {
                if (socket.read(buf) <= 0) {
                    readIsRunning = false;
                    inputQueue.offer(POISON);
                    sk.cancel();
                    srvSelector.wakeup();
                    socket.close();
                } else if (!inputQueue.offer(buf)) {
                    System.err.println("Couldn't put read bytes");
                }
            } catch (IOException e) {
                e.printStackTrace();
                readIsRunning = false;
                inputQueue.offer(POISON);
                sk.cancel();
                srvSelector.wakeup();
            }
        }
    }

    private static class Writer implements Runnable {
        private final FileChannel in;

        private Writer(FileChannel inputChannel) {
            this.in = inputChannel;
        }

        @Override
        public void run() {
            ByteBuffer buf = inputQueue.poll();
            if (buf != null) {
                if (buf == POISON) {
                    writeIsRunning = false;
                    return;
                }
                buf.flip();
                try {
                    while (buf.hasRemaining()) {
                        in.write(buf);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    readIsRunning = false;
                    writeIsRunning = false;
                    srvSelector.wakeup();
                }
            }
        }
    }
}
