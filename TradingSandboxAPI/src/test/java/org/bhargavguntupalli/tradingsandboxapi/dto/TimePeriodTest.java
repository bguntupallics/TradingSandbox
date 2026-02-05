package org.bhargavguntupalli.tradingsandboxapi.dto;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class TimePeriodTest {

    @Test
    void oneDay_HasCorrectProperties() {
        assertThat(TimePeriod.ONE_DAY.getLabel()).isEqualTo("1D");
        assertThat(TimePeriod.ONE_DAY.getTimeframe()).isEqualTo("5Min");
        assertThat(TimePeriod.ONE_DAY.getDaysBack()).isEqualTo(1);
    }

    @Test
    void oneWeek_HasCorrectProperties() {
        assertThat(TimePeriod.ONE_WEEK.getLabel()).isEqualTo("1W");
        assertThat(TimePeriod.ONE_WEEK.getTimeframe()).isEqualTo("1Hour");
        assertThat(TimePeriod.ONE_WEEK.getDaysBack()).isEqualTo(7);
    }

    @Test
    void oneMonth_HasCorrectProperties() {
        assertThat(TimePeriod.ONE_MONTH.getLabel()).isEqualTo("1M");
        assertThat(TimePeriod.ONE_MONTH.getTimeframe()).isEqualTo("1Day");
        assertThat(TimePeriod.ONE_MONTH.getDaysBack()).isEqualTo(30);
    }

    @Test
    void threeMonths_HasCorrectProperties() {
        assertThat(TimePeriod.THREE_MONTHS.getLabel()).isEqualTo("3M");
        assertThat(TimePeriod.THREE_MONTHS.getTimeframe()).isEqualTo("1Day");
        assertThat(TimePeriod.THREE_MONTHS.getDaysBack()).isEqualTo(90);
    }

    @ParameterizedTest
    @CsvSource({
            "1D, ONE_DAY",
            "1W, ONE_WEEK",
            "1M, ONE_MONTH",
            "3M, THREE_MONTHS"
    })
    void fromLabel_ValidLabel_ReturnsCorrectPeriod(String label, String expectedName) {
        TimePeriod result = TimePeriod.fromLabel(label);
        assertThat(result).isNotNull();
        assertThat(result.name()).isEqualTo(expectedName);
    }

    @Test
    void fromLabel_CaseInsensitive() {
        assertThat(TimePeriod.fromLabel("1d")).isEqualTo(TimePeriod.ONE_DAY);
        assertThat(TimePeriod.fromLabel("1w")).isEqualTo(TimePeriod.ONE_WEEK);
        assertThat(TimePeriod.fromLabel("1m")).isEqualTo(TimePeriod.ONE_MONTH);
        assertThat(TimePeriod.fromLabel("3m")).isEqualTo(TimePeriod.THREE_MONTHS);
    }

    @Test
    void fromLabel_InvalidLabel_ReturnsNull() {
        assertThat(TimePeriod.fromLabel("INVALID")).isNull();
        assertThat(TimePeriod.fromLabel("5Y")).isNull();
        assertThat(TimePeriod.fromLabel("")).isNull();
    }

    @Test
    void values_HasFourElements() {
        assertThat(TimePeriod.values()).hasSize(4);
    }
}
