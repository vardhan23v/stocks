package com.stockrecommendation.service;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stockrecommendation.dao.ActivityDAO;
import com.stockrecommendation.dao.IndicatorDAO;
import com.stockrecommendation.dao.RecommendationDAO;
import com.stockrecommendation.dao.RuleDAO;
import com.stockrecommendation.dao.StockDAO;
import com.stockrecommendation.model.Recommendation;
import com.stockrecommendation.model.RecommendationRule;
import com.stockrecommendation.model.Stock;
import com.stockrecommendation.model.StockIndicator;
import com.stockrecommendation.model.StockPrice;
import com.stockrecommendation.model.User;

/**
 * The explainable, rule-based recommendation engine.
 *
 * Scoring methodology (weights and thresholds live in recommendation_rules and
 * can be changed by the administrator):
 *
 *   Every enabled rule produces a signal of +1 (bullish), -1 (bearish) or 0
 *   (neutral). The score is the sum of signal x weight over all rules.
 *
 *     SMA_CROSSOVER  SMA(20) above / below SMA(50) by more than param_low %
 *     RSI            RSI(14) below param_low (oversold, +1) or above param_high (overbought, -1)
 *     MACD           MACD line above / below its signal line
 *     MOMENTUM       close-to-close change over 20 sessions beyond +/- param_low %
 *     VOLUME         newest volume >= param_low x its 20-session average, in the
 *                    direction of the momentum (confirmation); 0 otherwise
 *
 *   DECISION: score >= param_high -> BUY, score <= param_low -> SELL, otherwise HOLD.
 *   Fewer than MIN_BARS stored bars -> INSUFFICIENT_DATA (never a guess).
 *
 * Each rule writes one plain-English reason line, which is stored as the
 * explanation. The output is educational information, not financial advice.
 */
@Service
public class RecommendationService {


    /** SMA(50) needs 50 bars and the MACD signal 34; 60 leaves a small margin. */
    public static final int MIN_BARS = 60;

    private static final ObjectMapper JSON = new ObjectMapper();

    private final RecommendationDAO recommendationDAO;
    private final IndicatorDAO indicatorDAO;
    private final RuleDAO ruleDAO;
    private final StockDAO stockDAO;
    private final ActivityDAO activityDAO;
    private final MarketDataService marketDataService;
    private final StockAnalysisService analysisService;

    public RecommendationService(RecommendationDAO recommendationDAO, IndicatorDAO indicatorDAO, RuleDAO ruleDAO, StockDAO stockDAO, ActivityDAO activityDAO, MarketDataService marketDataService, StockAnalysisService analysisService) {
        this.recommendationDAO = recommendationDAO;
        this.indicatorDAO = indicatorDAO;
        this.ruleDAO = ruleDAO;
        this.stockDAO = stockDAO;
        this.activityDAO = activityDAO;
        this.marketDataService = marketDataService;
        this.analysisService = analysisService;
    }

    // ------------------------------------------------------------ queries

    /**
     * The recommendation for the stock's current stored data. A new row is
     * generated only when the newest bar (or its source) changed since the last
     * stored recommendation - opening a page never creates duplicates.
     */
    public Recommendation getCurrent(Stock stock) {
        MarketDataService.Series series = marketDataService.getSeries(stock);
        Recommendation stored = recommendationDAO.findLatestForStock(stock.getId());
        if (stored != null && isFor(stored, series)) {
            return stored;
        }
        return generate(stock, series);
    }

    /** Current recommendation of every active stock, best score first. */
    public List<Recommendation> getAllCurrent() {
        for (Stock stock : stockDAO.findAllActive()) {
            getCurrent(stock);
        }
        return recommendationDAO.findLatestPerStock();
    }

    public List<Recommendation> getHistory(int stockId) {
        return recommendationDAO.findHistory(stockId, 10);
    }

    /** Every stored recommendation, newest first (admin page). */
    public List<Recommendation> getRecent(User admin, int limit) {
        AdminService.requireAdmin(admin);
        return recommendationDAO.findRecent(limit);
    }

    public List<RecommendationRule> getRules() {
        return ruleDAO.findAll();
    }

    // ------------------------------------------------------------ commands

    /** Re-evaluates every active stock with the current rules (admin action). */
    public int regenerateAll(User admin) {
        AdminService.requireAdmin(admin);
        int count = 0;
        for (Stock stock : stockDAO.findAllActive()) {
            generate(stock, marketDataService.getSeries(stock));
            count++;
        }
        activityDAO.log(admin.getId(), "RECOMMENDATIONS_REGENERATED", count + " stocks re-evaluated");
        return count;
    }

    public void delete(User admin, long recommendationId) {
        AdminService.requireAdmin(admin);
        if (!recommendationDAO.delete(recommendationId)) {
            throw new IllegalStateException("That recommendation no longer exists.");
        }
        activityDAO.log(admin.getId(), "RECOMMENDATION_DELETED", "Recommendation #" + recommendationId);
    }

    private boolean isFor(Recommendation stored, MarketDataService.Series series) {
        return Objects.equals(stored.getDataSource(), series.sourceOrNone())
            && Objects.equals(stored.getDataTimestamp(), series.newestTimestamp());
    }

    private Recommendation generate(Stock stock, MarketDataService.Series series) {
        Recommendation recommendation = evaluate(stock.getId(), series.prices(), ruleDAO.findAll());
        recommendation.setDataSource(series.sourceOrNone());
        recommendationDAO.insert(recommendation);

        StockIndicator indicator = analysisService.calculate(stock.getId(), series.prices());
        if (indicator.getDataTimestamp() != null
                && !indicatorDAO.exists(stock.getId(), indicator.getDataSource(), indicator.getDataTimestamp())) {
            indicatorDAO.insert(indicator);
        }

        recommendation.setSymbol(stock.getSymbol());
        recommendation.setCompanyName(stock.getCompanyName());
        recommendation.setExchange(stock.getExchange());
        return recommendation;
    }

    // ------------------------------------------------------------ the engine

    /**
     * Evaluates one price series against the rules. Pure function: no database
     * access, so it is unit tested directly and reused by the sample data tool.
     */
    public static Recommendation evaluate(int stockId, List<StockPrice> prices, List<RecommendationRule> rules) {
        Recommendation recommendation = new Recommendation();
        recommendation.setStockId(stockId);
        if (!prices.isEmpty()) {
            StockPrice newest = prices.get(prices.size() - 1);
            recommendation.setDataTimestamp(newest.getRecordedAt());
            recommendation.setDataSource(newest.getDataSource());
        }

        if (prices.size() < MIN_BARS) {
            recommendation.setRecommendationType(Recommendation.INSUFFICIENT_DATA);
            recommendation.setRecommendationScore(BigDecimal.ZERO);
            recommendation.setExplanation(prices.isEmpty()
                    ? "Insufficient data: no price history is stored for this stock yet."
                    : "Insufficient data: only " + prices.size() + " price bars are stored, and at least "
                      + MIN_BARS + " are needed to calculate SMA(50) and MACD reliably. No recommendation is made.");
            return recommendation;
        }

        Map<String, RecommendationRule> byKey = new LinkedHashMap<>();
        for (RecommendationRule rule : rules) {
            byKey.put(rule.getRuleKey(), rule);
        }

        double[] closes = StockAnalysisService.closes(prices);
        int last = closes.length - 1;
        List<String> reasons = new ArrayList<>();
        List<Map<String, Object>> signals = new ArrayList<>();
        double score = 0;
        double maxScore = 0;

        // 1. Moving average trend / crossover
        RecommendationRule smaRule = active(byKey, RecommendationRule.SMA_CROSSOVER);
        double[] smaShort = StockAnalysisService.sma(closes, StockAnalysisService.SMA_SHORT);
        double[] smaLong = StockAnalysisService.sma(closes, StockAnalysisService.SMA_LONG);
        if (smaRule != null) {
            double band = param(smaRule.getParamLow(), 0.5);
            double gap = StockAnalysisService.percentChange(smaLong[last], smaShort[last]);
            int signal = gap > band ? 1 : gap < -band ? -1 : 0;
            String crossover = recentCrossover(smaShort, smaLong, 5);
            String text;
            if (signal > 0) {
                text = String.format(Locale.US, "SMA(20) %s is %.2f%% above SMA(50) %s: the short-term trend is stronger than the long-term trend.%s",
                        money(smaShort[last]), gap, money(smaLong[last]), crossover);
            } else if (signal < 0) {
                text = String.format(Locale.US, "SMA(20) %s is %.2f%% below SMA(50) %s: the short-term trend is weaker than the long-term trend.%s",
                        money(smaShort[last]), -gap, money(smaLong[last]), crossover);
            } else {
                text = String.format(Locale.US, "Short-term and long-term moving averages (SMA 20: %s, SMA 50: %s) are within %.2f%% of each other and do not show a strong trend.",
                        money(smaShort[last]), money(smaLong[last]), band);
            }
            score += add(reasons, signals, smaRule, signal, text);
            maxScore += smaRule.getWeight().doubleValue();
        }

        // 2. RSI
        RecommendationRule rsiRule = active(byKey, RecommendationRule.RSI);
        double rsi = StockAnalysisService.rsi(closes, StockAnalysisService.RSI_PERIOD)[last];
        if (rsiRule != null) {
            double low = param(rsiRule.getParamLow(), 30);
            double high = param(rsiRule.getParamHigh(), 70);
            int signal = rsi < low ? 1 : rsi > high ? -1 : 0;
            String text = signal > 0
                    ? String.format(Locale.US, "RSI(14) is %.1f, below %.0f: traditionally considered oversold, which some analysts read as room for a rebound.", rsi, low)
                    : signal < 0
                    ? String.format(Locale.US, "RSI(14) is %.1f, above %.0f: traditionally considered overbought, which some analysts read as a risk of a pullback.", rsi, high)
                    : String.format(Locale.US, "RSI(14) is %.1f, within the neutral %.0f-%.0f range.", rsi, low, high);
            score += add(reasons, signals, rsiRule, signal, text);
            maxScore += rsiRule.getWeight().doubleValue();
        }

        // 3. MACD
        RecommendationRule macdRule = active(byKey, RecommendationRule.MACD);
        StockAnalysisService.Macd macd = StockAnalysisService.macd(closes,
                StockAnalysisService.MACD_FAST, StockAnalysisService.MACD_SLOW, StockAnalysisService.MACD_SIGNAL);
        double macdLine = macd.line()[last];
        double signalLine = macd.signal()[last];
        if (macdRule != null) {
            int signal = Double.isNaN(macdLine - signalLine) ? 0 : Double.compare(macdLine, signalLine);
            String text = signal > 0
                    ? String.format(Locale.US, "MACD (%.3f) is above its signal line (%.3f): momentum is turning or staying positive.", macdLine, signalLine)
                    : signal < 0
                    ? String.format(Locale.US, "MACD (%.3f) is below its signal line (%.3f): momentum is turning or staying negative.", macdLine, signalLine)
                    : "MACD is level with its signal line: no momentum signal.";
            score += add(reasons, signals, macdRule, signal, text);
            maxScore += macdRule.getWeight().doubleValue();
        }

        // 4. Price momentum
        RecommendationRule momentumRule = active(byKey, RecommendationRule.MOMENTUM);
        double momentum = StockAnalysisService.percentChange(closes[last - StockAnalysisService.MOMENTUM_PERIOD], closes[last]);
        if (momentumRule != null) {
            double threshold = param(momentumRule.getParamLow(), 3);
            int signal = momentum > threshold ? 1 : momentum < -threshold ? -1 : 0;
            String text = signal != 0
                    ? String.format(Locale.US, "The price %s %.2f%% over the last 20 sessions, beyond the +/-%.1f%% momentum threshold.",
                            momentum > 0 ? "rose" : "fell", Math.abs(momentum), threshold)
                    : String.format(Locale.US, "The price changed %+.2f%% over the last 20 sessions, inside the +/-%.1f%% band: no clear momentum.",
                            momentum, threshold);
            score += add(reasons, signals, momentumRule, signal, text);
            maxScore += momentumRule.getWeight().doubleValue();
        }

        // 5. Volume confirmation
        RecommendationRule volumeRule = active(byKey, RecommendationRule.VOLUME);
        double volumeRatio = StockAnalysisService.volumeRatio(prices, StockAnalysisService.MOMENTUM_PERIOD);
        if (volumeRule != null) {
            double threshold = param(volumeRule.getParamLow(), 1.2);
            if (Double.isNaN(volumeRatio)) {
                reasons.add("Volume data is not available for this series, so the volume rule was skipped.");
            } else {
                int signal = volumeRatio >= threshold ? (int) Math.signum(momentum) : 0;
                String text = signal != 0
                        ? String.format(Locale.US, "The latest volume is %.2fx its 20-session average, confirming the %s move.",
                                volumeRatio, signal > 0 ? "upward" : "downward")
                        : String.format(Locale.US, "The latest volume is %.2fx its 20-session average: no unusual trading activity.", volumeRatio);
                score += add(reasons, signals, volumeRule, signal, text);
                maxScore += volumeRule.getWeight().doubleValue();
            }
        }

        RecommendationRule decision = byKey.get(RecommendationRule.DECISION);
        double buyAt = decision == null ? 25 : param(decision.getParamHigh(), 25);
        double sellAt = decision == null ? -25 : param(decision.getParamLow(), -25);
        String type = score >= buyAt ? Recommendation.BUY : score <= sellAt ? Recommendation.SELL : Recommendation.HOLD;

        if (Recommendation.HOLD.equals(type)) {
            reasons.add("Overall the available indicators are mixed or neutral, so the engine suggests HOLD.");
        }
        reasons.add(String.format(Locale.US, "Score %+.0f out of a possible +/-%.0f (BUY at %+.0f or more, SELL at %+.0f or less).",
                score, maxScore, buyAt, sellAt));

        recommendation.setRecommendationType(type);
        recommendation.setRecommendationScore(BigDecimal.valueOf(score).setScale(2, java.math.RoundingMode.HALF_UP));
        recommendation.setExplanation(String.join("\n", reasons));

        Map<String, Object> supporting = new LinkedHashMap<>();
        supporting.put("bars", prices.size());
        supporting.put("close", round(closes[last]));
        supporting.put("sma20", round(smaShort[last]));
        supporting.put("sma50", round(smaLong[last]));
        supporting.put("rsi14", round(rsi));
        supporting.put("macd", round(macdLine));
        supporting.put("macdSignal", round(signalLine));
        supporting.put("momentum20Pct", round(momentum));
        supporting.put("volumeRatio", Double.isNaN(volumeRatio) ? null : round(volumeRatio));
        supporting.put("maxScore", maxScore);
        supporting.put("signals", signals);
        try {
            recommendation.setSupportingJson(JSON.writeValueAsString(supporting));
        } catch (JsonProcessingException e) {
            recommendation.setSupportingJson(null);   // the explanation text still carries every reason
        }
        return recommendation;
    }

    /** The default rule set, identical to the rows in sample-data.sql. */
    public static List<RecommendationRule> defaultRules() {
        return List.of(
                new RecommendationRule(RecommendationRule.SMA_CROSSOVER, "SMA crossover", 20, 0.5, null),
                new RecommendationRule(RecommendationRule.RSI, "RSI zones", 15, 30.0, 70.0),
                new RecommendationRule(RecommendationRule.MACD, "MACD vs signal", 15, null, null),
                new RecommendationRule(RecommendationRule.MOMENTUM, "20-session momentum", 10, 3.0, null),
                new RecommendationRule(RecommendationRule.VOLUME, "Volume confirmation", 5, 1.2, null),
                new RecommendationRule(RecommendationRule.DECISION, "Decision thresholds", 0, -25.0, 25.0));
    }

    private static RecommendationRule active(Map<String, RecommendationRule> rules, String key) {
        RecommendationRule rule = rules.get(key);
        return rule != null && rule.isEnabled() ? rule : null;
    }

    private static double add(List<String> reasons, List<Map<String, Object>> signals,
                              RecommendationRule rule, int signal, String text) {
        double contribution = signal * rule.getWeight().doubleValue();
        reasons.add(text + (contribution == 0 ? " (0)" : String.format(Locale.US, " (%+.0f)", contribution)));
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("rule", rule.getRuleKey());
        entry.put("signal", signal);
        entry.put("contribution", contribution);
        signals.add(entry);
        return contribution;
    }

    /** " A bullish crossover happened in the last 5 sessions." or "". */
    private static String recentCrossover(double[] fast, double[] slow, int lookback) {
        int last = fast.length - 1;
        for (int i = last; i > last - lookback && i > 0; i--) {
            double now = fast[i] - slow[i];
            double before = fast[i - 1] - slow[i - 1];
            if (Double.isNaN(now) || Double.isNaN(before)) {
                break;
            }
            if (before <= 0 && now > 0) {
                return " A bullish crossover happened in the last " + lookback + " sessions.";
            }
            if (before >= 0 && now < 0) {
                return " A bearish crossover happened in the last " + lookback + " sessions.";
            }
        }
        return "";
    }

    private static double param(BigDecimal value, double defaultValue) {
        return value == null ? defaultValue : value.doubleValue();
    }

    private static String money(double value) {
        return String.format(Locale.US, "%,.2f", value);
    }

    private static Double round(double value) {
        return Double.isNaN(value) ? null : Math.round(value * 10000) / 10000.0;
    }
}
