package com.stockrecommendation.service;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.stockrecommendation.model.StockIndicator;
import com.stockrecommendation.model.StockPrice;

/**
 * Technical indicators calculated from stored price history.
 *
 * All methods are pure functions: they only read the arrays they are given.
 * Every series method returns an array of the same length as its input, with
 * Double.NaN where the indicator is not yet defined (for example the first 19
 * values of a 20-day SMA). Nothing here is ever filled with made-up values: a
 * stock with too little history simply gets null indicators.
 */
@Service
public class StockAnalysisService {


    public static final int SMA_SHORT = 20;
    public static final int SMA_LONG = 50;
    public static final int EMA_PERIOD = 20;
    public static final int RSI_PERIOD = 14;
    public static final int MACD_FAST = 12;
    public static final int MACD_SLOW = 26;
    public static final int MACD_SIGNAL = 9;
    public static final int MOMENTUM_PERIOD = 20;
    public static final int VOLATILITY_WINDOW = 60;
    public static final int TRADING_DAYS_PER_YEAR = 252;

    /** Simple moving average: the plain mean of the last {@code period} values. */
    public static double[] sma(double[] values, int period) {
        double[] result = nanArray(values.length);
        double sum = 0;
        for (int i = 0; i < values.length; i++) {
            sum += values[i];
            if (i >= period) {
                sum -= values[i - period];
            }
            if (i >= period - 1) {
                result[i] = sum / period;
            }
        }
        return result;
    }

    /**
     * Exponential moving average with multiplier 2 / (period + 1), seeded with
     * the SMA of the first {@code period} defined values. Leading NaN values in
     * the input (as in the MACD line) are skipped.
     */
    public static double[] ema(double[] values, int period) {
        double[] result = nanArray(values.length);
        int start = 0;
        while (start < values.length && Double.isNaN(values[start])) {
            start++;
        }
        if (values.length - start < period) {
            return result;
        }
        double seed = 0;
        for (int i = start; i < start + period; i++) {
            seed += values[i];
        }
        int seedIndex = start + period - 1;
        result[seedIndex] = seed / period;
        double multiplier = 2.0 / (period + 1);
        for (int i = seedIndex + 1; i < values.length; i++) {
            result[i] = (values[i] - result[i - 1]) * multiplier + result[i - 1];
        }
        return result;
    }

    /**
     * Relative Strength Index with Wilder's smoothing:
     * RSI = 100 - 100 / (1 + averageGain / averageLoss).
     */
    public static double[] rsi(double[] closes, int period) {
        double[] result = nanArray(closes.length);
        if (closes.length <= period) {
            return result;
        }
        double gain = 0;
        double loss = 0;
        for (int i = 1; i <= period; i++) {
            double change = closes[i] - closes[i - 1];
            gain += Math.max(change, 0);
            loss += Math.max(-change, 0);
        }
        double averageGain = gain / period;
        double averageLoss = loss / period;
        result[period] = rsiValue(averageGain, averageLoss);
        for (int i = period + 1; i < closes.length; i++) {
            double change = closes[i] - closes[i - 1];
            averageGain = (averageGain * (period - 1) + Math.max(change, 0)) / period;
            averageLoss = (averageLoss * (period - 1) + Math.max(-change, 0)) / period;
            result[i] = rsiValue(averageGain, averageLoss);
        }
        return result;
    }

    private static double rsiValue(double averageGain, double averageLoss) {
        if (averageLoss == 0) {
            return averageGain == 0 ? 50 : 100;
        }
        return 100 - 100 / (1 + averageGain / averageLoss);
    }

    /** MACD line, signal line and histogram. */
    public static Macd macd(double[] closes, int fast, int slow, int signal) {
        double[] fastEma = ema(closes, fast);
        double[] slowEma = ema(closes, slow);
        double[] line = nanArray(closes.length);
        for (int i = 0; i < closes.length; i++) {
            line[i] = fastEma[i] - slowEma[i];   // NaN until both are defined
        }
        double[] signalLine = ema(line, signal);
        double[] histogram = nanArray(closes.length);
        for (int i = 0; i < closes.length; i++) {
            histogram[i] = line[i] - signalLine[i];
        }
        return new Macd(line, signalLine, histogram);
    }

    /** The three MACD series, all as long as the input. */
    public record Macd(double[] line, double[] signal, double[] histogram) {
    }

    /**
     * Annualised historical volatility in percent: the sample standard
     * deviation of daily log returns over the last {@code window} returns,
     * multiplied by the square root of 252 trading days.
     *
     * @return NaN when fewer than {@code window} returns are available
     */
    public static double historicalVolatility(double[] closes, int window) {
        if (closes.length < window + 1) {
            return Double.NaN;
        }
        double[] returns = new double[window];
        for (int i = 0; i < window; i++) {
            int index = closes.length - window + i;
            returns[i] = Math.log(closes[index] / closes[index - 1]);
        }
        double mean = Arrays.stream(returns).average().orElse(0);
        double squares = 0;
        for (double value : returns) {
            squares += (value - mean) * (value - mean);
        }
        double deviation = Math.sqrt(squares / (window - 1));
        return deviation * Math.sqrt(TRADING_DAYS_PER_YEAR) * 100;
    }

    /** Percentage change from {@code from} to {@code to}, or NaN when from is 0. */
    public static double percentChange(double from, double to) {
        return from == 0 ? Double.NaN : (to - from) / from * 100;
    }

    /**
     * Latest value of every indicator for one price series (oldest bar first).
     * Values that cannot be calculated from the available history stay null.
     */
    public StockIndicator calculate(int stockId, List<StockPrice> prices) {
        StockIndicator indicator = new StockIndicator();
        indicator.setStockId(stockId);
        indicator.setBarCount(prices.size());
        if (prices.isEmpty()) {
            return indicator;
        }
        StockPrice newest = prices.get(prices.size() - 1);
        indicator.setDataSource(newest.getDataSource());
        indicator.setDataTimestamp(newest.getRecordedAt());

        double[] closes = closes(prices);
        int last = closes.length - 1;

        indicator.setSma(decimal(sma(closes, SMA_SHORT)[last], 4));
        indicator.setSmaLong(decimal(sma(closes, SMA_LONG)[last], 4));
        indicator.setEma(decimal(ema(closes, EMA_PERIOD)[last], 4));
        indicator.setRsi(decimal(rsi(closes, RSI_PERIOD)[last], 3));

        Macd macd = macd(closes, MACD_FAST, MACD_SLOW, MACD_SIGNAL);
        indicator.setMacd(decimal(macd.line()[last], 4));
        indicator.setSignalLine(decimal(macd.signal()[last], 4));

        // Volatility over the last 60 returns, or fewer when the history is
        // shorter - but never from fewer than 20, which would be meaningless.
        int window = Math.min(VOLATILITY_WINDOW, closes.length - 1);
        if (window >= SMA_SHORT) {
            indicator.setVolatility(decimal(historicalVolatility(closes, window), 3));
        }
        if (closes.length > MOMENTUM_PERIOD) {
            indicator.setMomentum(decimal(percentChange(closes[last - MOMENTUM_PERIOD], closes[last]), 3));
        }
        indicator.setVolumeRatio(decimal(volumeRatio(prices, MOMENTUM_PERIOD), 2));
        return indicator;
    }

    /**
     * Newest volume divided by the average of the {@code period} volumes before
     * it. NaN when volume is missing anywhere in that window.
     */
    public static double volumeRatio(List<StockPrice> prices, int period) {
        if (prices.size() < period + 1) {
            return Double.NaN;
        }
        Long newest = prices.get(prices.size() - 1).getVolume();
        if (newest == null) {
            return Double.NaN;
        }
        double sum = 0;
        for (int i = prices.size() - 1 - period; i < prices.size() - 1; i++) {
            Long volume = prices.get(i).getVolume();
            if (volume == null) {
                return Double.NaN;
            }
            sum += volume;
        }
        return sum == 0 ? Double.NaN : newest / (sum / period);
    }

    /**
     * Series for the charts on the stock details page.
     *
     * The indicators are calculated over the FULL stored history and then cut
     * to the bars from {@code fromIndex} on, so a 1-month chart still shows a
     * correct SMA(50) from its first day. NaN becomes null (a gap in the chart).
     */
    public Map<String, Object> buildChartData(List<StockPrice> prices, int fromIndex) {
        double[] closes = closes(prices);
        double[] sma20 = sma(closes, SMA_SHORT);
        double[] sma50 = sma(closes, SMA_LONG);
        double[] ema20 = ema(closes, EMA_PERIOD);
        double[] rsi14 = rsi(closes, RSI_PERIOD);
        Macd macd = macd(closes, MACD_FAST, MACD_SLOW, MACD_SIGNAL);

        int size = prices.size() - fromIndex;
        List<String> labels = new ArrayList<>(size);
        List<Long> volume = new ArrayList<>(size);
        for (int i = fromIndex; i < prices.size(); i++) {
            labels.add(prices.get(i).getRecordedAt().toLocalDate().toString());
            volume.add(prices.get(i).getVolume());
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("labels", labels);
        data.put("close", slice(closes, fromIndex));
        data.put("sma20", slice(sma20, fromIndex));
        data.put("sma50", slice(sma50, fromIndex));
        data.put("ema20", slice(ema20, fromIndex));
        data.put("volume", volume);
        data.put("rsi", slice(rsi14, fromIndex));
        data.put("macd", slice(macd.line(), fromIndex));
        data.put("signal", slice(macd.signal(), fromIndex));
        data.put("histogram", slice(macd.histogram(), fromIndex));
        return data;
    }

    private static List<Double> slice(double[] values, int fromIndex) {
        List<Double> list = new ArrayList<>(values.length - fromIndex);
        for (int i = fromIndex; i < values.length; i++) {
            list.add(Double.isNaN(values[i]) ? null : Math.round(values[i] * 10000) / 10000.0);
        }
        return list;
    }

    public static double[] closes(List<StockPrice> prices) {
        double[] closes = new double[prices.size()];
        for (int i = 0; i < closes.length; i++) {
            closes[i] = prices.get(i).getClosePrice().doubleValue();
        }
        return closes;
    }

    /** NaN or infinity -> null, anything else -> BigDecimal with the given scale. */
    public static BigDecimal decimal(double value, int scale) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return null;
        }
        return BigDecimal.valueOf(value).setScale(scale, RoundingMode.HALF_UP);
    }

    private static double[] nanArray(int length) {
        double[] array = new double[length];
        Arrays.fill(array, Double.NaN);
        return array;
    }
}
