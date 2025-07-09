package org.bhargavguntupalli.tradingsandboxapi.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class TradeResponseDto {
    @JsonProperty("price")
    private double price;

    @JsonProperty("timestamp")
    private String timestamp;

    @JsonProperty("volume")
    private int volume;
}
