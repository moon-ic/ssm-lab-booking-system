package com.lab.booking.dto;

import com.lab.booking.model.UserStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public final class UserDtos {

    private UserDtos() {
    }

    public record CreateAdminRequest(@NotBlank String name, @NotBlank String account, String phone) {
    }

    public record CreateTeacherRequest(@NotBlank String name, @NotBlank String jobNo, String phone) {
    }

    public record CreateStudentRequest(@NotBlank String name, @NotBlank String studentNo, String phone) {
    }

    public record UpdateUserStatusRequest(@NotNull UserStatus status) {
    }

    public record ResetPasswordRequest(@NotBlank String newPassword) {
    }
}
