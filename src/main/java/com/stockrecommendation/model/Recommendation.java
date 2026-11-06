package com.stockrecommendation.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

/**
 * One output of the rule-based engine. The explanation holds one reason per
 * line, so the page can show them as a list.
 */
public class Recommendation implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String BUY = "BUY";
    public static final String HOLD = "HOLD";
    public static final String SELL = "SELL";
    public static final String INSUFFICIENT_DATA = "INSUFFICIENT_DATA";

    private long id;
    private int stockId;
    private String recommendationType;
    private BigDecimal recommendationScore = BigDecimal.ZERO;
    private String explanation;
    private String supportingJson;
    private String dataSource;
    private LocalDateTime generatedAt;
    private LocalDateTime dataTimestamp;

    // joined from stocks
    private String symbol;
    private String companyName;
    private String exchange;

    public Recommendation() {
    }

    public List<String> getReasons() {
        if (explanation == null || explanation.isBlank()) {
            return List.of();
        }
        return Arrays.stream(explanation.split("\n")).filter(line -> !line.isBlank()).toList();
    }

    /** Text shown on the badge. */
    public String getLabel() {
        return INSUFFICIENT_DATA.equals(recommendationType) ? "Insufficient data" : recommendationType;
    }

    public boolean isInsufficient() {
        return INSUFFICIENT_DATA.equals(recommendationType);
    }

    /** Highest possible absolute score for the rules used, read from the supporting JSON. */
    public double getMaxScore() {
        if (supportingJson != null) {
            try {
                com.fasterxml.jackson.databind.JsonNode max = new com.fasterxml.jackson.databind.ObjectMapper()
                        .readTree(supportingJson).get("maxScore");
                if (max != null && max.isNumber() && max.asDouble() > 0) {
                    return max.asDouble();
                }
            } catch (com.fasterxml.jackson.core.JsonProcessingException | RuntimeException e) {
                // fall through to the default
            }
        }
        return 65;
    }

    /** 0..100: where the score sits between -maxScore and +maxScore (for the score bar). */
    public double getScorePosition() {
        double score = recommendationScore == null ? 0 : recommendationScore.doubleValue();
        double position = 50 + score / getMaxScore() * 50;
        return Math.max(0, Math.min(100, Math.round(position * 10) / 10.0));
    }

    public boolean isSampleData() {
        return "SAMPLE".equals(dataSource);
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public int getStockId() { return stockId; }
    public void setStockId(int stockId) { this.stockId = stockId; }

    public String getRecommendationType() { return recommendationType; }
    public void setRecommendationType(String recommendationType) { this.recommendationType = recommendationType; }

    public BigDecimal getRecommendationScore() { return recommendationScore; }
    public void setRecommendationScore(BigDecimal recommendationScore) { this.recommendationScore = recommendationScore; }

    public String getExplanation() { return explanation; }
    public void setExplanation(String explanation) { this.explanation = explanation; }

    public String getSupportingJson() { return supportingJson; }
    public void setSupportingJson(String supportingJson) { this.supportingJson = supportingJson; }

    public String getDataSource() { return dataSource; }
    public void setDataSource(String dataSource) { this.dataSource = dataSource; }

    public LocalDateTime getGeneratedAt() { return generatedAt; }
    public void setGeneratedAt(LocalDateTime generatedAt) { this.generatedAt = generatedAt; }

    public LocalDateTime getDataTimestamp() { return dataTimestamp; }
    public void setDataTimestamp(LocalDateTime dataTimestamp) { this.dataTimestamp = dataTimestamp; }

    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }

    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }

    public String getExchange() { return exchange; }
    public void setExchange(String exchange) { this.exchange = exchange; }
}
