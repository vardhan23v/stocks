package com.stockrecommendation.service;

import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.dao.DataAccessException;

import com.stockrecommendation.api.MarketDataClient;
import com.stockrecommendation.api.MarketDataException;
import com.stockrecommendation.api.MarketDataResponse;
import com.stockrecommendation.dao.ActivityDAO;
import com.stockrecommendation.dao.StockDAO;
import com.stockrecommendation.dao.StockPriceDAO;
import com.stockrecommendation.dao.SyncLogDAO;
import com.stockrecommendation.model.ApiSyncLog;
import com.stockrecommendation.model.Stock;
import com.stockrecommendation.model.StockPrice;
import com.stockrecommendation.model.User;
import com.stockrecommendation.config.MarketProperties;

/**
 * Decides where price data comes from and keeps the MySQL cache up to date.
 *
 * Two modes, chosen with market.provider in application.properties (or the
 * MARKET_PROVIDER environment variable):
 *
 *   DEMO           no network access at all. Only the rows with
 *                  data_source = 'SAMPLE' are used, and the UI labels them as
 *                  SAMPLE DATA everywhere.
 *   ALPHA_VANTAGE  with an API key. Downloaded bars are stored with
 *                  data_source = 'ALPHA_VANTAGE' and preferred over sample rows.
 *                  A stock is downloaded again only when its cached rows are
 *                  older than market.cache-minutes, calls are spaced by
 *                  market.min-interval-ms, and the free daily quota is respected.
 *
 * Every attempt - including cache hits and demo-mode refusals - is written to
 * api_sync_logs, which the admin market data page shows.
 */
@Service
public class MarketDataService {

    public static final String SAMPLE = "SAMPLE";
    public static final String NONE = "NONE";
    public static final String DEMO_PROVIDER = "DEMO";

    private static final AtomicBoolean BULK_RUNNING = new AtomicBoolean(false);
    private static final Object THROTTLE_LOCK = new Object();
    private static long lastCallMillis;

    private final StockPriceDAO priceDAO;
    private final StockDAO stockDAO;
    private final SyncLogDAO syncLogDAO;
    private final ActivityDAO activityDAO;
    private final MarketProperties properties;
    private final TaskExecutor background;

    public MarketDataService(StockPriceDAO priceDAO, StockDAO stockDAO, SyncLogDAO syncLogDAO, ActivityDAO activityDAO,
                             MarketProperties properties, @Qualifier("marketDataExecutor") TaskExecutor background) {
        this.priceDAO = priceDAO;
        this.stockDAO = stockDAO;
        this.syncLogDAO = syncLogDAO;
        this.activityDAO = activityDAO;
        this.properties = properties;
        this.background = background;
    }

    /** One price series and the source it was read from. */
    public record Series(String source, List<StockPrice> prices) {

        public String sourceOrNone() {
            return source == null ? NONE : source;
        }

        public LocalDateTime newestTimestamp() {
            return prices.isEmpty() ? null : prices.get(prices.size() - 1).getRecordedAt();
        }
    }

    /** What a refresh did, for the message shown to the admin. */
    public record SyncResult(String status, String message) {
    }

    // ------------------------------------------------------------ configuration

    public String getConfiguredProvider() {
        return properties.getProvider();
    }

    private String apiKey() {
        return properties.getApiKey();
    }

    /** True when no request will ever be sent: demo provider or no API key. */
    public boolean isDemoMode() {
        return !MarketDataClient.PROVIDER.equals(getConfiguredProvider()) || apiKey().isEmpty();
    }

    public boolean isApiKeyConfigured() {
        return !apiKey().isEmpty();
    }

    /** Short human-readable status for the dashboards. */
    public String getProviderLabel() {
        if (!MarketDataClient.PROVIDER.equals(getConfiguredProvider())) {
            return "Demo mode - sample data only";
        }
        return apiKey().isEmpty()
                ? "Alpha Vantage selected, but no API key - running in demo mode"
                : "Alpha Vantage (delayed end-of-day data)";
    }

    public int getCacheMinutes() {
        return properties.getCacheMinutes();
    }

    public int getDailyQuota() {
        return properties.getDailyQuota();
    }

    /** Sources to read, in order of preference. */
    public List<String> preferredSources() {
        return isDemoMode() ? List.of(SAMPLE) : List.of(MarketDataClient.PROVIDER, SAMPLE);
    }

    public boolean isBulkSyncRunning() {
        return BULK_RUNNING.get();
    }

    // ------------------------------------------------------------ reading

    /** The stored series of one stock from the most preferred source that has data. */
    public Series getSeries(Stock stock) {
        for (String source : preferredSources()) {
            List<StockPrice> prices = priceDAO.findSeries(stock.getId(), source);
            if (!prices.isEmpty()) {
                return new Series(source, prices);
            }
        }
        return new Series(null, List.of());
    }

    public int countCallsToday() {
        return syncLogDAO.countCallsToday(MarketDataClient.PROVIDER);
    }

    // ------------------------------------------------------------ refreshing

    /**
     * Cache-first refresh used when a user opens a stock page. Never waits for
     * the throttle: if a call was made a moment ago it simply keeps the cached
     * data. Does nothing in demo mode and never throws.
     */
    public void refreshIfStale(Stock stock) {
        if (isDemoMode()) {
            return;
        }
        try {
            if (isCacheFresh(stock) || !throttle(false)) {
                return;
            }
            download(stock, false);
        } catch (DataAccessException e) {
            // the page still shows the cached data
        }
    }

    /** Admin action: refresh one stock now (still respecting cache and quota). */
    public SyncResult refresh(User admin, Stock stock, boolean ignoreCache) {
        AdminService.requireAdmin(admin);
        if (isDemoMode()) {
            syncLogDAO.insert(DEMO_PROVIDER, stock.getApiSymbol(), ApiSyncLog.DEMO,
                    "Demo mode: no API request was made. Configure an API key to download data.");
            return new SyncResult(ApiSyncLog.DEMO, "Demo mode is active, so no request was sent. Sample data stays in use.");
        }
        if (!ignoreCache && isCacheFresh(stock)) {
            syncLogDAO.insert(MarketDataClient.PROVIDER, stock.getApiSymbol(), ApiSyncLog.CACHED,
                    "Cached data is younger than " + getCacheMinutes() + " minutes; no request made.");
            return new SyncResult(ApiSyncLog.CACHED, stock.getSymbol() + ": cached data is still fresh, no request made.");
        }
        throttle(true);
        SyncResult result = download(stock, true);
        activityDAO.log(admin.getId(), "MARKET_DATA_SYNC", stock.getApiSymbol() + ": " + result.status());
        return result;
    }

    /**
     * Admin action: refreshes every active stock with stale data on a
     * background thread, spacing the calls to respect the per-minute limit.
     *
     * @return false when a bulk sync is already running or demo mode is on
     */
    public boolean startBulkSync(User admin) {
        AdminService.requireAdmin(admin);
        if (isDemoMode() || !BULK_RUNNING.compareAndSet(false, true)) {
            return false;
        }
        activityDAO.log(admin.getId(), "MARKET_DATA_BULK_SYNC", "Started background sync of stale stocks");
        background.execute(() -> {
            try {
                for (Stock stock : stockDAO.findAllActive()) {
                    if (countCallsToday() >= getDailyQuota()) {
                        syncLogDAO.insert(MarketDataClient.PROVIDER, "*", ApiSyncLog.RATE_LIMITED,
                                "Daily quota of " + getDailyQuota() + " requests reached; bulk sync stopped.");
                        break;
                    }
                    if (isCacheFresh(stock)) {
                        continue;
                    }
                    throttle(true);
                    SyncResult result = download(stock, false);
                    if (ApiSyncLog.RATE_LIMITED.equals(result.status())) {
                        break;   // no point hammering the provider
                    }
                }
            } catch (RuntimeException e) {
                syncLogDAO.insert(MarketDataClient.PROVIDER, "*", ApiSyncLog.ERROR, "Bulk sync stopped: " + e.getMessage());
            } finally {
                BULK_RUNNING.set(false);
            }
        });
        return true;
    }

    private boolean isCacheFresh(Stock stock) {
        LocalDateTime storedAt = priceDAO.findLastStoredAt(stock.getId(), MarketDataClient.PROVIDER);
        return storedAt != null
            && Duration.between(storedAt, LocalDateTime.now()).toMinutes() < getCacheMinutes();
    }

    /**
     * Keeps calls at least market.minIntervalMs apart.
     *
     * @param wait true = sleep until the next call is allowed, false = give up
     * @return true when the caller may send a request now
     */
    private boolean throttle(boolean wait) {
        long interval = properties.getMinIntervalMs();
        synchronized (THROTTLE_LOCK) {
            long remaining = lastCallMillis + interval - System.currentTimeMillis();
            if (remaining > 0) {
                if (!wait) {
                    return false;
                }
                try {
                    Thread.sleep(remaining);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return false;
                }
            }
            lastCallMillis = System.currentTimeMillis();
            return true;
        }
    }

    /** One download of daily bars (plus fundamentals when missing), logged either way. */
    private SyncResult download(Stock stock, boolean withFundamentals) {
        String symbol = stock.getApiSymbol();
        if (countCallsToday() >= getDailyQuota()) {
            String message = "Daily quota of " + getDailyQuota() + " requests reached; cached data kept.";
            syncLogDAO.insert(MarketDataClient.PROVIDER, symbol, ApiSyncLog.RATE_LIMITED, message);
            return new SyncResult(ApiSyncLog.RATE_LIMITED, message);
        }
        MarketDataClient client = new MarketDataClient(
                properties.getBaseUrl(), apiKey(), properties.getTimeoutSeconds());
        try {
            MarketDataResponse response = client.fetchDaily(symbol);
            int stored = priceDAO.upsertAll(stock.getId(), response.getBars());
            String message = response.getBars().size() + " daily bars received (" + stored + " rows written), newest "
                    + response.getLastRefreshed().toLocalDate() + ".";
            syncLogDAO.insert(MarketDataClient.PROVIDER, symbol, ApiSyncLog.SUCCESS, message);

            if (withFundamentals && stock.getMarketCap() == null && countCallsToday() < getDailyQuota()) {
                refreshFundamentals(client, stock);
            }
            return new SyncResult(ApiSyncLog.SUCCESS, stock.getSymbol() + ": " + message);
        } catch (MarketDataException e) {
            syncLogDAO.insert(MarketDataClient.PROVIDER, symbol, e.getStatus(), e.getMessage());
            return new SyncResult(e.getStatus(), stock.getSymbol() + ": " + e.getMessage());
        }
    }

    private void refreshFundamentals(MarketDataClient client, Stock stock) {
        try {
            throttle(true);
            MarketDataResponse overview = client.fetchOverview(stock.getApiSymbol());
            if (overview.getMarketCap() == null && overview.getWeek52High() == null) {
                syncLogDAO.insert(MarketDataClient.PROVIDER, stock.getApiSymbol(), ApiSyncLog.NO_DATA,
                        "Company overview not available for this listing.");
                return;
            }
            stock.setMarketCap(overview.getMarketCap());
            stock.setWeek52High(overview.getWeek52High());
            stock.setWeek52Low(overview.getWeek52Low());
            stock.setFundamentalsSource(MarketDataClient.PROVIDER);
            stock.setFundamentalsUpdatedAt(LocalDateTime.now());
            stockDAO.updateFundamentals(stock);
            syncLogDAO.insert(MarketDataClient.PROVIDER, stock.getApiSymbol(), ApiSyncLog.SUCCESS, "Company overview stored.");
        } catch (MarketDataException e) {
            syncLogDAO.insert(MarketDataClient.PROVIDER, stock.getApiSymbol(), e.getStatus(), e.getMessage());
        }
    }
}
