package com.db.train.io;

import com.db.train.CommonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.Socket;

// 166_162ms
public class NetWorkIOTransferClient {
    private static final Logger log = LoggerFactory.getLogger(NetWorkIOTransferClient.class);
    private static final int BUFFER_SIZE = 128 * 1024;
    private static String srcFilePath;
    private static String host;
    private static int port;

    public static void main(String[] args) {
        long start = 0;
        if (log.isDebugEnabled()) {
            start = System.nanoTime();
        }
        parseArgs(args);
        transfer(srcFilePath, host, port);
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
            CommonUtils.printUsage(CommonUtils.DEFAULT_SRC_PATH, CommonUtils.DEFAULT_HOST, CommonUtils.DEFAULT_PORT);
            throw new IllegalArgumentException("Wrong program args");
        }
    }

    private static void transfer(String srcPath, String host, int port) {
        byte[] buf = new byte[BUFFER_SIZE];
        try (Socket dstSocket = new Socket(host, port);
             InputStream src = new BufferedInputStream(new FileInputStream(srcPath));
             OutputStream out = dstSocket.getOutputStream()) {
            int read;
            while ((read = src.read(buf)) != -1) {
                out.write(buf, 0, read);
            }
        } catch (IOException e) {
            log.error("Error transferring file", e);
        }
    }
}
