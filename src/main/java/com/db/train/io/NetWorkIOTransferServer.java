package com.db.train.io;

import com.db.train.CommonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

// 165_942ms
public class NetWorkIOTransferServer {
    private static final Logger log = LoggerFactory.getLogger(NetWorkIOTransferServer.class);
    private static final int BUFFER_SIZE = 128 * 1024;
    private static String dstFilePath;

    public static void main(String[] args) {
        parseArgs(args);
        Socket srcSocket = acceptFirstConnection();
        long start = getStart();
        transfer(dstFilePath, srcSocket);
        log.debug("Copy took {} ms", (System.nanoTime() - start) / 1e6);
    }

    private static void parseArgs(String[] args) {
        if (args.length == 1) {
            dstFilePath = args[0];
        } else if (args.length == 0) {
            dstFilePath = CommonUtils.DEFAULT_DEST_PATH;
        } else {
            CommonUtils.printUsage(CommonUtils.DEFAULT_DEST_PATH);
            throw new IllegalArgumentException("Wrong program args");
        }
    }

    private static Socket acceptFirstConnection() {
        try (ServerSocket srv = new ServerSocket(13000)) {
            return srv.accept();
        } catch (IOException e) {
            log.error("Error accepting connection", e);
            throw new RuntimeException(e);
        }
    }

    private static long getStart() {
        long start = 0;
        if (log.isDebugEnabled()) {
            start = System.nanoTime();
        }
        return start;
    }

    private static void transfer(String dstPath, Socket srcSocket) {
        File dst = new File(dstPath);
        try (OutputStream out = new BufferedOutputStream(new FileOutputStream(dst))) {
            BufferedInputStream inputStream = new BufferedInputStream(srcSocket.getInputStream());
            doTransfer(out, inputStream);
            srcSocket.close();
        } catch (IOException e) {
            log.error("Error transferring file", e);
            throw new RuntimeException(e);
        }
    }

    private static void doTransfer(OutputStream out, BufferedInputStream inputStream) throws IOException {
        byte[] buf = new byte[BUFFER_SIZE];
        int read;
        while ((read = inputStream.read(buf)) != -1) {
            out.write(buf, 0, read);
        }
    }
}
