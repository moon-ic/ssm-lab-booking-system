package com.lab.booking.dto;

import jakarta.validation.constraints.NotBlank;

public final class MenuDtos {

    private MenuDtos() {
    }

    public record UpdateMenuRequest(
            @NotBlank String menuName,
            @NotBlank String path,
            @NotBlank String icon,
            @NotBlank String permissionCode
    ) {
    }
}
