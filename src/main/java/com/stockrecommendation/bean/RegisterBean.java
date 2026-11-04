package com.stockrecommendation.bean;

import com.stockrecommendation.model.User;
import com.stockrecommendation.service.AuthService;
import com.stockrecommendation.util.DataAccessException;
import com.stockrecommendation.util.FacesUtil;
import com.stockrecommendation.util.SessionUtil;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Named;

/** Backing bean of register.xhtml: creates a USER account and logs it in. */
@Named("registerBean")
@RequestScoped
public class RegisterBean {

    private final AuthService authService = new AuthService();

    private String name;
    private String email;
    private String password;
    private String confirmPassword;

    public String register() {
        try {
            User user = authService.register(name, email, password, confirmPassword);
            password = null;
            confirmPassword = null;
            SessionUtil.setCurrentUser(user);
            FacesUtil.keepMessagesForRedirect();
            FacesUtil.addInfo("Welcome to StockSense, " + user.getName() + ". Your account is ready.");
            return "/dashboard.xhtml?faces-redirect=true";
        } catch (IllegalArgumentException | DataAccessException e) {
            FacesUtil.addError(e.getMessage());
            return null;
        }
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getConfirmPassword() { return confirmPassword; }
    public void setConfirmPassword(String confirmPassword) { this.confirmPassword = confirmPassword; }
}
