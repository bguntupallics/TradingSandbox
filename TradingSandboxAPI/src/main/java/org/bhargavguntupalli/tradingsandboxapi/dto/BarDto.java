package org.bhargavguntupalli.tradingsandboxapi.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;

public class BarDto {
    private String symbol;
    private Instant timestamp;
    private double open;
    private double high;
    private double low;
    private double close;
    private double volume;

    @JsonProperty("trade_count")
    private double tradeCount;

    private double vwap;

    public BarDto() { }

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

    public double getOpen() {
        return open;
    }
    public void setOpen(double open) {
        this.open = open;
    }

    public double getHigh() {
        return high;
    }
    public void setHigh(double high) {
        this.high = high;
    }

    public double getLow() {
        return low;
    }
    public void setLow(double low) {
        this.low = low;
    }

    public double getClose() {
        return close;
    }
    public void setClose(double close) {
        this.close = close;
    }

    public double getVolume() {
        return volume;
    }
    public void setVolume(double volume) {
        this.volume = volume;
    }

    public double getTradeCount() {
        return tradeCount;
    }
    public void setTradeCount(double tradeCount) {
        this.tradeCount = tradeCount;
    }

    public double getVwap() {
        return vwap;
    }
    public void setVwap(double vwap) {
        this.vwap = vwap;
    }
}
