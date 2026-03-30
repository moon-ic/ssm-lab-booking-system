package com.lab.booking.dto;

import com.lab.booking.model.RepairStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public final class RepairDtos {

    private RepairDtos() {
    }

    public record CreateRepairRequest(
            @NotNull Long deviceId,
            @NotBlank String description
    ) {
    }

    public record UpdateRepairStatusRequest(
            @NotNull RepairStatus status,
            String comment
    ) {
    }
}
