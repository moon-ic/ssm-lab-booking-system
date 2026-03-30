package com.lab.booking.controller;

import com.lab.booking.common.Result;
import com.lab.booking.dto.BorrowRecordDtos;
import com.lab.booking.model.BorrowStatus;
import com.lab.booking.service.BorrowRecordService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/borrow-records")
public class BorrowRecordController {

    private final BorrowRecordService borrowRecordService;

    public BorrowRecordController(BorrowRecordService borrowRecordService) {
        this.borrowRecordService = borrowRecordService;
    }

    @GetMapping
    public Result<Map<String, Object>> listRecords(
            @RequestParam(required = false) BorrowStatus status,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Long deviceId,
            @RequestParam(required = false) Integer pageNum,
            @RequestParam(required = false) Integer pageSize
    ) {
        return Result.success(borrowRecordService.listRecords(status, userId, deviceId, pageNum, pageSize));
    }

    @PutMapping("/{recordId}/pickup")
    public Result<Map<String, Object>> pickup(@PathVariable Long recordId, @Valid @RequestBody BorrowRecordDtos.PickupRequest request) {
        return Result.success(borrowRecordService.pickup(recordId, request));
    }

    @PutMapping("/{recordId}/return")
    public Result<Map<String, Object>> returnDevice(@PathVariable Long recordId, @Valid @RequestBody BorrowRecordDtos.ReturnRequest request) {
        return Result.success(borrowRecordService.returnDevice(recordId, request));
    }

    @PutMapping("/{recordId}/overdue")
    public Result<Void> markOverdue(@PathVariable Long recordId) {
        borrowRecordService.markOverdue(recordId);
        return Result.success();
    }

    @GetMapping("/reminders")
    public Result<List<Map<String, Object>>> listReminders(@RequestParam String type) {
        return Result.success(borrowRecordService.listReminders(type));
    }
}
