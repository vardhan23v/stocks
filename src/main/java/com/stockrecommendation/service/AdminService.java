package com.stockrecommendation.service;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

import com.stockrecommendation.dao.ActivityDAO;
import com.stockrecommendation.dao.RecommendationDAO;
import com.stockrecommendation.dao.RuleDAO;
import com.stockrecommendation.dao.StockPriceDAO;
import com.stockrecommendation.dao.SyncLogDAO;
import com.stockrecommendation.dao.UserDAO;
import com.stockrecommendation.dao.WatchlistDAO;
import com.stockrecommendation.model.ActivityLog;
import com.stockrecommendation.model.ApiSyncLog;
import com.stockrecommendation.model.RecommendationRule;
import com.stockrecommendation.model.User;

/**
 * Administrator-only operations. Every public method starts with
 * requireAdmin(): the AuthFilter already blocks the admin pages, and this is
 * the second, independent check on the server.
 */
public class AdminService implements Serializable {

    private static final long serialVersionUID = 1L;


    private final UserDAO userDAO = new UserDAO();
    private final WatchlistDAO watchlistDAO = new WatchlistDAO();
    private final RecommendationDAO recommendationDAO = new RecommendationDAO();
    private final StockPriceDAO priceDAO = new StockPriceDAO();
    private final SyncLogDAO syncLogDAO = new SyncLogDAO();
    private final RuleDAO ruleDAO = new RuleDAO();
    private final ActivityDAO activityDAO = new ActivityDAO();

    public static void requireAdmin(User user) {
        if (user == null || !User.ROLE_ADMIN.equals(user.getRole())) {
            throw new SecurityException("Only administrators can do this.");
        }
    }

    // ------------------------------------------------------------ statistics

    public int countUsers(User admin) {
        requireAdmin(admin);
        return userDAO.count();
    }

    public int countWatchlistEntries(User admin) {
        requireAdmin(admin);
        return watchlistDAO.countAll();
    }

    public int countRecommendations(User admin) {
        requireAdmin(admin);
        return recommendationDAO.count();
    }

    public long countPriceRows(User admin) {
        requireAdmin(admin);
        return priceDAO.countAll();
    }

    public List<Object[]> getPriceSummary(User admin) {
        requireAdmin(admin);
        return priceDAO.summariseBySource();
    }

    public ApiSyncLog getLatestSync(User admin) {
        requireAdmin(admin);
        return syncLogDAO.findLatest();
    }

    public ApiSyncLog getLatestSuccessfulSync(User admin) {
        requireAdmin(admin);
        return syncLogDAO.findLatestSuccess();
    }

    public List<ApiSyncLog> getSyncLogs(User admin, int limit) {
        requireAdmin(admin);
        return syncLogDAO.findRecent(limit);
    }

    public List<ActivityLog> getActivity(User admin, int limit) {
        requireAdmin(admin);
        return activityDAO.findRecent(limit);
    }

    // ------------------------------------------------------------ users

    public List<User> getUsers(User admin) {
        requireAdmin(admin);
        return userDAO.findAll();
    }

    public void setUserActive(User admin, int userId, boolean active) {
        requireAdmin(admin);
        refuseSelf(admin, userId, "deactivate");
        if (!userDAO.updateActive(userId, active)) {
            throw new IllegalStateException("That user no longer exists.");
        }
        activityDAO.log(admin.getId(), active ? "USER_ACTIVATED" : "USER_DEACTIVATED", "User #" + userId);
    }

    public void setUserRole(User admin, int userId, String role) {
        requireAdmin(admin);
        refuseSelf(admin, userId, "change the role of");
        if (!User.ROLE_USER.equals(role) && !User.ROLE_ADMIN.equals(role)) {
            throw new IllegalArgumentException("Unknown role.");
        }
        if (!userDAO.updateRole(userId, role)) {
            throw new IllegalStateException("That user no longer exists.");
        }
        activityDAO.log(admin.getId(), "USER_ROLE_CHANGED", "User #" + userId + " -> " + role);
    }

    public void deleteUser(User admin, int userId) {
        requireAdmin(admin);
        refuseSelf(admin, userId, "delete");
        if (!userDAO.delete(userId)) {
            throw new IllegalStateException("That user no longer exists.");
        }
        activityDAO.log(admin.getId(), "USER_DELETED", "User #" + userId);
    }

    private static void refuseSelf(User admin, int userId, String verb) {
        if (admin.getId() == userId) {
            throw new IllegalStateException("You cannot " + verb + " your own account.");
        }
    }

    // ------------------------------------------------------------ rules

    public List<RecommendationRule> getRules(User admin) {
        requireAdmin(admin);
        return ruleDAO.findAll();
    }

    /** Validates and saves all rules in one transaction. */
    public void saveRules(User admin, List<RecommendationRule> rules) {
        requireAdmin(admin);
        for (RecommendationRule rule : rules) {
            if (rule.getWeight() == null || rule.getWeight().compareTo(BigDecimal.ZERO) < 0
                    || rule.getWeight().compareTo(BigDecimal.valueOf(100)) > 0) {
                throw new IllegalArgumentException(rule.getName() + ": the weight must be between 0 and 100.");
            }
            if (RecommendationRule.RSI.equals(rule.getRuleKey()) && !inOrder(rule, 0, 100)) {
                throw new IllegalArgumentException("RSI zones: the lower band must be below the upper band, both within 0-100.");
            }
            if (RecommendationRule.DECISION.equals(rule.getRuleKey())
                    && (rule.getParamLow() == null || rule.getParamHigh() == null
                        || rule.getParamLow().signum() >= 0 || rule.getParamHigh().signum() <= 0)) {
                throw new IllegalArgumentException("Decision thresholds: SELL must be below 0 and BUY above 0.");
            }
        }
        ruleDAO.updateAll(rules);
        activityDAO.log(admin.getId(), "RULES_UPDATED", rules.size() + " rules saved");
    }

    private static boolean inOrder(RecommendationRule rule, double min, double max) {
        return rule.getParamLow() != null && rule.getParamHigh() != null
            && rule.getParamLow().doubleValue() >= min && rule.getParamHigh().doubleValue() <= max
            && rule.getParamLow().compareTo(rule.getParamHigh()) < 0;
    }
}
