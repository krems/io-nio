package com.db.train.io;

import com.db.train.CommonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;

// 116 ms
public class FileIOCopy {
    private static final Logger log = LoggerFactory.getLogger(FileIOCopy.class);
    private static final int BUFFER_SIZE = 128 * 1024;
    private static String srcFilePath;
    private static String dstFilePath;

    public static void main(String[] args) {
        long start = 0;
        if (log.isDebugEnabled()) {
            start = System.nanoTime();
        }
        parseArgs(args);
        File src = openFile(srcFilePath);
        File dst = openFile(dstFilePath);
        copy(src, dst);
        log.debug("Copy took {} ms", (System.nanoTime() - start) / 1e6);
    }

    private static void parseArgs(String[] args) {
        if (args.length == 2) {
            srcFilePath = args[0];
            dstFilePath = args[1];
        } else if (args.length == 0) {
            srcFilePath = CommonUtils.DEFAULT_SRC_PATH;
            dstFilePath = CommonUtils.DEFAULT_DEST_PATH;
        } else {
            CommonUtils.printUsage(CommonUtils.DEFAULT_SRC_PATH, CommonUtils.DEFAULT_DEST_PATH);
            throw new IllegalArgumentException("Wrong program args");
        }
    }

    private static File openFile(String srcPath) {
        File src = new File(srcPath);
        if (!src.isFile()) {
            throw new IllegalArgumentException("Not a file path! " + srcPath);
        }
        return src;
    }

    private static void copy(File src, File dst) {
        byte[] buffer = new byte[BUFFER_SIZE];
        try (InputStream in = new BufferedInputStream(new FileInputStream(src));
             OutputStream out = new BufferedOutputStream(new FileOutputStream(dst))) {
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
        } catch (IOException e) {
            log.error("Error copying file", e);
        }
    }
}
