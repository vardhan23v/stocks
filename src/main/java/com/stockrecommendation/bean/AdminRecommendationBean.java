package com.stockrecommendation.bean;

import java.io.Serializable;
import java.util.List;

import com.stockrecommendation.model.Recommendation;
import com.stockrecommendation.model.RecommendationRule;
import com.stockrecommendation.service.AdminService;
import com.stockrecommendation.service.RecommendationService;
import com.stockrecommendation.util.DataAccessException;
import com.stockrecommendation.util.FacesUtil;
import com.stockrecommendation.util.SessionUtil;

import jakarta.annotation.PostConstruct;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;

/** Backing bean of admin-recommendations.xhtml: rule weights and generated recommendations. */
@Named("adminRecommendationBean")
@ViewScoped
public class AdminRecommendationBean implements Serializable {

    private static final long serialVersionUID = 1L;

    private final AdminService adminService = new AdminService();
    private final RecommendationService recommendationService = new RecommendationService();

    private List<RecommendationRule> rules = List.of();
    private List<Recommendation> recent = List.of();

    @PostConstruct
    public void init() {
        reload();
    }

    private void reload() {
        try {
            rules = adminService.getRules(SessionUtil.getCurrentUser());
            recent = recommendationService.getRecent(SessionUtil.getCurrentUser(), 40);
        } catch (SecurityException | DataAccessException e) {
            FacesUtil.addError(e.getMessage());
        }
    }

    /** Saves the rules, then re-evaluates every stock so the new weights take effect at once. */
    public void saveRules() {
        try {
            adminService.saveRules(SessionUtil.getCurrentUser(), rules);
            int count = recommendationService.regenerateAll(SessionUtil.getCurrentUser());
            FacesUtil.addInfo("Rules saved. " + count + " stocks were re-evaluated with the new rules.");
        } catch (SecurityException | IllegalArgumentException | DataAccessException e) {
            FacesUtil.addError(e.getMessage());
        }
        reload();
    }

    public void regenerate() {
        try {
            int count = recommendationService.regenerateAll(SessionUtil.getCurrentUser());
            FacesUtil.addInfo(count + " recommendations were generated from the stored data.");
        } catch (SecurityException | DataAccessException e) {
            FacesUtil.addError(e.getMessage());
        }
        reload();
    }

    public void delete(long recommendationId) {
        try {
            recommendationService.delete(SessionUtil.getCurrentUser(), recommendationId);
            FacesUtil.addInfo("The recommendation was deleted.");
        } catch (SecurityException | IllegalStateException | DataAccessException e) {
            FacesUtil.addError(e.getMessage());
        }
        reload();
    }

    public List<RecommendationRule> getRules() { return rules; }
    public List<Recommendation> getRecent() { return recent; }
}
