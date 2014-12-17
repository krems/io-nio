package com.db.train;

public class CommonUtils {
    public static final String DEFAULT_SRC_PATH = "C:\\Users\\Student\\tmp\\data.bin";
    public static final String DEFAULT_DEST_PATH = "C:\\Users\\Student\\tmp\\dest.bin";
    public static final String DEFAULT_HOST = "localhost";//"192.168.1.122";
    public static final int DEFAULT_PORT = 13001;

    public static void printUsage(String src, String dst) {
        System.out.println("Usage:");
        System.out.println("program srcPath dstPath");
        System.out.println("Default src filepath: " + src + ", dst path: " + dst);
    }

    private CommonUtils() {}

    public static void printUsage(String dst) {
        System.out.println("Usage:");
        System.out.println("program path");
        System.out.println("Default path: " + dst);
    }

    public static void printUsage(String src, String host, int port) {
        System.out.println("Usage:");
        System.out.println("program srcPath dstPath");
        System.out.println("Default src path: " + src + ", host " + host + ", port " + port);
    }
}
