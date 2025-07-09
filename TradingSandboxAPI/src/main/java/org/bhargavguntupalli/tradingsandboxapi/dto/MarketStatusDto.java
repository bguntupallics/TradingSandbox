package org.bhargavguntupalli.tradingsandboxapi.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

public class MarketStatusDto {

    @Getter @Setter
    @JsonAlias("is_open")
    private boolean isOpen;

    @Getter @Setter
    @JsonAlias("next_open")
    private OffsetDateTime nextOpen;

    @Getter @Setter
    @JsonAlias("next_close")
    private OffsetDateTime nextClose;

    public MarketStatusDto() {}

    public MarketStatusDto(boolean isOpen, OffsetDateTime nextOpen, OffsetDateTime nextClose) {
        this.isOpen = isOpen;
        this.nextOpen = nextOpen;
        this.nextClose = nextClose;
    }
}
