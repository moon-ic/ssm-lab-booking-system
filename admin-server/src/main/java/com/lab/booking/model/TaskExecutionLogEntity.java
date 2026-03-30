package com.lab.booking.model;

import java.time.LocalDateTime;
import java.util.Map;

public class TaskExecutionLogEntity {

    private Long logId;
    private String taskCode;
    private String taskName;
    private TaskExecutionStatus status;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private Long durationMs;
    private String summary;
    private String errorMessage;
    private Map<String, Object> resultSnapshot;

    public Long getLogId() { return logId; }
    public void setLogId(Long logId) { this.logId = logId; }
    public String getTaskCode() { return taskCode; }
    public void setTaskCode(String taskCode) { this.taskCode = taskCode; }
    public String getTaskName() { return taskName; }
    public void setTaskName(String taskName) { this.taskName = taskName; }
    public TaskExecutionStatus getStatus() { return status; }
    public void setStatus(TaskExecutionStatus status) { this.status = status; }
    public LocalDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }
    public LocalDateTime getFinishedAt() { return finishedAt; }
    public void setFinishedAt(LocalDateTime finishedAt) { this.finishedAt = finishedAt; }
    public Long getDurationMs() { return durationMs; }
    public void setDurationMs(Long durationMs) { this.durationMs = durationMs; }
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public Map<String, Object> getResultSnapshot() { return resultSnapshot; }
    public void setResultSnapshot(Map<String, Object> resultSnapshot) { this.resultSnapshot = resultSnapshot; }
}
