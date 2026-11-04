package com.stockrecommendation.bean;

import com.stockrecommendation.model.User;
import com.stockrecommendation.service.AuthService;
import com.stockrecommendation.util.DataAccessException;
import com.stockrecommendation.util.FacesUtil;
import com.stockrecommendation.util.SessionUtil;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Named;

/**
 * Backing bean of login.xhtml and of the navigation menu.
 *
 * @RequestScoped: the form fields only live for one request. The logged-in
 * user is kept in the HTTP session (see SessionUtil), not in this bean.
 */
@Named("loginBean")
@RequestScoped
public class LoginBean {

    private final AuthService authService = new AuthService();

    private String email;
    private String password;

    /** One-time message from AuthFilter, shown after an unauthorized page request. */
    private String authMessage;
    private boolean authMessageLoaded;

    /**
     * Action method of the login form.
     *
     * @return "loginSuccess" / "adminLoginSuccess" or "loginFailure" - mapped
     *         to pages in faces-config.xml
     */
    public String login() {
        try {
            User user = authService.login(email, password);
            password = null;   // never keep the password in memory
            if (user == null) {
                FacesUtil.addError("Invalid email or password.");
                return "loginFailure";
            }
            SessionUtil.setCurrentUser(user);
            FacesUtil.keepMessagesForRedirect();
            FacesUtil.addInfo("Welcome back, " + user.getName() + ".");
            return user.isAdmin() ? "adminLoginSuccess" : "loginSuccess";
        } catch (IllegalStateException | DataAccessException e) {
            // deactivated account, or for example: MySQL is not running
            FacesUtil.addError(e.getMessage());
            return "loginFailure";
        }
    }

    /** Action method of the Logout link: drops the whole session. */
    public String logout() {
        SessionUtil.clearSession();
        return "logout";
    }

    // ------------------------------------------------- used by the template

    public boolean isLoggedIn() {
        return SessionUtil.getCurrentUser() != null;
    }

    public User getCurrentUser() {
        return SessionUtil.getCurrentUser();
    }

    public boolean isAdmin() {
        return SessionUtil.hasRole(User.ROLE_ADMIN);
    }

    /** Initials for the avatar in the header. */
    public String getInitials() {
        User user = getCurrentUser();
        if (user == null || user.getName() == null || user.getName().isBlank()) {
            return "?";
        }
        // Only words that start with a letter: "Asha Rao (Admin)" -> "AR".
        String[] parts = java.util.Arrays.stream(user.getName().trim().split("\\s+"))
                .filter(part -> Character.isLetter(part.charAt(0)))
                .toArray(String[]::new);
        if (parts.length == 0) {
            return "?";
        }
        String initials = parts[0].substring(0, 1)
                + (parts.length > 1 ? parts[parts.length - 1].substring(0, 1) : "");
        return initials.toUpperCase();
    }

    /** The message AuthFilter left behind; read once and then removed from the session. */
    public String getAuthMessage() {
        if (!authMessageLoaded) {
            authMessage = SessionUtil.consumeAuthMessage();
            authMessageLoaded = true;
        }
        return authMessage;
    }

    // ------------------------------------------------------ form properties

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}
