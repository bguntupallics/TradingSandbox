package org.bhargavguntupalli.tradingsandboxapi.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter @Setter
public class TradeHistoryDto {
    private Long id;
    private String symbol;
    private String type;
    private BigDecimal quantity;
    private BigDecimal pricePerShare;
    private BigDecimal totalCost;
    private LocalDateTime executedAt;
}
