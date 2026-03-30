package com.lab.booking.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lab.booking.model.BorrowRecordEntity;
import com.lab.booking.model.BorrowStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

@Repository
public class BorrowRecordRepository extends JdbcRepositorySupport {

    private final JdbcTemplate jdbcTemplate;

    public BorrowRecordRepository(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        super(objectMapper);
        this.jdbcTemplate = jdbcTemplate;
    }

    public long nextRecordId() {
        Long next = jdbcTemplate.queryForObject("SELECT COALESCE(MAX(record_id), 3000) + 1 FROM lab_borrow_record", Long.class);
        return next == null ? 3001L : next;
    }

    public void save(BorrowRecordEntity record) {
        jdbcTemplate.update("""
                        INSERT INTO lab_borrow_record (
                            record_id, reservation_id, user_id, device_id, status, pickup_time, expected_return_time, return_time, device_condition
                        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                        ON DUPLICATE KEY UPDATE
                            reservation_id = VALUES(reservation_id),
                            user_id = VALUES(user_id),
                            device_id = VALUES(device_id),
                            status = VALUES(status),
                            pickup_time = VALUES(pickup_time),
                            expected_return_time = VALUES(expected_return_time),
                            return_time = VALUES(return_time),
                            device_condition = VALUES(device_condition)
                        """,
                record.getRecordId(),
                record.getReservationId(),
                record.getUserId(),
                record.getDeviceId(),
                record.getStatus().name(),
                record.getPickupTime(),
                record.getExpectedReturnTime(),
                record.getReturnTime(),
                record.getDeviceCondition()
        );
    }

    public Optional<BorrowRecordEntity> findById(Long recordId) {
        return jdbcTemplate.query("SELECT * FROM lab_borrow_record WHERE record_id = ? LIMIT 1", this::mapRecord, recordId)
                .stream()
                .findFirst();
    }

    public Optional<BorrowRecordEntity> findByReservationId(Long reservationId) {
        return jdbcTemplate.query("SELECT * FROM lab_borrow_record WHERE reservation_id = ? LIMIT 1", this::mapRecord, reservationId)
                .stream()
                .findFirst();
    }

    public List<BorrowRecordEntity> findAll() {
        return jdbcTemplate.query("SELECT * FROM lab_borrow_record ORDER BY record_id", this::mapRecord);
    }

    private BorrowRecordEntity mapRecord(ResultSet rs, int rowNum) throws SQLException {
        BorrowRecordEntity record = new BorrowRecordEntity();
        record.setRecordId(rs.getLong("record_id"));
        record.setReservationId(rs.getLong("reservation_id"));
        record.setUserId(rs.getLong("user_id"));
        record.setDeviceId(rs.getLong("device_id"));
        record.setStatus(BorrowStatus.valueOf(rs.getString("status")));
        record.setPickupTime(toLocalDateTime(rs.getTimestamp("pickup_time")));
        record.setExpectedReturnTime(toLocalDateTime(rs.getTimestamp("expected_return_time")));
        record.setReturnTime(toLocalDateTime(rs.getTimestamp("return_time")));
        record.setDeviceCondition(rs.getString("device_condition"));
        return record;
    }
}
