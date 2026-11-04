package com.stockrecommendation.api;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.stockrecommendation.model.StockPrice;

/**
 * What the provider returned for one symbol, already converted to model
 * objects: the daily bars (oldest first) and, from the company overview
 * endpoint, the fundamentals when they were present.
 */
public class MarketDataResponse {

    private final String provider;
    private final String symbol;
    private final List<StockPrice> bars = new ArrayList<>();
    private LocalDateTime lastRefreshed;
    private BigDecimal marketCap;
    private BigDecimal week52High;
    private BigDecimal week52Low;

    public MarketDataResponse(String provider, String symbol) {
        this.provider = provider;
        this.symbol = symbol;
    }

    public String getProvider() { return provider; }
    public String getSymbol() { return symbol; }
    public List<StockPrice> getBars() { return bars; }

    public LocalDateTime getLastRefreshed() { return lastRefreshed; }
    public void setLastRefreshed(LocalDateTime lastRefreshed) { this.lastRefreshed = lastRefreshed; }

    public BigDecimal getMarketCap() { return marketCap; }
    public void setMarketCap(BigDecimal marketCap) { this.marketCap = marketCap; }

    public BigDecimal getWeek52High() { return week52High; }
    public void setWeek52High(BigDecimal week52High) { this.week52High = week52High; }

    public BigDecimal getWeek52Low() { return week52Low; }
    public void setWeek52Low(BigDecimal week52Low) { this.week52Low = week52Low; }
}
