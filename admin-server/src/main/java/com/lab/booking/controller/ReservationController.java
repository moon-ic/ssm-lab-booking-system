package com.lab.booking.controller;

import com.lab.booking.common.Result;
import com.lab.booking.dto.ReservationDtos;
import com.lab.booking.model.ReservationStatus;
import com.lab.booking.service.ReservationService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/reservations")
public class ReservationController {

    private final ReservationService reservationService;

    public ReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @PostMapping
    public Result<Map<String, Object>> createReservation(@Valid @RequestBody ReservationDtos.CreateReservationRequest request) {
        return Result.success(reservationService.createReservation(request));
    }

    @GetMapping
    public Result<Map<String, Object>> listReservations(
            @RequestParam(required = false) ReservationStatus status,
            @RequestParam(required = false) Long deviceId,
            @RequestParam(required = false) Long applicantId,
            @RequestParam(required = false) Integer pageNum,
            @RequestParam(required = false) Integer pageSize
    ) {
        return Result.success(reservationService.listReservations(status, deviceId, applicantId, pageNum, pageSize));
    }

    @GetMapping("/{reservationId}")
    public Result<Map<String, Object>> getReservationDetail(@PathVariable Long reservationId) {
        return Result.success(reservationService.getReservationDetail(reservationId));
    }

    @PutMapping("/{reservationId}/approve")
    public Result<Map<String, Object>> approveReservation(
            @PathVariable Long reservationId,
            @Valid @RequestBody ReservationDtos.ApproveReservationRequest request
    ) {
        return Result.success(reservationService.approveReservation(reservationId, request));
    }

    @PutMapping("/{reservationId}/cancel")
    public Result<Void> cancelReservation(@PathVariable Long reservationId) {
        reservationService.cancelReservation(reservationId);
        return Result.success();
    }

    @PutMapping("/{reservationId}/expire")
    public Result<Void> expireReservation(@PathVariable Long reservationId) {
        reservationService.expireReservation(reservationId);
        return Result.success();
    }
}
