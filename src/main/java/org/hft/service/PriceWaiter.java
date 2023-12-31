package org.hft.service;

import org.hft.data.PriceHolder;
import org.hft.task.PriceWaitTask;

import java.util.List;
import java.util.concurrent.ExecutorService;

public class PriceWaiter {

    private static final int NUMBER_PRICE_WAITING_TASKS = 2;

    private static final List<String> TICKER_SYMBOLS = List.of("AAPL", "AMZN");

    private final PriceHolder priceHolder;
    private final ExecutorService executorService;

    public PriceWaiter(PriceHolder priceHolder, ExecutorService executorService) {
        this.priceHolder = priceHolder;
        this.executorService = executorService;
    }

    public void waitForPrices() {
        for (int i = 0; i < NUMBER_PRICE_WAITING_TASKS; i++) {
            executorService.submit(new PriceWaitTask(TICKER_SYMBOLS.get(i), priceHolder));
        }
    }
}
