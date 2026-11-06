package com.stockrecommendation.web.admin;

import java.time.LocalDateTime;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.stockrecommendation.model.User;
import com.stockrecommendation.security.AppUser;
import com.stockrecommendation.service.AdminService;
import com.stockrecommendation.service.MarketDataService;
import com.stockrecommendation.service.StockService;

/** Administrator overview. /admin/** needs role ADMIN (SecurityConfig). */
@Controller
public class AdminDashboardController {

    private final AdminService adminService;
    private final StockService stockService;
    private final MarketDataService marketDataService;

    public AdminDashboardController(AdminService adminService, StockService stockService, MarketDataService marketDataService) {
        this.adminService = adminService;
        this.stockService = stockService;
        this.marketDataService = marketDataService;
    }

    @GetMapping("/admin")
    public String dashboard(@AuthenticationPrincipal AppUser principal, @RequestParam(required = false) String welcome,
                            Model model) {
        User admin = principal.getUser();
        if (welcome != null) {
            model.addAttribute("success", "Welcome back, " + admin.getName() + ".");
        }
        model.addAttribute("userCount", adminService.countUsers(admin));
        model.addAttribute("stockCount", stockService.countActive());
        model.addAttribute("watchlistCount", adminService.countWatchlistEntries(admin));
        model.addAttribute("recommendationCount", adminService.countRecommendations(admin));
        model.addAttribute("priceRowCount", adminService.countPriceRows(admin));
        LocalDateTime latestBar = null;
        String latestBarSource = null;
        for (Object[] row : adminService.getPriceSummary(admin)) {
            LocalDateTime newest = (LocalDateTime) row[3];
            if (newest != null && (latestBar == null || newest.isAfter(latestBar))) {
                latestBar = newest;
                latestBarSource = (String) row[0];
            }
        }
        model.addAttribute("latestBar", latestBar);
        model.addAttribute("latestBarSource", latestBarSource);
        model.addAttribute("latestSync", adminService.getLatestSync(admin));
        model.addAttribute("latestSuccess", adminService.getLatestSuccessfulSync(admin));
        model.addAttribute("callsToday", marketDataService.countCallsToday());
        model.addAttribute("activity", adminService.getActivity(admin, 12));
        model.addAttribute("syncLogs", adminService.getSyncLogs(admin, 6));
        return "admin/dashboard";
    }
}
