package com.lab.booking.mapper;

import com.lab.booking.model.MenuEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface MenuMapper {

    List<MenuEntity> selectAllMenus();

    MenuEntity selectMenuById(@Param("menuId") Long menuId);

    void upsertMenu(MenuEntity menu);

    List<String> selectAllIcons();
}
