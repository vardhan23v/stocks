package com.stockrecommendation.model;

import java.io.Serializable;
import java.time.LocalDateTime;

/** One line of the system activity trail. */
public class ActivityLog implements Serializable {

    private static final long serialVersionUID = 1L;

    private long id;
    private Integer userId;
    private String userName;
    private String action;
    private String details;
    private LocalDateTime createdAt;

    public ActivityLog() {
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
