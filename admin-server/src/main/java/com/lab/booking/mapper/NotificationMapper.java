package com.lab.booking.mapper;

import com.lab.booking.model.NotificationEntity;
import com.lab.booking.model.NotificationType;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface NotificationMapper {

    Long selectNextNotificationId();

    void upsertNotification(NotificationEntity notification);

    NotificationEntity selectById(@Param("notificationId") Long notificationId);

    List<NotificationEntity> selectAll();

    Integer countUnread(
            @Param("userId") Long userId,
            @Param("type") NotificationType type,
            @Param("relatedBizType") String relatedBizType,
            @Param("relatedBizId") Long relatedBizId
    );
}
