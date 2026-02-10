package org.bhargavguntupalli.tradingsandboxapi.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

public class MarketStatusDto {

    @Getter @Setter
    @JsonAlias("is_open")
    private boolean isOpen;

    @Getter @Setter
    @JsonAlias("next_open")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssXXX")
    private OffsetDateTime nextOpen;

    @Getter @Setter
    @JsonAlias("next_close")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssXXX")
    private OffsetDateTime nextClose;

    public MarketStatusDto() {}

    public MarketStatusDto(boolean isOpen, OffsetDateTime nextOpen, OffsetDateTime nextClose) {
        this.isOpen = isOpen;
        this.nextOpen = nextOpen;
        this.nextClose = nextClose;
    }
}
