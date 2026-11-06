package com.stockrecommendation.web;

import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import com.stockrecommendation.model.Stock;
import com.stockrecommendation.model.Watchlist;
import com.stockrecommendation.security.AppUser;
import com.stockrecommendation.service.StockDetailsService;
import com.stockrecommendation.service.StockDetailsView;
import com.stockrecommendation.service.StockService;
import com.stockrecommendation.service.WatchlistService;

/** Stock explorer (bookmarkable GET search) and stock details. */
@Controller
public class StockController {

    private final StockService stockService;
    private final WatchlistService watchlistService;
    private final StockDetailsService detailsService;

    public StockController(StockService stockService, WatchlistService watchlistService, StockDetailsService detailsService) {
        this.stockService = stockService;
        this.watchlistService = watchlistService;
        this.detailsService = detailsService;
    }

    @GetMapping("/stocks")
    public String stocks(@AuthenticationPrincipal AppUser principal,
                         @RequestParam(required = false) String q, @RequestParam(required = false) String sector,
                         @RequestParam(required = false) String exchange,
                         @RequestParam(required = false, defaultValue = StockService.SORT_SYMBOL) String sort, Model model) {
        var stocks = stockService.search(q, sector, exchange, sort);
        Set<Integer> watched = watchlistService.getWatchlist(principal.getUser(), null).stream()
                .map(Watchlist::getStockId).collect(Collectors.toSet());
        model.addAttribute("stocks", stocks);
        model.addAttribute("watchedIds", watched);
        model.addAttribute("sectors", stockService.getSectors());
        model.addAttribute("exchanges", stockService.getExchanges());
        model.addAttribute("q", q);
        model.addAttribute("sector", sector);
        model.addAttribute("exchange", exchange);
        model.addAttribute("sort", sort);
        model.addAttribute("filtered", (q != null && !q.isBlank()) || (sector != null && !sector.isBlank())
                || (exchange != null && !exchange.isBlank()));
        model.addAttribute("anySample", stocks.stream().anyMatch(Stock::isSampleData));
        return "stocks";
    }

    @GetMapping("/stocks/{id}")
    public String details(@AuthenticationPrincipal AppUser principal, @PathVariable int id,
                          @RequestParam(required = false) String range, Model model) {
        StockDetailsView view = detailsService.load(principal.getUser(), id, range);
        model.addAttribute("d", view);
        return "stock-details";
    }
}
