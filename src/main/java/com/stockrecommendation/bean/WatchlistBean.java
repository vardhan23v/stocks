package com.stockrecommendation.bean;

import java.io.Serializable;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.stockrecommendation.model.Stock;
import com.stockrecommendation.model.Watchlist;
import com.stockrecommendation.service.StockService;
import com.stockrecommendation.service.WatchlistService;
import com.stockrecommendation.util.DataAccessException;
import com.stockrecommendation.util.FacesUtil;
import com.stockrecommendation.util.SessionUtil;

import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;

/**
 * Backing bean of watchlist.xhtml?sort=change_desc.
 *
 * The user is ALWAYS the session user - there is no user id parameter, so
 * there is no way to ask for somebody else's watchlist.
 */
@Named("watchlistBean")
@ViewScoped
public class WatchlistBean implements Serializable {

    private static final long serialVersionUID = 1L;

    private final WatchlistService watchlistService = new WatchlistService();
    private final StockService stockService = new StockService();

    private String sort = StockService.SORT_CHANGE_DESC;
    private List<Watchlist> entries = List.of();
    private List<Stock> availableStocks = List.of();
    private Integer selectedStockId;

    public void load() {
        try {
            entries = watchlistService.getWatchlist(SessionUtil.getCurrentUser(), sort);
            Set<Integer> watched = entries.stream().map(Watchlist::getStockId).collect(Collectors.toSet());
            availableStocks = stockService.search(null, null, null, StockService.SORT_SYMBOL).stream()
                    .filter(stock -> !watched.contains(stock.getId()))
                    .toList();
        } catch (DataAccessException e) {
            FacesUtil.addError(e.getMessage());
        }
    }

    public String add() {
        if (selectedStockId == null) {
            FacesUtil.addError("Choose a stock to add.");
            return null;
        }
        try {
            watchlistService.add(SessionUtil.getCurrentUser(), selectedStockId);
            FacesUtil.keepMessagesForRedirect();
            FacesUtil.addInfo("Added to your watchlist.");
            return "watchlist?faces-redirect=true&includeViewParams=true";
        } catch (IllegalStateException | DataAccessException e) {
            FacesUtil.addError(e.getMessage());
            return null;
        }
    }

    public String remove(int stockId) {
        try {
            watchlistService.remove(SessionUtil.getCurrentUser(), stockId);
            FacesUtil.keepMessagesForRedirect();
            FacesUtil.addInfo("Removed from your watchlist.");
            return "watchlist?faces-redirect=true&includeViewParams=true";
        } catch (IllegalStateException | DataAccessException e) {
            FacesUtil.addError(e.getMessage());
            return null;
        }
    }

    public long getGainersCount() {
        return entries.stream().filter(e -> "up".equals(e.getStock().getDirection())).count();
    }

    public long getDecliningCount() {
        return entries.stream().filter(e -> "down".equals(e.getStock().getDirection())).count();
    }

    public boolean isAnySample() {
        return entries.stream().anyMatch(e -> e.getStock().isSampleData());
    }

    public String getSort() { return sort; }
    public void setSort(String sort) { this.sort = sort == null || sort.isBlank() ? StockService.SORT_CHANGE_DESC : sort; }

    public List<Watchlist> getEntries() { return entries; }
    public List<Stock> getAvailableStocks() { return availableStocks; }

    public Integer getSelectedStockId() { return selectedStockId; }
    public void setSelectedStockId(Integer selectedStockId) { this.selectedStockId = selectedStockId; }
}
