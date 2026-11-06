package com.stockrecommendation.web;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.stockrecommendation.model.Recommendation;
import com.stockrecommendation.service.RecommendationService;

/** All current recommendations, filterable by type, plus the scoring methodology. */
@Controller
public class RecommendationController {

    private static final List<String> TYPES = List.of(Recommendation.BUY, Recommendation.HOLD, Recommendation.SELL,
            Recommendation.INSUFFICIENT_DATA);

    private final RecommendationService recommendationService;

    public RecommendationController(RecommendationService recommendationService) {
        this.recommendationService = recommendationService;
    }

    @GetMapping("/recommendations")
    public String recommendations(@RequestParam(required = false) String type, Model model) {
        List<Recommendation> all = recommendationService.getAllCurrent();
        String filter = type != null && TYPES.contains(type) ? type : null;
        model.addAttribute("type", filter);
        model.addAttribute("types", TYPES);
        model.addAttribute("total", all.size());
        model.addAttribute("counts", TYPES.stream().collect(java.util.stream.Collectors.toMap(t -> t,
                t -> all.stream().filter(r -> t.equals(r.getRecommendationType())).count())));
        model.addAttribute("recommendations", filter == null ? all
                : all.stream().filter(r -> filter.equals(r.getRecommendationType())).toList());
        model.addAttribute("rules", recommendationService.getRules());
        return "recommendations";
    }
}
