package org.hft;

import org.hft.data.PriceHolder;
import org.hft.receiver.PriceReceiver;
import org.hft.service.PriceWaiter;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Main {

    private static final int CORE_COUNT = Runtime.getRuntime().availableProcessors();

    public static void main(String[] args) {
        ExecutorService executorService = Executors.newFixedThreadPool(CORE_COUNT);

        try {
            PriceHolder priceHolder = new PriceHolder();

            PriceWaiter priceWaiter = new PriceWaiter(priceHolder, executorService);
            priceWaiter.waitForPrices();

            // todo: start multiple instances of stock exchange order book & matching engine -> for further entities/tickers
            // todo: listen to prices on multiple ports (maybe 1 port per ticker)
            PriceReceiver priceReceiver = new PriceReceiver(priceHolder, executorService);
            priceReceiver.receivePrices();

        } finally {
            executorService.shutdown();
        }
    }
}