package com.db.train.io;

import com.db.train.CommonUtils;

import java.io.*;
import java.net.Socket;

// 166_162ms
public class NetWorkIOTransferClient {
    private static final int BUFFER_SIZE = 128 * 1024;

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
        byte[] buf = new byte[BUFFER_SIZE];
        try (Socket dstSocket = new Socket(host, port);
             InputStream src = new BufferedInputStream(new FileInputStream(srcPath));
             OutputStream out = dstSocket.getOutputStream()) {
            int read;
            while ((read = src.read(buf)) != -1) {
                out.write(buf, 0, read);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("Copy took " + ((System.nanoTime() - start) / 1e6) + "ms");
    }
}
