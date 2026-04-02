package com.lab.booking.service;

import com.lab.booking.common.ApiException;
import com.lab.booking.dto.UserDtos;
import com.lab.booking.model.NotificationType;
import com.lab.booking.model.RoleCode;
import com.lab.booking.model.UserEntity;
import com.lab.booking.model.UserStatus;
import com.lab.booking.repository.AuthRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class UserService {

    private final AuthRepository authRepository;
    private final AuthService authService;
    private final NotificationService notificationService;

    public UserService(AuthRepository authRepository, AuthService authService, NotificationService notificationService) {
        this.authRepository = authRepository;
        this.authService = authService;
        this.notificationService = notificationService;
    }

    public Map<String, Object> listUsers(String keyword, RoleCode roleCode, UserStatus status, Integer pageNum, Integer pageSize) {
        UserEntity currentUser = requireRoles(RoleCode.SUPER_ADMIN, RoleCode.ADMIN, RoleCode.TEACHER);
        List<Map<String, Object>> filtered = authRepository.getUsers().values().stream()
                .filter(user -> !user.isDeleted())
                .filter(user -> visibleTo(currentUser, user))
                .filter(user -> keyword == null || containsKeyword(user, keyword))
                .filter(user -> roleCode == null || user.getRoleCode() == roleCode)
                .filter(user -> status == null || user.getStatus() == status)
                .sorted(Comparator.comparing(UserEntity::getUserId))
                .map(this::toUserView)
                .toList();

        int actualPageNum = pageNum == null || pageNum < 1 ? 1 : pageNum;
        int actualPageSize = pageSize == null || pageSize < 1 ? 10 : pageSize;
        int fromIndex = Math.min((actualPageNum - 1) * actualPageSize, filtered.size());
        int toIndex = Math.min(fromIndex + actualPageSize, filtered.size());

        return Map.of(
                "list", filtered.subList(fromIndex, toIndex),
                "pageNum", actualPageNum,
                "pageSize", actualPageSize,
                "total", filtered.size()
        );
    }

    public Map<String, Object> getUserDetail(Long userId) {
        UserEntity currentUser = requireRoles(RoleCode.SUPER_ADMIN, RoleCode.ADMIN, RoleCode.TEACHER);
        UserEntity target = getExistingUser(userId);
        if (!visibleTo(currentUser, target)) {
            throw new ApiException(403, "无权限访问");
        }
        return toUserView(target);
    }

    public Map<String, Object> createAdmin(UserDtos.CreateAdminRequest request) {
        UserEntity currentUser = requireRoles(RoleCode.SUPER_ADMIN);
        return toUserView(createUser(request.name(), request.account(), request.account(), request.phone(), RoleCode.ADMIN, currentUser.getUserId()));
    }

    public Map<String, Object> createTeacher(UserDtos.CreateTeacherRequest request) {
        UserEntity currentUser = requireRoles(RoleCode.ADMIN);
        return toUserView(createUser(request.name(), request.jobNo(), request.jobNo(), request.phone(), RoleCode.TEACHER, currentUser.getUserId()));
    }

    public Map<String, Object> createStudent(UserDtos.CreateStudentRequest request) {
        UserEntity currentUser = requireRoles(RoleCode.TEACHER);
        return toUserView(createUser(request.name(), request.studentNo(), request.studentNo(), request.phone(), RoleCode.STUDENT, currentUser.getUserId()));
    }

    public void deleteStudent(Long userId) {
        UserEntity currentUser = requireRoles(RoleCode.TEACHER);
        UserEntity target = getExistingUser(userId);
        if (target.getRoleCode() != RoleCode.STUDENT || !Objects.equals(target.getManagerId(), currentUser.getUserId())) {
            throw new ApiException(403, "无权限删除该学生");
        }
        target.setDeleted(true);
        target.setStatus(UserStatus.DISABLED);
        authRepository.save(target);
    }

    public void updateUserStatus(Long userId, UserDtos.UpdateUserStatusRequest request) {
        UserEntity currentUser = requireRoles(RoleCode.SUPER_ADMIN, RoleCode.ADMIN);
        UserEntity target = getExistingUser(userId);
        if (!visibleTo(currentUser, target)) {
            throw new ApiException(403, "无权限修改该用户状态");
        }
        target.setStatus(request.status());
        authRepository.save(target);
    }

    public void resetPassword(Long userId, UserDtos.ResetPasswordRequest request) {
        UserEntity currentUser = requireRoles(RoleCode.SUPER_ADMIN, RoleCode.ADMIN);
        UserEntity target = getExistingUser(userId);
        if (!visibleTo(currentUser, target)) {
            throw new ApiException(403, "无权限重置该用户密码");
        }
        target.setPassword(request.newPassword());
        target.setFirstLoginRequired(true);
        authRepository.save(target);
        notificationService.createSystemNotificationIfAbsent(
                target.getUserId(),
                NotificationType.PASSWORD_RESET,
                "密码已重置",
                "您的账号密码已被重置，请使用新密码登录并及时修改密码。",
                "USER",
                target.getUserId(),
                LocalDateTime.now()
        );
    }

    private UserEntity createUser(String name, String account, String loginId, String phone, RoleCode roleCode, Long managerId) {
        boolean duplicated = authRepository.getUsers().values().stream()
                .filter(user -> !user.isDeleted())
                .anyMatch(user -> Objects.equals(user.getAccount(), account) || Objects.equals(user.getLoginId(), loginId));
        if (duplicated) {
            throw new ApiException(409, "账号或学号/工号已存在");
        }

        UserEntity user = new UserEntity();
        user.setUserId(authRepository.nextUserId());
        user.setName(name);
        user.setAccount(account);
        user.setLoginId(loginId);
        user.setPhone(phone);
        user.setRoleCode(roleCode);
        user.setStatus(UserStatus.ENABLED);
        user.setCreditScore(100);
        user.setPassword("0000");
        user.setFirstLoginRequired(true);
        user.setManagerId(managerId);
        authRepository.save(user);
        return user;
    }

    private UserEntity getExistingUser(Long userId) {
        return authRepository.findById(userId)
                .orElseThrow(() -> new ApiException(404, "用户不存在"));
    }

    private UserEntity requireRoles(RoleCode... allowedRoles) {
        UserEntity currentUser = authService.currentUser();
        for (RoleCode role : allowedRoles) {
            if (currentUser.getRoleCode() == role) {
                return currentUser;
            }
        }
        throw new ApiException(403, "无权限访问");
    }

    private boolean visibleTo(UserEntity currentUser, UserEntity target) {
        return switch (currentUser.getRoleCode()) {
            case SUPER_ADMIN -> target.getRoleCode() != RoleCode.SUPER_ADMIN || Objects.equals(currentUser.getUserId(), target.getUserId());
            case ADMIN -> target.getRoleCode() == RoleCode.TEACHER || target.getRoleCode() == RoleCode.STUDENT;
            case TEACHER -> target.getRoleCode() == RoleCode.STUDENT && Objects.equals(target.getManagerId(), currentUser.getUserId());
            default -> false;
        };
    }

    private boolean containsKeyword(UserEntity user, String keyword) {
        return user.getName().contains(keyword)
                || user.getAccount().contains(keyword)
                || user.getLoginId().contains(keyword);
    }

    private Map<String, Object> toUserView(UserEntity user) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("userId", user.getUserId());
        result.put("name", user.getName());
        result.put("account", user.getAccount());
        result.put("jobNoOrStudentNo", user.getLoginId());
        result.put("roleCode", user.getRoleCode());
        result.put("creditScore", user.getCreditScore());
        result.put("status", user.getStatus());
        result.put("phone", user.getPhone());
        result.put("managerId", user.getManagerId());
        result.put("firstLoginRequired", user.isFirstLoginRequired());
        return result;
    }
}
