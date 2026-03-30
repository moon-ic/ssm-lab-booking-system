package com.lab.booking.controller;

import com.lab.booking.common.Result;
import com.lab.booking.model.RankScope;
import com.lab.booking.service.StatisticsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/statistics")
public class StatisticsController {

    private final StatisticsService statisticsService;

    public StatisticsController(StatisticsService statisticsService) {
        this.statisticsService = statisticsService;
    }

    @GetMapping("/devices/hot")
    public Result<List<Map<String, Object>>> hotDevices(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) RankScope rankScope,
            @RequestParam(required = false) Integer topN
    ) {
        return Result.success(statisticsService.hotDevices(startDate, endDate, rankScope, topN));
    }

    @GetMapping("/devices/damage")
    public Result<List<Map<String, Object>>> deviceDamageStatistics(
            @RequestParam(required = false) RankScope rankScope,
            @RequestParam(required = false) Integer topN
    ) {
        return Result.success(statisticsService.deviceDamageStatistics(rankScope, topN));
    }

    @GetMapping("/users/violations")
    public Result<List<Map<String, Object>>> userViolationStatistics(
            @RequestParam(required = false) RankScope rankScope,
            @RequestParam(required = false) Integer topN
    ) {
        return Result.success(statisticsService.userViolationStatistics(rankScope, topN));
    }

    @GetMapping("/overview")
    public Result<Map<String, Object>> overview() {
        return Result.success(statisticsService.overview());
    }
}
