# Verification report

Environment: macOS, OpenJDK 21.0.13, Maven 3.10, MySQL 26.10 (Connector/J
8.4.0), Apache Tomcat 10.1.61. The WAR was deployed to a separate Tomcat
instance (own `CATALINA_BASE`, port 8090) and driven in a browser.

## Automated

`mvn clean package` — build succeeds, **13 / 13 JUnit tests pass**:

| Test | Checks |
|---|---|
| `smaIsTheMeanOfTheWindow` | SMA values and the undefined warm-up period |
| `emaIsSeededWithSmaAndLagsALinearSeriesByHalfThePeriod` | EMA seed and recursion |
| `rsiMatchesWildersPublishedExample` | Wilder's 14-period example, exact (unrounded) values 70.464 and 66.249 |
| `rsiOfOnlyGainsIs100AndOfOnlyLossesIs0` | RSI limits |
| `macdOfALinearSeriesIsConstantAndEqualsItsSignal` | MACD = 7 for a slope-1 series, signal warm-up at bar 34 |
| `volatilityOfConstantGrowthIsZero` | volatility maths and short-history NaN |
| `calculateLeavesIndicatorsNullWhenHistoryIsTooShort` | no made-up values with 10 bars |
| `calculateFillsEveryIndicatorWithEnoughHistory` | all indicators with 120 bars |
| `acceleratingUptrendIsBuy` | score +35 → BUY, overbought reason present |
| `acceleratingDowntrendIsSell` | score −35 → SELL |
| `sidewaysMarketIsHold` | neutral signals → HOLD |
| `shortHistoryGivesInsufficientDataInsteadOfAGuess` | 59 bars → INSUFFICIENT_DATA |
| `disabledRulesDoNotContribute` | rule switches |

## End-to-end checklist

| # | Requirement | How it was verified | Result |
|---|---|---|---|
| 1 | User registration | Registered a new account in the browser; redirected to the dashboard and logged in | Pass |
| 2 | User authentication | Wrong password → "Invalid email or password."; correct password → dashboard; deactivated account refused | Pass |
| 3 | Administrator authentication | Admin login lands on the admin overview | Pass |
| 4 | Stock listing | 15 stocks with latest close, change, date and SAMPLE badge | Pass |
| 5 | Stock search | `?q=infy` → 1 result; exchange/sector/sort filters | Pass |
| 6 | Stock detail page | Price, change, volume, 52-week range, market cap "Not available", company info, OHLCV table | Pass |
| 7 | Market data retrieval | Provider mode against a local Alpha Vantage stub: 100 bars parsed and stored, source shown as "Alpha Vantage · delayed", preferred over SAMPLE rows | Pass |
| 8 | Historical chart generation | Price + SMA/EMA, volume, RSI, MACD charts; 1Y = 261 bars; ranges without enough data are disabled; 1D disabled | Pass |
| 9 | Technical indicator calculation | Unit tests above; values on the page match the engine's reasons | Pass |
| 10 | Recommendation generation | Explanations with per-rule contributions; HINDUNILVR (30 bars) shows "Insufficient data"; changing a rule weight re-evaluated 15 stocks | Pass |
| 11 | Watchlist addition | Add from details page and explorer | Pass |
| 12 | Watchlist removal | Remove from watchlist page | Pass |
| 13 | Duplicate watchlist prevention | Stale page in a second tab tried to add the same stock → "TCS is already in your watchlist."; DB has UNIQUE(user_id, stock_id) | Pass |
| 14 | Admin stock management | Add (symbol normalised to upper case), edit, deactivate, delete | Pass |
| 15 | User authorization | A USER opening `/admin-users.xhtml` is redirected with "You are not allowed to open that page."; watchlist queries always use the session user | Pass |
| 16 | API error handling | Stub returned a rate-limit `Note` → RATE_LIMITED, `Error Message` → NO_DATA, HTTP 500 → ERROR; each logged in `api_sync_logs`, cached data kept; a second sync within the cache window → "no request made" | Pass |
| 17 | Demo mode | Default config: SAMPLE ribbon on every page, Sync refused with a DEMO log row, no network calls | Pass |
| 18 | MySQL connectivity | `TestConnection` prints MySQL 26.10.0 and the row counts | Pass |

Also checked: every page at 392 px width has no horizontal page scroll
(tables scroll inside their cards); timestamps display in local time.

After testing, the database was reloaded from `schema.sql` +
`sample-data.sql`, so the test account and stub-written rows are gone.

## Not verified

* A real Alpha Vantage key was not used (none was available); the provider
  path was exercised against a local stub that mimics the documented response
  shapes. Run one real sync after configuring a key, especially for `.BSE`
  symbols.
* Eclipse / IntelliJ import was not exercised; the Maven build and the WAR on
  standalone Tomcat were.
