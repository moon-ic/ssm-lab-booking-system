package com.lab.booking.service;

import com.lab.booking.common.ApiException;
import com.lab.booking.model.NotificationEntity;
import com.lab.booking.model.NotificationType;
import com.lab.booking.model.RoleCode;
import com.lab.booking.model.UserEntity;
import com.lab.booking.repository.NotificationRepository;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class NotificationService {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final NotificationRepository notificationRepository;
    private final AuthService authService;

    public NotificationService(NotificationRepository notificationRepository, @Lazy AuthService authService) {
        this.notificationRepository = notificationRepository;
        this.authService = authService;
    }

    public boolean createSystemNotificationIfAbsent(
            Long userId,
            NotificationType type,
            String title,
            String content,
            String relatedBizType,
            Long relatedBizId,
            LocalDateTime createdAt
    ) {
        if (notificationRepository.existsUnread(userId, type, relatedBizType, relatedBizId)) {
            return false;
        }
        NotificationEntity notification = new NotificationEntity();
        notification.setNotificationId(notificationRepository.nextNotificationId());
        notification.setUserId(userId);
        notification.setType(type);
        notification.setTitle(title);
        notification.setContent(content);
        notification.setRelatedBizType(relatedBizType);
        notification.setRelatedBizId(relatedBizId);
        notification.setRead(false);
        notification.setCreatedAt(createdAt);
        notificationRepository.save(notification);
        return true;
    }

    public Map<String, Object> listMyMessages(Boolean confirmed, NotificationType type, Integer pageNum, Integer pageSize) {
        UserEntity currentUser = authService.currentUser();
        return toPage(notificationRepository.findAll().stream()
                .filter(notification -> currentUser.getUserId().equals(notification.getUserId()))
                .filter(notification -> confirmed == null || notification.isRead() == confirmed)
                .filter(notification -> type == null || notification.getType() == type)
                .sorted(Comparator.comparing(NotificationEntity::getNotificationId).reversed())
                .map(this::toView)
                .toList(), pageNum, pageSize);
    }

    public Map<String, Object> listMessages(Long userId, NotificationType type, Boolean confirmed, Integer pageNum, Integer pageSize) {
        UserEntity currentUser = authService.currentUser();
        if (currentUser.getRoleCode() == RoleCode.STUDENT) {
            throw new ApiException(403, "无权限访问");
        }
        return toPage(notificationRepository.findAll().stream()
                .filter(notification -> userId == null || userId.equals(notification.getUserId()))
                .filter(notification -> type == null || notification.getType() == type)
                .filter(notification -> confirmed == null || notification.isRead() == confirmed)
                .sorted(Comparator.comparing(NotificationEntity::getNotificationId).reversed())
                .map(this::toView)
                .toList(), pageNum, pageSize);
    }

    public Map<String, Object> unconfirmedSummary() {
        UserEntity currentUser = authService.currentUser();
        List<NotificationEntity> notifications = notificationRepository.findAll().stream()
                .filter(notification -> currentUser.getUserId().equals(notification.getUserId()))
                .filter(notification -> !notification.isRead())
                .toList();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total", notifications.size());
        result.put("aboutToExpireCount", countByType(notifications, NotificationType.ABOUT_TO_EXPIRE_REMINDER));
        result.put("overdueCount", countByType(notifications, NotificationType.OVERDUE_REMINDER));
        result.put("firstLoginCount", countByType(notifications, NotificationType.FIRST_LOGIN_PASSWORD_CHANGE));
        result.put("passwordResetCount", countByType(notifications, NotificationType.PASSWORD_RESET));
        return result;
    }

    public Map<String, Object> summary() {
        UserEntity currentUser = authService.currentUser();
        List<NotificationEntity> notifications = notificationRepository.findAll().stream()
                .filter(notification -> currentUser.getUserId().equals(notification.getUserId()))
                .sorted(Comparator.comparing(NotificationEntity::getNotificationId).reversed())
                .toList();

        Map<String, Integer> unreadByType = new LinkedHashMap<>();
        int unreadTotal = 0;
        for (NotificationType type : NotificationType.values()) {
            int count = (int) notifications.stream()
                    .filter(notification -> !notification.isRead() && notification.getType() == type)
                    .count();
            unreadByType.put(type.name(), count);
            unreadTotal += count;
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("unreadTotal", unreadTotal);
        result.put("unreadByType", unreadByType);
        result.put("latestNotifications", notifications.stream().limit(5).map(this::toView).toList());
        return result;
    }

    public Map<String, Object> confirmMessage(Long notificationId) {
        UserEntity currentUser = authService.currentUser();
        NotificationEntity notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ApiException(404, "消息不存在"));
        if (!currentUser.getUserId().equals(notification.getUserId())) {
            throw new ApiException(403, "仅可处理本人的消息");
        }
        notification.setRead(true);
        notification.setReadAt(LocalDateTime.now());
        notificationRepository.save(notification);
        return toView(notification);
    }

    private int countByType(List<NotificationEntity> notifications, NotificationType type) {
        return (int) notifications.stream().filter(notification -> notification.getType() == type).count();
    }

    private Map<String, Object> toPage(List<Map<String, Object>> list, Integer pageNum, Integer pageSize) {
        int actualPageNum = pageNum == null || pageNum < 1 ? 1 : pageNum;
        int actualPageSize = pageSize == null || pageSize < 1 ? 10 : pageSize;
        int fromIndex = Math.min((actualPageNum - 1) * actualPageSize, list.size());
        int toIndex = Math.min(fromIndex + actualPageSize, list.size());
        return Map.of(
                "list", list.subList(fromIndex, toIndex),
                "pageNum", actualPageNum,
                "pageSize", actualPageSize,
                "total", list.size()
        );
    }

    private Map<String, Object> toView(NotificationEntity notification) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("messageId", notification.getNotificationId());
        result.put("notificationId", notification.getNotificationId());
        result.put("type", notification.getType());
        result.put("title", notification.getTitle());
        result.put("content", notification.getContent());
        result.put("relatedBizType", notification.getRelatedBizType());
        result.put("relatedBizId", notification.getRelatedBizId());
        result.put("confirmStatus", notification.isRead() ? "CONFIRMED" : "UNCONFIRMED");
        result.put("read", notification.isRead());
        result.put("createdAt", formatDateTime(notification.getCreatedAt()));
        result.put("confirmedAt", formatDateTime(notification.getReadAt()));
        result.put("readAt", formatDateTime(notification.getReadAt()));
        return result;
    }

    private String formatDateTime(LocalDateTime dateTime) {
        return dateTime == null ? null : dateTime.format(DATE_TIME_FORMATTER);
    }
}
