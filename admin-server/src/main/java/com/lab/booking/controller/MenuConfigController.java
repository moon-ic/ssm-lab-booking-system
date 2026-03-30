package com.lab.booking.controller;

import com.lab.booking.common.Result;
import com.lab.booking.dto.MenuDtos;
import com.lab.booking.model.MenuEntity;
import com.lab.booking.service.MenuConfigService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class MenuConfigController {

    private final MenuConfigService menuConfigService;

    public MenuConfigController(MenuConfigService menuConfigService) {
        this.menuConfigService = menuConfigService;
    }

    @GetMapping("/api/icons")
    public Result<List<String>> listIcons() {
        return Result.success(menuConfigService.listIcons());
    }

    @GetMapping("/api/menus")
    public Result<List<MenuEntity>> listMenus() {
        return Result.success(menuConfigService.listMenus());
    }

    @PutMapping("/api/menus/{menuId}")
    public Result<MenuEntity> updateMenu(@PathVariable Long menuId, @Valid @RequestBody MenuDtos.UpdateMenuRequest request) {
        return Result.success(menuConfigService.updateMenu(menuId, request));
    }
}
