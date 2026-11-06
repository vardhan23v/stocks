package com.stockrecommendation.web;

import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.stockrecommendation.model.Watchlist;
import com.stockrecommendation.security.AppUser;
import com.stockrecommendation.service.StockService;
import com.stockrecommendation.service.WatchlistService;

/**
 * The private watchlist. The user is ALWAYS the logged-in principal - there is
 * no user id parameter, so nobody can read or change someone else's list.
 */
@Controller
public class WatchlistController {

    private final WatchlistService watchlistService;
    private final StockService stockService;

    public WatchlistController(WatchlistService watchlistService, StockService stockService) {
        this.watchlistService = watchlistService;
        this.stockService = stockService;
    }

    @GetMapping("/watchlist")
    public String watchlist(@AuthenticationPrincipal AppUser principal,
                            @RequestParam(required = false, defaultValue = StockService.SORT_CHANGE_DESC) String sort,
                            Model model) {
        var entries = watchlistService.getWatchlist(principal.getUser(), sort);
        Set<Integer> watched = entries.stream().map(Watchlist::getStockId).collect(Collectors.toSet());
        model.addAttribute("entries", entries);
        model.addAttribute("sort", sort);
        model.addAttribute("availableStocks", stockService.search(null, null, null, StockService.SORT_SYMBOL).stream()
                .filter(stock -> !watched.contains(stock.getId())).toList());
        model.addAttribute("gainersCount", entries.stream().filter(e -> "up".equals(e.getStock().getDirection())).count());
        model.addAttribute("decliningCount", entries.stream().filter(e -> "down".equals(e.getStock().getDirection())).count());
        return "watchlist";
    }

    @PostMapping("/watchlist/add")
    public String add(@AuthenticationPrincipal AppUser principal, @RequestParam(required = false) Integer stockId,
                      @RequestParam(required = false) String returnTo, RedirectAttributes redirect) {
        if (stockId == null) {
            redirect.addFlashAttribute("error", "Choose a stock to add.");
        } else {
            try {
                watchlistService.add(principal.getUser(), stockId);
                redirect.addFlashAttribute("success", "Added to your watchlist.");
            } catch (IllegalStateException e) {
                redirect.addFlashAttribute("error", e.getMessage());
            }
        }
        return "redirect:" + safeReturn(returnTo);
    }

    @PostMapping("/watchlist/remove")
    public String remove(@AuthenticationPrincipal AppUser principal, @RequestParam int stockId,
                         @RequestParam(required = false) String returnTo, RedirectAttributes redirect) {
        try {
            watchlistService.remove(principal.getUser(), stockId);
            redirect.addFlashAttribute("success", "Removed from your watchlist.");
        } catch (IllegalStateException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:" + safeReturn(returnTo);
    }

    /** Only same-site paths, never "//evil.example" or a full URL. */
    static String safeReturn(String returnTo) {
        if (returnTo == null || !returnTo.startsWith("/") || returnTo.startsWith("//") || returnTo.contains("\\")
                || returnTo.contains("\r") || returnTo.contains("\n")) {
            return "/watchlist";
        }
        return returnTo;
    }
}
