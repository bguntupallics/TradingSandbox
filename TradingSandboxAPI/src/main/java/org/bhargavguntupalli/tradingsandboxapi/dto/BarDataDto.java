package org.bhargavguntupalli.tradingsandboxapi.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public class BarDataDto {
    private String symbol;

    @JsonProperty("start_date")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime startDate;

    @JsonProperty("end_date")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime endDate;

    private String timeframe;

    /**
     * JSON comes in as:
     *   "bars": { "NVDA": [ {...}, {...} ] }
     */
    private Map<String, List<BarDto>> bars;

    public BarDataDto() { }

    public String getSymbol() {
        return symbol;
    }
    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public LocalDateTime getStartDate() {
        return startDate;
    }
    public void setStartDate(LocalDateTime startDate) {
        this.startDate = startDate;
    }

    public LocalDateTime getEndDate() {
        return endDate;
    }
    public void setEndDate(LocalDateTime endDate) {
        this.endDate = endDate;
    }

    public String getTimeframe() {
        return timeframe;
    }
    public void setTimeframe(String timeframe) {
        this.timeframe = timeframe;
    }

    public Map<String, List<BarDto>> getBars() {
        return bars;
    }
    public void setBars(Map<String, List<BarDto>> bars) {
        this.bars = bars;
    }
}
