package org.bhargavguntupalli.tradingsandboxapi.models;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.Column;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(
        name = "daily_prices",
        indexes = @Index(name = "idx_symbol_date", columnList = "symbol, date")
)
public class DailyPrice {

    @EmbeddedId
    private DailyPriceId id;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal closingPrice;

    protected DailyPrice() {}

    public DailyPrice(DailyPriceId id, BigDecimal closingPrice) {
        this.id = id;
        this.closingPrice = closingPrice;
    }

    public DailyPriceId getId() { return id; }
    public BigDecimal getClosingPrice() { return closingPrice; }

    public String getSymbol() { return id.getSymbol(); }
    public LocalDate getDate()    { return id.getDate(); }
}
