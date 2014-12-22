package com.db.train.atm.server;

import com.db.train.CommonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// 21.399 msg/sec
public class ATMScanner {
    private static final Logger log = LoggerFactory.getLogger(ATMScanner.class);

    public static void main(String[] args) {
        Server server = startServer();
        await(server);
    }

    private static Server startServer() {
        Server server = new Server(CommonUtils.DEFAULT_PORT);
        server.start();
        log.info("Server started");
        return server;
    }

    private static void await(Server server) {
        try {
            Thread.sleep(Long.MAX_VALUE);
        } catch (InterruptedException e) {
            log.error("Scanner interrupted");
            server.stop();
        }
    }
}
