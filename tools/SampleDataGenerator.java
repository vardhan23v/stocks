import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

import com.stockrecommendation.model.Recommendation;
import com.stockrecommendation.model.RecommendationRule;
import com.stockrecommendation.model.StockIndicator;
import com.stockrecommendation.model.StockPrice;
import com.stockrecommendation.service.RecommendationService;
import com.stockrecommendation.service.StockAnalysisService;
import com.stockrecommendation.util.PasswordUtil;

/**
 * Development tool that writes src/main/resources/sample-data.sql.
 *
 * The price history it produces is SYNTHETIC: a seeded random walk per stock,
 * stored with data_source = 'SAMPLE'. It is not, and must never be presented
 * as, real market data. The seed is fixed, so every run produces the same
 * prices; only the password hashes change (they use random salts).
 *
 * The indicators and recommendations in the file are calculated by the
 * application's own StockAnalysisService and RecommendationService, so the
 * stored rows are exactly what the running application would produce.
 *
 * Run from the project root after `mvn package`:
 *
 *   java -cp "target/classes:$(mvn -q dependency:build-classpath -Dmdep.outputFile=/dev/stdout)" \
 *        tools/SampleDataGenerator.java > src/main/resources/sample-data.sql
 */
public class SampleDataGenerator {

    private static final LocalDate LAST_DAY = LocalDate.of(2026, 10, 30);
    private static final int BARS = 265;   // a little over one year of trading days
    private static final DateTimeFormatter SQL_DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd 00:00:00");

    /** company, symbol, exchange, api symbol, currency, sector, industry, description, start price, drift, volatility, base volume, bars */
    private static final Object[][] STOCKS = {
        {"Reliance Industries Limited", "RELIANCE", "BSE", "RELIANCE.BSE", "INR", "Energy", "Oil, Gas & Petrochemicals",
         "Indian conglomerate with businesses in oil-to-chemicals, retail, and digital and telecom services (Jio).",
         1400.0, 0.0006, 0.014, 9_000_000L, BARS},
        {"Tata Consultancy Services Limited", "TCS", "BSE", "TCS.BSE", "INR", "Information Technology", "IT Services & Consulting",
         "IT services, consulting and business solutions company of the Tata Group, headquartered in Mumbai.",
         3500.0, -0.0009, 0.013, 2_500_000L, BARS},
        {"Infosys Limited", "INFY", "BSE", "INFY.BSE", "INR", "Information Technology", "IT Services & Consulting",
         "Global provider of digital services and consulting, headquartered in Bengaluru.",
         1500.0, 0.0001, 0.015, 6_000_000L, BARS},
        {"HDFC Bank Limited", "HDFCBANK", "BSE", "HDFCBANK.BSE", "INR", "Financial Services", "Private Sector Bank",
         "One of India's largest private sector banks by assets.",
         950.0, 0.0008, 0.011, 12_000_000L, BARS},
        {"ICICI Bank Limited", "ICICIBANK", "BSE", "ICICIBANK.BSE", "INR", "Financial Services", "Private Sector Bank",
         "Large Indian private sector bank offering retail, corporate and treasury services.",
         1350.0, 0.0005, 0.012, 10_000_000L, BARS},
        {"State Bank of India", "SBIN", "BSE", "SBIN.BSE", "INR", "Financial Services", "Public Sector Bank",
         "India's largest public sector bank, majority owned by the Government of India.",
         820.0, -0.0004, 0.016, 14_000_000L, BARS},
        {"ITC Limited", "ITC", "BSE", "ITC.BSE", "INR", "Consumer Staples", "Diversified FMCG",
         "Diversified company with interests in FMCG, hotels, paperboards and agri-business.",
         420.0, -0.0012, 0.010, 15_000_000L, BARS},
        {"Tata Motors Limited", "TATAMOTORS", "BSE", "TATAMOTORS.BSE", "INR", "Consumer Discretionary", "Automobiles",
         "Automobile manufacturer of the Tata Group (passenger and commercial vehicles, Jaguar Land Rover). "
         + "Note: the company was demerged in 2025 and its listed ticker changed; update the symbols in Admin > Stocks before syncing real data.",
         680.0, 0.0002, 0.020, 18_000_000L, BARS},
        {"Hindustan Unilever Limited", "HINDUNILVR", "BSE", "HINDUNILVR.BSE", "INR", "Consumer Staples", "Personal & Household Products",
         "Indian consumer goods company, a subsidiary of Unilever. This sample series is deliberately short to demonstrate the "
         + "'Insufficient data' state of the recommendation engine.",
         2400.0, 0.0003, 0.011, 1_500_000L, 30},
        {"Apple Inc.", "AAPL", "NASDAQ", "AAPL", "USD", "Information Technology", "Consumer Electronics",
         "Designs iPhone, Mac, iPad and wearables, and runs a services business including the App Store.",
         230.0, 0.0007, 0.015, 55_000_000L, BARS},
        {"Microsoft Corporation", "MSFT", "NASDAQ", "MSFT", "USD", "Information Technology", "Software - Infrastructure",
         "Software and cloud company: Azure, Microsoft 365, Windows, LinkedIn and Xbox.",
         420.0, 0.0004, 0.013, 22_000_000L, BARS},
        {"Alphabet Inc. (Class A)", "GOOGL", "NASDAQ", "GOOGL", "USD", "Communication Services", "Internet Content & Information",
         "Parent company of Google: Search, YouTube, Android and Google Cloud.",
         175.0, 0.0010, 0.017, 28_000_000L, BARS},
        {"Amazon.com, Inc.", "AMZN", "NASDAQ", "AMZN", "USD", "Consumer Discretionary", "Internet Retail",
         "Online retail, Amazon Web Services cloud computing, advertising and subscriptions.",
         200.0, -0.0002, 0.018, 40_000_000L, BARS},
        {"NVIDIA Corporation", "NVDA", "NASDAQ", "NVDA", "USD", "Information Technology", "Semiconductors",
         "Designs GPUs and accelerated computing platforms for gaming, data centres and AI.",
         130.0, 0.0015, 0.028, 250_000_000L, BARS},
        {"Tesla, Inc.", "TSLA", "NASDAQ", "TSLA", "USD", "Consumer Discretionary", "Automobiles",
         "Electric vehicles, battery energy storage and solar products.",
         260.0, -0.0015, 0.032, 90_000_000L, BARS},
    };

    public static void main(String[] args) {
        StringBuilder sql = new StringBuilder();
        line(sql, "-- ---------------------------------------------------------------------------");
        line(sql, "-- StockSense - sample data");
        line(sql, "--");
        line(sql, "-- !! ALL PRICE DATA IN THIS FILE IS SYNTHETIC SAMPLE DATA !!");
        line(sql, "-- It was generated by tools/SampleDataGenerator.java (seeded random walk) and is");
        line(sql, "-- stored with data_source = 'SAMPLE'. It is NOT real market data, and the");
        line(sql, "-- application labels it as SAMPLE DATA wherever it is shown.");
        line(sql, "--");
        line(sql, "-- Run after schema.sql:  mysql -u root -p < src/main/resources/sample-data.sql");
        line(sql, "-- Safe to run again: INSERT IGNORE / NOT EXISTS guards prevent duplicates.");
        line(sql, "--");
        line(sql, "-- Demo accounts:  admin@stocksense.test / Admin@123   (ADMIN)");
        line(sql, "--                 priya@stocksense.test / User@1234  (USER)");
        line(sql, "--                 arjun@stocksense.test / User@1234  (USER)");
        line(sql, "--                 emma@stocksense.test  / User@1234  (USER)");
        line(sql, "-- ---------------------------------------------------------------------------");
        line(sql, "");
        line(sql, "USE stock_recommendation_db;");
        line(sql, "");

        line(sql, "-- Users (PBKDF2-SHA256 hashes, see PasswordUtil)");
        line(sql, "INSERT IGNORE INTO users (name, email, password_hash, role) VALUES");
        line(sql, "  ('Asha Rao (Admin)', 'admin@stocksense.test', '" + PasswordUtil.hash("Admin@123") + "', 'ADMIN'),");
        line(sql, "  ('Priya Sharma', 'priya@stocksense.test', '" + PasswordUtil.hash("User@1234") + "', 'USER'),");
        line(sql, "  ('Arjun Mehta', 'arjun@stocksense.test', '" + PasswordUtil.hash("User@1234") + "', 'USER'),");
        line(sql, "  ('Emma Clarke', 'emma@stocksense.test', '" + PasswordUtil.hash("User@1234") + "', 'USER');");
        line(sql, "");

        line(sql, "-- Recommendation rules (weights and thresholds of the scoring engine)");
        line(sql, "INSERT IGNORE INTO recommendation_rules (rule_key, name, description, weight, param_low, param_high) VALUES");
        line(sql, "  ('SMA_CROSSOVER', 'SMA crossover', 'SMA(20) above SMA(50) by more than param_low % is bullish (+weight), below by more than param_low % is bearish (-weight).', 20, 0.5, NULL),");
        line(sql, "  ('RSI', 'RSI zones', 'RSI(14) below param_low (oversold) adds +weight, above param_high (overbought) adds -weight.', 15, 30, 70),");
        line(sql, "  ('MACD', 'MACD vs signal', 'MACD(12,26) above its 9-period signal line adds +weight, below adds -weight.', 15, NULL, NULL),");
        line(sql, "  ('MOMENTUM', '20-session momentum', 'Price change over 20 sessions above +param_low % adds +weight, below -param_low % adds -weight.', 10, 3, NULL),");
        line(sql, "  ('VOLUME', 'Volume confirmation', 'Latest volume at least param_low x its 20-session average confirms the momentum direction (+/-weight).', 5, 1.2, NULL),");
        line(sql, "  ('DECISION', 'Decision thresholds', 'Score >= param_high is BUY, score <= param_low is SELL, anything between is HOLD. Weight is unused.', 0, -25, 25);");
        line(sql, "");

        line(sql, "-- Stocks. market_cap and the 52-week range stay NULL: they are only filled in");
        line(sql, "-- when a real data provider supplies them.");
        line(sql, "INSERT IGNORE INTO stocks (company_name, symbol, exchange, api_symbol, currency, sector, industry, description) VALUES");
        for (int i = 0; i < STOCKS.length; i++) {
            Object[] s = STOCKS[i];
            line(sql, String.format("  (%s, %s, %s, %s, %s, %s, %s, %s)%s", q(s[0]), q(s[1]), q(s[2]), q(s[3]), q(s[4]),
                    q(s[5]), q(s[6]), q(s[7]), i == STOCKS.length - 1 ? ";" : ","));
        }
        line(sql, "");

        List<RecommendationRule> rules = RecommendationService.defaultRules();
        StockAnalysisService analysis = new StockAnalysisService();
        for (Object[] s : STOCKS) {
            String symbol = (String) s[1];
            String exchange = (String) s[2];
            List<StockPrice> prices = generate(symbol, (double) s[8], (double) s[9], (double) s[10], (long) s[11], (int) s[12]);

            line(sql, "-- " + exchange + ":" + symbol + " - " + prices.size() + " SAMPLE bars (synthetic)");
            line(sql, "SET @stock_id := (SELECT id FROM stocks WHERE exchange = " + q(exchange) + " AND symbol = " + q(symbol) + ");");
            line(sql, "INSERT IGNORE INTO stock_prices (stock_id, open_price, high_price, low_price, close_price, adjusted_close, volume, recorded_at, data_source) VALUES");
            for (int i = 0; i < prices.size(); i++) {
                StockPrice p = prices.get(i);
                line(sql, String.format(Locale.US, "  (@stock_id, %s, %s, %s, %s, %s, %d, '%s', 'SAMPLE')%s",
                        p.getOpenPrice(), p.getHighPrice(), p.getLowPrice(), p.getClosePrice(), p.getClosePrice(),
                        p.getVolume(), p.getRecordedAt().format(SQL_DATE), i == prices.size() - 1 ? ";" : ","));
            }

            String timestamp = q(prices.get(prices.size() - 1).getRecordedAt().format(SQL_DATE));
            String notExists = " FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM %s WHERE stock_id = @stock_id AND data_source = 'SAMPLE' AND data_timestamp = " + timestamp + ");";

            StockIndicator ind = analysis.calculate(0, prices);
            line(sql, "INSERT INTO stock_indicators (stock_id, sma, sma_long, ema, rsi, macd, signal_line, volatility, data_source, data_timestamp)");
            line(sql, String.format("  SELECT @stock_id, %s, %s, %s, %s, %s, %s, %s, 'SAMPLE', %s", n(ind.getSma()), n(ind.getSmaLong()),
                    n(ind.getEma()), n(ind.getRsi()), n(ind.getMacd()), n(ind.getSignalLine()), n(ind.getVolatility()), timestamp)
                    + String.format(notExists, "stock_indicators"));

            Recommendation rec = RecommendationService.evaluate(0, prices, rules);
            line(sql, "INSERT INTO recommendations (stock_id, recommendation_type, recommendation_score, explanation, supporting_json, data_source, data_timestamp)");
            line(sql, String.format("  SELECT @stock_id, %s, %s, %s, %s, 'SAMPLE', %s", q(rec.getRecommendationType()),
                    rec.getRecommendationScore(), q(rec.getExplanation()), q(rec.getSupportingJson()), timestamp)
                    + String.format(notExists, "recommendations"));
            line(sql, "");
            System.err.printf("%-11s %4d bars  last %10s  %-17s %s%n", symbol, prices.size(),
                    prices.get(prices.size() - 1).getClosePrice(), rec.getRecommendationType(), rec.getRecommendationScore());
        }

        line(sql, "-- Watchlists of the demo users");
        line(sql, "INSERT IGNORE INTO watchlists (user_id, stock_id)");
        line(sql, "  SELECT u.id, s.id FROM users u JOIN stocks s ON");
        line(sql, "    (u.email = 'priya@stocksense.test' AND s.symbol IN ('INFY', 'TCS', 'AAPL', 'NVDA'))");
        line(sql, "    OR (u.email = 'arjun@stocksense.test' AND s.symbol IN ('RELIANCE', 'HDFCBANK', 'SBIN'))");
        line(sql, "    OR (u.email = 'emma@stocksense.test' AND s.symbol IN ('MSFT', 'GOOGL', 'TSLA', 'AMZN', 'ITC'));");
        line(sql, "");
        line(sql, "-- Sync log: the sample dataset was loaded, no API was called");
        line(sql, "INSERT INTO api_sync_logs (provider, symbol, status, response_message)");
        line(sql, "  SELECT 'DEMO', '*', 'DEMO', 'Sample dataset loaded from sample-data.sql (synthetic prices, no API call).' FROM DUAL");
        line(sql, "  WHERE NOT EXISTS (SELECT 1 FROM api_sync_logs WHERE provider = 'DEMO' AND symbol = '*');");
        line(sql, "INSERT INTO activity_logs (user_id, action, details)");
        line(sql, "  SELECT NULL, 'SAMPLE_DATA_LOADED', 'sample-data.sql executed' FROM DUAL");
        line(sql, "  WHERE NOT EXISTS (SELECT 1 FROM activity_logs WHERE action = 'SAMPLE_DATA_LOADED');");
        System.out.print(sql);
    }

    /** Seeded geometric random walk on weekdays, ending at LAST_DAY. */
    private static List<StockPrice> generate(String symbol, double start, double drift, double volatility, long baseVolume, int bars) {
        Random random = new Random(symbol.hashCode() * 31L + 7);
        List<LocalDate> days = new ArrayList<>();
        LocalDate day = LAST_DAY;
        while (days.size() < bars) {
            if (day.getDayOfWeek() != DayOfWeek.SATURDAY && day.getDayOfWeek() != DayOfWeek.SUNDAY) {
                days.add(0, day);
            }
            day = day.minusDays(1);
        }

        List<StockPrice> prices = new ArrayList<>();
        double close = start;
        for (LocalDate date : days) {
            double open = close * (1 + random.nextGaussian() * volatility * 0.3);
            double shock = random.nextGaussian();
            close = Math.max(1, close * Math.exp(drift - volatility * volatility / 2 + volatility * shock));
            double high = Math.max(open, close) * (1 + Math.abs(random.nextGaussian()) * volatility * 0.4);
            double low = Math.min(open, close) * (1 - Math.abs(random.nextGaussian()) * volatility * 0.4);
            long volume = Math.round(baseVolume * Math.exp(0.25 * random.nextGaussian() + 0.4 * Math.abs(shock) - 0.2));
            prices.add(new StockPrice(date.atStartOfDay(), money(open), money(high), money(low), money(close), volume, "SAMPLE"));
        }
        return prices;
    }

    private static BigDecimal money(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP);
    }

    private static String n(BigDecimal value) {
        return value == null ? "NULL" : value.toPlainString();
    }

    private static String q(Object value) {
        return value == null ? "NULL" : "'" + value.toString().replace("\\", "\\\\").replace("'", "''").replace("\n", "\\n") + "'";
    }

    private static void line(StringBuilder sql, String text) {
        sql.append(text).append('\n');
    }
}
