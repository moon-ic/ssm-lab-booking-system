package com.lab.booking.service;

import com.lab.booking.common.ApiException;
import com.lab.booking.dto.AuthDtos;
import com.lab.booking.model.NotificationType;
import com.lab.booking.model.UserEntity;
import com.lab.booking.model.UserStatus;
import com.lab.booking.repository.AuthRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class AuthService {

    private final AuthRepository authRepository;
    private final NotificationService notificationService;

    public AuthService(AuthRepository authRepository, NotificationService notificationService) {
        this.authRepository = authRepository;
        this.notificationService = notificationService;
    }

    public Map<String, Object> login(AuthDtos.LoginRequest request) {
        UserEntity user = authRepository.findByLoginId(request.loginId())
                .orElseThrow(() -> new ApiException(401, "账号或密码错误"));
        if (!user.getPassword().equals(request.password())) {
            throw new ApiException(401, "账号或密码错误");
        }
        if (user.getStatus() != UserStatus.ENABLED) {
            throw new ApiException(403, "账号已禁用");
        }

        if (user.isFirstLoginRequired()) {
            notificationService.createSystemNotificationIfAbsent(
                    user.getUserId(),
                    NotificationType.FIRST_LOGIN_PASSWORD_CHANGE,
                    "首次登录修改密码",
                    "当前账号为首次登录，请先修改密码后继续使用系统。",
                    "USER",
                    user.getUserId(),
                    LocalDateTime.now()
            );
        }

        String token = authRepository.createToken(user.getUserId());
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("token", token);
        result.put("tokenType", "Bearer");
        result.put("expiresIn", 7200);
        result.put("firstLoginRequired", user.isFirstLoginRequired());
        result.put("userInfo", toUserInfo(user));
        return result;
    }

    public Map<String, Object> me() {
        return toCurrentUserView(currentUser());
    }

    public void changePassword(AuthDtos.ChangePasswordRequest request) {
        UserEntity currentUser = currentUser();
        if (!currentUser.getPassword().equals(request.oldPassword())) {
            throw new ApiException(400, "旧密码错误");
        }
        if (request.newPassword() == null || request.newPassword().trim().length() < 8) {
            throw new ApiException(400, "新密码长度不能少于 8 位");
        }
        currentUser.setPassword(request.newPassword().trim());
        currentUser.setFirstLoginRequired(false);
        authRepository.save(currentUser);
    }

    public UserEntity currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserEntity user)) {
            throw new ApiException(401, "未登录或 token 无效");
        }
        return user;
    }

    private Map<String, Object> toUserInfo(UserEntity user) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("userId", user.getUserId());
        result.put("name", user.getName());
        result.put("account", user.getAccount());
        result.put("roleCode", user.getRoleCode());
        result.put("status", user.getStatus());
        result.put("firstLoginRequired", user.isFirstLoginRequired());
        return result;
    }

    private Map<String, Object> toCurrentUserView(UserEntity user) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("userId", user.getUserId());
        result.put("name", user.getName());
        result.put("account", user.getAccount());
        result.put("jobNoOrStudentNo", user.getLoginId());
        result.put("roleCode", user.getRoleCode());
        result.put("creditScore", user.getCreditScore());
        result.put("status", user.getStatus());
        result.put("firstLoginRequired", user.isFirstLoginRequired());
        return result;
    }
}
