package com.stockrecommendation.bean;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeSet;

import com.google.gson.GsonBuilder;
import com.stockrecommendation.model.Recommendation;
import com.stockrecommendation.model.Stock;
import com.stockrecommendation.model.StockPrice;
import com.stockrecommendation.model.User;
import com.stockrecommendation.model.Watchlist;
import com.stockrecommendation.service.MarketDataService;
import com.stockrecommendation.service.RecommendationService;
import com.stockrecommendation.service.StockService;
import com.stockrecommendation.service.WatchlistService;
import com.stockrecommendation.util.DataAccessException;
import com.stockrecommendation.util.FacesUtil;
import com.stockrecommendation.util.SessionUtil;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Named;

/**
 * Backing bean of dashboard.xhtml. Everything is loaded once per request in
 * init(); the page only reads properties.
 */
@Named("dashboardBean")
@RequestScoped
public class DashboardBean {

    /** About three months of trading days for the performance chart. */
    private static final int CHART_BARS = 63;

    private final StockService stockService = new StockService();
    private final WatchlistService watchlistService = new WatchlistService();
    private final RecommendationService recommendationService = new RecommendationService();
    private final MarketDataService marketDataService = new MarketDataService();

    private User user;
    private int trackedCount;
    private LocalDateTime latestDataTimestamp;
    private boolean sampleData;
    private List<Stock> gainers = List.of();
    private List<Stock> losers = List.of();
    private List<Watchlist> watchlist = List.of();
    private int watchlistSize;
    private List<Recommendation> recommendations = List.of();
    private List<Stock> recentlyViewed = List.of();
    private String chartJson = "{}";
    private boolean loaded;

    @PostConstruct
    public void init() {
        user = SessionUtil.getCurrentUser();
        if (user == null) {
            return;
        }
        try {
            List<Stock> stocks = stockService.findAllActiveWithQuotes();
            trackedCount = stocks.size();
            latestDataTimestamp = stocks.stream().map(Stock::getLastUpdated).filter(Objects::nonNull)
                    .max(LocalDateTime::compareTo).orElse(null);
            sampleData = stocks.stream().anyMatch(Stock::isSampleData);
            gainers = stockService.topMovers(stocks, false, 5);
            losers = stockService.topMovers(stocks, true, 5);

            List<Watchlist> all = watchlistService.getWatchlist(user, StockService.SORT_CHANGE_DESC);
            watchlistSize = all.size();
            watchlist = all.subList(0, Math.min(5, all.size()));

            recommendations = recommendationService.getAllCurrent().stream()
                    .filter(recommendation -> !recommendation.isInsufficient())
                    .limit(6)
                    .toList();
            recentlyViewed = stockService.getRecentlyViewed(user, 5);
            chartJson = buildPerformanceChart(all.isEmpty() ? stocks.stream().limit(4).toList()
                                                            : all.stream().map(Watchlist::getStock).limit(5).toList(),
                                              !all.isEmpty());
            loaded = true;
        } catch (DataAccessException e) {
            FacesUtil.addError(e.getMessage());
        }
    }

    /**
     * Percentage performance of a few stocks over the last ~3 months, each
     * rebased to 0% on its first bar. Built only from stored rows.
     */
    private String buildPerformanceChart(List<Stock> stocks, boolean fromWatchlist) {
        TreeSet<String> dates = new TreeSet<>();
        List<Map<String, Object>> datasets = new ArrayList<>();
        List<Map<String, Double>> valuesByStock = new ArrayList<>();
        for (Stock stock : stocks) {
            List<StockPrice> prices = marketDataService.getSeries(stock).prices();
            if (prices.size() < 2) {
                continue;
            }
            List<StockPrice> window = prices.subList(Math.max(0, prices.size() - CHART_BARS), prices.size());
            double base = window.get(0).getClosePrice().doubleValue();
            Map<String, Double> values = new LinkedHashMap<>();
            for (StockPrice price : window) {
                String date = price.getRecordedAt().toLocalDate().toString();
                dates.add(date);
                values.put(date, Math.round((price.getClosePrice().doubleValue() / base - 1) * 10000) / 100.0);
            }
            Map<String, Object> dataset = new LinkedHashMap<>();
            dataset.put("label", stock.getSymbol());
            datasets.add(dataset);
            valuesByStock.add(values);
        }
        for (int i = 0; i < datasets.size(); i++) {
            List<Double> data = new ArrayList<>();
            for (String date : dates) {
                data.add(valuesByStock.get(i).get(date));
            }
            datasets.get(i).put("data", data);
        }
        Map<String, Object> chart = new LinkedHashMap<>();
        chart.put("labels", new ArrayList<>(dates));
        chart.put("datasets", datasets);
        chart.put("title", fromWatchlist ? "Your watchlist" : "Tracked stocks");
        return new GsonBuilder().serializeNulls().create().toJson(chart);
    }

    public User getUser() { return user; }
    public int getTrackedCount() { return trackedCount; }
    public LocalDateTime getLatestDataTimestamp() { return latestDataTimestamp; }
    public boolean isSampleData() { return sampleData; }
    public List<Stock> getGainers() { return gainers; }
    public List<Stock> getLosers() { return losers; }
    public List<Watchlist> getWatchlist() { return watchlist; }
    public int getWatchlistSize() { return watchlistSize; }
    public List<Recommendation> getRecommendations() { return recommendations; }
    public List<Stock> getRecentlyViewed() { return recentlyViewed; }
    public String getChartJson() { return chartJson; }
    public boolean isLoaded() { return loaded; }
    public String getProviderLabel() { return MarketDataService.getProviderLabel(); }
}
