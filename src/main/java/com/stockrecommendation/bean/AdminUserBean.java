package com.stockrecommendation.bean;

import java.io.Serializable;
import java.util.List;

import com.stockrecommendation.model.User;
import com.stockrecommendation.service.AdminService;
import com.stockrecommendation.util.DataAccessException;
import com.stockrecommendation.util.FacesUtil;
import com.stockrecommendation.util.SessionUtil;

import jakarta.annotation.PostConstruct;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;

/** Backing bean of admin-users.xhtml: activate, deactivate, change role, delete. */
@Named("adminUserBean")
@ViewScoped
public class AdminUserBean implements Serializable {

    private static final long serialVersionUID = 1L;

    private final AdminService adminService = new AdminService();

    private List<User> users = List.of();

    @PostConstruct
    public void init() {
        reload();
    }

    private void reload() {
        try {
            users = adminService.getUsers(SessionUtil.getCurrentUser());
        } catch (SecurityException | DataAccessException e) {
            FacesUtil.addError(e.getMessage());
        }
    }

    public void changeActive(int userId, boolean active) {
        run(() -> adminService.setUserActive(SessionUtil.getCurrentUser(), userId, active),
                active ? "The account was activated." : "The account was deactivated.");
    }

    public void changeRole(int userId, String role) {
        run(() -> adminService.setUserRole(SessionUtil.getCurrentUser(), userId, role), "The role was changed to " + role + ".");
    }

    public void delete(int userId) {
        run(() -> adminService.deleteUser(SessionUtil.getCurrentUser(), userId), "The account was deleted.");
    }

    public boolean isSelf(int userId) {
        User current = SessionUtil.getCurrentUser();
        return current != null && current.getId() == userId;
    }

    public long getActiveCount() {
        return users.stream().filter(User::isActive).count();
    }

    public long getAdminCount() {
        return users.stream().filter(User::isAdmin).count();
    }

    private void run(Runnable action, String success) {
        try {
            action.run();
            FacesUtil.addInfo(success);
        } catch (SecurityException | IllegalArgumentException | IllegalStateException | DataAccessException e) {
            FacesUtil.addError(e.getMessage());
        }
        reload();
    }

    public List<User> getUsers() { return users; }
}
