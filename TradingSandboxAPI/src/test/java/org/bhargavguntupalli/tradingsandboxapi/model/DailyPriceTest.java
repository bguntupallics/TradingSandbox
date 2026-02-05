package org.bhargavguntupalli.tradingsandboxapi.model;

import org.bhargavguntupalli.tradingsandboxapi.models.DailyPrice;
import org.bhargavguntupalli.tradingsandboxapi.models.DailyPriceId;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class DailyPriceTest {

    @Test
    void constructor_SetsIdAndClosingPrice() {
        DailyPriceId id = new DailyPriceId("AAPL", LocalDate.of(2025, 7, 9));
        DailyPrice price = new DailyPrice(id, BigDecimal.valueOf(150.25));

        assertThat(price.getId()).isEqualTo(id);
        assertThat(price.getClosingPrice()).isEqualByComparingTo(BigDecimal.valueOf(150.25));
    }

    @Test
    void getSymbol_DelegatesToId() {
        DailyPriceId id = new DailyPriceId("GOOG", LocalDate.of(2025, 7, 9));
        DailyPrice price = new DailyPrice(id, BigDecimal.valueOf(200.00));

        assertThat(price.getSymbol()).isEqualTo("GOOG");
    }

    @Test
    void getDate_DelegatesToId() {
        LocalDate date = LocalDate.of(2025, 7, 9);
        DailyPriceId id = new DailyPriceId("AAPL", date);
        DailyPrice price = new DailyPrice(id, BigDecimal.valueOf(150.00));

        assertThat(price.getDate()).isEqualTo(date);
    }

    @Test
    void closingPrice_PreservesPrecision() {
        DailyPriceId id = new DailyPriceId("MSFT", LocalDate.of(2025, 7, 9));
        BigDecimal precisePrice = new BigDecimal("340.1234");
        DailyPrice price = new DailyPrice(id, precisePrice);

        assertThat(price.getClosingPrice()).isEqualByComparingTo(precisePrice);
    }
}
