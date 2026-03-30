package com.lab.booking.service;

import com.lab.booking.common.ApiException;
import com.lab.booking.dto.MenuDtos;
import com.lab.booking.infrastructure.cache.AppCacheService;
import com.lab.booking.model.MenuEntity;
import com.lab.booking.model.PermissionEntity;
import com.lab.booking.model.RoleCode;
import com.lab.booking.model.UserEntity;
import com.lab.booking.repository.MenuConfigRepository;
import com.lab.booking.repository.RolePermissionRepository;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

@Service
public class MenuConfigService {

    private static final String MENUS_CACHE_KEY = "menus:all";
    private static final String ICONS_CACHE_KEY = "icons:all";

    private final MenuConfigRepository menuConfigRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final AuthService authService;
    private final AppCacheService cacheService;

    public MenuConfigService(
            MenuConfigRepository menuConfigRepository,
            RolePermissionRepository rolePermissionRepository,
            AuthService authService,
            AppCacheService cacheService
    ) {
        this.menuConfigRepository = menuConfigRepository;
        this.rolePermissionRepository = rolePermissionRepository;
        this.authService = authService;
        this.cacheService = cacheService;
    }

    public List<String> listIcons() {
        requireSuperAdmin();
        return cacheService.get(ICONS_CACHE_KEY, String[].class)
                .map(List::of)
                .orElseGet(() -> {
                    List<String> icons = menuConfigRepository.findAllIcons();
                    cacheService.put(ICONS_CACHE_KEY, icons);
                    return icons;
                });
    }

    public List<MenuEntity> listMenus() {
        requireSuperAdmin();
        return cacheService.get(MENUS_CACHE_KEY, MenuEntity[].class)
                .map(List::of)
                .orElseGet(() -> {
                    List<MenuEntity> menus = menuConfigRepository.findAllMenus().stream()
                            .sorted(Comparator.comparing(MenuEntity::getMenuId))
                            .toList();
                    cacheService.put(MENUS_CACHE_KEY, menus);
                    return menus;
                });
    }

    public MenuEntity updateMenu(Long menuId, MenuDtos.UpdateMenuRequest request) {
        requireSuperAdmin();
        if (menuConfigRepository.findAllIcons().stream().noneMatch(icon -> icon.equals(request.icon()))) {
            throw new ApiException(400, "图标不存在");
        }
        boolean permissionExists = rolePermissionRepository.findPermissions(null).stream()
                .map(PermissionEntity::permissionCode)
                .anyMatch(code -> code.equals(request.permissionCode()));
        if (!permissionExists) {
            throw new ApiException(400, "权限编码不存在");
        }

        MenuEntity menu = menuConfigRepository.findMenuById(menuId)
                .orElseThrow(() -> new ApiException(404, "菜单不存在"));
        menu.setMenuName(request.menuName());
        menu.setPath(request.path());
        menu.setIcon(request.icon());
        menu.setPermissionCode(request.permissionCode());
        menuConfigRepository.saveMenu(menu);
        cacheService.evict(MENUS_CACHE_KEY);
        cacheService.evict(ICONS_CACHE_KEY);
        return menu;
    }

    private void requireSuperAdmin() {
        UserEntity currentUser = authService.currentUser();
        if (currentUser.getRoleCode() != RoleCode.SUPER_ADMIN) {
            throw new ApiException(403, "无权限访问");
        }
    }
}
