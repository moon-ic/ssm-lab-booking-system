package com.lab.booking.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public final class RoleDtos {

    private RoleDtos() {
    }

    public record SaveRoleRequest(@NotBlank String roleName, @NotBlank String roleCode, String remark) {
    }

    public record AssignPermissionsRequest(@NotEmpty List<Long> permissionIds, @NotEmpty List<Long> menuIds) {
    }
}
