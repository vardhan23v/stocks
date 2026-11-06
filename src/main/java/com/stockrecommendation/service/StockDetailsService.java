package com.stockrecommendation.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stockrecommendation.model.Stock;
import com.stockrecommendation.model.StockPrice;
import com.stockrecommendation.model.User;

/**
 * Builds the stock details page: stored series, indicators, the recommendation
 * for exactly this data, chart JSON and the selectable ranges. A range is
 * offered only when the stored history covers it.
 */
@Service
public class StockDetailsService {

    private static final String[][] RANGES = {
        {"1W", "1 Week"}, {"1M", "1 Month"}, {"3M", "3 Months"}, {"6M", "6 Months"}, {"1Y", "1 Year"}
    };
    private static final ObjectMapper JSON = new ObjectMapper();

    private final StockService stockService;
    private final MarketDataService marketDataService;
    private final StockAnalysisService analysisService;
    private final RecommendationService recommendationService;
    private final WatchlistService watchlistService;

    public StockDetailsService(StockService stockService, MarketDataService marketDataService,
                               StockAnalysisService analysisService, RecommendationService recommendationService,
                               WatchlistService watchlistService) {
        this.stockService = stockService;
        this.marketDataService = marketDataService;
        this.analysisService = analysisService;
        this.recommendationService = recommendationService;
        this.watchlistService = watchlistService;
    }

    /** @return the page data, or null when the stock does not exist or is hidden from this user */
    public StockDetailsView load(User user, int stockId, String requestedRange) {
        Stock stock = stockService.getStock(stockId);
        if (stock == null || (!stock.isActive() && !user.isAdmin())) {
            return null;
        }
        // Cache-first: only downloads when a provider key is configured and the cache is stale.
        marketDataService.refreshIfStale(stock);
        stock = stockService.getStock(stockId);

        StockDetailsView view = new StockDetailsView();
        view.stock = stock;
        view.range = requestedRange == null || requestedRange.isBlank() ? "3M" : requestedRange;

        MarketDataService.Series series = marketDataService.getSeries(stock);
        List<StockPrice> prices = series.prices();
        view.dataSource = series.source();
        view.barCount = prices.size();
        stockService.recordView(user, stockId);
        view.watched = watchlistService.isWatched(user, stockId);
        view.indicator = analysisService.calculate(stockId, prices);
        view.recommendation = recommendationService.getCurrent(stock);
        if (prices.isEmpty()) {
            return view;
        }

        LocalDateTime newest = prices.get(prices.size() - 1).getRecordedAt();
        LocalDateTime oldest = prices.get(0).getRecordedAt();
        List<StockDetailsView.Range> ranges = new ArrayList<>();
        for (String[] r : RANGES) {
            // A few days of tolerance for weekends and holidays at the start.
            boolean covered = !oldest.isAfter(cutoff(newest, r[0]).plusDays(4));
            ranges.add(new StockDetailsView.Range(r[0], r[1], covered));
        }
        view.ranges = ranges;
        String range = view.range;
        if (ranges.stream().noneMatch(r -> r.getCode().equals(range) && r.isAvailable())) {
            view.range = ranges.stream().filter(StockDetailsView.Range::isAvailable).reduce((first, second) -> second)
                    .map(StockDetailsView.Range::getCode).orElse("1W");
        }

        int fromIndex = firstIndexOnOrAfter(prices, cutoff(newest, view.range));
        List<StockPrice> window = prices.subList(fromIndex, prices.size());
        List<StockPrice> newestFirst = new ArrayList<>(window);
        Collections.reverse(newestFirst);
        view.rangeBars = newestFirst;
        double first = window.get(0).getClosePrice().doubleValue();
        double last = window.get(window.size() - 1).getClosePrice().doubleValue();
        view.rangeChangePercent = StockAnalysisService.decimal(StockAnalysisService.percentChange(first, last), 2);

        calculate52WeekRange(view, prices, newest);

        Map<String, Object> chart = analysisService.buildChartData(prices, fromIndex);
        chart.put("symbol", stock.getSymbol());
        chart.put("currency", stock.getCurrency());
        chart.put("source", view.dataSource);
        try {
            view.chartJson = JSON.writeValueAsString(chart);
        } catch (JsonProcessingException e) {
            view.chartJson = "{}";   // the charts show their empty state; tables still show the data
        }
        return view;
    }

    /** The provider's 52-week range when one was downloaded, otherwise calculated from stored bars (and labelled). */
    private static void calculate52WeekRange(StockDetailsView view, List<StockPrice> prices, LocalDateTime newest) {
        Stock stock = view.stock;
        if (stock.getWeek52High() != null && stock.getWeek52Low() != null) {
            view.high52 = stock.getWeek52High();
            view.low52 = stock.getWeek52Low();
            view.range52Note = "Reported by " + stock.getFundamentalsSource();
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
        view.high52 = high;
        view.low52 = low;
        boolean fullYear = !prices.get(0).getRecordedAt().isAfter(yearAgo.plusDays(4));
        String source = view.isSampleData() ? "SAMPLE" : view.dataSource.replace('_', ' ');
        view.range52Note = (fullYear ? "Calculated from stored " : "Only " + prices.size() + " stored bars - range of available ")
                + source + " data";
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
}
