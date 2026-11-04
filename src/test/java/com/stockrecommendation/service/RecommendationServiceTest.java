package com.stockrecommendation.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.stockrecommendation.model.Recommendation;
import com.stockrecommendation.model.RecommendationRule;
import com.stockrecommendation.model.StockPrice;

/** The scoring engine on synthetic series whose expected signals are known in advance. */
class RecommendationServiceTest {

    private final List<RecommendationRule> rules = RecommendationService.defaultRules();

    @Test
    void acceleratingUptrendIsBuy() {
        // SMA +20, RSI 100 (overbought) -15, MACD above signal +15, momentum +10, volume x2 +5 = +35
        Recommendation result = RecommendationService.evaluate(1,
                StockAnalysisServiceTest.series(120, i -> 100 + 0.02 * i * i), rules);
        assertEquals(Recommendation.BUY, result.getRecommendationType());
        assertEquals(35.0, result.getRecommendationScore().doubleValue(), 1e-9);
        assertTrue(result.getExplanation().contains("overbought"));
        assertTrue(result.getSupportingJson().contains("\"signals\""));
    }

    @Test
    void acceleratingDowntrendIsSell() {
        // SMA -20, RSI 0 (oversold) +15, MACD below signal -15, momentum -10, volume x2 -5 = -35
        Recommendation result = RecommendationService.evaluate(1,
                StockAnalysisServiceTest.series(120, i -> 400 - 0.02 * i * i), rules);
        assertEquals(Recommendation.SELL, result.getRecommendationType());
        assertEquals(-35.0, result.getRecommendationScore().doubleValue(), 1e-9);
    }

    @Test
    void sidewaysMarketIsHold() {
        List<StockPrice> prices = StockAnalysisServiceTest.series(120, i -> 100 + 2 * Math.sin(2 * Math.PI * i / 20));
        prices.get(prices.size() - 1).setVolume(1000L);   // no volume spike
        Recommendation result = RecommendationService.evaluate(1, prices, rules);
        assertEquals(Recommendation.HOLD, result.getRecommendationType());
        assertTrue(Math.abs(result.getRecommendationScore().doubleValue()) < 25);
        assertTrue(result.getExplanation().contains("mixed or neutral"));
    }

    @Test
    void shortHistoryGivesInsufficientDataInsteadOfAGuess() {
        Recommendation result = RecommendationService.evaluate(1,
                StockAnalysisServiceTest.series(RecommendationService.MIN_BARS - 1, i -> 100 + i), rules);
        assertEquals(Recommendation.INSUFFICIENT_DATA, result.getRecommendationType());
        assertEquals("Insufficient data", result.getLabel());
        assertEquals(0, result.getRecommendationScore().signum());
    }

    @Test
    void disabledRulesDoNotContribute() {
        List<RecommendationRule> onlySma = RecommendationService.defaultRules().stream().peek(rule -> {
            if (!RecommendationRule.SMA_CROSSOVER.equals(rule.getRuleKey()) && !RecommendationRule.DECISION.equals(rule.getRuleKey())) {
                rule.setEnabled(false);
            }
        }).toList();
        Recommendation result = RecommendationService.evaluate(1,
                StockAnalysisServiceTest.series(120, i -> 100 + 0.02 * i * i), onlySma);
        assertEquals(20.0, result.getRecommendationScore().doubleValue(), 1e-9);
        assertEquals(Recommendation.HOLD, result.getRecommendationType());
    }
}
