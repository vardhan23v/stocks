package com.stockrecommendation.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ModelAttribute;

import com.stockrecommendation.model.User;
import com.stockrecommendation.security.AppUser;
import com.stockrecommendation.service.MarketDataService;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Values every page template needs (logged-in user, current path for the
 * active menu item, data provider status) and one friendly page for
 * database failures instead of a stack trace.
 */
@ControllerAdvice
public class GlobalModelAdvice {

    private static final Logger LOG = LoggerFactory.getLogger(GlobalModelAdvice.class);

    private final MarketDataService marketDataService;

    public GlobalModelAdvice(MarketDataService marketDataService) {
        this.marketDataService = marketDataService;
    }

    @ModelAttribute("currentUser")
    public User currentUser() {
        return CurrentUser.get();
    }

    @ModelAttribute("currentPath")
    public String currentPath(HttpServletRequest request) {
        return request.getRequestURI().substring(request.getContextPath().length());
    }

    /** Path plus query string, used as the "returnTo" of small forms. */
    @ModelAttribute("currentUrl")
    public String currentUrl(HttpServletRequest request) {
        String query = request.getQueryString();
        return currentPath(request) + (query == null ? "" : "?" + query);
    }

    @ModelAttribute("market")
    public MarketDataService market() {
        return marketDataService;
    }

    @ExceptionHandler(DataAccessException.class)
    public String databaseError(DataAccessException e, Model model) {
        LOG.error("Database error", e);
        model.addAttribute("currentUser", CurrentUser.get());
        model.addAttribute("market", marketDataService);
        model.addAttribute("currentPath", "");
        model.addAttribute("currentUrl", "");
        return "db-error";
    }

    /** Helper used by controllers and the advice. */
    public static final class CurrentUser {

        private CurrentUser() {
        }

        public static User get() {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            return authentication != null && authentication.getPrincipal() instanceof AppUser appUser
                    ? appUser.getUser() : null;
        }
    }
}
