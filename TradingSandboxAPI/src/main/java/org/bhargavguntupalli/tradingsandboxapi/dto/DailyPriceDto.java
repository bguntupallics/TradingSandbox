package org.bhargavguntupalli.tradingsandboxapi.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

public class DailyPriceDto {
    @Getter @Setter
    String symbol;
    @Getter @Setter
    LocalDate date;
    @Getter @Setter
    BigDecimal closingPrice;

    public DailyPriceDto(String symbol, LocalDate date, BigDecimal closingPrice) {
        this.symbol = symbol;
        this.date = date;
        this.closingPrice = closingPrice;
    }
}
