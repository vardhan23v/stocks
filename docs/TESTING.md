# Verification report

## Spring Boot version (current)

Environment: OpenJDK 21.0.13, Maven 3.10, MySQL 26.10, Spring Boot 3.3.5
(embedded Tomcat), run as `java -jar` on port 8090 and driven in a browser.

`mvn clean package`: **19 / 19 tests pass** — the 13 engine/indicator tests
below, 2 password-encoder tests (including the stored demo admin hash) and 4
MockMvc security tests (anonymous → login; USER blocked from `/admin/**` for
GET and POST; POST without CSRF token → 403).

End-to-end, all passed: registration with auto-login; wrong password and
deactivated account refused (`/login?error`, `/login?disabled`); admin login
with a PBKDF2 hash created before the rewrite; search; INFY details (same
indicator values, BUY with 6 reasons, 4 charts, 261 bars for 1Y); watchlist
add / duplicate refused / remove; `returnTo` cannot redirect off-site; USER
opening `/admin/users` → dashboard with "You are not allowed to open that
page."; logout; stock add (symbol normalised) / duplicate symbol refused /
edit / deactivate / delete; user deactivate, own-account delete refused; rule
weight 150 refused, weight 25 saved and 15 stocks re-evaluated (TATAMOTORS
−45 → −50), default restored; demo-mode sync refused and logged; provider mode
against a local Alpha Vantage stub: 100 bars stored, RATE_LIMITED / NO_DATA /
HTTP 500 handled with cached data kept, second sync served from cache, AAPL
then uses provider data with 6M/1Y disabled; no horizontal scrolling at 293 px.

One bug was found and fixed during this pass: the recommendation filter tabs
showed "null" counts (SpEL `map[var]` looks up the literal key).

## JSF version (tag `jsf-version`)


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
