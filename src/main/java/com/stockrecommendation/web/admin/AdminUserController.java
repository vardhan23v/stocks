package com.stockrecommendation.web.admin;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.stockrecommendation.model.User;
import com.stockrecommendation.security.AppUser;
import com.stockrecommendation.service.AdminService;

/** User management: activate, deactivate, change role, delete. */
@Controller
public class AdminUserController {

    private final AdminService adminService;

    public AdminUserController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/admin/users")
    public String users(@AuthenticationPrincipal AppUser principal, Model model) {
        var users = adminService.getUsers(principal.getUser());
        model.addAttribute("users", users);
        model.addAttribute("activeCount", users.stream().filter(User::isActive).count());
        model.addAttribute("adminCount", users.stream().filter(User::isAdmin).count());
        return "admin/users";
    }

    @PostMapping("/admin/users/{id}/active")
    public String active(@AuthenticationPrincipal AppUser principal, @PathVariable int id, @RequestParam boolean active,
                         RedirectAttributes redirect) {
        return run(redirect, () -> adminService.setUserActive(principal.getUser(), id, active),
                active ? "The account was activated." : "The account was deactivated.");
    }

    @PostMapping("/admin/users/{id}/role")
    public String role(@AuthenticationPrincipal AppUser principal, @PathVariable int id, @RequestParam String role,
                       RedirectAttributes redirect) {
        return run(redirect, () -> adminService.setUserRole(principal.getUser(), id, role), "The role was changed to " + role + ".");
    }

    @PostMapping("/admin/users/{id}/delete")
    public String delete(@AuthenticationPrincipal AppUser principal, @PathVariable int id, RedirectAttributes redirect) {
        return run(redirect, () -> adminService.deleteUser(principal.getUser(), id), "The account was deleted.");
    }

    private static String run(RedirectAttributes redirect, Runnable action, String success) {
        try {
            action.run();
            redirect.addFlashAttribute("success", success);
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/users";
    }
}
