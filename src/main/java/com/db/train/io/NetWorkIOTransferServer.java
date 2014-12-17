package com.db.train.io;

import com.db.train.CommonUtils;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

// 165_942ms
public class NetWorkIOTransferServer {
    private static final int BUFFER_SIZE = 128 * 1024;

    public static void main(String[] args) {
        String dstPath;
        if (args.length == 1) {
            dstPath = args[0];
        } else {
            dstPath = CommonUtils.DEFAULT_DEST_PATH;
            CommonUtils.printUsage(dstPath);
        }
        File dst = new File(dstPath);
        Socket srcSocket = acceptFirstConnection();
        long start = System.nanoTime();
        try (OutputStream out = new BufferedOutputStream(new FileOutputStream(dst))) {
            BufferedInputStream inputStream = new BufferedInputStream(srcSocket.getInputStream());
            byte[] buf = new byte[BUFFER_SIZE];
            int read;
            while ((read = inputStream.read(buf)) != -1) {
                out.write(buf, 0, read);
            }
            srcSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("Copy took " + ((System.nanoTime() - start) / 1e6) + "ms");
    }

    private static Socket acceptFirstConnection() {
        try (ServerSocket srv = new ServerSocket(13000)) {
            return srv.accept();
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
}
