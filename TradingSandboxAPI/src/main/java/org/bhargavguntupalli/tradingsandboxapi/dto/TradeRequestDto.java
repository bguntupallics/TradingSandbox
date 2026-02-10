package org.bhargavguntupalli.tradingsandboxapi.dto;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;
import org.bhargavguntupalli.tradingsandboxapi.models.TradeType;

import java.math.BigDecimal;

@Getter @Setter
public class TradeRequestDto {

    @NotBlank(message = "symbol is required")
    private String symbol;

    @NotNull(message = "quantity is required")
    @DecimalMin(value = "0.01", message = "quantity must be at least 0.01")
    @Digits(integer = 10, fraction = 2, message = "quantity allows up to 2 decimal places")
    private BigDecimal quantity;

    @NotNull(message = "type is required")
    private TradeType type;
}
