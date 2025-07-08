package org.bhargavguntupalli.tradingsandboxapi.models;

import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;

@Embeddable
public class DailyPriceId implements Serializable {
    private String symbol;
    private LocalDate date;

    // default constructor for JPA
    protected DailyPriceId() {}

    public DailyPriceId(String symbol, LocalDate date) {
        this.symbol = symbol;
        this.date   = date;
    }

    // getters
    public String getSymbol() { return symbol; }
    public LocalDate getDate() { return date; }

    // equals & hashCode
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DailyPriceId)) return false;
        DailyPriceId that = (DailyPriceId) o;
        return Objects.equals(symbol, that.symbol) &&
                Objects.equals(date,   that.date);
    }

    @Override
    public int hashCode() {
        return Objects.hash(symbol, date);
    }
}
