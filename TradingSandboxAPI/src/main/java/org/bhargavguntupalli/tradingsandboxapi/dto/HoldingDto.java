package org.bhargavguntupalli.tradingsandboxapi.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter @Setter
public class HoldingDto {
    private String symbol;
    private BigDecimal quantity;
    private BigDecimal averageCost;
    private BigDecimal currentPrice;
    private BigDecimal marketValue;
    private BigDecimal totalGainLoss;
    private BigDecimal totalGainLossPercent;
}
