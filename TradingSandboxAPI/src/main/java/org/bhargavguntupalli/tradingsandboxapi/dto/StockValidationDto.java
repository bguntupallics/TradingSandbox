package org.bhargavguntupalli.tradingsandboxapi.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StockValidationDto {
    private boolean valid;
    private String symbol;
    private String name;
    private String exchange;
    private boolean tradable;
    private String error;

    public static StockValidationDto valid(String symbol, String name, String exchange) {
        return new StockValidationDto(true, symbol, name, exchange, true, null);
    }

    public static StockValidationDto invalid(String error) {
        return new StockValidationDto(false, null, null, null, false, error);
    }
}
