package org.bhargavguntupalli.tradingsandboxapi.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter @Setter
public class TradeResultDto {
    private Long tradeId;
    private String symbol;
    private String type;
    private BigDecimal quantity;
    private BigDecimal pricePerShare;
    private BigDecimal totalCost;
    private BigDecimal remainingCashBalance;
    private LocalDateTime executedAt;
}
