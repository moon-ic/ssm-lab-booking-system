package com.lab.booking.service;

import com.lab.booking.common.ApiException;
import com.lab.booking.model.RoleCode;
import com.lab.booking.model.TaskExecutionLogEntity;
import com.lab.booking.model.TaskExecutionStatus;
import com.lab.booking.model.UserEntity;
import com.lab.booking.repository.TaskExecutionLogRepository;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class TaskExecutionLogService {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final TaskExecutionLogRepository taskExecutionLogRepository;
    private final AuthService authService;

    public TaskExecutionLogService(TaskExecutionLogRepository taskExecutionLogRepository, AuthService authService) {
        this.taskExecutionLogRepository = taskExecutionLogRepository;
        this.authService = authService;
    }

    public void recordSuccess(
            String taskCode,
            String taskName,
            LocalDateTime startedAt,
            LocalDateTime finishedAt,
            Map<String, Object> resultSnapshot
    ) {
        TaskExecutionLogEntity log = new TaskExecutionLogEntity();
        log.setLogId(taskExecutionLogRepository.nextLogId());
        log.setTaskCode(taskCode);
        log.setTaskName(taskName);
        log.setStatus(TaskExecutionStatus.SUCCESS);
        log.setStartedAt(startedAt);
        log.setFinishedAt(finishedAt);
        log.setDurationMs(Duration.between(startedAt, finishedAt).toMillis());
        log.setSummary(buildSummary(resultSnapshot));
        log.setResultSnapshot(new LinkedHashMap<>(resultSnapshot));
        taskExecutionLogRepository.save(log);
    }

    public void recordFailure(
            String taskCode,
            String taskName,
            LocalDateTime startedAt,
            LocalDateTime finishedAt,
            RuntimeException ex
    ) {
        TaskExecutionLogEntity log = new TaskExecutionLogEntity();
        log.setLogId(taskExecutionLogRepository.nextLogId());
        log.setTaskCode(taskCode);
        log.setTaskName(taskName);
        log.setStatus(TaskExecutionStatus.FAILED);
        log.setStartedAt(startedAt);
        log.setFinishedAt(finishedAt);
        log.setDurationMs(Duration.between(startedAt, finishedAt).toMillis());
        log.setSummary("task failed");
        log.setErrorMessage(ex.getMessage());
        log.setResultSnapshot(Map.of());
        taskExecutionLogRepository.save(log);
    }

    public Map<String, Object> listLogs(String taskCode, TaskExecutionStatus status, Integer pageNum, Integer pageSize) {
        requireAdminRoles();
        List<Map<String, Object>> filtered = taskExecutionLogRepository.findAll().stream()
                .filter(log -> taskCode == null || taskCode.equalsIgnoreCase(log.getTaskCode()))
                .filter(log -> status == null || log.getStatus() == status)
                .sorted(Comparator.comparing(TaskExecutionLogEntity::getLogId).reversed())
                .map(this::toView)
                .toList();

        int actualPageNum = pageNum == null || pageNum < 1 ? 1 : pageNum;
        int actualPageSize = pageSize == null || pageSize < 1 ? 10 : pageSize;
        int fromIndex = Math.min((actualPageNum - 1) * actualPageSize, filtered.size());
        int toIndex = Math.min(fromIndex + actualPageSize, filtered.size());

        return Map.of(
                "list", filtered.subList(fromIndex, toIndex),
                "pageNum", actualPageNum,
                "pageSize", actualPageSize,
                "total", filtered.size()
        );
    }

    public Map<String, Object> getLog(Long logId) {
        requireAdminRoles();
        TaskExecutionLogEntity log = taskExecutionLogRepository.findById(logId)
                .orElseThrow(() -> new ApiException(404, "任务执行日志不存在"));
        return toView(log);
    }

    private UserEntity requireAdminRoles() {
        UserEntity currentUser = authService.currentUser();
        if (currentUser.getRoleCode() != RoleCode.SUPER_ADMIN && currentUser.getRoleCode() != RoleCode.ADMIN) {
            throw new ApiException(403, "无权限访问");
        }
        return currentUser;
    }

    private Map<String, Object> toView(TaskExecutionLogEntity log) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("logId", log.getLogId());
        result.put("taskCode", log.getTaskCode());
        result.put("taskName", log.getTaskName());
        result.put("status", log.getStatus());
        result.put("startedAt", formatDateTime(log.getStartedAt()));
        result.put("finishedAt", formatDateTime(log.getFinishedAt()));
        result.put("durationMs", log.getDurationMs());
        result.put("summary", log.getSummary());
        result.put("errorMessage", log.getErrorMessage());
        result.put("resultSnapshot", log.getResultSnapshot());
        return result;
    }

    private String buildSummary(Map<String, Object> resultSnapshot) {
        if (resultSnapshot.isEmpty()) {
            return "no result";
        }
        return resultSnapshot.entrySet().stream()
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .reduce((left, right) -> left + ", " + right)
                .orElse("no result");
    }

    private String formatDateTime(LocalDateTime dateTime) {
        return dateTime == null ? null : dateTime.format(DATE_TIME_FORMATTER);
    }
}
