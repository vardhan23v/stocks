package com.stockrecommendation.web;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.stockrecommendation.security.AppUser;
import com.stockrecommendation.service.AuthService;

/** Profile: account details, name change, password change. */
@Controller
public class ProfileController {

    private final AuthService authService;

    public ProfileController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/profile")
    public String profile(@AuthenticationPrincipal AppUser principal, Model model) {
        model.addAttribute("profile", authService.reload(principal.getUser()));
        return "profile";
    }

    @PostMapping("/profile/name")
    public String saveName(@AuthenticationPrincipal AppUser principal, @RequestParam String name, RedirectAttributes redirect) {
        try {
            authService.updateName(principal.getUser(), name);   // also updates the user held in the session
            redirect.addFlashAttribute("success", "Your name was updated.");
        } catch (IllegalArgumentException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/profile";
    }

    @PostMapping("/profile/password")
    public String changePassword(@AuthenticationPrincipal AppUser principal, @RequestParam String currentPassword,
                                 @RequestParam String newPassword, @RequestParam String confirmPassword,
                                 RedirectAttributes redirect) {
        try {
            authService.changePassword(principal.getUser(), currentPassword, newPassword, confirmPassword);
            redirect.addFlashAttribute("success", "Your password was changed.");
        } catch (IllegalArgumentException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/profile";
    }
}
