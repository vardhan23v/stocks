package com.stockrecommendation.web.admin;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.stockrecommendation.model.ApiSyncLog;
import com.stockrecommendation.model.Stock;
import com.stockrecommendation.security.AppUser;
import com.stockrecommendation.service.AdminService;
import com.stockrecommendation.service.MarketDataService;
import com.stockrecommendation.service.StockService;

/** Provider configuration, stored data, manual syncs and the sync log. The API key is never shown. */
@Controller
public class AdminMarketDataController {

    private final AdminService adminService;
    private final StockService stockService;
    private final MarketDataService marketDataService;

    public AdminMarketDataController(AdminService adminService, StockService stockService, MarketDataService marketDataService) {
        this.adminService = adminService;
        this.stockService = stockService;
        this.marketDataService = marketDataService;
    }

    @GetMapping("/admin/market-data")
    public String page(@AuthenticationPrincipal AppUser principal, Model model) {
        model.addAttribute("stocks", stockService.findAllActiveWithQuotes());
        model.addAttribute("logs", adminService.getSyncLogs(principal.getUser(), 60));
        model.addAttribute("summary", adminService.getPriceSummary(principal.getUser()));
        model.addAttribute("callsToday", marketDataService.countCallsToday());
        return "admin/market-data";
    }

    @PostMapping("/admin/market-data/sync/{id}")
    public String sync(@AuthenticationPrincipal AppUser principal, @PathVariable int id,
                       @RequestParam(defaultValue = "false") boolean force, RedirectAttributes redirect) {
        Stock stock = stockService.getStock(id);
        if (stock == null) {
            redirect.addFlashAttribute("error", "That stock no longer exists.");
            return "redirect:/admin/market-data";
        }
        MarketDataService.SyncResult result = marketDataService.refresh(principal.getUser(), stock, force);
        if (ApiSyncLog.SUCCESS.equals(result.status()) || ApiSyncLog.CACHED.equals(result.status())) {
            redirect.addFlashAttribute("success", result.message());
        } else if (ApiSyncLog.DEMO.equals(result.status())) {
            redirect.addFlashAttribute("warning", result.message());
        } else {
            redirect.addFlashAttribute("error", result.status() + ": " + result.message() + " Cached data is kept.");
        }
        return "redirect:/admin/market-data";
    }

    @PostMapping("/admin/market-data/sync-all")
    public String syncAll(@AuthenticationPrincipal AppUser principal, RedirectAttributes redirect) {
        if (marketDataService.startBulkSync(principal.getUser())) {
            redirect.addFlashAttribute("success", "Background sync started. Stale stocks are fetched one by one within "
                    + "the provider limits; reload this page to follow the log.");
        } else if (marketDataService.isDemoMode()) {
            redirect.addFlashAttribute("warning", "Demo mode is active, so nothing was requested from a provider.");
        } else {
            redirect.addFlashAttribute("warning", "A background sync is already running.");
        }
        return "redirect:/admin/market-data";
    }
}
