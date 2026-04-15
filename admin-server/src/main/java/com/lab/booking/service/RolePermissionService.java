package com.lab.booking.service;

import com.lab.booking.common.ApiException;
import com.lab.booking.dto.RoleDtos;
import com.lab.booking.mapper.RoleMapper;
import com.lab.booking.model.PermissionEntity;
import com.lab.booking.model.RoleCode;
import com.lab.booking.model.RoleEntity;
import com.lab.booking.model.UserEntity;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class RolePermissionService {

    private final RoleMapper roleMapper;
    private final AuthService authService;

    public RolePermissionService(RoleMapper roleMapper, AuthService authService) {
        this.roleMapper = roleMapper;
        this.authService = authService;
    }

    public List<RoleEntity> listRoles() {
        requireSuperAdmin();
        return roleMapper.selectAllRoles().stream()
                .map(this::fillRelations)
                .sorted(Comparator.comparing(RoleEntity::getRoleId))
                .toList();
    }

    public RoleEntity createRole(RoleDtos.SaveRoleRequest request) {
        requireSuperAdmin();
        Integer count = roleMapper.countByRoleCode(request.roleCode(), -1L);
        if (count != null && count > 0) {
            throw new ApiException(409, "角色编码已存在");
        }
        RoleEntity role = new RoleEntity();
        Long nextId = roleMapper.selectNextRoleId();
        role.setRoleId(nextId == null ? 11L : nextId);
        role.setRoleName(request.roleName());
        role.setRoleCode(request.roleCode());
        role.setRemark(request.remark());
        role.setPermissionIds(new ArrayList<>());
        role.setMenuIds(new ArrayList<>());
        saveRoleWithRelations(role);
        return role;
    }

    public RoleEntity updateRole(Long roleId, RoleDtos.SaveRoleRequest request) {
        requireSuperAdmin();
        RoleEntity role = getRole(roleId);
        Integer count = roleMapper.countByRoleCode(request.roleCode(), roleId);
        if (count != null && count > 0) {
            throw new ApiException(409, "角色编码已存在");
        }
        role.setRoleName(request.roleName());
        role.setRoleCode(request.roleCode());
        role.setRemark(request.remark());
        saveRoleWithRelations(role);
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
        saveRoleWithRelations(role);
        return role;
    }

    public List<PermissionEntity> listPermissions(String type) {
        requireSuperAdmin();
        List<PermissionEntity> permissions = type == null
                ? roleMapper.selectAllPermissions()
                : roleMapper.selectPermissionsByType(type);
        return permissions.stream()
                .sorted(Comparator.comparing(PermissionEntity::permissionId))
                .toList();
    }

    private RoleEntity getRole(Long roleId) {
        RoleEntity role = roleMapper.selectRoleById(roleId);
        if (role == null) {
            throw new ApiException(404, "角色不存在");
        }
        return fillRelations(role);
    }

    private RoleEntity fillRelations(RoleEntity role) {
        role.setPermissionIds(new ArrayList<>(roleMapper.selectPermissionIdsByRoleId(role.getRoleId())));
        role.setMenuIds(new ArrayList<>(roleMapper.selectMenuIdsByRoleId(role.getRoleId())));
        return role;
    }

    private void saveRoleWithRelations(RoleEntity role) {
        roleMapper.upsertRole(role);
        roleMapper.deleteRolePermissions(role.getRoleId());
        for (Long permissionId : role.getPermissionIds()) {
            roleMapper.insertRolePermission(role.getRoleId(), permissionId);
        }
        roleMapper.deleteRoleMenus(role.getRoleId());
        for (Long menuId : role.getMenuIds()) {
            roleMapper.insertRoleMenu(role.getRoleId(), menuId);
        }
    }

    private void requireSuperAdmin() {
        UserEntity currentUser = authService.currentUser();
        if (currentUser.getRoleCode() != RoleCode.SUPER_ADMIN) {
            throw new ApiException(403, "无权限访问");
        }
    }
}
