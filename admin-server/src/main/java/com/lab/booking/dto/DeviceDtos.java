package com.lab.booking.dto;

import com.lab.booking.model.DeviceStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public final class DeviceDtos {

    private DeviceDtos() {
    }

    public record SaveDeviceRequest(
            @NotBlank String deviceName,
            @NotBlank String deviceCode,
            @NotBlank String category,
            @NotBlank String location,
            String imageUrl,
            String description
    ) {
    }

    public record UpdateDeviceStatusRequest(@NotNull DeviceStatus status) {
    }
}
