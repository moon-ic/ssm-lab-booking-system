package com.lab.booking.service;

import com.lab.booking.common.ApiException;
import com.lab.booking.dto.RoleDtos;
import com.lab.booking.model.PermissionEntity;
import com.lab.booking.model.RoleCode;
import com.lab.booking.model.RoleEntity;
import com.lab.booking.model.UserEntity;
import com.lab.booking.repository.RolePermissionRepository;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Service
public class RolePermissionService {

    private final RolePermissionRepository rolePermissionRepository;
    private final AuthService authService;

    public RolePermissionService(RolePermissionRepository rolePermissionRepository, AuthService authService) {
        this.rolePermissionRepository = rolePermissionRepository;
        this.authService = authService;
    }

    public List<RoleEntity> listRoles() {
        requireSuperAdmin();
        return rolePermissionRepository.findAllRoles().stream()
                .sorted(Comparator.comparing(RoleEntity::getRoleId))
                .toList();
    }

    public RoleEntity createRole(RoleDtos.SaveRoleRequest request) {
        requireSuperAdmin();
        if (rolePermissionRepository.existsByRoleCode(request.roleCode(), -1L)) {
            throw new ApiException(409, "角色编码已存在");
        }
        RoleEntity role = new RoleEntity();
        role.setRoleId(rolePermissionRepository.nextRoleId());
        role.setRoleName(request.roleName());
        role.setRoleCode(request.roleCode());
        role.setRemark(request.remark());
        rolePermissionRepository.saveRole(role);
        return role;
    }

    public RoleEntity updateRole(Long roleId, RoleDtos.SaveRoleRequest request) {
        requireSuperAdmin();
        RoleEntity role = getRole(roleId);
        if (rolePermissionRepository.existsByRoleCode(request.roleCode(), roleId)) {
            throw new ApiException(409, "角色编码已存在");
        }
        role.setRoleName(request.roleName());
        role.setRoleCode(request.roleCode());
        role.setRemark(request.remark());
        rolePermissionRepository.saveRole(role);
        return role;
    }

    public RoleEntity getRoleDetail(Long roleId) {
        requireSuperAdmin();
        return getRole(roleId);
    }

    public RoleEntity assignPermissions(Long roleId, RoleDtos.AssignPermissionsRequest request) {
        requireSuperAdmin();
        RoleEntity role = getRole(roleId);
        role.setPermissionIds(request.permissionIds());
        role.setMenuIds(request.menuIds());
        rolePermissionRepository.saveRole(role);
        return role;
    }

    public List<PermissionEntity> listPermissions(String type) {
        requireSuperAdmin();
        return rolePermissionRepository.findPermissions(type).stream()
                .sorted(Comparator.comparing(PermissionEntity::permissionId))
                .toList();
    }

    private RoleEntity getRole(Long roleId) {
        return rolePermissionRepository.findRoleById(roleId)
                .orElseThrow(() -> new ApiException(404, "角色不存在"));
    }

    private void requireSuperAdmin() {
        UserEntity currentUser = authService.currentUser();
        if (currentUser.getRoleCode() != RoleCode.SUPER_ADMIN) {
            throw new ApiException(403, "无权限访问");
        }
    }
}
