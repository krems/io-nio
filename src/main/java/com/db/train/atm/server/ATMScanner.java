package com.db.train.atm.server;

import com.db.train.CommonUtils;

// 21.399 msg/sec
public class ATMScanner {
    public static void main(String[] args) {
        Server server = new Server(CommonUtils.DEFAULT_PORT);
        server.start();
        try {
            Thread.sleep(Long.MAX_VALUE);
        } catch (InterruptedException e) {
            server.stop();
        }
    }
}
