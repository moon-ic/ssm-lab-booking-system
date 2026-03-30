package com.lab.booking.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lab.booking.model.TaskExecutionLogEntity;
import com.lab.booking.model.TaskExecutionStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

@Repository
public class TaskExecutionLogRepository extends JdbcRepositorySupport {

    private final JdbcTemplate jdbcTemplate;

    public TaskExecutionLogRepository(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        super(objectMapper);
        this.jdbcTemplate = jdbcTemplate;
    }

    public long nextLogId() {
        Long next = jdbcTemplate.queryForObject("SELECT COALESCE(MAX(log_id), 9000) + 1 FROM lab_task_execution_log", Long.class);
        return next == null ? 9001L : next;
    }

    public void save(TaskExecutionLogEntity log) {
        jdbcTemplate.update("""
                        INSERT INTO lab_task_execution_log (
                            log_id, task_code, task_name, status, started_at, finished_at, duration_ms, summary, error_message, result_snapshot_json
                        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        ON DUPLICATE KEY UPDATE
                            task_code = VALUES(task_code),
                            task_name = VALUES(task_name),
                            status = VALUES(status),
                            started_at = VALUES(started_at),
                            finished_at = VALUES(finished_at),
                            duration_ms = VALUES(duration_ms),
                            summary = VALUES(summary),
                            error_message = VALUES(error_message),
                            result_snapshot_json = VALUES(result_snapshot_json)
                        """,
                log.getLogId(),
                log.getTaskCode(),
                log.getTaskName(),
                log.getStatus().name(),
                log.getStartedAt(),
                log.getFinishedAt(),
                log.getDurationMs(),
                log.getSummary(),
                log.getErrorMessage(),
                toJson(log.getResultSnapshot())
        );
    }

    public Optional<TaskExecutionLogEntity> findById(Long logId) {
        return jdbcTemplate.query("SELECT * FROM lab_task_execution_log WHERE log_id = ? LIMIT 1", this::mapLog, logId)
                .stream()
                .findFirst();
    }

    public List<TaskExecutionLogEntity> findAll() {
        return jdbcTemplate.query("SELECT * FROM lab_task_execution_log ORDER BY log_id", this::mapLog);
    }

    private TaskExecutionLogEntity mapLog(ResultSet rs, int rowNum) throws SQLException {
        TaskExecutionLogEntity log = new TaskExecutionLogEntity();
        log.setLogId(rs.getLong("log_id"));
        log.setTaskCode(rs.getString("task_code"));
        log.setTaskName(rs.getString("task_name"));
        log.setStatus(TaskExecutionStatus.valueOf(rs.getString("status")));
        log.setStartedAt(toLocalDateTime(rs.getTimestamp("started_at")));
        log.setFinishedAt(toLocalDateTime(rs.getTimestamp("finished_at")));
        log.setDurationMs(rs.getObject("duration_ms", Long.class));
        log.setSummary(rs.getString("summary"));
        log.setErrorMessage(rs.getString("error_message"));
        log.setResultSnapshot(toMap(rs.getString("result_snapshot_json")));
        return log;
    }
}
