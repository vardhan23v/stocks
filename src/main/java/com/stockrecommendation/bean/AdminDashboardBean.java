package com.stockrecommendation.bean;

import java.time.LocalDateTime;
import java.util.List;

import com.stockrecommendation.model.ActivityLog;
import com.stockrecommendation.model.ApiSyncLog;
import com.stockrecommendation.model.User;
import com.stockrecommendation.service.AdminService;
import com.stockrecommendation.service.MarketDataService;
import com.stockrecommendation.service.StockService;
import com.stockrecommendation.util.DataAccessException;
import com.stockrecommendation.util.FacesUtil;
import com.stockrecommendation.util.SessionUtil;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Named;

/** Backing bean of admin-dashboard.xhtml. */
@Named("adminDashboardBean")
@RequestScoped
public class AdminDashboardBean {

    private final AdminService adminService = new AdminService();
    private final StockService stockService = new StockService();
    private final MarketDataService marketDataService = new MarketDataService();

    private int userCount;
    private int stockCount;
    private int watchlistCount;
    private int recommendationCount;
    private long priceRowCount;
    private LocalDateTime latestBar;
    private String latestBarSource;
    private ApiSyncLog latestSync;
    private ApiSyncLog latestSuccess;
    private int callsToday;
    private List<ActivityLog> activity = List.of();
    private List<ApiSyncLog> syncLogs = List.of();

    @PostConstruct
    public void init() {
        User admin = SessionUtil.getCurrentUser();
        try {
            userCount = adminService.countUsers(admin);
            stockCount = stockService.countActive();
            watchlistCount = adminService.countWatchlistEntries(admin);
            recommendationCount = adminService.countRecommendations(admin);
            priceRowCount = adminService.countPriceRows(admin);
            for (Object[] row : adminService.getPriceSummary(admin)) {
                LocalDateTime newest = (LocalDateTime) row[3];
                if (newest != null && (latestBar == null || newest.isAfter(latestBar))) {
                    latestBar = newest;
                    latestBarSource = (String) row[0];
                }
            }
            latestSync = adminService.getLatestSync(admin);
            latestSuccess = adminService.getLatestSuccessfulSync(admin);
            callsToday = marketDataService.countCallsToday();
            activity = adminService.getActivity(admin, 12);
            syncLogs = adminService.getSyncLogs(admin, 6);
        } catch (SecurityException | DataAccessException e) {
            FacesUtil.addError(e.getMessage());
        }
    }

    public int getUserCount() { return userCount; }
    public int getStockCount() { return stockCount; }
    public int getWatchlistCount() { return watchlistCount; }
    public int getRecommendationCount() { return recommendationCount; }
    public long getPriceRowCount() { return priceRowCount; }
    public LocalDateTime getLatestBar() { return latestBar; }
    public String getLatestBarSource() { return latestBarSource; }
    public ApiSyncLog getLatestSync() { return latestSync; }
    public ApiSyncLog getLatestSuccess() { return latestSuccess; }
    public int getCallsToday() { return callsToday; }
    public List<ActivityLog> getActivity() { return activity; }
    public List<ApiSyncLog> getSyncLogs() { return syncLogs; }
    public boolean isDemoMode() { return MarketDataService.isDemoMode(); }
    public String getProviderLabel() { return MarketDataService.getProviderLabel(); }
}
