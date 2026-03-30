package com.lab.booking.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lab.booking.model.ReservationEntity;
import com.lab.booking.model.ReservationStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

@Repository
public class ReservationRepository extends JdbcRepositorySupport {

    private final JdbcTemplate jdbcTemplate;

    public ReservationRepository(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        super(objectMapper);
        this.jdbcTemplate = jdbcTemplate;
    }

    public long nextReservationId() {
        Long next = jdbcTemplate.queryForObject("SELECT COALESCE(MAX(reservation_id), 2000) + 1 FROM lab_reservation", Long.class);
        return next == null ? 2001L : next;
    }

    public void save(ReservationEntity reservation) {
        jdbcTemplate.update("""
                        INSERT INTO lab_reservation (
                            reservation_id, applicant_id, device_id, start_time, end_time, purpose, status, reviewer_id, review_comment, created_at
                        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        ON DUPLICATE KEY UPDATE
                            applicant_id = VALUES(applicant_id),
                            device_id = VALUES(device_id),
                            start_time = VALUES(start_time),
                            end_time = VALUES(end_time),
                            purpose = VALUES(purpose),
                            status = VALUES(status),
                            reviewer_id = VALUES(reviewer_id),
                            review_comment = VALUES(review_comment),
                            created_at = VALUES(created_at)
                        """,
                reservation.getReservationId(),
                reservation.getApplicantId(),
                reservation.getDeviceId(),
                reservation.getStartTime(),
                reservation.getEndTime(),
                reservation.getPurpose(),
                reservation.getStatus().name(),
                reservation.getReviewerId(),
                reservation.getReviewComment(),
                reservation.getCreatedAt()
        );
    }

    public Optional<ReservationEntity> findById(Long reservationId) {
        return jdbcTemplate.query("SELECT * FROM lab_reservation WHERE reservation_id = ? LIMIT 1", this::mapReservation, reservationId)
                .stream()
                .findFirst();
    }

    public List<ReservationEntity> findAll() {
        return jdbcTemplate.query("SELECT * FROM lab_reservation ORDER BY reservation_id", this::mapReservation);
    }

    private ReservationEntity mapReservation(ResultSet rs, int rowNum) throws SQLException {
        ReservationEntity reservation = new ReservationEntity();
        reservation.setReservationId(rs.getLong("reservation_id"));
        reservation.setApplicantId(rs.getLong("applicant_id"));
        reservation.setDeviceId(rs.getLong("device_id"));
        reservation.setStartTime(toLocalDateTime(rs.getTimestamp("start_time")));
        reservation.setEndTime(toLocalDateTime(rs.getTimestamp("end_time")));
        reservation.setPurpose(rs.getString("purpose"));
        reservation.setStatus(ReservationStatus.valueOf(rs.getString("status")));
        reservation.setReviewerId(rs.getObject("reviewer_id", Long.class));
        reservation.setReviewComment(rs.getString("review_comment"));
        reservation.setCreatedAt(toLocalDateTime(rs.getTimestamp("created_at")));
        return reservation;
    }
}
