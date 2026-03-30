package com.lab.booking.controller;

import com.lab.booking.common.Result;
import com.lab.booking.model.NotificationType;
import com.lab.booking.service.NotificationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public Result<Map<String, Object>> listNotifications(
            @RequestParam(required = false) Boolean read,
            @RequestParam(required = false) NotificationType type,
            @RequestParam(required = false) Integer pageNum,
            @RequestParam(required = false) Integer pageSize
    ) {
        Boolean confirmed = read == null ? null : read;
        return Result.success(notificationService.listMyMessages(confirmed, type, pageNum, pageSize));
    }

    @GetMapping("/summary")
    public Result<Map<String, Object>> summary() {
        return Result.success(notificationService.summary());
    }

    @PutMapping("/{notificationId}/read")
    public Result<Map<String, Object>> markRead(@PathVariable Long notificationId) {
        return Result.success(notificationService.confirmMessage(notificationId));
    }
}
