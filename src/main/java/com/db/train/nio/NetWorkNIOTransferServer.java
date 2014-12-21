package com.db.train.nio;

import com.db.train.CommonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    private static final Logger log = LoggerFactory.getLogger(NetWorkNIOTransferServer.class);
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
        closeSelector();
        log.debug("Copy took {} ms", (System.nanoTime() - start) / 1e6);
    }

    private static void openSelector() {
        try {
            srvSelector = Selector.open();
        } catch (IOException e) {
            log.error("Error opening selector", e);
            throw new RuntimeException(e);
        }
    }

    private static void prepareServerSocketChannel() {
        try {
            ServerSocketChannel srvSocket = ServerSocketChannel.open();
            srvSocket.socket().bind(new InetSocketAddress(CommonUtils.DEFAULT_PORT));
            srvSocket.configureBlocking(false);
            SelectionKey sk = srvSocket.register(srvSelector, SelectionKey.OP_ACCEPT);
            sk.attach(new Acceptor(sk));
        } catch (IOException e) {
            log.error("Error preparing socket channel", e);
            throw new RuntimeException(e);
        }
    }

    private static Writer prepareFileReaderChannel() {
        try {
            RandomAccessFile inputFile = new RandomAccessFile(CommonUtils.DEFAULT_DEST_PATH, "rw");
            FileChannel channel = inputFile.getChannel();
            return new Writer(channel);
        } catch (FileNotFoundException e) {
            log.error("Error opening file channel", e);
            throw new RuntimeException(e);
        }
    }

    private static void runMainReactorLoop(Writer writer) {
        readIsRunning = true;
        writeIsRunning = true;
        while (isRunning() && !Thread.currentThread().isInterrupted()) {
            try {
                srvSelector.select();
                processSelected(writer);
            } catch (IOException e) {
                log.error("Error in server reactor", e);
                readIsRunning = false;
                writeIsRunning = false;
            }
        }
    }

    private static void processSelected(Writer writer) {
        Set<SelectionKey> keys = srvSelector.selectedKeys();
        keys.forEach(key -> {
            Runnable attachment = (Runnable) key.attachment();
            attachment.run();
        });
        keys.clear();
        writer.run();
    }

    private static void closeSelector() {
        try {
            srvSelector.close();
        } catch (IOException e) {
            log.error("Error closing selector", e);
        }
    }

    private static boolean isRunning() {
        return readIsRunning || writeIsRunning;
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
                accept();
            } catch (IOException e) {
                log.error("Error accepting connection", e);
                handleException();
            } finally {
                sk.cancel();
                srvSelector.wakeup();
            }
        }

        private void accept() throws IOException {
            SocketChannel inputSocket = srvSocket.accept();
            if (inputSocket != null) {
                if (log.isDebugEnabled()) {
                    start = System.nanoTime();
                }
                prepareInputSocketChannel(inputSocket);
            }
        }

        private void prepareInputSocketChannel(SocketChannel inputSocket) throws IOException {
            inputSocket.configureBlocking(false);
            SelectionKey sk = inputSocket.register(srvSelector, SelectionKey.OP_READ);
            sk.attach(new Reader(sk));
        }

        private void handleException() {
            readIsRunning = false;
            writeIsRunning = false;
            inputQueue.offer(POISON);
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
            read();
        }

        private void read() {
            try {
                ByteBuffer buf = ByteBuffer.allocate(NETWORK_BUFFER_SIZE);
                doRead(buf);
            } catch (IOException e) {
                log.error("Error reading message", e);
                finishReading();
            }
        }

        private void doRead(ByteBuffer buf) throws IOException {
            if (socket.read(buf) <= 0) {
                handleAllRead();
            } else if (!inputQueue.offer(buf)) {
                log.warn("Lost message");
            }
        }

        private void handleAllRead() throws IOException {
            finishReading();
            socket.close();
        }

        private void finishReading() {
            readIsRunning = false;
            inputQueue.offer(POISON);
            sk.cancel();
            srvSelector.wakeup();
        }
    }

    private static class Writer implements Runnable {
        private final FileChannel out;

        private Writer(FileChannel writeChannel) {
            this.out = writeChannel;
        }

        @Override
        public void run() {
            ByteBuffer buf = inputQueue.poll();
            if (buf == POISON) {
                writeIsRunning = false;
            } else if (buf != null) {
                write(buf);
            }
        }

        private void write(ByteBuffer buf) {
            try {
                buf.flip();
                doWrite(buf);
                log.trace("Buffer written");
            } catch (IOException e) {
                log.error("Error writing to file", e);
                handleException();
            }
        }

        private void doWrite(ByteBuffer buf) throws IOException {
            while (buf.hasRemaining()) {
                out.write(buf);
            }
        }

        private void handleException() {
            readIsRunning = false;
            writeIsRunning = false;
            srvSelector.wakeup();
        }
    }
}
