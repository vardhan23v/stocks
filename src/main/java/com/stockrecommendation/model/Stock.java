package com.stockrecommendation.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * A listed company plus, when loaded for a listing, its latest stored quote.
 *
 * The quote fields (latestPrice ... dataSource) are not columns of the stocks
 * table: StockDAO fills them from the two newest stock_prices rows, so every
 * price shown on screen carries the timestamp and source it came from.
 */
public class Stock implements Serializable {

    private static final long serialVersionUID = 1L;

    private int id;
    private String companyName;
    private String symbol;
    private String exchange;
    private String apiSymbol;
    private String currency = "USD";
    private String sector;
    private String industry;
    private String description;
    private BigDecimal marketCap;
    private BigDecimal week52High;
    private BigDecimal week52Low;
    private String fundamentalsSource;
    private LocalDateTime fundamentalsUpdatedAt;
    private boolean active = true;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // ---------------------------------------------- latest stored quote
    private BigDecimal latestPrice;
    private BigDecimal previousClose;
    private Long latestVolume;
    private LocalDateTime lastUpdated;
    private String dataSource;

    /** Latest recommendation type, when the listing joined it in. */
    private String recommendationType;

    public Stock() {
    }

    /** Absolute change between the two newest closes, or null. */
    public BigDecimal getPriceChange() {
        if (latestPrice == null || previousClose == null) {
            return null;
        }
        return latestPrice.subtract(previousClose);
    }

    /** Percentage change between the two newest closes, or null. */
    public BigDecimal getPercentChange() {
        if (latestPrice == null || previousClose == null || previousClose.signum() == 0) {
            return null;
        }
        return latestPrice.subtract(previousClose)
                .multiply(BigDecimal.valueOf(100))
                .divide(previousClose, 2, java.math.RoundingMode.HALF_UP);
    }

    /** "up", "down" or "flat" - used as a CSS class for green / red / grey. */
    public String getDirection() {
        BigDecimal change = getPriceChange();
        if (change == null || change.signum() == 0) {
            return "flat";
        }
        return change.signum() > 0 ? "up" : "down";
    }

    public boolean isSampleData() {
        return "SAMPLE".equals(dataSource);
    }

    public boolean isHasPrice() {
        return latestPrice != null;
    }

    public String getCurrencySymbol() {
        return "INR".equals(currency) ? "₹" : "USD".equals(currency) ? "$" : currency + " ";
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }

    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }

    public String getExchange() { return exchange; }
    public void setExchange(String exchange) { this.exchange = exchange; }

    public String getApiSymbol() { return apiSymbol; }
    public void setApiSymbol(String apiSymbol) { this.apiSymbol = apiSymbol; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public String getSector() { return sector; }
    public void setSector(String sector) { this.sector = sector; }

    public String getIndustry() { return industry; }
    public void setIndustry(String industry) { this.industry = industry; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public BigDecimal getMarketCap() { return marketCap; }
    public void setMarketCap(BigDecimal marketCap) { this.marketCap = marketCap; }

    public BigDecimal getWeek52High() { return week52High; }
    public void setWeek52High(BigDecimal week52High) { this.week52High = week52High; }

    public BigDecimal getWeek52Low() { return week52Low; }
    public void setWeek52Low(BigDecimal week52Low) { this.week52Low = week52Low; }

    public String getFundamentalsSource() { return fundamentalsSource; }
    public void setFundamentalsSource(String fundamentalsSource) { this.fundamentalsSource = fundamentalsSource; }

    public LocalDateTime getFundamentalsUpdatedAt() { return fundamentalsUpdatedAt; }
    public void setFundamentalsUpdatedAt(LocalDateTime fundamentalsUpdatedAt) { this.fundamentalsUpdatedAt = fundamentalsUpdatedAt; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public BigDecimal getLatestPrice() { return latestPrice; }
    public void setLatestPrice(BigDecimal latestPrice) { this.latestPrice = latestPrice; }

    public BigDecimal getPreviousClose() { return previousClose; }
    public void setPreviousClose(BigDecimal previousClose) { this.previousClose = previousClose; }

    public Long getLatestVolume() { return latestVolume; }
    public void setLatestVolume(Long latestVolume) { this.latestVolume = latestVolume; }

    public LocalDateTime getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(LocalDateTime lastUpdated) { this.lastUpdated = lastUpdated; }

    public String getDataSource() { return dataSource; }
    public void setDataSource(String dataSource) { this.dataSource = dataSource; }

    public String getRecommendationType() { return recommendationType; }
    public void setRecommendationType(String recommendationType) { this.recommendationType = recommendationType; }
}
