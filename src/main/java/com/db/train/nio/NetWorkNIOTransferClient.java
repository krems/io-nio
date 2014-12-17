package com.db.train.nio;

import com.db.train.CommonUtils;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SocketChannel;

// 84_252ms
public class NetWorkNIOTransferClient {
    private static final int NETWORK_BUFFER_SIZE = 128 * 1024;
    private static volatile boolean stop;
    private static ByteBuffer buf;
    public static void main(String[] args) {
        long start = System.nanoTime();
        String srcPath;
        String host;
        int port;
        if (args.length == 1) {
            srcPath = args[0];
            host = CommonUtils.DEFAULT_HOST;
            port = CommonUtils.DEFAULT_PORT;
        } else if (args.length == 3) {
            srcPath = args[0];
            host = args[1];
            port = Integer.parseInt(args[2]);
        } else {
            srcPath = CommonUtils.DEFAULT_SRC_PATH;
            host = CommonUtils.DEFAULT_HOST;
            port = CommonUtils.DEFAULT_PORT;
            CommonUtils.printUsage(srcPath);
        }
        SocketChannel outSocket = null;
        RandomAccessFile inFile = null;
        try {
            outSocket = SocketChannel.open();
            outSocket.connect(new InetSocketAddress(host, port));
            inFile = new RandomAccessFile(srcPath, "r");
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        buf = ByteBuffer.allocate(NETWORK_BUFFER_SIZE);
        while (!stop && !Thread.currentThread().isInterrupted()) {
            readFromFile(inFile.getChannel());
            writeToSocket(outSocket);
        }
        close(outSocket);
        System.out.println("Copy took " + ((System.nanoTime() - start) / 1e6) + "ms");
    }

    private static void close(SocketChannel outSocket) {
        try {
            outSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void writeToSocket(SocketChannel outSocket) {
        try {
            while (buf.hasRemaining()) {
                outSocket.write(buf);
            }
            buf.clear();
        } catch (IOException e) {
            e.printStackTrace();
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
            e.printStackTrace();
        }
    }
}
