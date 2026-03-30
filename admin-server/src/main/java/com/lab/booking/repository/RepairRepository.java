package com.lab.booking.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lab.booking.model.RepairEntity;
import com.lab.booking.model.RepairStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

@Repository
public class RepairRepository extends JdbcRepositorySupport {

    private final JdbcTemplate jdbcTemplate;

    public RepairRepository(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        super(objectMapper);
        this.jdbcTemplate = jdbcTemplate;
    }

    public long nextRepairId() {
        Long next = jdbcTemplate.queryForObject("SELECT COALESCE(MAX(repair_id), 4000) + 1 FROM lab_repair", Long.class);
        return next == null ? 4001L : next;
    }

    public void save(RepairEntity repair) {
        jdbcTemplate.update("""
                        INSERT INTO lab_repair (
                            repair_id, device_id, applicant_id, description, status, comment, created_at, updated_at
                        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                        ON DUPLICATE KEY UPDATE
                            device_id = VALUES(device_id),
                            applicant_id = VALUES(applicant_id),
                            description = VALUES(description),
                            status = VALUES(status),
                            comment = VALUES(comment),
                            created_at = VALUES(created_at),
                            updated_at = VALUES(updated_at)
                        """,
                repair.getRepairId(),
                repair.getDeviceId(),
                repair.getApplicantId(),
                repair.getDescription(),
                repair.getStatus().name(),
                repair.getComment(),
                repair.getCreatedAt(),
                repair.getUpdatedAt()
        );
    }

    public Optional<RepairEntity> findById(Long repairId) {
        return jdbcTemplate.query("SELECT * FROM lab_repair WHERE repair_id = ? LIMIT 1", this::mapRepair, repairId)
                .stream()
                .findFirst();
    }

    public List<RepairEntity> findAll() {
        return jdbcTemplate.query("SELECT * FROM lab_repair ORDER BY repair_id", this::mapRepair);
    }

    private RepairEntity mapRepair(ResultSet rs, int rowNum) throws SQLException {
        RepairEntity repair = new RepairEntity();
        repair.setRepairId(rs.getLong("repair_id"));
        repair.setDeviceId(rs.getLong("device_id"));
        repair.setApplicantId(rs.getLong("applicant_id"));
        repair.setDescription(rs.getString("description"));
        repair.setStatus(RepairStatus.valueOf(rs.getString("status")));
        repair.setComment(rs.getString("comment"));
        repair.setCreatedAt(toLocalDateTime(rs.getTimestamp("created_at")));
        repair.setUpdatedAt(toLocalDateTime(rs.getTimestamp("updated_at")));
        return repair;
    }
}
