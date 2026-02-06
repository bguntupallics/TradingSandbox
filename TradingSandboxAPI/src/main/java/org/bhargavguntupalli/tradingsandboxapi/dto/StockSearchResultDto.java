package org.bhargavguntupalli.tradingsandboxapi.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StockSearchResultDto {
    private List<StockSuggestionDto> suggestions;
}
