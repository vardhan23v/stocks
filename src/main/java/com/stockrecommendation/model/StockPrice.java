package com.stockrecommendation.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** One OHLCV bar of one stock, from one data source. */
public class StockPrice implements Serializable {

    private static final long serialVersionUID = 1L;

    private long id;
    private int stockId;
    private BigDecimal openPrice;
    private BigDecimal highPrice;
    private BigDecimal lowPrice;
    private BigDecimal closePrice;
    private BigDecimal adjustedClose;
    private Long volume;
    private LocalDateTime recordedAt;
    private String dataSource;

    public StockPrice() {
    }

    public StockPrice(LocalDateTime recordedAt, BigDecimal open, BigDecimal high, BigDecimal low,
                      BigDecimal close, Long volume, String dataSource) {
        this.recordedAt = recordedAt;
        this.openPrice = open;
        this.highPrice = high;
        this.lowPrice = low;
        this.closePrice = close;
        this.volume = volume;
        this.dataSource = dataSource;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public int getStockId() { return stockId; }
    public void setStockId(int stockId) { this.stockId = stockId; }

    public BigDecimal getOpenPrice() { return openPrice; }
    public void setOpenPrice(BigDecimal openPrice) { this.openPrice = openPrice; }

    public BigDecimal getHighPrice() { return highPrice; }
    public void setHighPrice(BigDecimal highPrice) { this.highPrice = highPrice; }

    public BigDecimal getLowPrice() { return lowPrice; }
    public void setLowPrice(BigDecimal lowPrice) { this.lowPrice = lowPrice; }

    public BigDecimal getClosePrice() { return closePrice; }
    public void setClosePrice(BigDecimal closePrice) { this.closePrice = closePrice; }

    public BigDecimal getAdjustedClose() { return adjustedClose; }
    public void setAdjustedClose(BigDecimal adjustedClose) { this.adjustedClose = adjustedClose; }

    public Long getVolume() { return volume; }
    public void setVolume(Long volume) { this.volume = volume; }

    public LocalDateTime getRecordedAt() { return recordedAt; }
    public void setRecordedAt(LocalDateTime recordedAt) { this.recordedAt = recordedAt; }

    public String getDataSource() { return dataSource; }
    public void setDataSource(String dataSource) { this.dataSource = dataSource; }
}
