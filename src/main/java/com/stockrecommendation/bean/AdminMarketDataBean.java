package com.stockrecommendation.bean;

import java.io.Serializable;
import java.util.List;

import com.stockrecommendation.model.ApiSyncLog;
import com.stockrecommendation.model.Stock;
import com.stockrecommendation.service.AdminService;
import com.stockrecommendation.service.MarketDataService;
import com.stockrecommendation.service.StockService;
import com.stockrecommendation.util.DataAccessException;
import com.stockrecommendation.util.FacesUtil;
import com.stockrecommendation.util.SessionUtil;

import jakarta.annotation.PostConstruct;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;

/**
 * Backing bean of admin-market-data.xhtml: provider configuration, stored data
 * per source, manual syncs and the api_sync_logs history.
 * The API key itself is never shown - only whether one is configured.
 */
@Named("adminMarketDataBean")
@ViewScoped
public class AdminMarketDataBean implements Serializable {

    private static final long serialVersionUID = 1L;

    private final AdminService adminService = new AdminService();
    private final StockService stockService = new StockService();
    private final MarketDataService marketDataService = new MarketDataService();

    private List<Stock> stocks = List.of();
    private List<ApiSyncLog> logs = List.of();
    private List<Object[]> summary = List.of();
    private int callsToday;

    @PostConstruct
    public void init() {
        reload();
    }

    public void reload() {
        try {
            stocks = stockService.findAllActiveWithQuotes();
            logs = adminService.getSyncLogs(SessionUtil.getCurrentUser(), 60);
            summary = adminService.getPriceSummary(SessionUtil.getCurrentUser());
            callsToday = marketDataService.countCallsToday();
        } catch (SecurityException | DataAccessException e) {
            FacesUtil.addError(e.getMessage());
        }
    }

    public void sync(int stockId, boolean ignoreCache) {
        try {
            Stock stock = stockService.getStock(stockId);
            if (stock == null) {
                FacesUtil.addError("That stock no longer exists.");
                return;
            }
            MarketDataService.SyncResult result = marketDataService.refresh(SessionUtil.getCurrentUser(), stock, ignoreCache);
            if (ApiSyncLog.SUCCESS.equals(result.status()) || ApiSyncLog.CACHED.equals(result.status())) {
                FacesUtil.addInfo(result.message());
            } else if (ApiSyncLog.DEMO.equals(result.status())) {
                FacesUtil.addWarn(result.message());
            } else {
                FacesUtil.addError(result.status() + ": " + result.message() + " Cached data is kept.");
            }
        } catch (SecurityException | DataAccessException e) {
            FacesUtil.addError(e.getMessage());
        }
        reload();
    }

    public void syncAll() {
        try {
            if (marketDataService.startBulkSync(SessionUtil.getCurrentUser())) {
                FacesUtil.addInfo("Background sync started. Stale stocks are fetched one by one within the provider limits; "
                        + "use Refresh to follow the log.");
            } else if (MarketDataService.isDemoMode()) {
                FacesUtil.addWarn("Demo mode is active, so nothing was requested from a provider.");
            } else {
                FacesUtil.addWarn("A background sync is already running.");
            }
        } catch (SecurityException | DataAccessException e) {
            FacesUtil.addError(e.getMessage());
        }
        reload();
    }

    public List<Stock> getStocks() { return stocks; }
    public List<ApiSyncLog> getLogs() { return logs; }
    public List<Object[]> getSummary() { return summary; }
    public int getCallsToday() { return callsToday; }
    public boolean isDemoMode() { return MarketDataService.isDemoMode(); }
    public boolean isApiKeyConfigured() { return MarketDataService.isApiKeyConfigured(); }
    public boolean isBulkRunning() { return MarketDataService.isBulkSyncRunning(); }
    public String getConfiguredProvider() { return MarketDataService.getConfiguredProvider(); }
    public String getProviderLabel() { return MarketDataService.getProviderLabel(); }
    public int getCacheMinutes() { return MarketDataService.getCacheMinutes(); }
}
