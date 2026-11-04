package com.stockrecommendation.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.stockrecommendation.model.StockIndicator;
import com.stockrecommendation.model.StockPrice;

/** Indicator maths checked against hand-computed and published reference values. */
class StockAnalysisServiceTest {

    private static final double EPS = 1e-9;

    @Test
    void smaIsTheMeanOfTheWindow() {
        double[] sma = StockAnalysisService.sma(new double[] {1, 2, 3, 4, 5}, 3);
        assertTrue(Double.isNaN(sma[0]) && Double.isNaN(sma[1]));
        assertEquals(2.0, sma[2], EPS);
        assertEquals(3.0, sma[3], EPS);
        assertEquals(4.0, sma[4], EPS);
    }

    @Test
    void emaIsSeededWithSmaAndLagsALinearSeriesByHalfThePeriod() {
        double[] values = new double[10];
        for (int i = 0; i < values.length; i++) {
            values[i] = i + 1;
        }
        double[] ema = StockAnalysisService.ema(values, 3);
        assertTrue(Double.isNaN(ema[1]));
        assertEquals(2.0, ema[2], EPS);          // SMA of 1, 2, 3
        assertEquals(3.0, ema[3], EPS);          // (4 - 2) * 0.5 + 2
        assertEquals(9.0, ema[9], EPS);          // always value - (period - 1) / 2
    }

    @Test
    void rsiMatchesWildersPublishedExample() {
        // The classic 14-period example (Wilder / StockCharts). The published
        // table rounds the averages to 2 decimals and shows 70.53 / 66.32; the
        // exact values are: gains 3.34 / 14, losses 1.40 / 14 -> RS 2.3857 -> 70.464,
        // then the -0.28 day -> RS 0.22153 / 0.11286 -> 66.249.
        double[] closes = {44.34, 44.09, 44.15, 43.61, 44.33, 44.83, 45.10, 45.42, 45.84, 46.08,
                           45.89, 46.03, 45.61, 46.28, 46.28, 46.00};
        double[] rsi = StockAnalysisService.rsi(closes, 14);
        assertTrue(Double.isNaN(rsi[13]));
        assertEquals(70.464, rsi[14], 0.001);
        assertEquals(66.249, rsi[15], 0.001);
    }

    @Test
    void rsiOfOnlyGainsIs100AndOfOnlyLossesIs0() {
        double[] up = new double[20];
        double[] down = new double[20];
        for (int i = 0; i < 20; i++) {
            up[i] = 10 + i;
            down[i] = 50 - i;
        }
        assertEquals(100.0, StockAnalysisService.rsi(up, 14)[19], EPS);
        assertEquals(0.0, StockAnalysisService.rsi(down, 14)[19], EPS);
    }

    @Test
    void macdOfALinearSeriesIsConstantAndEqualsItsSignal() {
        double[] closes = new double[80];
        for (int i = 0; i < closes.length; i++) {
            closes[i] = 100 + i;
        }
        StockAnalysisService.Macd macd = StockAnalysisService.macd(closes, 12, 26, 9);
        assertTrue(Double.isNaN(macd.line()[24]));
        assertEquals(7.0, macd.line()[79], 1e-6);       // lag 12.5 - lag 5.5
        assertEquals(7.0, macd.signal()[79], 1e-6);
        assertEquals(0.0, macd.histogram()[79], 1e-6);
        assertTrue(Double.isNaN(macd.signal()[32]));    // signal needs 9 MACD values
        assertTrue(!Double.isNaN(macd.signal()[33]));
    }

    @Test
    void volatilityOfConstantGrowthIsZero() {
        double[] closes = new double[30];
        closes[0] = 100;
        for (int i = 1; i < closes.length; i++) {
            closes[i] = closes[i - 1] * 1.01;
        }
        assertEquals(0.0, StockAnalysisService.historicalVolatility(closes, 20), 1e-9);
        assertTrue(Double.isNaN(StockAnalysisService.historicalVolatility(closes, 40)));
    }

    @Test
    void calculateLeavesIndicatorsNullWhenHistoryIsTooShort() {
        StockIndicator indicator = new StockAnalysisService().calculate(1, series(10, i -> 100.0 + i));
        assertEquals(10, indicator.getBarCount());
        assertNull(indicator.getSma());
        assertNull(indicator.getSmaLong());
        assertNull(indicator.getRsi());
        assertNull(indicator.getMacd());
        assertNull(indicator.getVolatility());
    }

    @Test
    void calculateFillsEveryIndicatorWithEnoughHistory() {
        StockIndicator indicator = new StockAnalysisService().calculate(1, series(120, i -> 100 + 5 * Math.sin(i / 4.0)));
        assertTrue(indicator.getSma() != null && indicator.getSmaLong() != null && indicator.getEma() != null);
        assertTrue(indicator.getRsi() != null && indicator.getMacd() != null && indicator.getSignalLine() != null);
        assertTrue(indicator.getVolatility() != null && indicator.getMomentum() != null);
        assertEquals("SAMPLE", indicator.getDataSource());
    }

    static List<StockPrice> series(int count, java.util.function.IntToDoubleFunction close) {
        List<StockPrice> prices = new ArrayList<>();
        LocalDateTime day = LocalDateTime.of(2026, 1, 1, 0, 0);
        for (int i = 0; i < count; i++) {
            BigDecimal value = BigDecimal.valueOf(close.applyAsDouble(i));
            long volume = i == count - 1 ? 2000 : 1000;
            prices.add(new StockPrice(day.plusDays(i), value, value, value, value, volume, "SAMPLE"));
        }
        return prices;
    }
}
