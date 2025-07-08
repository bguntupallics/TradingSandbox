package org.bhargavguntupalli.tradingsandboxapi.dto;

public class TradeResponseDto {
    private Object trade_data;  // You can replace Object with a more strongly typed class if you like

    public TradeResponseDto() {}

    public Object getTrade_data() {
        return trade_data;
    }

    public void setTrade_data(Object trade_data) {
        this.trade_data = trade_data;
    }
}
