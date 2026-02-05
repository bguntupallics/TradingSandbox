package org.bhargavguntupalli.tradingsandboxapi.model;

import org.bhargavguntupalli.tradingsandboxapi.models.DailyPriceId;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class DailyPriceIdTest {

    @Test
    void constructor_SetsSymbolAndDate() {
        LocalDate date = LocalDate.of(2025, 7, 9);
        DailyPriceId id = new DailyPriceId("AAPL", date);

        assertThat(id.getSymbol()).isEqualTo("AAPL");
        assertThat(id.getDate()).isEqualTo(date);
    }

    @Test
    void equals_SameSymbolAndDate_ReturnsTrue() {
        LocalDate date = LocalDate.of(2025, 7, 9);
        DailyPriceId id1 = new DailyPriceId("AAPL", date);
        DailyPriceId id2 = new DailyPriceId("AAPL", date);

        assertThat(id1).isEqualTo(id2);
    }

    @Test
    void equals_DifferentSymbol_ReturnsFalse() {
        LocalDate date = LocalDate.of(2025, 7, 9);
        DailyPriceId id1 = new DailyPriceId("AAPL", date);
        DailyPriceId id2 = new DailyPriceId("GOOG", date);

        assertThat(id1).isNotEqualTo(id2);
    }

    @Test
    void equals_DifferentDate_ReturnsFalse() {
        DailyPriceId id1 = new DailyPriceId("AAPL", LocalDate.of(2025, 7, 9));
        DailyPriceId id2 = new DailyPriceId("AAPL", LocalDate.of(2025, 7, 10));

        assertThat(id1).isNotEqualTo(id2);
    }

    @Test
    void equals_SameInstance_ReturnsTrue() {
        DailyPriceId id = new DailyPriceId("AAPL", LocalDate.of(2025, 7, 9));
        assertThat(id).isEqualTo(id);
    }

    @Test
    void equals_Null_ReturnsFalse() {
        DailyPriceId id = new DailyPriceId("AAPL", LocalDate.of(2025, 7, 9));
        assertThat(id).isNotEqualTo(null);
    }

    @Test
    void equals_DifferentType_ReturnsFalse() {
        DailyPriceId id = new DailyPriceId("AAPL", LocalDate.of(2025, 7, 9));
        assertThat(id).isNotEqualTo("not a DailyPriceId");
    }

    @Test
    void hashCode_SameValues_SameHashCode() {
        LocalDate date = LocalDate.of(2025, 7, 9);
        DailyPriceId id1 = new DailyPriceId("AAPL", date);
        DailyPriceId id2 = new DailyPriceId("AAPL", date);

        assertThat(id1.hashCode()).isEqualTo(id2.hashCode());
    }

    @Test
    void hashCode_DifferentValues_DifferentHashCode() {
        DailyPriceId id1 = new DailyPriceId("AAPL", LocalDate.of(2025, 7, 9));
        DailyPriceId id2 = new DailyPriceId("GOOG", LocalDate.of(2025, 7, 10));

        assertThat(id1.hashCode()).isNotEqualTo(id2.hashCode());
    }
}
