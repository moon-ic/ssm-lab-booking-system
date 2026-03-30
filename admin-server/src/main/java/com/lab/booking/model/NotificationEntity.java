package com.lab.booking.model;

import java.time.LocalDateTime;

public class NotificationEntity {

    private Long notificationId;
    private Long userId;
    private NotificationType type;
    private String title;
    private String content;
    private String relatedBizType;
    private Long relatedBizId;
    private boolean read;
    private LocalDateTime createdAt;
    private LocalDateTime readAt;

    public Long getNotificationId() { return notificationId; }
    public void setNotificationId(Long notificationId) { this.notificationId = notificationId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public NotificationType getType() { return type; }
    public void setType(NotificationType type) { this.type = type; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getRelatedBizType() { return relatedBizType; }
    public void setRelatedBizType(String relatedBizType) { this.relatedBizType = relatedBizType; }
    public Long getRelatedBizId() { return relatedBizId; }
    public void setRelatedBizId(Long relatedBizId) { this.relatedBizId = relatedBizId; }
    public boolean isRead() { return read; }
    public void setRead(boolean read) { this.read = read; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getReadAt() { return readAt; }
    public void setReadAt(LocalDateTime readAt) { this.readAt = readAt; }
}
