package com.stockrecommendation.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeSet;

import org.springframework.stereotype.Service;
import org.springframework.ui.Model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stockrecommendation.model.Stock;
import com.stockrecommendation.model.StockPrice;
import com.stockrecommendation.model.User;
import com.stockrecommendation.model.Watchlist;

/** Collects everything the user dashboard shows. */
@Service
public class DashboardService {

    /** About three months of trading days for the performance chart. */
    private static final int CHART_BARS = 63;
    private static final ObjectMapper JSON = new ObjectMapper();

    private final StockService stockService;
    private final WatchlistService watchlistService;
    private final RecommendationService recommendationService;
    private final MarketDataService marketDataService;

    public DashboardService(StockService stockService, WatchlistService watchlistService,
                            RecommendationService recommendationService, MarketDataService marketDataService) {
        this.stockService = stockService;
        this.watchlistService = watchlistService;
        this.recommendationService = recommendationService;
        this.marketDataService = marketDataService;
    }

    public void fill(User user, Model model) {
        List<Stock> stocks = stockService.findAllActiveWithQuotes();
        model.addAttribute("trackedCount", stocks.size());
        model.addAttribute("latestDataTimestamp", stocks.stream().map(Stock::getLastUpdated).filter(Objects::nonNull)
                .max(LocalDateTime::compareTo).orElse(null));
        model.addAttribute("sampleData", stocks.stream().anyMatch(Stock::isSampleData));
        model.addAttribute("gainers", stockService.topMovers(stocks, false, 5));
        model.addAttribute("losers", stockService.topMovers(stocks, true, 5));

        List<Watchlist> all = watchlistService.getWatchlist(user, StockService.SORT_CHANGE_DESC);
        model.addAttribute("watchlistSize", all.size());
        model.addAttribute("watchlist", all.subList(0, Math.min(5, all.size())));
        model.addAttribute("recommendations", recommendationService.getAllCurrent().stream()
                .filter(recommendation -> !recommendation.isInsufficient()).limit(6).toList());
        model.addAttribute("recentlyViewed", stockService.getRecentlyViewed(user, 5));
        model.addAttribute("chartJson", performanceChart(all.isEmpty()
                ? stocks.stream().limit(4).toList()
                : all.stream().map(Watchlist::getStock).limit(5).toList()));
    }

    /** Percentage performance over ~3 months, each stock rebased to 0% on its first bar. Stored rows only. */
    private String performanceChart(List<Stock> stocks) {
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
        try {
            return JSON.writeValueAsString(chart);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }
}
