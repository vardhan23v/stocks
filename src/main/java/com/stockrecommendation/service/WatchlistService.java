package com.stockrecommendation.service;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.stockrecommendation.dao.StockDAO;
import com.stockrecommendation.dao.WatchlistDAO;
import com.stockrecommendation.model.Stock;
import com.stockrecommendation.model.User;
import com.stockrecommendation.model.Watchlist;

/**
 * Business rules for the personal watchlist.
 *
 * The user is always the session user passed in by the bean, so a watchlist
 * can only ever be read or changed by its owner. The duplicate rule is checked
 * here first and enforced by UNIQUE (user_id, stock_id) in the database.
 */
@Service
public class WatchlistService {


    private final WatchlistDAO watchlistDAO;
    private final StockDAO stockDAO;
    private final StockService stockService;

    public WatchlistService(WatchlistDAO watchlistDAO, StockDAO stockDAO, StockService stockService) {
        this.watchlistDAO = watchlistDAO;
        this.stockDAO = stockDAO;
        this.stockService = stockService;
    }

    public List<Watchlist> getWatchlist(User user, String sort) {
        requireUser(user);
        List<Watchlist> entries = new ArrayList<>(watchlistDAO.findByUser(user.getId()));
        stockService.attachQuotes(entries.stream().map(Watchlist::getStock).toList());

        Comparator<Watchlist> byChange = Comparator.comparing(
                (Watchlist entry) -> entry.getStock().getPercentChange(),
                Comparator.nullsLast(Comparator.naturalOrder()));
        if (StockService.SORT_CHANGE_DESC.equals(sort)) {
            entries.sort(Comparator.comparing((Watchlist entry) -> entry.getStock().getPercentChange(),
                    Comparator.nullsLast(Comparator.reverseOrder())));
        } else if (StockService.SORT_CHANGE_ASC.equals(sort)) {
            entries.sort(byChange);
        } else if (StockService.SORT_SYMBOL.equals(sort)) {
            entries.sort(Comparator.comparing(entry -> entry.getStock().getSymbol()));
        }
        return entries;
    }

    public void add(User user, int stockId) {
        requireUser(user);
        Stock stock = stockDAO.findById(stockId);
        if (stock == null || !stock.isActive()) {
            throw new IllegalStateException("That stock is not available.");
        }
        // Duplicate check before the INSERT ...
        if (watchlistDAO.exists(user.getId(), stockId)) {
            throw new IllegalStateException(stock.getSymbol() + " is already in your watchlist.");
        }
        // ... and the UNIQUE constraint as the final guarantee.
        if (!watchlistDAO.insert(user.getId(), stockId)) {
            throw new IllegalStateException(stock.getSymbol() + " is already in your watchlist.");
        }
    }

    public void remove(User user, int stockId) {
        requireUser(user);
        if (!watchlistDAO.delete(user.getId(), stockId)) {
            throw new IllegalStateException("That stock is not in your watchlist.");
        }
    }

    public boolean isWatched(User user, int stockId) {
        return user != null && watchlistDAO.exists(user.getId(), stockId);
    }

    private static void requireUser(User user) {
        if (user == null) {
            throw new IllegalStateException("Please log in first.");
        }
    }
}
