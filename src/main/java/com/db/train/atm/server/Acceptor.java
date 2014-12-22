package com.db.train.atm.server;

import com.db.train.atm.ATMData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Queue;
import java.util.concurrent.ExecutorService;

class Acceptor implements Runnable {
    private static final Logger log = LoggerFactory.getLogger(Acceptor.class);
    private final SelectorReactor[] clientSelectorReactors = new SelectorReactor[Server.CLIENT_SELECTORS_NUMBER];
    private boolean measure = true;
    private final SelectionKey key;
    private final ServerSocketChannel channel;
    private long index = 0;

    public Acceptor(ExecutorService executor, SelectionKey key, Queue<ATMData> resultQueue) {
        this.key = key;
        this.channel = (ServerSocketChannel) key.channel();
        try {
            for (int i = 0; i < clientSelectorReactors.length; i++) {
                clientSelectorReactors[i] = new SelectorReactor(resultQueue, Selector.open());
                executor.submit(clientSelectorReactors[i]);
            }
        } catch (Exception e) {
            log.error("Error opening selector", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void run() {
        try {
            SocketChannel inputSocket = channel.accept();
            if (inputSocket != null) {
                log.debug("Connection accepted");
                inputSocket.configureBlocking(false);
                int nextReactorIndex = getNextReactorIndex();
                clientSelectorReactors[nextReactorIndex].enqueueForRegister(inputSocket);
                clientSelectorReactors[nextReactorIndex].wakeUpSelector();
                if (measure) {
                    ThroughputCounter.run();
                    measure = false;
                }
            }
        } catch (Exception e) {
            log.error("Error accepting connection", e);
            key.selector().wakeup();
            throw new RuntimeException(e);
        }
    }

    private int getNextReactorIndex() {
        return (int) (index++ % clientSelectorReactors.length);
    }
}
