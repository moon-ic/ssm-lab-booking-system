package com.lab.booking.service;

import com.lab.booking.common.ApiException;
import com.lab.booking.dto.RepairDtos;
import com.lab.booking.mapper.BorrowRecordMapper;
import com.lab.booking.mapper.DeviceMapper;
import com.lab.booking.mapper.RepairMapper;
import com.lab.booking.model.BorrowStatus;
import com.lab.booking.model.DeviceEntity;
import com.lab.booking.model.DeviceStatus;
import com.lab.booking.model.RepairEntity;
import com.lab.booking.model.RepairStatus;
import com.lab.booking.model.RoleCode;
import com.lab.booking.model.UserEntity;
import com.lab.booking.repository.AuthRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class RepairService {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final RepairMapper repairMapper;
    private final BorrowRecordMapper borrowRecordMapper;
    private final DeviceMapper deviceMapper;
    private final AuthRepository authRepository;
    private final AuthService authService;

    public RepairService(
            RepairMapper repairMapper,
            BorrowRecordMapper borrowRecordMapper,
            DeviceMapper deviceMapper,
            AuthRepository authRepository,
            AuthService authService
    ) {
        this.repairMapper = repairMapper;
        this.borrowRecordMapper = borrowRecordMapper;
        this.deviceMapper = deviceMapper;
        this.authRepository = authRepository;
        this.authService = authService;
    }

    public Map<String, Object> createRepair(RepairDtos.CreateRepairRequest request) {
        UserEntity applicant = requireRoles(RoleCode.STUDENT);
        DeviceEntity device = getExistingDevice(request.deviceId());
        ensureBorrowingByApplicant(request.deviceId(), applicant.getUserId());
        ensureNoActiveRepair(request.deviceId());

        RepairEntity repair = new RepairEntity();
        Long nextId = repairMapper.selectNextRepairId();
        repair.setRepairId(nextId == null ? 4001L : nextId);
        repair.setDeviceId(request.deviceId());
        repair.setApplicantId(applicant.getUserId());
        repair.setDescription(request.description().trim());
        repair.setStatus(RepairStatus.PENDING);
        repair.setCreatedAt(LocalDateTime.now());
        repair.setUpdatedAt(repair.getCreatedAt());
        repairMapper.upsertRepair(repair);

        device.setStatus(DeviceStatus.REPAIRING);
        deviceMapper.upsertDevice(device);
        return toRepairView(repair);
    }

    public Map<String, Object> listRepairs(RepairStatus status, Long deviceId, Long applicantId, Integer pageNum, Integer pageSize) {
        UserEntity currentUser = requireRoles(RoleCode.SUPER_ADMIN, RoleCode.ADMIN, RoleCode.TEACHER, RoleCode.STUDENT);
        List<Map<String, Object>> filtered = repairMapper.selectAll().stream()
                .filter(repair -> visibleTo(currentUser, repair))
                .filter(repair -> status == null || repair.getStatus() == status)
                .filter(repair -> deviceId == null || Objects.equals(repair.getDeviceId(), deviceId))
                .filter(repair -> applicantId == null || Objects.equals(repair.getApplicantId(), applicantId))
                .sorted(Comparator.comparing(RepairEntity::getRepairId))
                .map(this::toRepairView)
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

    public Map<String, Object> getRepairDetail(Long repairId) {
        UserEntity currentUser = requireRoles(RoleCode.SUPER_ADMIN, RoleCode.ADMIN, RoleCode.TEACHER, RoleCode.STUDENT);
        RepairEntity repair = getExistingRepair(repairId);
        if (!visibleTo(currentUser, repair)) {
            throw new ApiException(403, "权限不够");
        }
        return toRepairView(repair);
    }

    public Map<String, Object> updateRepairStatus(Long repairId, RepairDtos.UpdateRepairStatusRequest request) {
        requireRoles(RoleCode.SUPER_ADMIN, RoleCode.ADMIN);
        RepairEntity repair = getExistingRepair(repairId);
        DeviceEntity device = getExistingDevice(repair.getDeviceId());

        repair.setStatus(request.status());
        repair.setComment(blankToNull(request.comment()));
        repair.setUpdatedAt(LocalDateTime.now());
        repairMapper.upsertRepair(repair);

        switch (request.status()) {
            case PENDING, PROCESSING -> device.setStatus(DeviceStatus.REPAIRING);
            case COMPLETED -> device.setStatus(DeviceStatus.AVAILABLE);
            case UNREPAIRABLE -> device.setStatus(DeviceStatus.DAMAGED);
        }
        deviceMapper.upsertDevice(device);

        return toRepairView(repair);
    }

    private void ensureBorrowingByApplicant(Long deviceId, Long applicantId) {
        boolean borrowingRecordExists = borrowRecordMapper.selectAll().stream()
                .anyMatch(record -> Objects.equals(record.getDeviceId(), deviceId)
                        && Objects.equals(record.getUserId(), applicantId)
                        && (record.getStatus() == BorrowStatus.BORROWING || record.getStatus() == BorrowStatus.OVERDUE));
        if (!borrowingRecordExists) {
            throw new ApiException(409, "仅可为当前正在借用中的设备申请报修");
        }
    }

    private void ensureNoActiveRepair(Long deviceId) {
        boolean activeRepairExists = repairMapper.selectAll().stream()
                .anyMatch(repair -> Objects.equals(repair.getDeviceId(), deviceId)
                        && repair.getStatus() != RepairStatus.COMPLETED
                        && repair.getStatus() != RepairStatus.UNREPAIRABLE);
        if (activeRepairExists) {
            throw new ApiException(409, "该设备已存在进行中的维修申请");
        }
    }

    private boolean visibleTo(UserEntity currentUser, RepairEntity repair) {
        return switch (currentUser.getRoleCode()) {
            case SUPER_ADMIN, ADMIN -> true;
            case TEACHER -> {
                UserEntity applicant = getExistingUser(repair.getApplicantId());
                yield applicant.getRoleCode() == RoleCode.STUDENT
                        && Objects.equals(applicant.getManagerId(), currentUser.getUserId());
            }
            case STUDENT -> Objects.equals(repair.getApplicantId(), currentUser.getUserId());
        };
    }

    private RepairEntity getExistingRepair(Long repairId) {
        RepairEntity repair = repairMapper.selectById(repairId);
        if (repair == null) {
            throw new ApiException(404, "维修申请不存在");
        }
        return repair;
    }

    private DeviceEntity getExistingDevice(Long deviceId) {
        DeviceEntity device = deviceMapper.selectById(deviceId);
        if (device == null) {
            throw new ApiException(404, "设备不存在");
        }
        return device;
    }

    private DeviceEntity findDevice(Long deviceId) {
        return deviceMapper.selectById(deviceId);
    }

    private UserEntity getExistingUser(Long userId) {
        return authRepository.findById(userId)
                .orElseThrow(() -> new ApiException(404, "用户不存在"));
    }

    private UserEntity requireRoles(RoleCode... allowedRoles) {
        UserEntity currentUser = authService.currentUser();
        for (RoleCode role : allowedRoles) {
            if (currentUser.getRoleCode() == role) {
                return currentUser;
            }
        }
        throw new ApiException(403, "权限不够");
    }

    private Map<String, Object> toRepairView(RepairEntity repair) {
        UserEntity applicant = getExistingUser(repair.getApplicantId());
        DeviceEntity device = findDevice(repair.getDeviceId());
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("repairId", repair.getRepairId());
        result.put("deviceId", repair.getDeviceId());
        result.put("deviceName", device == null ? "已删除设备 #" + repair.getDeviceId() : device.getDeviceName());
        result.put("applicantId", repair.getApplicantId());
        result.put("applicantName", applicant.getName());
        result.put("description", repair.getDescription());
        result.put("status", repair.getStatus());
        result.put("comment", repair.getComment());
        result.put("createdAt", formatDateTime(repair.getCreatedAt()));
        result.put("updatedAt", formatDateTime(repair.getUpdatedAt()));
        return result;
    }

    private String formatDateTime(LocalDateTime dateTime) {
        return dateTime == null ? null : dateTime.format(DATE_TIME_FORMATTER);
    }

    private String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
