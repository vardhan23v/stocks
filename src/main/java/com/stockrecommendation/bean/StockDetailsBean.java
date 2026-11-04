package com.stockrecommendation.bean;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.google.gson.GsonBuilder;
import com.stockrecommendation.model.Recommendation;
import com.stockrecommendation.model.Stock;
import com.stockrecommendation.model.StockIndicator;
import com.stockrecommendation.model.StockPrice;
import com.stockrecommendation.model.User;
import com.stockrecommendation.service.MarketDataService;
import com.stockrecommendation.service.RecommendationService;
import com.stockrecommendation.service.StockAnalysisService;
import com.stockrecommendation.service.StockService;
import com.stockrecommendation.service.WatchlistService;
import com.stockrecommendation.util.DataAccessException;
import com.stockrecommendation.util.FacesUtil;
import com.stockrecommendation.util.SessionUtil;

import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;

/**
 * Backing bean of stock-details.xhtml?stockId=3&range=3M.
 *
 * load() reads the stored series, calculates the indicators, gets (or
 * generates) the recommendation for exactly this data, and prepares the chart
 * JSON. Range buttons are offered only when the stored history covers them.
 */
@Named("stockDetailsBean")
@ViewScoped
public class StockDetailsBean implements Serializable {

    private static final long serialVersionUID = 1L;

    /** A selectable chart range and whether the stored data covers it. */
    public static class Range implements Serializable {

        private static final long serialVersionUID = 1L;

        private final String code;
        private final String label;
        private final boolean available;

        Range(String code, String label, boolean available) {
            this.code = code;
            this.label = label;
            this.available = available;
        }

        public String getCode() { return code; }
        public String getLabel() { return label; }
        public boolean isAvailable() { return available; }
    }

    private static final String[][] RANGES = {
        {"1W", "1 Week"}, {"1M", "1 Month"}, {"3M", "3 Months"}, {"6M", "6 Months"}, {"1Y", "1 Year"}
    };

    private final StockService stockService = new StockService();
    private final MarketDataService marketDataService = new MarketDataService();
    private final StockAnalysisService analysisService = new StockAnalysisService();
    private final RecommendationService recommendationService = new RecommendationService();
    private final WatchlistService watchlistService = new WatchlistService();

    private Integer stockId;
    private String range = "3M";

    private Stock stock;
    private String dataSource;
    private int barCount;
    private StockIndicator indicator;
    private Recommendation recommendation;
    private List<Range> ranges = List.of();
    private List<StockPrice> rangeBars = List.of();
    private BigDecimal rangeChangePercent;
    private BigDecimal high52;
    private BigDecimal low52;
    private String range52Note;
    private String chartJson = "{}";
    private boolean watched;

    /** Called by <f:viewAction>. */
    public void load() {
        if (stockId == null) {
            FacesUtil.addError("No stock was selected.");
            return;
        }
        try {
            User user = SessionUtil.getCurrentUser();
            stock = stockService.getStock(stockId);
            if (stock == null || (!stock.isActive() && !user.isAdmin())) {
                stock = null;
                FacesUtil.addError("That stock does not exist or is no longer listed.");
                return;
            }
            // Cache-first: only downloads when the provider is configured and the cache is stale.
            marketDataService.refreshIfStale(stock);
            stock = stockService.getStock(stockId);

            MarketDataService.Series series = marketDataService.getSeries(stock);
            List<StockPrice> prices = series.prices();
            dataSource = series.source();
            barCount = prices.size();
            stockService.recordView(user, stockId);
            watched = watchlistService.isWatched(user, stockId);

            indicator = analysisService.calculate(stockId, prices);
            recommendation = recommendationService.getCurrent(stock);
            if (prices.isEmpty()) {
                return;
            }

            LocalDateTime newest = prices.get(prices.size() - 1).getRecordedAt();
            LocalDateTime oldest = prices.get(0).getRecordedAt();
            ranges = new ArrayList<>();
            for (String[] r : RANGES) {
                // A few days of tolerance for weekends and holidays at the start.
                boolean covered = !oldest.isAfter(cutoff(newest, r[0]).plusDays(4));
                ranges.add(new Range(r[0], r[1], covered));
            }
            if (ranges.stream().noneMatch(r -> r.getCode().equals(range) && r.isAvailable())) {
                range = ranges.stream().filter(Range::isAvailable).reduce((first, second) -> second)
                        .map(Range::getCode).orElse("1W");
            }

            int fromIndex = firstIndexOnOrAfter(prices, cutoff(newest, range));
            List<StockPrice> window = prices.subList(fromIndex, prices.size());
            rangeBars = new ArrayList<>(window);
            Collections.reverse(rangeBars);
            double first = window.get(0).getClosePrice().doubleValue();
            double last = window.get(window.size() - 1).getClosePrice().doubleValue();
            rangeChangePercent = StockAnalysisService.decimal(StockAnalysisService.percentChange(first, last), 2);

            calculate52WeekRange(prices, newest);

            Map<String, Object> chart = analysisService.buildChartData(prices, fromIndex);
            chart.put("symbol", stock.getSymbol());
            chart.put("currency", stock.getCurrency());
            chart.put("source", dataSource);
            chartJson = new GsonBuilder().serializeNulls().create().toJson(chart);
        } catch (DataAccessException e) {
            FacesUtil.addError(e.getMessage());
        }
    }

    /**
     * 52-week high / low: the provider's figure when one was downloaded,
     * otherwise calculated from the stored bars (and labelled as such).
     */
    private void calculate52WeekRange(List<StockPrice> prices, LocalDateTime newest) {
        if (stock.getWeek52High() != null && stock.getWeek52Low() != null) {
            high52 = stock.getWeek52High();
            low52 = stock.getWeek52Low();
            range52Note = "Reported by " + stock.getFundamentalsSource();
            return;
        }
        LocalDateTime yearAgo = newest.minusWeeks(52);
        BigDecimal high = null;
        BigDecimal low = null;
        for (StockPrice price : prices) {
            if (price.getRecordedAt().isBefore(yearAgo)) {
                continue;
            }
            high = high == null || price.getHighPrice().compareTo(high) > 0 ? price.getHighPrice() : high;
            low = low == null || price.getLowPrice().compareTo(low) < 0 ? price.getLowPrice() : low;
        }
        high52 = high;
        low52 = low;
        boolean fullYear = !prices.get(0).getRecordedAt().isAfter(yearAgo.plusDays(4));
        range52Note = (fullYear ? "Calculated from stored " : "Only " + prices.size() + " stored bars - range of available ")
                + label(dataSource) + " data";
    }

    private static LocalDateTime cutoff(LocalDateTime newest, String code) {
        return switch (code) {
            case "1W" -> newest.minusWeeks(1);
            case "1M" -> newest.minusMonths(1);
            case "6M" -> newest.minusMonths(6);
            case "1Y" -> newest.minusYears(1);
            default -> newest.minusMonths(3);
        };
    }

    private static int firstIndexOnOrAfter(List<StockPrice> prices, LocalDateTime cutoff) {
        for (int i = 0; i < prices.size(); i++) {
            if (prices.get(i).getRecordedAt().isAfter(cutoff)) {
                return Math.min(i, Math.max(0, prices.size() - 2));   // always at least two bars
            }
        }
        return Math.max(0, prices.size() - 2);
    }

    private static String label(String source) {
        return source == null ? "" : "SAMPLE".equals(source) ? "SAMPLE" : source.replace('_', ' ');
    }

    // ------------------------------------------------------------ actions

    public String addToWatchlist() {
        try {
            watchlistService.add(SessionUtil.getCurrentUser(), stockId == null ? 0 : stockId);
            FacesUtil.keepMessagesForRedirect();
            FacesUtil.addInfo(stock.getSymbol() + " was added to your watchlist.");
            return "stock-details?faces-redirect=true&includeViewParams=true";
        } catch (IllegalStateException | DataAccessException e) {
            FacesUtil.addError(e.getMessage());
            return null;
        }
    }

    public String removeFromWatchlist() {
        try {
            watchlistService.remove(SessionUtil.getCurrentUser(), stockId == null ? 0 : stockId);
            FacesUtil.keepMessagesForRedirect();
            FacesUtil.addInfo(stock.getSymbol() + " was removed from your watchlist.");
            return "stock-details?faces-redirect=true&includeViewParams=true";
        } catch (IllegalStateException | DataAccessException e) {
            FacesUtil.addError(e.getMessage());
            return null;
        }
    }

    // ------------------------------------------------------------ properties

    public boolean isSampleData() {
        return MarketDataService.SAMPLE.equals(dataSource);
    }

    public String getDataSourceLabel() {
        return dataSource == null ? "No data" : isSampleData() ? "SAMPLE DATA (synthetic)" : label(dataSource) + " (delayed end-of-day)";
    }

    public LocalDate getNewestDate() {
        return stock == null || stock.getLastUpdated() == null ? null : stock.getLastUpdated().toLocalDate();
    }

    public String getRangeDirection() {
        return rangeChangePercent == null || rangeChangePercent.signum() == 0 ? "flat"
             : rangeChangePercent.signum() > 0 ? "up" : "down";
    }

    /** Distance of the close from SMA(20), in percent - shown under the SMA value. */
    public BigDecimal getDistanceFromSma() {
        if (indicator == null || indicator.getSma() == null || stock == null || stock.getLatestPrice() == null) {
            return null;
        }
        return stock.getLatestPrice().subtract(indicator.getSma()).multiply(BigDecimal.valueOf(100))
                .divide(indicator.getSma(), 2, RoundingMode.HALF_UP);
    }

    public Integer getStockId() { return stockId; }
    public void setStockId(Integer stockId) { this.stockId = stockId; }

    public String getRange() { return range; }
    public void setRange(String range) { this.range = range == null || range.isBlank() ? "3M" : range; }

    public Stock getStock() { return stock; }
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
