package org.bhargavguntupalli.tradingsandboxapi.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StockSuggestionDto {
    private String symbol;
    private String name;
    private String exchange;
}
