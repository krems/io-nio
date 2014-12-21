package com.db.train.nio;

import com.db.train.CommonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SocketChannel;

// 84_252ms
public class NetWorkNIOTransferClient {
    private static final Logger log = LoggerFactory.getLogger(NetWorkNIOTransferClient.class);
    private static final int NETWORK_BUFFER_SIZE = 128 * 1024;
    private static volatile boolean stop;
    private static ByteBuffer buf;
    private static String srcFilePath;
    private static String host;
    private static int port;

    public static void main(String[] args) {
        long start = 0;
        if (log.isDebugEnabled()) {
            start = System.nanoTime();
        }
        parseArgs(args);
        transfer();
        log.debug("Copy took {} ms", (System.nanoTime() - start) / 1e6);
    }

    private static void parseArgs(String[] args) {
        if (args.length == 1) {
            srcFilePath = args[0];
            host = CommonUtils.DEFAULT_HOST;
            port = CommonUtils.DEFAULT_PORT;
        } else if (args.length == 3) {
            srcFilePath = args[0];
            host = args[1];
            port = Integer.parseInt(args[2]);
        } else if (args.length == 0) {
            srcFilePath = CommonUtils.DEFAULT_SRC_PATH;
            host = CommonUtils.DEFAULT_HOST;
            port = CommonUtils.DEFAULT_PORT;
        } else {
            CommonUtils.printUsage(CommonUtils.DEFAULT_SRC_PATH);
            throw new IllegalArgumentException("Wrong program args");
        }
    }

    private static void transfer() {
        try (SocketChannel outSocket = SocketChannel.open();
             RandomAccessFile inFile = new RandomAccessFile(srcFilePath, "r")) {
            outSocket.connect(new InetSocketAddress(host, port));
            buf = ByteBuffer.allocate(NETWORK_BUFFER_SIZE);
            doTransfer(outSocket, inFile);
        } catch (IOException e) {
            log.error("Error transferring file", e);
            throw new RuntimeException(e);
        }
    }

    private static void doTransfer(SocketChannel outSocket, RandomAccessFile inFile) {
        while (!stop && !Thread.currentThread().isInterrupted()) {
            readFromFile(inFile.getChannel());
            writeToSocket(outSocket);
        }
    }

    private static void readFromFile(FileChannel in) {
        try {
            if (in.read(buf) <= 0) {
                stop = true;
                return;
            }
            buf.flip();
        } catch (IOException e) {
            log.error("Error reading from file", e);
            stop = true;
        }
    }

    private static void writeToSocket(SocketChannel outSocket) {
        try {
            while (buf.hasRemaining()) {
                outSocket.write(buf);
            }
            buf.clear();
        } catch (IOException e) {
            log.error("Error writing to socket", e);
            stop = true;
        }
    }
}
