package com.stockrecommendation.web;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.stockrecommendation.model.User;
import com.stockrecommendation.security.AppUser;
import com.stockrecommendation.service.AuthService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Login page (the POST itself is handled by Spring Security) and registration.
 * A new account is logged in straight away.
 */
@Controller
public class AuthController {

    private final AuthService authService;
    private final SecurityContextRepository contextRepository = new HttpSessionSecurityContextRepository();

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/login")
    public String login(@RequestParam(required = false) String error, @RequestParam(required = false) String logout,
                        @RequestParam(required = false) String disabled, Model model) {
        if (error != null) {
            model.addAttribute("error", "Invalid email or password.");
        } else if (disabled != null) {
            model.addAttribute("error", "This account has been deactivated. Please contact the administrator.");
        } else if (logout != null) {
            model.addAttribute("success", "You have been logged out.");
        }
        return "login";
    }

    @GetMapping("/register")
    public String registerForm() {
        return "register";
    }

    @PostMapping("/register")
    public String register(@RequestParam String name, @RequestParam String email, @RequestParam String password,
                           @RequestParam String confirmPassword, Model model, RedirectAttributes redirect,
                           HttpServletRequest request, HttpServletResponse response) {
        User user;
        try {
            user = authService.register(name, email, password, confirmPassword);
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("name", name);
            model.addAttribute("email", email);
            return "register";
        }
        // Log the new user in: a fresh session id, then the authentication stored in it.
        request.getSession(true);
        request.changeSessionId();
        AppUser principal = new AppUser(user);
        Authentication authentication = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        contextRepository.saveContext(context, request, response);

        redirect.addFlashAttribute("success", "Welcome to StockSense, " + user.getName() + ". Your account is ready.");
        return "redirect:/dashboard";
    }
}
