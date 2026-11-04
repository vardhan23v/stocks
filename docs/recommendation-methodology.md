# Recommendation methodology

StockSense produces **BUY / HOLD / SELL** labels with a fixed, documented set of
technical rules. There is no machine learning and no AI service involved: the
same stored prices and the same rule settings always produce the same result,
and every result lists the reasons behind it.

> The output is educational information. It is not personalised financial
> advice, it does not take anyone's goals or risk tolerance into account, and
> technical indicators do not guarantee future prices.

Code: `RecommendationService.evaluate()` (the engine) and
`StockAnalysisService` (the indicator maths). Both are unit tested in
`src/test/java`.

## 1. Input

The engine reads the stored daily bars of one stock from **one** source:

* `ALPHA_VANTAGE` rows when a provider key is configured and the stock has them,
* otherwise the `SAMPLE` demonstration rows.

Sources are never mixed in one calculation.

If fewer than **60 bars** are stored, the result is `INSUFFICIENT_DATA`
("Insufficient data" in the UI) with score 0. 60 bars covers SMA(50) and the
MACD signal line (26 + 9 - 1 = 34 bars) with a margin.

## 2. Indicators

| Indicator | Definition |
|---|---|
| SMA(n) | Mean of the last *n* closes. SMA 20 and SMA 50 are used. |
| EMA(n) | `EMA_t = (close_t - EMA_{t-1}) * 2/(n+1) + EMA_{t-1}`, seeded with the SMA of the first *n* closes. EMA 20 is shown. |
| RSI(14) | Wilder's RSI: first average gain/loss is the simple mean of 14 changes, then `avg = (avg * 13 + current) / 14`. `RSI = 100 - 100 / (1 + avgGain/avgLoss)`. |
| MACD(12, 26, 9) | MACD line = EMA 12 - EMA 26; signal = EMA 9 of the MACD line; histogram = MACD - signal. |
| 20-session momentum | `(close_t / close_{t-20} - 1) * 100` |
| Volume ratio | Newest volume / mean volume of the 20 sessions before it. |
| Historical volatility | Sample standard deviation of daily log returns over the last 60 sessions, x sqrt(252), in percent. Not used for scoring; shown for context. |

## 3. Rules and default weights

Each enabled rule gives a signal of **+1** (bullish), **-1** (bearish) or
**0** (neutral). Its contribution is `signal x weight`.

| Rule key | Bullish (+1) | Bearish (-1) | Neutral (0) | Default weight |
|---|---|---|---|---|
| `SMA_CROSSOVER` | SMA20 above SMA50 by more than `param_low` % (0.5) | SMA20 below SMA50 by more than 0.5 % | within the band | 20 |
| `RSI` | RSI below `param_low` (30, "oversold") | RSI above `param_high` (70, "overbought") | 30 - 70 | 15 |
| `MACD` | MACD above its signal line | MACD below its signal line | equal | 15 |
| `MOMENTUM` | 20-session change above +`param_low` % (3) | below -3 % | inside the band | 10 |
| `VOLUME` | volume ratio >= `param_low` (1.2) **and** momentum positive | ratio >= 1.2 and momentum negative | normal volume | 5 |

The RSI rule is a *contrarian* reading (oversold counts as bullish); the other
rules follow the trend. That is why a strong, steady rally often scores
"SMA +20, RSI -15": the trend is up but the move looks stretched.

When the provider supplies no volume, the volume rule is skipped and does not
count towards the maximum score.

## 4. Decision

```
score     = sum(signal_i x weight_i)           over enabled rules
max score = sum(weight_i)                       (65 with the defaults)

score >= DECISION.param_high (+25)  -> BUY
score <= DECISION.param_low  (-25)  -> SELL
otherwise                           -> HOLD
```

## 5. Explanation format

Every rule writes one sentence followed by its contribution in brackets, e.g.

```
SMA(20) 2,097.21 is 8.47% above SMA(50) 1,933.51: the short-term trend is stronger than the long-term trend. (+20)
RSI(14) is 65.2, within the neutral 30-70 range. (0)
MACD (81.126) is above its signal line (79.337): momentum is turning or staying positive. (+15)
The price rose 13.60% over the last 20 sessions, beyond the +/-3.0% momentum threshold. (+10)
The latest volume is 1.29x its 20-session average, confirming the upward move. (+5)
Score +50 out of a possible +/-65 (BUY at +25 or more, SELL at -25 or less).
```

HOLD results add: *"Overall the available indicators are mixed or neutral, so
the engine suggests HOLD."*

The full indicator values and per-rule contributions are also stored as JSON
in `recommendations.supporting_json`.

## 6. When a recommendation is (re)generated

* Opening a stock page or the recommendations page: a new row is written only
  if the newest stored bar (or its source) differs from the last stored
  recommendation. Repeated page views never create duplicates.
* Admin > Rules > **Save rules and re-evaluate**: rules are saved in one
  transaction, then every active stock is re-evaluated.
* Admin > Rules > **Re-evaluate all stocks**.

Each row stores `generated_at` (when) and `data_timestamp` + `data_source`
(which data), which the UI shows next to the result.
