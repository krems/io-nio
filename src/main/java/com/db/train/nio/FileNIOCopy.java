package com.db.train.nio;

import com.db.train.CommonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;

// 76 ms
public class FileNIOCopy {
    private static final Logger log = LoggerFactory.getLogger(FileNIOCopy.class);
    private static String srcFilePath;
    private static String dstFilePath;

    public static void main(String[] args) {
        long start = 0;
        if (log.isDebugEnabled()) {
            start = System.nanoTime();
        }
        parseArgs(args);
        File srcFile = openFile(srcFilePath);
        File dstFile = openFile(dstFilePath);
        copy(srcFile, dstFile);
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
        File srcFile = new File(srcPath);
        if (!srcFile.isFile()) {
            throw new IllegalArgumentException("Not a file path!" + srcPath);
        }
        return srcFile;
    }

    private static void copy(File srcFile, File dstFile) {
        try (FileInputStream in = new FileInputStream(srcFile);
             FileOutputStream out = new FileOutputStream(dstFile);
             FileChannel src = in.getChannel();
             FileChannel dst = out.getChannel()) {
            dst.transferFrom(src, 0, src.size());
        } catch (IOException e) {
            log.error("Error copying file", e);
            throw new RuntimeException(e);
        }
    }
}
