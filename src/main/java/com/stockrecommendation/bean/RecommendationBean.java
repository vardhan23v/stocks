package com.stockrecommendation.bean;

import java.util.List;

import com.stockrecommendation.model.Recommendation;
import com.stockrecommendation.model.RecommendationRule;
import com.stockrecommendation.service.RecommendationService;
import com.stockrecommendation.util.DataAccessException;
import com.stockrecommendation.util.FacesUtil;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Named;

/** Backing bean of recommendations.xhtml?type=BUY. */
@Named("recommendationBean")
@RequestScoped
public class RecommendationBean {

    private final RecommendationService recommendationService = new RecommendationService();

    private String type;
    private List<Recommendation> all = List.of();
    private List<RecommendationRule> rules = List.of();

    public void load() {
        try {
            all = recommendationService.getAllCurrent();
            rules = recommendationService.getRules();
        } catch (DataAccessException e) {
            FacesUtil.addError(e.getMessage());
        }
    }

    public List<Recommendation> getRecommendations() {
        if (type == null || type.isBlank()) {
            return all;
        }
        return all.stream().filter(r -> type.equals(r.getRecommendationType())).toList();
    }

    public long count(String recommendationType) {
        return all.stream().filter(r -> recommendationType.equals(r.getRecommendationType())).count();
    }

    public int getTotal() {
        return all.size();
    }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public List<RecommendationRule> getRules() { return rules; }
}
