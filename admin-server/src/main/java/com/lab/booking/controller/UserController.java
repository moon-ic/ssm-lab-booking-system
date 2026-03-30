package com.lab.booking.controller;

import com.lab.booking.common.Result;
import com.lab.booking.dto.UserDtos;
import com.lab.booking.model.RoleCode;
import com.lab.booking.model.UserStatus;
import com.lab.booking.service.UserService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/users")
    public Result<Map<String, Object>> listUsers(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) RoleCode roleCode,
            @RequestParam(required = false) UserStatus status,
            @RequestParam(required = false) Integer pageNum,
            @RequestParam(required = false) Integer pageSize
    ) {
        return Result.success(userService.listUsers(keyword, roleCode, status, pageNum, pageSize));
    }

    @GetMapping("/users/{userId}")
    public Result<Map<String, Object>> getUserDetail(@PathVariable Long userId) {
        return Result.success(userService.getUserDetail(userId));
    }

    @PostMapping("/users/admins")
    public Result<Map<String, Object>> createAdmin(@Valid @RequestBody UserDtos.CreateAdminRequest request) {
        return Result.success(userService.createAdmin(request));
    }

    @PostMapping("/users/teachers")
    public Result<Map<String, Object>> createTeacher(@Valid @RequestBody UserDtos.CreateTeacherRequest request) {
        return Result.success(userService.createTeacher(request));
    }

    @PostMapping("/users/students")
    public Result<Map<String, Object>> createStudent(@Valid @RequestBody UserDtos.CreateStudentRequest request) {
        return Result.success(userService.createStudent(request));
    }

    @DeleteMapping("/users/students/{userId}")
    public Result<Void> deleteStudent(@PathVariable Long userId) {
        userService.deleteStudent(userId);
        return Result.success();
    }

    @PutMapping("/users/{userId}/status")
    public Result<Void> updateUserStatus(@PathVariable Long userId, @Valid @RequestBody UserDtos.UpdateUserStatusRequest request) {
        userService.updateUserStatus(userId, request);
        return Result.success();
    }

    @PutMapping("/users/{userId}/reset-password")
    public Result<Void> resetPassword(@PathVariable Long userId, @Valid @RequestBody UserDtos.ResetPasswordRequest request) {
        userService.resetPassword(userId, request);
        return Result.success();
    }
}
