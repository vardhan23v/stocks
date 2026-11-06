package com.stockrecommendation.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

import com.stockrecommendation.model.Recommendation;
import com.stockrecommendation.model.Stock;
import com.stockrecommendation.model.StockIndicator;
import com.stockrecommendation.model.StockPrice;

/** Everything the stock details page shows, prepared by StockDetailsService. */
public class StockDetailsView {

    /** A selectable chart range and whether the stored data covers it. */
    public static class Range {

        private final String code;
        private final String label;
        private final boolean available;

        public Range(String code, String label, boolean available) {
            this.code = code;
            this.label = label;
            this.available = available;
        }

        public String getCode() { return code; }
        public String getLabel() { return label; }
        public boolean isAvailable() { return available; }
    }

    Stock stock;
    String range;
    String dataSource;
    int barCount;
    StockIndicator indicator;
    Recommendation recommendation;
    List<Range> ranges = List.of();
    List<StockPrice> rangeBars = List.of();
    BigDecimal rangeChangePercent;
    BigDecimal high52;
    BigDecimal low52;
    String range52Note;
    String chartJson = "{}";
    boolean watched;

    public boolean isSampleData() {
        return MarketDataService.SAMPLE.equals(dataSource);
    }

    public String getDataSourceLabel() {
        if (dataSource == null) {
            return "No data";
        }
        return isSampleData() ? "SAMPLE DATA (synthetic)" : dataSource.replace('_', ' ') + " (delayed end-of-day)";
    }

    public LocalDate getNewestDate() {
        return stock == null || stock.getLastUpdated() == null ? null : stock.getLastUpdated().toLocalDate();
    }

    public String getRangeDirection() {
        return rangeChangePercent == null || rangeChangePercent.signum() == 0 ? "flat"
             : rangeChangePercent.signum() > 0 ? "up" : "down";
    }

    /** Distance of the close from SMA(20), in percent. */
    public BigDecimal getDistanceFromSma() {
        if (indicator == null || indicator.getSma() == null || stock == null || stock.getLatestPrice() == null) {
            return null;
        }
        return stock.getLatestPrice().subtract(indicator.getSma()).multiply(BigDecimal.valueOf(100))
                .divide(indicator.getSma(), 2, RoundingMode.HALF_UP);
    }

    public Stock getStock() { return stock; }
    public String getRange() { return range; }
    public String getDataSource() { return dataSource; }
    public int getBarCount() { return barCount; }
    public StockIndicator getIndicator() { return indicator; }
    public Recommendation getRecommendation() { return recommendation; }
    public List<Range> getRanges() { return ranges; }
    public List<StockPrice> getRangeBars() { return rangeBars; }
    public BigDecimal getRangeChangePercent() { return rangeChangePercent; }
    public BigDecimal getHigh52() { return high52; }
    public BigDecimal getLow52() { return low52; }
    public String getRange52Note() { return range52Note; }
    public String getChartJson() { return chartJson; }
    public boolean isWatched() { return watched; }
}
