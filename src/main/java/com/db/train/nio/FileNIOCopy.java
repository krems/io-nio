package com.db.train.nio;

import com.db.train.CommonUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;

// 76 ms
public class FileNIOCopy {
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
        File srcFile = new File(srcPath);
        File dstFile = new File(dstPath);
        if (!srcFile.isFile()) {
            throw new IllegalArgumentException("Not a file path!" + srcPath);
        }
        if (!dstFile.isFile()) {
            throw new IllegalArgumentException("Not a file path!" + dstPath);
        }
        try (FileInputStream in = new FileInputStream(srcFile);
             FileOutputStream out = new FileOutputStream(dstFile);
             FileChannel src = in.getChannel();
             FileChannel dst = out.getChannel()) {
            dst.transferFrom(src, 0, src.size());
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("Copy took " + ((System.nanoTime() - start) / 1e6) + "ms");
    }
}
