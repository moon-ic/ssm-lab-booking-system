package com.lab.booking.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public final class ReservationDtos {

    private ReservationDtos() {
    }

    public record CreateReservationRequest(
            @NotNull Long deviceId,
            @NotBlank String startTime,
            @NotBlank String endTime,
            @NotBlank String purpose
    ) {
    }

    public record ApproveReservationRequest(
            @NotBlank String action,
            String comment
    ) {
    }
}
