package org.bhargavguntupalli.tradingsandboxapi.dto;

public enum TimePeriod {
    ONE_DAY("1D", "5Min", 1),
    ONE_WEEK("1W", "1Hour", 7),
    ONE_MONTH("1M", "1Day", 30),
    THREE_MONTHS("3M", "1Day", 90);

    private final String label;
    private final String timeframe;
    private final int daysBack;

    TimePeriod(String label, String timeframe, int daysBack) {
        this.label = label;
        this.timeframe = timeframe;
        this.daysBack = daysBack;
    }

    public String getLabel() {
        return label;
    }

    public String getTimeframe() {
        return timeframe;
    }

    public int getDaysBack() {
        return daysBack;
    }

    public static TimePeriod fromLabel(String label) {
        for (TimePeriod p : values()) {
            if (p.label.equalsIgnoreCase(label)) {
                return p;
            }
        }
        return null;
    }
}
