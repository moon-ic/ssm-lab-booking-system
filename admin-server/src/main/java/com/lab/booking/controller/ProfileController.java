package com.lab.booking.controller;

import com.lab.booking.common.ApiException;
import com.lab.booking.common.Result;
import com.lab.booking.model.BorrowStatus;
import com.lab.booking.model.NotificationType;
import com.lab.booking.service.NotificationService;
import com.lab.booking.service.ProfileService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/profile")
public class ProfileController {

    private final ProfileService profileService;
    private final NotificationService notificationService;

    public ProfileController(ProfileService profileService, NotificationService notificationService) {
        this.profileService = profileService;
        this.notificationService = notificationService;
    }

    @GetMapping
    public Result<Map<String, Object>> getProfile() {
        return Result.success(profileService.getProfile());
    }

    @GetMapping("/borrow-records")
    public Result<Map<String, Object>> listMyBorrowRecords(
            @RequestParam(required = false) BorrowStatus status,
            @RequestParam(required = false) Integer pageNum,
            @RequestParam(required = false) Integer pageSize
    ) {
        return Result.success(profileService.listMyBorrowRecords(status, pageNum, pageSize));
    }

    @GetMapping("/messages")
    public Result<Map<String, Object>> listMyMessages(
            @RequestParam(required = false) String confirmStatus,
            @RequestParam(required = false) NotificationType type,
            @RequestParam(required = false) Integer pageNum,
            @RequestParam(required = false) Integer pageSize
    ) {
        return Result.success(notificationService.listMyMessages(parseConfirmStatus(confirmStatus), type, pageNum, pageSize));
    }

    @PutMapping("/messages/{messageId}/confirm")
    public Result<Map<String, Object>> confirmMyMessage(@PathVariable Long messageId) {
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
