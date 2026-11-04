package com.stockrecommendation.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Latest technical indicators of one stock, calculated by StockAnalysisService
 * from stored prices. A null value means "not enough history for this one".
 */
public class StockIndicator implements Serializable {

    private static final long serialVersionUID = 1L;

    private long id;
    private int stockId;
    private BigDecimal sma;          // SMA(20)
    private BigDecimal smaLong;      // SMA(50)
    private BigDecimal ema;          // EMA(20)
    private BigDecimal rsi;          // RSI(14)
    private BigDecimal macd;         // EMA(12) - EMA(26)
    private BigDecimal signalLine;   // EMA(9) of MACD
    private BigDecimal volatility;   // annualised, percent
    private BigDecimal momentum;     // % change over the last 20 bars
    private BigDecimal volumeRatio;  // last volume / 20 bar average
    private int barCount;
    private String dataSource;
    private LocalDateTime dataTimestamp;
    private LocalDateTime calculatedAt;

    public StockIndicator() {
    }

    public BigDecimal getMacdHistogram() {
        return macd == null || signalLine == null ? null : macd.subtract(signalLine);
    }

    /** Text used under the RSI value - a common interpretation, not a signal. */
    public String getRsiZone() {
        if (rsi == null) {
            return "Not enough data";
        }
        if (rsi.doubleValue() < 30) {
            return "Below 30 - traditionally considered oversold";
        }
        if (rsi.doubleValue() > 70) {
            return "Above 70 - traditionally considered overbought";
        }
        return "Between 30 and 70 - intermediate range";
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public int getStockId() { return stockId; }
    public void setStockId(int stockId) { this.stockId = stockId; }

    public BigDecimal getSma() { return sma; }
    public void setSma(BigDecimal sma) { this.sma = sma; }

    public BigDecimal getSmaLong() { return smaLong; }
    public void setSmaLong(BigDecimal smaLong) { this.smaLong = smaLong; }

    public BigDecimal getEma() { return ema; }
    public void setEma(BigDecimal ema) { this.ema = ema; }

    public BigDecimal getRsi() { return rsi; }
    public void setRsi(BigDecimal rsi) { this.rsi = rsi; }

    public BigDecimal getMacd() { return macd; }
    public void setMacd(BigDecimal macd) { this.macd = macd; }

    public BigDecimal getSignalLine() { return signalLine; }
    public void setSignalLine(BigDecimal signalLine) { this.signalLine = signalLine; }

    public BigDecimal getVolatility() { return volatility; }
    public void setVolatility(BigDecimal volatility) { this.volatility = volatility; }

    public BigDecimal getMomentum() { return momentum; }
    public void setMomentum(BigDecimal momentum) { this.momentum = momentum; }

    public BigDecimal getVolumeRatio() { return volumeRatio; }
    public void setVolumeRatio(BigDecimal volumeRatio) { this.volumeRatio = volumeRatio; }

    public int getBarCount() { return barCount; }
    public void setBarCount(int barCount) { this.barCount = barCount; }

    public String getDataSource() { return dataSource; }
    public void setDataSource(String dataSource) { this.dataSource = dataSource; }

    public LocalDateTime getDataTimestamp() { return dataTimestamp; }
    public void setDataTimestamp(LocalDateTime dataTimestamp) { this.dataTimestamp = dataTimestamp; }

    public LocalDateTime getCalculatedAt() { return calculatedAt; }
    public void setCalculatedAt(LocalDateTime calculatedAt) { this.calculatedAt = calculatedAt; }
}
