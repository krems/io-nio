package com.db.train.io;

import com.db.train.CommonUtils;

import java.io.*;

// 116 ms
public class FileIOCopy {
    private static final int BUFFER_SIZE = 128 * 1024;

    public static void main(String[] args) {
        long start = System.nanoTime();
        String srcPath;
        String dstPath;
        if (args.length == 2) {
            srcPath = args[0];
            dstPath = args[1];
        } else {
            srcPath = CommonUtils.DEFAULT_SRC_PATH;
            dstPath = CommonUtils.DEFAULT_DEST_PATH;
            CommonUtils.printUsage(srcPath, dstPath);
        }
        File src = new File(srcPath);
        File dst = new File(dstPath);
        if (!src.isFile()) {
            throw new IllegalArgumentException("Not a file path!" + srcPath);
        }
        if (!dst.isFile()) {
            throw new IllegalArgumentException("Not a file path!" + dstPath);
        }
        byte[] buffer = new byte[BUFFER_SIZE];
        try (InputStream in = new BufferedInputStream(new FileInputStream(src));
             OutputStream out = new BufferedOutputStream(new FileOutputStream(dst))) {
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("Copy took " + ((System.nanoTime() - start) / 1e6) + "ms");
    }
}
