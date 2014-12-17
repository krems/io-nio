package com.db.train.atm.client;

import com.db.train.CommonUtils;

import java.net.InetSocketAddress;
import java.util.HashSet;
import java.util.Set;

// 3.907ms (2.0 - 48.0 ms)
public class ATMSpamer {
    public static void main(String[] args) {
        Set<InetSocketAddress> servers = new HashSet<>();
        servers.add(new InetSocketAddress(CommonUtils.DEFAULT_HOST, CommonUtils.DEFAULT_PORT));
        Client client = new Client(servers);
        client.start();
        try {
            Thread.sleep(Long.MAX_VALUE);
        } catch (InterruptedException e) {
            e.printStackTrace();
            client.stop();
        }
    }
}
