package com.lab.booking.controller;

import com.lab.booking.common.Result;
import com.lab.booking.dto.RepairDtos;
import com.lab.booking.model.RepairStatus;
import com.lab.booking.service.RepairService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/repairs")
public class RepairController {

    private final RepairService repairService;

    public RepairController(RepairService repairService) {
        this.repairService = repairService;
    }

    @PostMapping
    public Result<Map<String, Object>> createRepair(@Valid @RequestBody RepairDtos.CreateRepairRequest request) {
        return Result.success(repairService.createRepair(request));
    }

    @GetMapping
    public Result<Map<String, Object>> listRepairs(
            @RequestParam(required = false) RepairStatus status,
            @RequestParam(required = false) Long deviceId,
            @RequestParam(required = false) Long applicantId,
            @RequestParam(required = false) Integer pageNum,
            @RequestParam(required = false) Integer pageSize
    ) {
        return Result.success(repairService.listRepairs(status, deviceId, applicantId, pageNum, pageSize));
    }

    @GetMapping("/{repairId}")
    public Result<Map<String, Object>> getRepairDetail(@PathVariable Long repairId) {
        return Result.success(repairService.getRepairDetail(repairId));
    }

    @PutMapping("/{repairId}/status")
    public Result<Map<String, Object>> updateRepairStatus(
            @PathVariable Long repairId,
            @Valid @RequestBody RepairDtos.UpdateRepairStatusRequest request
    ) {
        return Result.success(repairService.updateRepairStatus(repairId, request));
    }
}
