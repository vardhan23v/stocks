package com.stockrecommendation.web.admin;

import java.util.ArrayList;
import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.stockrecommendation.model.RecommendationRule;
import com.stockrecommendation.security.AppUser;
import com.stockrecommendation.service.AdminService;
import com.stockrecommendation.service.RecommendationService;

/** Recommendation rules (weights, thresholds) and the generated recommendations. */
@Controller
public class AdminRulesController {

    /** Form object for editing all rules at once (rules[0].weight, ...). */
    public static class RulesForm {

        private List<RecommendationRule> rules = new ArrayList<>();

        public List<RecommendationRule> getRules() { return rules; }
        public void setRules(List<RecommendationRule> rules) { this.rules = rules; }
    }

    private final AdminService adminService;
    private final RecommendationService recommendationService;

    public AdminRulesController(AdminService adminService, RecommendationService recommendationService) {
        this.adminService = adminService;
        this.recommendationService = recommendationService;
    }

    @GetMapping("/admin/rules")
    public String rules(@AuthenticationPrincipal AppUser principal, Model model) {
        RulesForm form = new RulesForm();
        form.setRules(adminService.getRules(principal.getUser()));
        model.addAttribute("rulesForm", form);
        model.addAttribute("recent", recommendationService.getRecent(principal.getUser(), 40));
        return "admin/rules";
    }

    /** Saves the rules in one transaction, then re-evaluates every stock with them. */
    @PostMapping("/admin/rules")
    public String save(@AuthenticationPrincipal AppUser principal, @ModelAttribute RulesForm rulesForm, RedirectAttributes redirect) {
        // Names and keys come from the database, never from the form.
        List<RecommendationRule> stored = adminService.getRules(principal.getUser());
        for (RecommendationRule rule : stored) {
            rulesForm.getRules().stream().filter(edited -> edited.getId() == rule.getId()).findFirst().ifPresent(edited -> {
                if (!RecommendationRule.DECISION.equals(rule.getRuleKey())) {
                    rule.setWeight(edited.getWeight());
                    rule.setEnabled(edited.isEnabled());
                }
                if (!RecommendationRule.MACD.equals(rule.getRuleKey())) {
                    rule.setParamLow(edited.getParamLow());
                }
                if (RecommendationRule.RSI.equals(rule.getRuleKey()) || RecommendationRule.DECISION.equals(rule.getRuleKey())) {
                    rule.setParamHigh(edited.getParamHigh());
                }
            });
        }
        try {
            adminService.saveRules(principal.getUser(), stored);
            int count = recommendationService.regenerateAll(principal.getUser());
            redirect.addFlashAttribute("success", "Rules saved. " + count + " stocks were re-evaluated with the new rules.");
        } catch (IllegalArgumentException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/rules";
    }

    @PostMapping("/admin/rules/regenerate")
    public String regenerate(@AuthenticationPrincipal AppUser principal, RedirectAttributes redirect) {
        int count = recommendationService.regenerateAll(principal.getUser());
        redirect.addFlashAttribute("success", count + " recommendations were generated from the stored data.");
        return "redirect:/admin/rules";
    }

    @PostMapping("/admin/recommendations/{id}/delete")
    public String delete(@AuthenticationPrincipal AppUser principal, @PathVariable long id, RedirectAttributes redirect) {
        try {
            recommendationService.delete(principal.getUser(), id);
            redirect.addFlashAttribute("success", "The recommendation was deleted.");
        } catch (IllegalStateException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/rules";
    }
}
