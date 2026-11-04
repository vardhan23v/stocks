package com.stockrecommendation.bean;

import java.io.Serializable;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.stockrecommendation.model.Stock;
import com.stockrecommendation.model.User;
import com.stockrecommendation.model.Watchlist;
import com.stockrecommendation.service.StockService;
import com.stockrecommendation.service.WatchlistService;
import com.stockrecommendation.util.DataAccessException;
import com.stockrecommendation.util.FacesUtil;
import com.stockrecommendation.util.SessionUtil;

import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;

/**
 * Backing bean of stocks.xhtml (the stock explorer).
 *
 * The search form is a plain GET form, so a search result is a bookmarkable
 * URL: stocks.xhtml?q=bank&sector=...&sort=change_desc. The <f:viewParam>s
 * copy the parameters in and <f:viewAction> calls load().
 *
 * @ViewScoped so the result list still exists when a "Watch" button inside
 * the table is clicked (JSF needs the rows again to find the clicked one).
 */
@Named("stockBean")
@ViewScoped
public class StockBean implements Serializable {

    private static final long serialVersionUID = 1L;

    private final StockService stockService = new StockService();
    private final WatchlistService watchlistService = new WatchlistService();

    private String q;
    private String sector;
    private String exchange;
    private String sort = StockService.SORT_SYMBOL;

    private List<Stock> stocks = List.of();
    private List<String> sectors = List.of();
    private List<String> exchanges = List.of();
    private final Set<Integer> watchedIds = new HashSet<>();

    public void load() {
        try {
            stocks = stockService.search(q, sector, exchange, sort);
            sectors = stockService.getSectors();
            exchanges = stockService.getExchanges();
            watchedIds.clear();
            User user = SessionUtil.getCurrentUser();
            for (Watchlist entry : watchlistService.getWatchlist(user, null)) {
                watchedIds.add(entry.getStockId());
            }
        } catch (DataAccessException e) {
            FacesUtil.addError(e.getMessage());
        }
    }

    public String addToWatchlist(int stockId) {
        try {
            watchlistService.add(SessionUtil.getCurrentUser(), stockId);
            FacesUtil.keepMessagesForRedirect();
            FacesUtil.addInfo("Added to your watchlist.");
            return "stocks?faces-redirect=true&includeViewParams=true";
        } catch (IllegalStateException | DataAccessException e) {
            FacesUtil.addError(e.getMessage());
            return null;
        }
    }

    public boolean isWatched(int stockId) {
        return watchedIds.contains(stockId);
    }

    public boolean isFiltered() {
        return (q != null && !q.isBlank()) || (sector != null && !sector.isBlank())
            || (exchange != null && !exchange.isBlank());
    }

    public boolean isAnySample() {
        return stocks.stream().anyMatch(Stock::isSampleData);
    }

    public String getQ() { return q; }
    public void setQ(String q) { this.q = q; }

    public String getSector() { return sector; }
    public void setSector(String sector) { this.sector = sector; }

    public String getExchange() { return exchange; }
    public void setExchange(String exchange) { this.exchange = exchange; }

    public String getSort() { return sort; }
    public void setSort(String sort) { this.sort = sort == null || sort.isBlank() ? StockService.SORT_SYMBOL : sort; }

    public List<Stock> getStocks() { return stocks; }
    public List<String> getSectors() { return sectors; }
    public List<String> getExchanges() { return exchanges; }
}
