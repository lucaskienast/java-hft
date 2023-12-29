package org.hft.task;

import org.hft.data.PriceHolder;
import org.hft.dto.PriceDto;

import java.math.BigDecimal;

public class PriceUpdateTask implements Runnable {

    private final PriceDto priceDto;
    private final PriceHolder priceHolder;

    public PriceUpdateTask(PriceHolder priceHolder, PriceDto priceDto) {
        this.priceHolder = priceHolder;
        this.priceDto = priceDto;
    }

    @Override
    public void run() {
        BigDecimal price = BigDecimal.valueOf(priceDto.price());

        priceHolder.putPrice(priceDto.tickerSymbol(), price);
        System.out.printf("%s - Entity %s, Updated Price to %s%n",
                Thread.currentThread().getName(), priceDto.tickerSymbol(), price);

        BigDecimal retrievedPrice = priceHolder.getPrice(priceDto.tickerSymbol());
        boolean priceHasChanged = priceHolder.hasPriceChanged(priceDto.tickerSymbol());

        System.out.printf("%s - Entity %s, Price %s, Retrieved Price %s, Price Changed %s%n",
                Thread.currentThread().getName(), priceDto.tickerSymbol(), price, retrievedPrice, priceHasChanged);
    }
}
