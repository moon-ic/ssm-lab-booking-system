package com.lab.booking.service;

import com.lab.booking.common.ApiException;
import com.lab.booking.dto.BorrowRecordDtos;
import com.lab.booking.mapper.BorrowRecordMapper;
import com.lab.booking.mapper.DeviceMapper;
import com.lab.booking.mapper.ReservationMapper;
import com.lab.booking.model.BorrowRecordEntity;
import com.lab.booking.model.BorrowStatus;
import com.lab.booking.model.DeviceEntity;
import com.lab.booking.model.DeviceStatus;
import com.lab.booking.model.ReservationEntity;
import com.lab.booking.model.ReservationStatus;
import com.lab.booking.model.RoleCode;
import com.lab.booking.model.UserEntity;
import com.lab.booking.repository.AuthRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class BorrowRecordService {

    private static final DateTimeFormatter SECOND_DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter MINUTE_DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final BorrowRecordMapper borrowRecordMapper;
    private final ReservationMapper reservationMapper;
    private final DeviceMapper deviceMapper;
    private final AuthRepository authRepository;
    private final AuthService authService;

    public BorrowRecordService(
            BorrowRecordMapper borrowRecordMapper,
            ReservationMapper reservationMapper,
            DeviceMapper deviceMapper,
            AuthRepository authRepository,
            AuthService authService
    ) {
        this.borrowRecordMapper = borrowRecordMapper;
        this.reservationMapper = reservationMapper;
        this.deviceMapper = deviceMapper;
        this.authRepository = authRepository;
        this.authService = authService;
    }

    public Map<String, Object> listRecords(BorrowStatus status, Long userId, Long deviceId, Integer pageNum, Integer pageSize) {
        UserEntity currentUser = requireRoles(RoleCode.SUPER_ADMIN, RoleCode.ADMIN, RoleCode.TEACHER, RoleCode.STUDENT);
        return buildRecordPage(currentUser, status, userId, deviceId, pageNum, pageSize);
    }

    public Map<String, Object> listCurrentUserRecords(BorrowStatus status, Integer pageNum, Integer pageSize) {
        UserEntity currentUser = authService.currentUser();
        return buildRecordPage(currentUser, status, currentUser.getUserId(), null, pageNum, pageSize);
    }

    private Map<String, Object> buildRecordPage(
            UserEntity currentUser,
            BorrowStatus status,
            Long userId,
            Long deviceId,
            Integer pageNum,
            Integer pageSize
    ) {
        List<Map<String, Object>> filtered = borrowRecordMapper.selectAll().stream()
                .filter(record -> visibleTo(currentUser, record))
                .filter(record -> status == null || record.getStatus() == status)
                .filter(record -> userId == null || Objects.equals(record.getUserId(), userId))
                .filter(record -> deviceId == null || Objects.equals(record.getDeviceId(), deviceId))
                .sorted(Comparator.comparing(BorrowRecordEntity::getRecordId))
                .map(this::toRecordView)
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

    public Map<String, Object> pickup(Long recordId, BorrowRecordDtos.PickupRequest request) {
        UserEntity currentUser = requireRoles(RoleCode.STUDENT, RoleCode.TEACHER);
        BorrowRecordEntity record = getExistingRecord(recordId);
        if (!Objects.equals(record.getUserId(), currentUser.getUserId())) {
            throw new ApiException(403, "仅本人可确认领取");
        }
        if (record.getStatus() != BorrowStatus.PICKUP_PENDING) {
            throw new ApiException(409, "当前借用记录状态不可领取");
        }

        ReservationEntity reservation = getExistingReservation(record.getReservationId());
        if (reservation.getStatus() != ReservationStatus.PICKUP_PENDING) {
            throw new ApiException(409, "当前预约状态不可领取");
        }

        record.setPickupTime(LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES));
        record.setStatus(BorrowStatus.BORROWING);
        borrowRecordMapper.upsertBorrowRecord(record);

        DeviceEntity device = getExistingDevice(record.getDeviceId());
        device.setStatus(DeviceStatus.BORROWED);
        deviceMapper.upsertDevice(device);

        return toRecordView(record);
    }

    public Map<String, Object> returnDevice(Long recordId, BorrowRecordDtos.ReturnRequest request) {
        UserEntity currentUser = requireRoles(RoleCode.STUDENT, RoleCode.TEACHER);
        BorrowRecordEntity record = getExistingRecord(recordId);
        if (!Objects.equals(record.getUserId(), currentUser.getUserId())) {
            throw new ApiException(403, "仅本人可归还设备");
        }
        if (record.getStatus() != BorrowStatus.BORROWING && record.getStatus() != BorrowStatus.OVERDUE) {
            throw new ApiException(409, "当前借用记录状态不可归还");
        }

        LocalDateTime returnTime = parseDateTime(request.returnTime(), "returnTime");
        String actualCondition = request.deviceCondition().trim().toUpperCase();
        record.setReturnTime(returnTime);
        record.setDeviceCondition(actualCondition);
        if (record.getStatus() == BorrowStatus.OVERDUE || returnTime.isAfter(record.getExpectedReturnTime())) {
            record.setStatus(BorrowStatus.OVERDUE);
        } else {
            record.setStatus(BorrowStatus.RETURNED);
        }
        borrowRecordMapper.upsertBorrowRecord(record);

        DeviceEntity device = getExistingDevice(record.getDeviceId());
        device.setStatus("NORMAL".equals(actualCondition) ? DeviceStatus.AVAILABLE : DeviceStatus.DAMAGED);
        deviceMapper.upsertDevice(device);

        return toRecordView(record);
    }

    public void markOverdue(Long recordId) {
        requireRoles(RoleCode.SUPER_ADMIN, RoleCode.ADMIN);
        BorrowRecordEntity record = getExistingRecord(recordId);
        if (record.getStatus() != BorrowStatus.BORROWING) {
            throw new ApiException(409, "当前借用记录状态不可标记逾期");
        }
        record.setStatus(BorrowStatus.OVERDUE);
        borrowRecordMapper.upsertBorrowRecord(record);
    }

    public List<Map<String, Object>> listReminders(String type) {
        requireRoles(RoleCode.SUPER_ADMIN, RoleCode.ADMIN);
        String actualType = type == null ? null : type.trim().toUpperCase();
        if (!"ABOUT_TO_EXPIRE".equals(actualType) && !"OVERDUE".equals(actualType)) {
            throw new ApiException(400, "提醒类型仅支持 ABOUT_TO_EXPIRE 或 OVERDUE");
        }

        LocalDateTime now = LocalDateTime.now();
        return borrowRecordMapper.selectAll().stream()
                .filter(record -> matchesReminderType(actualType, record, now))
                .sorted(Comparator.comparing(BorrowRecordEntity::getRecordId))
                .map(record -> {
                    Map<String, Object> result = toRecordView(record);
                    result.put("reminderType", actualType);
                    return result;
                })
                .toList();
    }

    public void createPendingRecordFromReservation(ReservationEntity reservation) {
        BorrowRecordEntity existing = borrowRecordMapper.selectByReservationId(reservation.getReservationId());
        if (existing != null) {
            return;
        }

        BorrowRecordEntity record = new BorrowRecordEntity();
        Long nextId = borrowRecordMapper.selectNextRecordId();
        record.setRecordId(nextId == null ? 3001L : nextId);
        record.setReservationId(reservation.getReservationId());
        record.setUserId(reservation.getApplicantId());
        record.setDeviceId(reservation.getDeviceId());
        record.setStatus(BorrowStatus.PICKUP_PENDING);
        record.setExpectedReturnTime(reservation.getEndTime());
        borrowRecordMapper.upsertBorrowRecord(record);
    }

    public boolean canExpireReservation(Long reservationId) {
        BorrowRecordEntity record = borrowRecordMapper.selectByReservationId(reservationId);
        return record != null && record.getStatus() == BorrowStatus.PICKUP_PENDING;
    }

    private boolean matchesReminderType(String type, BorrowRecordEntity record, LocalDateTime now) {
        if ("ABOUT_TO_EXPIRE".equals(type)) {
            return record.getStatus() == BorrowStatus.BORROWING
                    && !record.getExpectedReturnTime().isBefore(now)
                    && !record.getExpectedReturnTime().isAfter(now.plusDays(3));
        }
        return record.getStatus() == BorrowStatus.OVERDUE
                || (record.getStatus() == BorrowStatus.BORROWING && record.getExpectedReturnTime().isBefore(now));
    }

    private boolean visibleTo(UserEntity currentUser, BorrowRecordEntity record) {
        return switch (currentUser.getRoleCode()) {
            case SUPER_ADMIN, ADMIN -> true;
            case TEACHER -> {
                if (Objects.equals(record.getUserId(), currentUser.getUserId())) {
                    yield true;
                }
                UserEntity applicant = getExistingUser(record.getUserId());
                yield applicant.getRoleCode() == RoleCode.STUDENT
                        && Objects.equals(applicant.getManagerId(), currentUser.getUserId());
            }
            case STUDENT -> Objects.equals(record.getUserId(), currentUser.getUserId());
        };
    }

    private BorrowRecordEntity getExistingRecord(Long recordId) {
        BorrowRecordEntity record = borrowRecordMapper.selectById(recordId);
        if (record == null) {
            throw new ApiException(404, "借用记录不存在");
        }
        return record;
    }

    private ReservationEntity getExistingReservation(Long reservationId) {
        ReservationEntity reservation = reservationMapper.selectById(reservationId);
        if (reservation == null) {
            throw new ApiException(404, "预约不存在");
        }
        return reservation;
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

    private Map<String, Object> toRecordView(BorrowRecordEntity record) {
        UserEntity applicant = getExistingUser(record.getUserId());
        DeviceEntity device = findDevice(record.getDeviceId());
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("recordId", record.getRecordId());
        result.put("reservationId", record.getReservationId());
        result.put("userId", record.getUserId());
        result.put("userName", applicant.getName());
        result.put("deviceId", record.getDeviceId());
        result.put("deviceName", device == null ? "已删除设备 #" + record.getDeviceId() : device.getDeviceName());
        result.put("status", record.getStatus());
        result.put("pickupTime", formatDateTime(record.getPickupTime()));
        result.put("expectedReturnTime", formatDateTime(record.getExpectedReturnTime()));
        result.put("returnTime", formatDateTime(record.getReturnTime()));
        result.put("deviceCondition", record.getDeviceCondition());
        return result;
    }

    private LocalDateTime parseDateTime(String value, String fieldName) {
        String actualValue = value == null ? "" : value.trim();
        if (actualValue.isEmpty()) {
            throw new ApiException(400, fieldName + " 时间不能为空");
        }
        try {
            return LocalDateTime.parse(actualValue, SECOND_DATE_TIME_FORMATTER);
        } catch (DateTimeParseException ex) {
            try {
                return LocalDateTime.parse(actualValue, MINUTE_DATE_TIME_FORMATTER);
            } catch (DateTimeParseException ignored) {
                throw new ApiException(400, fieldName + " 时间格式错误，需要 yyyy-MM-dd HH:mm");
            }
        }
    }

    private String formatDateTime(LocalDateTime dateTime) {
        return dateTime == null ? null : dateTime.format(MINUTE_DATE_TIME_FORMATTER);
    }
}
