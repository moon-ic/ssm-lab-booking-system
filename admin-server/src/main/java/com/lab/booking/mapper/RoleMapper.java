package com.lab.booking.mapper;

import com.lab.booking.model.PermissionEntity;
import com.lab.booking.model.RoleEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface RoleMapper {

    List<RoleEntity> selectAllRoles();

    RoleEntity selectRoleById(@Param("roleId") Long roleId);

    List<Long> selectPermissionIdsByRoleId(@Param("roleId") Long roleId);

    List<Long> selectMenuIdsByRoleId(@Param("roleId") Long roleId);

    void upsertRole(RoleEntity role);

    void deleteRolePermissions(@Param("roleId") Long roleId);

    void insertRolePermission(@Param("roleId") Long roleId, @Param("permissionId") Long permissionId);

    void deleteRoleMenus(@Param("roleId") Long roleId);

    void insertRoleMenu(@Param("roleId") Long roleId, @Param("menuId") Long menuId);

    Long selectNextRoleId();

    Integer countByRoleCode(@Param("roleCode") String roleCode, @Param("excludeRoleId") Long excludeRoleId);

    List<PermissionEntity> selectAllPermissions();

    List<PermissionEntity> selectPermissionsByType(@Param("type") String type);
}
