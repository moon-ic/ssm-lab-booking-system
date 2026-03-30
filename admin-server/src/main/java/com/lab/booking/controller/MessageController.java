package com.lab.booking.controller;

import com.lab.booking.common.ApiException;
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
@RequestMapping("/api/messages")
public class MessageController {

    private final NotificationService notificationService;

    public MessageController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public Result<Map<String, Object>> listMessages(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) NotificationType type,
            @RequestParam(required = false) String confirmStatus,
            @RequestParam(required = false) Integer pageNum,
            @RequestParam(required = false) Integer pageSize
    ) {
        return Result.success(notificationService.listMessages(userId, type, parseConfirmStatus(confirmStatus), pageNum, pageSize));
    }

    @GetMapping("/unconfirmed-summary")
    public Result<Map<String, Object>> unconfirmedSummary() {
        return Result.success(notificationService.unconfirmedSummary());
    }

    @PutMapping("/{messageId}/confirm")
    public Result<Map<String, Object>> confirmMessage(@PathVariable Long messageId) {
        return Result.success(notificationService.confirmMessage(messageId));
    }

    private Boolean parseConfirmStatus(String confirmStatus) {
        if (confirmStatus == null || confirmStatus.isBlank()) {
            return null;
        }
        return switch (confirmStatus.trim()) {
            case "CONFIRMED" -> true;
            case "UNCONFIRMED" -> false;
            default -> throw new ApiException(400, "confirmStatus 必须为 CONFIRMED 或 UNCONFIRMED");
        };
    }
}
