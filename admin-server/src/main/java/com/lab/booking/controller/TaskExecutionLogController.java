package com.lab.booking.controller;

import com.lab.booking.common.Result;
import com.lab.booking.model.TaskExecutionStatus;
import com.lab.booking.service.TaskExecutionLogService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/task-logs")
public class TaskExecutionLogController {

    private final TaskExecutionLogService taskExecutionLogService;

    public TaskExecutionLogController(TaskExecutionLogService taskExecutionLogService) {
        this.taskExecutionLogService = taskExecutionLogService;
    }

    @GetMapping
    public Result<Map<String, Object>> listLogs(
            @RequestParam(required = false) String taskCode,
            @RequestParam(required = false) TaskExecutionStatus status,
            @RequestParam(required = false) Integer pageNum,
            @RequestParam(required = false) Integer pageSize
    ) {
        return Result.success(taskExecutionLogService.listLogs(taskCode, status, pageNum, pageSize));
    }

    @GetMapping("/{logId}")
    public Result<Map<String, Object>> getLog(@PathVariable Long logId) {
        return Result.success(taskExecutionLogService.getLog(logId));
    }
}
