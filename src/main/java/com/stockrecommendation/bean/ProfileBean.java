package com.stockrecommendation.bean;

import java.io.Serializable;

import com.stockrecommendation.model.User;
import com.stockrecommendation.service.AuthService;
import com.stockrecommendation.util.DataAccessException;
import com.stockrecommendation.util.FacesUtil;
import com.stockrecommendation.util.SessionUtil;

import jakarta.annotation.PostConstruct;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;

/** Backing bean of profile.xhtml: account details, name change, password change. */
@Named("profileBean")
@ViewScoped
public class ProfileBean implements Serializable {

    private static final long serialVersionUID = 1L;

    private final AuthService authService = new AuthService();

    private User profile;
    private String name;
    private String currentPassword;
    private String newPassword;
    private String confirmPassword;

    @PostConstruct
    public void init() {
        try {
            profile = authService.reload(SessionUtil.getCurrentUser());
            name = profile == null ? null : profile.getName();
        } catch (DataAccessException e) {
            FacesUtil.addError(e.getMessage());
        }
    }

    public String saveName() {
        try {
            authService.updateName(SessionUtil.getCurrentUser(), name);
            FacesUtil.keepMessagesForRedirect();
            FacesUtil.addInfo("Your name was updated.");
            return "profile?faces-redirect=true";
        } catch (IllegalArgumentException | DataAccessException e) {
            FacesUtil.addError(e.getMessage());
            return null;
        }
    }

    public String changePassword() {
        try {
            authService.changePassword(SessionUtil.getCurrentUser(), currentPassword, newPassword, confirmPassword);
            FacesUtil.keepMessagesForRedirect();
            FacesUtil.addInfo("Your password was changed.");
            return "profile?faces-redirect=true";
        } catch (IllegalArgumentException | DataAccessException e) {
            FacesUtil.addError(e.getMessage());
            return null;
        } finally {
            currentPassword = null;
            newPassword = null;
            confirmPassword = null;
        }
    }

    public User getProfile() { return profile; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCurrentPassword() { return currentPassword; }
    public void setCurrentPassword(String currentPassword) { this.currentPassword = currentPassword; }

    public String getNewPassword() { return newPassword; }
    public void setNewPassword(String newPassword) { this.newPassword = newPassword; }

    public String getConfirmPassword() { return confirmPassword; }
    public void setConfirmPassword(String confirmPassword) { this.confirmPassword = confirmPassword; }
}
