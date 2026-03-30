package com.lab.booking.controller;

import com.lab.booking.common.Result;
import com.lab.booking.dto.RoleDtos;
import com.lab.booking.model.PermissionEntity;
import com.lab.booking.model.RoleEntity;
import com.lab.booking.service.RolePermissionService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class RolePermissionController {

    private final RolePermissionService rolePermissionService;

    public RolePermissionController(RolePermissionService rolePermissionService) {
        this.rolePermissionService = rolePermissionService;
    }

    @GetMapping("/roles")
    public Result<List<RoleEntity>> listRoles() {
        return Result.success(rolePermissionService.listRoles());
    }

    @PostMapping("/roles")
    public Result<RoleEntity> createRole(@Valid @RequestBody RoleDtos.SaveRoleRequest request) {
        return Result.success(rolePermissionService.createRole(request));
    }

    @PutMapping("/roles/{roleId}")
    public Result<RoleEntity> updateRole(@PathVariable Long roleId, @Valid @RequestBody RoleDtos.SaveRoleRequest request) {
        return Result.success(rolePermissionService.updateRole(roleId, request));
    }

    @GetMapping("/roles/{roleId}")
    public Result<RoleEntity> getRoleDetail(@PathVariable Long roleId) {
        return Result.success(rolePermissionService.getRoleDetail(roleId));
    }

    @PutMapping("/roles/{roleId}/permissions")
    public Result<RoleEntity> assignPermissions(@PathVariable Long roleId, @Valid @RequestBody RoleDtos.AssignPermissionsRequest request) {
        return Result.success(rolePermissionService.assignPermissions(roleId, request));
    }

    @GetMapping("/permissions")
    public Result<List<PermissionEntity>> listPermissions(@RequestParam(required = false) String type) {
        return Result.success(rolePermissionService.listPermissions(type));
    }
}
