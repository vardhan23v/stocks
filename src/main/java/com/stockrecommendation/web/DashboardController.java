package com.stockrecommendation.web;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.stockrecommendation.security.AppUser;
import com.stockrecommendation.service.DashboardService;

/** The personal dashboard. */
@Controller
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal AppUser principal, @RequestParam(required = false) String denied,
                            @RequestParam(required = false) String welcome, Model model) {
        if (denied != null) {
            model.addAttribute("error", "You are not allowed to open that page.");
        } else if (welcome != null) {
            model.addAttribute("success", "Welcome back, " + principal.getUser().getName() + ".");
        }
        dashboardService.fill(principal.getUser(), model);
        return "dashboard";
    }
}
