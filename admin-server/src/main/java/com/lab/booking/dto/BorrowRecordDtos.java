package com.lab.booking.dto;

import jakarta.validation.constraints.NotBlank;

public final class BorrowRecordDtos {

    private BorrowRecordDtos() {
    }

    public record PickupRequest() {
    }

    public record ReturnRequest(
            @NotBlank String returnTime,
            @NotBlank String deviceCondition
    ) {
    }
}
