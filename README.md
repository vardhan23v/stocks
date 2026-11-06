# StockSense — Stocks Recommendations System

A Java web application for exploring stocks, studying their price history and
technical indicators, getting **explainable rule-based BUY / HOLD / SELL
recommendations**, and keeping a private watchlist. Administrators manage
users, stock records, the scoring rules and the market data integration.

**Built with Spring Boot 3** — Spring MVC with **JSP + JSTL views**, Spring
Security and JdbcTemplate — on MySQL.

**Live demo:** <https://web-production-4861b6.up.railway.app> (sample data;
demo logins below).

> **Educational and informational purposes only.** Recommendations come from
> fixed technical rules, not personalised financial advice, and they do not
> guarantee any return. Without an API key the app runs on clearly labelled
> **synthetic SAMPLE DATA**, never presented as real prices.

> **History:** versions up to the git tag `jsf-version` were built with JSF
> (Jakarta Faces 4.0) + CDI + JDBC on Tomcat 10.1. The application was then
> rewritten on Spring Boot with the same features, database and design, first
> with Thymeleaf views and then with the JSP views it uses now.

![Architecture](docs/architecture.svg)

---

## Contents

1. [Features](#features)
2. [Technology stack](#technology-stack)
3. [Where Spring is used](#where-spring-is-used)
4. [Architecture](#architecture)
5. [Project structure](#project-structure)
6. [Database setup](#database-setup)
7. [Market data provider and API key](#market-data-provider-and-api-key)
8. [Demo mode](#demo-mode)
9. [Build and run](#build-and-run)
10. [Deploy on Railway](#deploy-on-railway)
11. [Eclipse / IntelliJ setup](#eclipse--intellij-setup)
12. [Demo credentials](#demo-credentials)
13. [Recommendation engine](#recommendation-engine)
14. [Testing](#testing)
15. [Troubleshooting](#troubleshooting)

---

## Features

### Regular user
* Register (PBKDF2-hashed password, logged in straight away), log in, log out.
* **Dashboard**: market overview (tracked stocks, latest data date, provider
  status), stock search, 3-month performance chart of the watchlist,
  watchlist preview, top gainers / decliners, latest recommendations,
  recently viewed stocks.
* **Stock explorer** (`/stocks`): search by ticker or company, filter by
  exchange and sector, sort by ticker, name, price or movement. Bookmarkable URLs.
* **Stock details** (`/stocks/{id}?range=3M`): latest stored close with change,
  data date and source, volume, range change, 52-week high/low, market cap
  (only when a provider reports it), company info.
  * Charts: close with SMA 20 / SMA 50 / EMA 20, volume, RSI(14) with 30/70
    bands, MACD(12,26,9) with signal and histogram.
  * Ranges 1W / 1M / 3M / 6M / 1Y, offered only when the stored history covers
    them; 1D is disabled (end-of-day data only).
  * Indicator cards with plain-language explanations, the recommendation with
    every reason, and the daily OHLCV table.
* **Recommendations** (`/recommendations?type=BUY`): all stocks, filter by
  BUY / HOLD / SELL / Insufficient, methodology and current rule weights.
* **Watchlist** (`/watchlist`): add / remove, duplicates prevented, sort by
  gain / fall / ticker / date added. Private: the user always comes from the
  logged-in principal, never from the request.
* **Profile**: account details, change name, change password.

### Administrator (`/admin/**`)
* **Overview**: users, active stocks, watchlist entries, recommendations,
  latest market data date, API status and calls today, activity log, recent syncs.
* **Users**: activate / deactivate, make admin / user, delete (not your own account).
* **Stocks**: add, edit, activate / deactivate, delete.
* **Rules**: edit weights and thresholds (validated, saved in one transaction,
  then every stock is re-evaluated); view and delete generated recommendations.
* **Market data**: provider configuration (key shown only as configured / not),
  stored rows per source, per-stock **Sync** / **Force**, **Sync all stale
  stocks** on a background thread, and the full `api_sync_logs` history.

---

## Technology stack

| Layer | Technology |
|---|---|
| Language | Java 21 |
| Framework | **Spring Boot 3.3.5** |
| Web | **Spring MVC** controllers, embedded Tomcat 10.1 |
| Views | **JSP** pages + **JSTL** (`c:`, `fn:`), JSP tag files for the layout and widgets, Spring Security taglib (`sec:authorize`, `sec:csrfInput`), Spring form taglib (`form:form`); compiled by Tomcat Jasper. HTML5, CSS3, vanilla JS, Bootstrap 5.3.3, Chart.js 4.4.1 |
| Security | **Spring Security** — form login, roles, CSRF protection, session fixation protection |
| Data access | **Spring JDBC** (`JdbcTemplate`, `RowMapper`, `@Transactional`), HikariCP pool, MySQL Connector/J 8.4.0 |
| Database | MySQL 8 or newer (verified on MySQL 26.10) |
| JSON | Jackson (API responses, chart data) |
| HTTP client | `java.net.http.HttpClient` |
| Build | Maven 3.8+, **WAR** (`spring-boot-maven-plugin`): runs with `java -jar` or deploys to an external Tomcat 10.1 |
| Tests | JUnit 5, Spring Boot Test, MockMvc, Spring Security Test |

No JPA/Hibernate: the SQL is written by hand and run with `JdbcTemplate`.

---

## Where Spring is used

| Spring part | Where in this project |
|---|---|
| Spring Boot | `StockSenseApplication` (`@SpringBootApplication`), `pom.xml` parent `spring-boot-starter-parent` |
| Configuration | `application.properties` with `${ENV_VAR:default}` placeholders; `@ConfigurationProperties` in `config/MarketProperties` |
| Dependency injection | `@Service`, `@Repository`, `@Controller`, `@Component`; constructor injection everywhere |
| Spring MVC | `web/*Controller`, `web/admin/*Controller`: `@GetMapping`, `@PostMapping`, `@PathVariable`, `@RequestParam`, `@ModelAttribute`, `RedirectAttributes` flash messages (post-redirect-get) |
| `@ControllerAdvice` | `web/GlobalModelAdvice`: current user, current path, provider status for every page; friendly page for `DataAccessException` |
| JSP views | `config/WebConfig` (`InternalResourceViewResolver` + `JstlView`, exposes the `fmt` formatter bean), `src/main/webapp/WEB-INF/jsp/**` (14 pages), `WEB-INF/tags/*.tag` (layout and widgets), `form:form` data binding on the admin stock and rules forms |
| Spring Security | `config/SecurityConfig` (URL rules, form login, logout, CSRF, access-denied handling), `security/AppUserDetailsService`, `security/StockSensePasswordEncoder`, `@AuthenticationPrincipal AppUser` |
| Spring JDBC | `dao/*DAO` on `JdbcTemplate` with `RowMapper`s, `GeneratedKeyHolder`, `batchUpdate`, `DuplicateKeyException` |
| Transactions | `@Transactional` on the price upsert and the rules update |
| Task executor | `config/AsyncConfig` — `ThreadPoolTaskExecutor` for background market data syncs |
| Startup hook | `init/DatabaseInitializer` (`ApplicationRunner` + `ScriptUtils`) |
| Testing | `@WebMvcTest` + `MockMvc` + `@MockBean` in `SecurityRulesTest` |

---

## Architecture

```
Browser ─► Spring Security filter chain (login, roles, CSRF)
        ─► Spring MVC @Controller ─► JSP view (/WEB-INF/jsp/*.jsp)
        ─► @Service (business rules, 2nd authorisation check)
        ─► @Repository DAO (JdbcTemplate) ─► MySQL

MarketDataService ─► MarketDataClient (HttpClient + Jackson) ─► Alpha Vantage
                  ─► StockPriceDAO (cache in MySQL)          (only with an API key)
```

* URL security in `SecurityConfig`: `/`, `/login`, `/register` and static files
  are public; `/admin/**` needs role ADMIN; everything else needs a login.
  A USER opening an admin URL is sent back to the dashboard with a message;
  a request without a valid CSRF token gets 403.
* Services check the admin role again (`AdminService.requireAdmin`) before
  changing data, and watchlist queries always use the logged-in user's id.
* Passwords: PBKDF2-SHA256, 210 000 iterations, random salt per user, wrapped
  as a Spring `PasswordEncoder` — hashes created before the Spring rewrite
  still work.

Database diagram: [docs/er-diagram.svg](docs/er-diagram.svg)

![ER diagram](docs/er-diagram.svg)

---

## Project structure

```
pom.xml                         Spring Boot parent + starters
Dockerfile                      Maven build -> Java 21 JRE, java -jar app.war
README.md
docs/
  architecture.svg  er-diagram.svg  recommendation-methodology.md  TESTING.md
tools/
  SampleDataGenerator.java      dev tool that writes sample-data.sql
src/main/java/com/stockrecommendation/
  StockSenseApplication.java
  config/    SecurityConfig, WebConfig, MarketProperties, AsyncConfig
  security/  AppUser, AppUserDetailsService, StockSensePasswordEncoder
  init/      DatabaseInitializer
  web/       HomeController, AuthController, DashboardController, StockController,
             WatchlistController, RecommendationController, ProfileController,
             GlobalModelAdvice, Formats
  web/admin/ AdminDashboardController, AdminUserController, AdminStockController,
             AdminRulesController, AdminMarketDataController
  service/   AuthService, StockService, StockDetailsService (+ StockDetailsView),
             DashboardService, MarketDataService, StockAnalysisService,
             RecommendationService, WatchlistService, AdminService
  dao/       UserDAO, StockDAO, StockPriceDAO, IndicatorDAO, RecommendationDAO,
             WatchlistDAO, RuleDAO, SyncLogDAO, ActivityDAO
  api/       MarketDataClient, MarketDataResponse, MarketDataException
  model/     User, Stock, StockPrice, StockIndicator, Recommendation, Watchlist,
             RecommendationRule, ApiSyncLog, ActivityLog
  util/      PasswordUtil, JdbcUtil
src/main/resources/
  application.properties
  schema.sql  sample-data.sql
  static/     css/style.css  js/charts.js  img/favicon.svg
src/main/webapp/WEB-INF/
  jsp/        index, login, register, dashboard, stocks, stock-details,
              recommendations, watchlist, profile, error, db-error (.jsp),
              admin/{dashboard,users,stocks,rules,market-data}.jsp,
              common/taglibs.jspf (taglib declarations, included by every page)
  tags/       page.tag (layout: head, menu, footer), messages, priceChange,
              dataBadge, recBadge, scoreBar, recCard (.tag)
src/test/java/com/stockrecommendation/
  service/   StockAnalysisServiceTest, RecommendationServiceTest
  security/  StockSensePasswordEncoderTest
  web/       SecurityRulesTest
```

---

## Database setup

Requires MySQL 8 or newer (window functions are used).

```bash
mysql -u root -p < src/main/resources/schema.sql
```

```bash
mysql -u root -p < src/main/resources/sample-data.sql
```

(or run both files in MySQL Workbench). `schema.sql` creates
`stock_recommendation_db` with `CREATE ... IF NOT EXISTS` and never drops
anything; `sample-data.sql` is idempotent and safe to run again.

**Or let the app do it:** start it once with `DB_INIT=true` and an empty
database — `DatabaseInitializer` runs both scripts only when the `users`
table does not exist yet.

Connection settings (defaults in `application.properties`, override with
environment variables — no password in any file):

| Variable | Default |
|---|---|
| `DB_URL` | `jdbc:mysql://localhost:3306/stock_recommendation_db?sslMode=DISABLED&allowPublicKeyRetrieval=true&createDatabaseIfNotExist=true` |
| `DB_USER` | `root` |
| `DB_PASSWORD` | *(empty)* |

Tables: `users`, `stocks`, `stock_prices`, `stock_indicators`,
`recommendations`, `watchlists`, `api_sync_logs`, `recommendation_rules`,
`stock_views`, `activity_logs`. Prices use `DECIMAL(19,4)`. Key constraints:
`UNIQUE(exchange, symbol)`, `UNIQUE(stock_id, recorded_at, data_source)`,
`UNIQUE(user_id, stock_id)` on watchlists.

---

## Market data provider and API key

**Provider: [Alpha Vantage](https://www.alphavantage.co/documentation/)** —
its free key covers US listings and Indian listings on BSE (`RELIANCE.BSE`).

Free-tier facts the app is built around (limits change; check alphavantage.co):
about **25 requests per day** and 5 per minute; `TIME_SERIES_DAILY` compact =
the latest **100 daily bars**; delayed **end-of-day** data, never called "live".

`MarketDataService` behaviour:

* **Cache first**: no download while the stored provider rows are younger than
  `market.cache-minutes` (720).
* Calls spaced by `market.min-interval-ms` (13 s); stops at
  `market.daily-quota` (25) calls per day, counted from `api_sync_logs`.
* Downloads stored in one transaction (`INSERT … ON DUPLICATE KEY UPDATE`,
  `data_source = 'ALPHA_VANTAGE'`), preferred over SAMPLE rows, never mixed.
* Errors logged and cached data kept: `Note`/`Information` → `RATE_LIMITED`,
  `Error Message` → `NO_DATA` / `ERROR`, HTTP failures → `ERROR`. The key is
  scrubbed from any provider message.

### Setting a key

1. Get a free key at <https://www.alphavantage.co/support/#api-key>.
2. Set two environment variables (never commit a key):

   ```bash
   export MARKET_PROVIDER=ALPHA_VANTAGE
   ```

   ```bash
   export ALPHA_VANTAGE_API_KEY=your-key-here
   ```

3. Restart, log in as admin, open **Market data** and click **Sync**.

Indian symbols use the `.BSE` suffix. Tata Motors was demerged in 2025 and its
ticker changed; if `TATAMOTORS.BSE` returns `NO_DATA`, fix the provider symbol
in **Admin > Stocks**.

---

## Demo mode

With `MARKET_PROVIDER=DEMO` (default) or no API key the app never calls the
internet and uses only the `data_source = 'SAMPLE'` rows from
`sample-data.sql`: 265 synthetic trading days per stock (one stock has 30, to
show "Insufficient data"). An amber **SAMPLE DATA mode** ribbon and "Sample
data" badges are shown everywhere; market cap stays "Not available".

Regenerate the sample file (after `mvn package`):

```bash
java -cp "target/classes:$(mvn -q dependency:build-classpath -Dmdep.outputFile=/dev/stdout)" tools/SampleDataGenerator.java > src/main/resources/sample-data.sql
```

---

## Build and run

Requires JDK 21 and Maven 3.8+ (macOS/Homebrew: `export JAVA_HOME=$(brew --prefix openjdk@21)`).

```bash
mvn clean package
```

runs the 20 tests and builds `target/stock-recommendation.war`. Then either:

```bash
java -jar target/stock-recommendation.war
```

```bash
mvn spring-boot:run
```

and open <http://localhost:8080/>. Use `PORT=8090` to change the port.

The same WAR also deploys to a standalone **Apache Tomcat 10.1**: copy it to
`webapps/` and open <http://localhost:8080/stock-recommendation/> (the database
settings then come from environment variables of the Tomcat process).

---

## Deploy on Railway

The `Dockerfile` builds the WAR (running the tests) and runs it on a Java 21
JRE; Spring Boot listens on Railway's `$PORT`.

```bash
railway login
```

```bash
railway init --name stock-recommendation
```

```bash
railway add --database mysql
```

```bash
railway add --service web
```

Web service variables (they reference the MySQL service, so no password is copied):

```bash
railway variable set --service web 'DB_URL=jdbc:mysql://${{MySQL.MYSQLHOST}}:${{MySQL.MYSQLPORT}}/stock_recommendation_db?sslMode=DISABLED&allowPublicKeyRetrieval=true&createDatabaseIfNotExist=true' 'DB_USER=${{MySQL.MYSQLUSER}}' 'DB_PASSWORD=${{MySQL.MYSQLPASSWORD}}' 'DB_INIT=true' 'MARKET_PROVIDER=DEMO'
```

```bash
railway up --service web --detach
```

```bash
railway domain --service web
```

Railway's MySQL is only reachable privately, so `DB_INIT=true` lets the app
create the schema and sample data itself on first start; it never touches a
database that already has tables. Rename the `*.up.railway.app` address in the
Railway dashboard → **web** → **Settings → Networking**.

---

## Eclipse / IntelliJ setup

**IntelliJ IDEA**: *File > Open* → `pom.xml` → *Open as Project*, SDK 21. Run
`StockSenseApplication` (Community edition works — no external Tomcat needed).
Set the run configuration's working directory to the project folder (so the JSPs
in `src/main/webapp` are found) and tick *Add dependencies with "provided" scope
to classpath* (the JSP compiler is a provided dependency).

**Eclipse** (with Spring Tools 4 recommended): *File > Import > Maven >
Existing Maven Projects*, then right-click `StockSenseApplication` → *Run As >
Java Application* (or *Spring Boot App*).

Set `DB_PASSWORD` in the run configuration's environment if your MySQL root
user has a password.

---

## Demo credentials

| Role | Email | Password |
|---|---|---|
| Administrator | `admin@stocksense.test` | `Admin@123` |
| User | `priya@stocksense.test` | `User@1234` |
| User | `arjun@stocksense.test` | `User@1234` |
| User | `emma@stocksense.test` | `User@1234` |

Change or remove these accounts before using the app anywhere but your own machine.

---

## Recommendation engine

Five weighted rules each give +1 / 0 / −1; the score is their weighted sum.

| Rule | Bullish | Bearish | Weight |
|---|---|---|---|
| SMA crossover | SMA20 > SMA50 by > 0.5 % | SMA20 < SMA50 by > 0.5 % | 20 |
| RSI zones | RSI(14) < 30 (oversold) | RSI(14) > 70 (overbought) | 15 |
| MACD | MACD above signal | MACD below signal | 15 |
| Momentum | 20-session change > +3 % | < −3 % | 10 |
| Volume | volume ≥ 1.2 × 20-day avg, with the momentum direction | same, downward | 5 |

**BUY** at ≥ +25, **SELL** at ≤ −25, otherwise **HOLD**; fewer than 60 bars →
**Insufficient data**. Every result lists one reason per rule with its
contribution, plus the data date and source. Details:
[docs/recommendation-methodology.md](docs/recommendation-methodology.md).

---

## Testing

`mvn clean package` runs **19 tests**: indicator maths (SMA, EMA, Wilder's RSI
example, MACD, volatility), the engine's BUY / SELL / HOLD / Insufficient /
disabled-rule cases, the password encoder against the stored demo hash, and
MockMvc security tests (anonymous → login, USER blocked from `/admin/**`, POST
without CSRF → 403). The end-to-end checklist and results are in
[docs/TESTING.md](docs/TESTING.md).

---

## Troubleshooting

| Symptom | Fix |
|---|---|
| "The database is not reachable" page | MySQL not running or wrong `DB_URL` / `DB_USER` / `DB_PASSWORD`. |
| `Public Key Retrieval is not allowed` | Keep `allowPublicKeyRetrieval=true` in `DB_URL`. |
| `Unknown database` / empty pages | Run `schema.sql` + `sample-data.sql`, or start once with `DB_INIT=true`. |
| Times shifted by your UTC offset | Don't add `serverTimezone=UTC` unless MySQL and the JVM both run in UTC. |
| `Port 8080 already in use` | Start with `PORT=8090` (or stop the other program). |
| "This form has expired" (403) | The CSRF token expired with the session; reload the page and submit again. |
| Charts are empty boxes | Chart.js could not load from jsdelivr (offline). The tables still show every value. |
| "Demo mode" although a key is set | `MARKET_PROVIDER` must also be `ALPHA_VANTAGE`. Restart after changing. |
| A demo login stopped working on a hosted copy | Set `RESET_DEMO_PASSWORDS=admin@stocksense.test,priya@stocksense.test` (any of the four demo emails), deploy once, then remove the variable. Listed demo accounts get their README password back, and are re-added if they were deleted. Other emails are ignored. |
| Sync says `RATE_LIMITED` | Free tier used up (25/day or 5/min). Cached data stays. |
| `release version 21 not supported` | Maven is using an older JDK: set `JAVA_HOME` to JDK 21. |
