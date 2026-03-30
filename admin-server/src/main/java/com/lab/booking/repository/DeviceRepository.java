package com.lab.booking.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lab.booking.model.DeviceEntity;
import com.lab.booking.model.DeviceStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

@Repository
public class DeviceRepository extends JdbcRepositorySupport {

    private final JdbcTemplate jdbcTemplate;

    public DeviceRepository(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        super(objectMapper);
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<DeviceEntity> findAll() {
        return jdbcTemplate.query("SELECT * FROM lab_device ORDER BY device_id", this::mapDevice);
    }

    public Optional<DeviceEntity> findById(Long deviceId) {
        return jdbcTemplate.query("SELECT * FROM lab_device WHERE device_id = ? LIMIT 1", this::mapDevice, deviceId)
                .stream()
                .findFirst();
    }

    public boolean existsByDeviceCode(String deviceCode, Long excludeId) {
        Integer count = jdbcTemplate.queryForObject("""
                        SELECT COUNT(1) FROM lab_device
                        WHERE device_code = ? AND (? IS NULL OR device_id <> ?)
                        """,
                Integer.class,
                deviceCode,
                excludeId,
                excludeId
        );
        return count != null && count > 0;
    }

    public long nextDeviceId() {
        Long next = jdbcTemplate.queryForObject("SELECT COALESCE(MAX(device_id), 1000) + 1 FROM lab_device", Long.class);
        return next == null ? 1001L : next;
    }

    public void save(DeviceEntity device) {
        jdbcTemplate.update("""
                        INSERT INTO lab_device (
                            device_id, device_name, device_code, category, status, location, image_url, description
                        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                        ON DUPLICATE KEY UPDATE
                            device_name = VALUES(device_name),
                            device_code = VALUES(device_code),
                            category = VALUES(category),
                            status = VALUES(status),
                            location = VALUES(location),
                            image_url = VALUES(image_url),
                            description = VALUES(description)
                        """,
                device.getDeviceId(),
                device.getDeviceName(),
                device.getDeviceCode(),
                device.getCategory(),
                device.getStatus().name(),
                device.getLocation(),
                device.getImageUrl(),
                device.getDescription()
        );
    }

    private DeviceEntity mapDevice(ResultSet rs, int rowNum) throws SQLException {
        DeviceEntity device = new DeviceEntity();
        device.setDeviceId(rs.getLong("device_id"));
        device.setDeviceName(normalizeLegacyText(rs.getString("device_name")));
        device.setDeviceCode(rs.getString("device_code"));
        device.setCategory(normalizeLegacyText(rs.getString("category")));
        device.setStatus(DeviceStatus.valueOf(rs.getString("status")));
        device.setLocation(normalizeLegacyText(rs.getString("location")));
        device.setImageUrl(rs.getString("image_url"));
        device.setDescription(normalizeLegacyText(rs.getString("description")));
        return device;
    }
}
