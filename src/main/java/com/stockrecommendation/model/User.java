package com.stockrecommendation.model;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * A person who can log in: a regular user or an administrator.
 * Plain data holder - all behaviour lives in the service and bean classes.
 * Serializable because the logged-in User is kept in the HTTP session.
 */
public class User implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String ROLE_USER = "USER";
    public static final String ROLE_ADMIN = "ADMIN";

    private int id;
    private String name;
    private String email;
    private String passwordHash;
    private String role;
    private boolean active = true;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /** Number of watchlist entries - filled in only by the admin user list. */
    private int watchlistCount;

    public User() {
    }

    public boolean isAdmin() {
        return ROLE_ADMIN.equals(role);
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public int getWatchlistCount() { return watchlistCount; }
    public void setWatchlistCount(int watchlistCount) { this.watchlistCount = watchlistCount; }
}
