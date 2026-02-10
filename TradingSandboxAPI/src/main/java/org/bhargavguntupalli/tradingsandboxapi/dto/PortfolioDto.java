package org.bhargavguntupalli.tradingsandboxapi.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter @Setter
public class PortfolioDto {
    private BigDecimal cashBalance;
    private BigDecimal holdingsValue;
    private BigDecimal totalPortfolioValue;
    private BigDecimal totalGainLoss;
    private List<HoldingDto> holdings;
}
