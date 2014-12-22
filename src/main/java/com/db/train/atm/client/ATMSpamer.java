package com.db.train.atm.client;

import com.db.train.CommonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.HashSet;
import java.util.Set;

// 3.907ms (2.0 - 48.0 ms)
public class ATMSpamer {
    private static final Logger log = LoggerFactory.getLogger(ATMSpamer.class);

    public static void main(String[] args) {
        Set<InetSocketAddress> servers = populateServers();
        Client client = startClient(servers);
        await(client);
    }

    private static Set<InetSocketAddress> populateServers() {
        Set<InetSocketAddress> servers = new HashSet<>();
        servers.add(new InetSocketAddress(CommonUtils.DEFAULT_HOST, CommonUtils.DEFAULT_PORT));
        return servers;
    }

    private static Client startClient(Set<InetSocketAddress> servers) {
        Client client = new Client(servers);
        client.start();
        log.info("Client started");
        return client;
    }

    private static void await(Client client) {
        try {
            Thread.sleep(Long.MAX_VALUE);
        } catch (InterruptedException e) {
            log.error("Spamer interrupted", e);
            client.stop();
        }
    }
}
