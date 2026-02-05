package org.bhargavguntupalli.tradingsandboxapi.dto;

import java.math.BigDecimal;
import java.time.Instant;

public class PriceDataDto {
    private String symbol;
    private Instant timestamp;
    private String dateLabel;
    private BigDecimal closingPrice;

    public PriceDataDto() {}

    public PriceDataDto(String symbol, Instant timestamp, String dateLabel, BigDecimal closingPrice) {
        this.symbol = symbol;
        this.timestamp = timestamp;
        this.dateLabel = dateLabel;
        this.closingPrice = closingPrice;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public String getDateLabel() {
        return dateLabel;
    }

    public void setDateLabel(String dateLabel) {
        this.dateLabel = dateLabel;
    }

    public BigDecimal getClosingPrice() {
        return closingPrice;
    }

    public void setClosingPrice(BigDecimal closingPrice) {
        this.closingPrice = closingPrice;
    }
}
