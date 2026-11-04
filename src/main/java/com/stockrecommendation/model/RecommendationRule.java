package com.stockrecommendation.model;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * One weighted rule of the recommendation engine (see RecommendationService).
 * paramLow / paramHigh mean different things per rule, explained in the
 * description column - for RSI they are the 30 / 70 bands.
 */
public class RecommendationRule implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String SMA_CROSSOVER = "SMA_CROSSOVER";
    public static final String RSI = "RSI";
    public static final String MACD = "MACD";
    public static final String MOMENTUM = "MOMENTUM";
    public static final String VOLUME = "VOLUME";
    public static final String DECISION = "DECISION";

    private int id;
    private String ruleKey;
    private String name;
    private String description;
    private BigDecimal weight = BigDecimal.ZERO;
    private BigDecimal paramLow;
    private BigDecimal paramHigh;
    private boolean enabled = true;

    public RecommendationRule() {
    }

    public RecommendationRule(String ruleKey, String name, double weight, Double paramLow, Double paramHigh) {
        this.ruleKey = ruleKey;
        this.name = name;
        this.weight = BigDecimal.valueOf(weight);
        this.paramLow = paramLow == null ? null : BigDecimal.valueOf(paramLow);
        this.paramHigh = paramHigh == null ? null : BigDecimal.valueOf(paramHigh);
    }

    /** paramLow without trailing zeros, for display ("0.5" instead of "0.5000"). */
    public String getParamLowText() {
        return paramLow == null ? "-" : paramLow.stripTrailingZeros().toPlainString();
    }

    public String getParamHighText() {
        return paramHigh == null ? "-" : paramHigh.stripTrailingZeros().toPlainString();
    }

    public String getWeightText() {
        return weight == null ? "-" : weight.stripTrailingZeros().toPlainString();
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getRuleKey() { return ruleKey; }
    public void setRuleKey(String ruleKey) { this.ruleKey = ruleKey; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public BigDecimal getWeight() { return weight; }
    public void setWeight(BigDecimal weight) { this.weight = weight; }

    public BigDecimal getParamLow() { return paramLow; }
    public void setParamLow(BigDecimal paramLow) { this.paramLow = paramLow; }

    public BigDecimal getParamHigh() { return paramHigh; }
    public void setParamHigh(BigDecimal paramHigh) { this.paramHigh = paramHigh; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
}
