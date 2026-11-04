package com.stockrecommendation.service;

import java.io.Serializable;
import java.util.Locale;
import java.util.regex.Pattern;

import com.stockrecommendation.dao.ActivityDAO;
import com.stockrecommendation.dao.UserDAO;
import com.stockrecommendation.model.User;
import com.stockrecommendation.util.DataAccessException;
import com.stockrecommendation.util.PasswordUtil;

/**
 * Login, registration and profile logic.
 *
 * Login returns null for every kind of failure (unknown email, wrong password,
 * empty input) so the login page shows one single message and an attacker
 * learns nothing about which emails exist. Registration validates its input
 * again here: the HTML form is only a convenience, never the security check.
 */
public class AuthService implements Serializable {

    private static final long serialVersionUID = 1L;


    private static final Pattern EMAIL = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]{2,}$");

    private final UserDAO userDAO = new UserDAO();
    private final ActivityDAO activityDAO = new ActivityDAO();

    /**
     * @return the logged-in User, or null when the credentials are not valid
     * @throws IllegalStateException when the password is right but the account was deactivated
     */
    public User login(String email, String password) {
        if (email == null || password == null || email.isBlank() || password.isEmpty()) {
            return null;
        }
        User user = userDAO.findByEmail(email.trim().toLowerCase(Locale.ROOT));
        if (user == null || !PasswordUtil.matches(password, user.getPasswordHash())) {
            return null;
        }
        if (!user.isActive()) {
            throw new IllegalStateException("This account has been deactivated. Please contact the administrator.");
        }
        user.setPasswordHash(null);   // the hash is not needed in the session
        activityDAO.log(user.getId(), "LOGIN", user.getEmail());
        return user;
    }

    /**
     * Creates a regular USER account. Administrators cannot be self-registered.
     *
     * @throws IllegalArgumentException with a message safe to show to the user
     * @throws DataAccessException      when the database rejects the insert
     */
    public User register(String name, String email, String password, String confirmPassword) {
        String cleanName = name == null ? "" : name.trim();
        String cleanEmail = email == null ? "" : email.trim().toLowerCase(Locale.ROOT);

        validateName(cleanName);
        if (cleanEmail.isEmpty() || cleanEmail.length() > 150 || !EMAIL.matcher(cleanEmail).matches()) {
            throw new IllegalArgumentException("Enter a valid email address.");
        }
        validatePassword(password, confirmPassword);
        if (userDAO.findByEmail(cleanEmail) != null) {
            throw new IllegalArgumentException("An account with that email already exists.");
        }

        User user = new User();
        user.setName(cleanName);
        user.setEmail(cleanEmail);
        user.setPasswordHash(PasswordUtil.hash(password));
        user.setRole(User.ROLE_USER);
        userDAO.create(user);   // UNIQUE(email) catches a simultaneous duplicate
        user.setPasswordHash(null);
        activityDAO.log(user.getId(), "REGISTER", cleanEmail);
        return user;
    }

    /** Changes the display name of the logged-in user. */
    public void updateName(User user, String name) {
        String cleanName = name == null ? "" : name.trim();
        validateName(cleanName);
        userDAO.updateName(user.getId(), cleanName);
        user.setName(cleanName);
    }

    /** Changes the password after checking the current one. */
    public void changePassword(User user, String currentPassword, String newPassword, String confirmPassword) {
        User stored = userDAO.findById(user.getId());
        if (stored == null || !PasswordUtil.matches(currentPassword, stored.getPasswordHash())) {
            throw new IllegalArgumentException("The current password is not correct.");
        }
        validatePassword(newPassword, confirmPassword);
        userDAO.updatePassword(user.getId(), PasswordUtil.hash(newPassword));
        activityDAO.log(user.getId(), "PASSWORD_CHANGED", null);
    }

    /** Fresh copy of the user from the database (for the profile page). */
    public User reload(User user) {
        User stored = userDAO.findById(user.getId());
        if (stored != null) {
            stored.setPasswordHash(null);
        }
        return stored;
    }

    private static void validateName(String name) {
        if (name.length() < 2 || name.length() > 100) {
            throw new IllegalArgumentException("Please enter your name (2 to 100 characters).");
        }
    }

    private static void validatePassword(String password, String confirmPassword) {
        if (password == null || password.length() < 8 || password.length() > 128) {
            throw new IllegalArgumentException("Choose a password with 8 to 128 characters.");
        }
        if (!password.matches(".*[A-Za-z].*") || !password.matches(".*\\d.*")) {
            throw new IllegalArgumentException("The password needs at least one letter and one digit.");
        }
        if (!password.equals(confirmPassword)) {
            throw new IllegalArgumentException("The two passwords do not match.");
        }
    }
}
