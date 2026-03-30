package com.lab.booking.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lab.booking.model.NotificationEntity;
import com.lab.booking.model.NotificationType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

@Repository
public class NotificationRepository extends JdbcRepositorySupport {

    private final JdbcTemplate jdbcTemplate;

    public NotificationRepository(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        super(objectMapper);
        this.jdbcTemplate = jdbcTemplate;
    }

    public long nextNotificationId() {
        Long next = jdbcTemplate.queryForObject("SELECT COALESCE(MAX(notification_id), 8000) + 1 FROM lab_notification", Long.class);
        return next == null ? 8001L : next;
    }

    public void save(NotificationEntity notification) {
        jdbcTemplate.update("""
                        INSERT INTO lab_notification (
                            notification_id, user_id, type, title, content, related_biz_type, related_biz_id, is_read, created_at, read_at
                        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        ON DUPLICATE KEY UPDATE
                            user_id = VALUES(user_id),
                            type = VALUES(type),
                            title = VALUES(title),
                            content = VALUES(content),
                            related_biz_type = VALUES(related_biz_type),
                            related_biz_id = VALUES(related_biz_id),
                            is_read = VALUES(is_read),
                            created_at = VALUES(created_at),
                            read_at = VALUES(read_at)
                        """,
                notification.getNotificationId(),
                notification.getUserId(),
                notification.getType().name(),
                notification.getTitle(),
                notification.getContent(),
                notification.getRelatedBizType(),
                notification.getRelatedBizId(),
                notification.isRead(),
                notification.getCreatedAt(),
                notification.getReadAt()
        );
    }

    public Optional<NotificationEntity> findById(Long notificationId) {
        return jdbcTemplate.query("SELECT * FROM lab_notification WHERE notification_id = ? LIMIT 1", this::mapNotification, notificationId)
                .stream()
                .findFirst();
    }

    public List<NotificationEntity> findAll() {
        return jdbcTemplate.query("SELECT * FROM lab_notification ORDER BY notification_id", this::mapNotification);
    }

    public boolean existsUnread(Long userId, NotificationType type, String relatedBizType, Long relatedBizId) {
        Integer count = jdbcTemplate.queryForObject("""
                        SELECT COUNT(1) FROM lab_notification
                        WHERE user_id = ? AND type = ? AND is_read = 0
                          AND ((related_biz_type IS NULL AND ? IS NULL) OR related_biz_type = ?)
                          AND ((related_biz_id IS NULL AND ? IS NULL) OR related_biz_id = ?)
                        """,
                Integer.class,
                userId,
                type.name(),
                relatedBizType,
                relatedBizType,
                relatedBizId,
                relatedBizId
        );
        return count != null && count > 0;
    }

    private NotificationEntity mapNotification(ResultSet rs, int rowNum) throws SQLException {
        NotificationEntity notification = new NotificationEntity();
        notification.setNotificationId(rs.getLong("notification_id"));
        notification.setUserId(rs.getLong("user_id"));
        notification.setType(NotificationType.valueOf(rs.getString("type")));
        notification.setTitle(rs.getString("title"));
        notification.setContent(rs.getString("content"));
        notification.setRelatedBizType(rs.getString("related_biz_type"));
        notification.setRelatedBizId(rs.getObject("related_biz_id", Long.class));
        notification.setRead(rs.getBoolean("is_read"));
        notification.setCreatedAt(toLocalDateTime(rs.getTimestamp("created_at")));
        notification.setReadAt(toLocalDateTime(rs.getTimestamp("read_at")));
        return notification;
    }
}
