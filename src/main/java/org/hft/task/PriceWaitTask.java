package org.hft.task;

import org.hft.data.PriceHolder;

import java.math.BigDecimal;

public class PriceWaitTask implements Runnable {

    private final String tickerSymbol;
    private final PriceHolder priceHolder;

    public PriceWaitTask(String tickerSymbol, PriceHolder priceHolder) {
        this.tickerSymbol = tickerSymbol;
        this.priceHolder = priceHolder;
    }

    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                System.out.printf("%s - Waiting for next price of Entity %s%n",
                        Thread.currentThread().getName(), tickerSymbol);
                // todo: use async here?
                BigDecimal newPrice = priceHolder.waitForNextPrice(tickerSymbol);
                System.out.printf("%s - Entity %s, New Price %s%n",
                        Thread.currentThread().getName(), tickerSymbol, newPrice);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
