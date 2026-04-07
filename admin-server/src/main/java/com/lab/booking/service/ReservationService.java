package com.lab.booking.service;

import com.lab.booking.common.ApiException;
import com.lab.booking.dto.ReservationDtos;
import com.lab.booking.model.BorrowRecordEntity;
import com.lab.booking.model.BorrowStatus;
import com.lab.booking.model.DeviceEntity;
import com.lab.booking.model.DeviceStatus;
import com.lab.booking.model.ReservationEntity;
import com.lab.booking.model.ReservationStatus;
import com.lab.booking.model.RoleCode;
import com.lab.booking.model.UserEntity;
import com.lab.booking.repository.AuthRepository;
import com.lab.booking.repository.BorrowRecordRepository;
import com.lab.booking.repository.DeviceRepository;
import com.lab.booking.repository.ReservationRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class ReservationService {

    private static final DateTimeFormatter SECOND_DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter MINUTE_DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final ReservationRepository reservationRepository;
    private final DeviceRepository deviceRepository;
    private final AuthRepository authRepository;
    private final BorrowRecordRepository borrowRecordRepository;
    private final AuthService authService;

    public ReservationService(
            ReservationRepository reservationRepository,
            DeviceRepository deviceRepository,
            AuthRepository authRepository,
            BorrowRecordRepository borrowRecordRepository,
            AuthService authService
    ) {
        this.reservationRepository = reservationRepository;
        this.deviceRepository = deviceRepository;
        this.authRepository = authRepository;
        this.borrowRecordRepository = borrowRecordRepository;
        this.authService = authService;
    }

    public Map<String, Object> createReservation(ReservationDtos.CreateReservationRequest request) {
        UserEntity applicant = requireRoles(RoleCode.STUDENT);
        DeviceEntity device = getExistingDevice(request.deviceId());
        LocalDateTime startTime = parseDateTime(request.startTime(), "startTime");
        LocalDateTime endTime = parseDateTime(request.endTime(), "endTime");
        if (!endTime.isAfter(startTime)) {
            throw new ApiException(400, "预约结束时间必须晚于开始时间");
        }
        ensureReservable(device);
        ensureNoTimeConflict(request.deviceId(), startTime, endTime, null);

        ReservationEntity reservation = new ReservationEntity();
        reservation.setReservationId(reservationRepository.nextReservationId());
        reservation.setApplicantId(applicant.getUserId());
        reservation.setDeviceId(request.deviceId());
        reservation.setStartTime(startTime);
        reservation.setEndTime(endTime);
        reservation.setPurpose(request.purpose().trim());
        reservation.setStatus(ReservationStatus.PENDING);
        reservation.setCreatedAt(LocalDateTime.now());
        reservationRepository.save(reservation);
        return toReservationDetail(reservation);
    }

    public Map<String, Object> listReservations(
            ReservationStatus status,
            Long deviceId,
            Long applicantId,
            Integer pageNum,
            Integer pageSize
    ) {
        UserEntity currentUser = requireRoles(RoleCode.SUPER_ADMIN, RoleCode.ADMIN, RoleCode.TEACHER, RoleCode.STUDENT);
        List<Map<String, Object>> filtered = reservationRepository.findAll().stream()
                .filter(reservation -> visibleTo(currentUser, reservation))
                .filter(reservation -> status == null || reservation.getStatus() == status)
                .filter(reservation -> deviceId == null || Objects.equals(reservation.getDeviceId(), deviceId))
                .filter(reservation -> applicantId == null || Objects.equals(reservation.getApplicantId(), applicantId))
                .sorted(Comparator.comparing(ReservationEntity::getReservationId))
                .map(this::toReservationSummary)
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

    public Map<String, Object> getReservationDetail(Long reservationId) {
        UserEntity currentUser = requireRoles(RoleCode.SUPER_ADMIN, RoleCode.ADMIN, RoleCode.TEACHER, RoleCode.STUDENT);
        ReservationEntity reservation = getExistingReservation(reservationId);
        if (!visibleTo(currentUser, reservation)) {
            throw new ApiException(403, "权限不够");
        }
        return toReservationDetail(reservation);
    }

    public Map<String, Object> approveReservation(Long reservationId, ReservationDtos.ApproveReservationRequest request) {
        UserEntity reviewer = requireRoles(RoleCode.SUPER_ADMIN, RoleCode.ADMIN, RoleCode.TEACHER);
        ReservationEntity reservation = getExistingReservation(reservationId);
        if (reviewer.getRoleCode() == RoleCode.TEACHER && !visibleTo(reviewer, reservation)) {
            throw new ApiException(403, "权限不够，不能审核该预约");
        }
        if (reservation.getStatus() != ReservationStatus.PENDING && reservation.getStatus() != ReservationStatus.APPROVED) {
            throw new ApiException(409, "当前预约状态不可审核");
        }

        String action = request.action().trim().toUpperCase();
        switch (action) {
            case "APPROVE" -> approveReservationInternal(reservation, reviewer, request.comment());
            case "REJECT" -> rejectReservationInternal(reservation, reviewer, request.comment());
            default -> throw new ApiException(400, "审核动作仅支持 APPROVE 或 REJECT");
        }

        reservationRepository.save(reservation);
        return toReservationDetail(reservation);
    }

    public void cancelReservation(Long reservationId) {
        UserEntity currentUser = requireRoles(RoleCode.STUDENT);
        ReservationEntity reservation = getExistingReservation(reservationId);
        if (!Objects.equals(reservation.getApplicantId(), currentUser.getUserId())) {
            throw new ApiException(403, "仅本人可取消预约");
        }
        if (reservation.getStatus() != ReservationStatus.PENDING && reservation.getStatus() != ReservationStatus.APPROVED) {
            throw new ApiException(409, "当前预约状态不可取消");
        }
        reservation.setStatus(ReservationStatus.CANCELLED);
        reservationRepository.save(reservation);
    }

    public void expireReservation(Long reservationId) {
        requireRoles(RoleCode.SUPER_ADMIN, RoleCode.ADMIN);
        ReservationEntity reservation = getExistingReservation(reservationId);
        if (reservation.getStatus() != ReservationStatus.PICKUP_PENDING) {
            throw new ApiException(409, "当前预约状态不可失效");
        }
        DeviceEntity device = getExistingDevice(reservation.getDeviceId());
        BorrowRecordEntity record = borrowRecordRepository.findByReservationId(reservationId)
                .orElseThrow(() -> new ApiException(404, "借用记录不存在"));
        if (record.getStatus() != BorrowStatus.PICKUP_PENDING || device.getStatus() != DeviceStatus.RESERVED) {
            throw new ApiException(409, "当前预约状态不可失效");
        }
        reservation.setStatus(ReservationStatus.EXPIRED);
        record.setStatus(BorrowStatus.OVERDUE);
        borrowRecordRepository.save(record);
        device.setStatus(DeviceStatus.AVAILABLE);
        deviceRepository.save(device);
        reservationRepository.save(reservation);
    }

    private void approveReservationInternal(ReservationEntity reservation, UserEntity reviewer, String comment) {
        if (reviewer.getRoleCode() == RoleCode.TEACHER) {
            if (reservation.getStatus() != ReservationStatus.PENDING) {
                throw new ApiException(409, "当前预约状态不可由教师审核通过");
            }
            reservation.setStatus(ReservationStatus.APPROVED);
            reservation.setReviewerId(reviewer.getUserId());
            reservation.setReviewComment(blankToNull(comment));
            return;
        }

        if (reservation.getStatus() != ReservationStatus.APPROVED) {
            throw new ApiException(409, "请先由教师审核通过");
        }

        DeviceEntity device = getExistingDevice(reservation.getDeviceId());
        ensureReservableForApproval(device, reservation);
        ensureNoTimeConflict(reservation.getDeviceId(), reservation.getStartTime(), reservation.getEndTime(), reservation.getReservationId());
        reservation.setStatus(ReservationStatus.PICKUP_PENDING);
        reservation.setReviewerId(reviewer.getUserId());
        reservation.setReviewComment(blankToNull(comment));
        device.setStatus(DeviceStatus.RESERVED);
        deviceRepository.save(device);
        createBorrowRecord(reservation);
    }

    private void rejectReservationInternal(ReservationEntity reservation, UserEntity reviewer, String comment) {
        if (reviewer.getRoleCode() == RoleCode.TEACHER && reservation.getStatus() != ReservationStatus.PENDING) {
            throw new ApiException(409, "当前预约状态不可由教师驳回");
        }
        String actualComment = blankToNull(comment);
        if (actualComment == null) {
            throw new ApiException(400, "驳回原因不能为空");
        }
        reservation.setStatus(ReservationStatus.REJECTED);
        reservation.setReviewerId(reviewer.getUserId());
        reservation.setReviewComment(actualComment);
    }

    private void ensureReservable(DeviceEntity device) {
        if (device.getStatus() == DeviceStatus.RESERVED
                || device.getStatus() == DeviceStatus.BORROWED
                || device.getStatus() == DeviceStatus.REPAIRING
                || device.getStatus() == DeviceStatus.DAMAGED
                || device.getStatus() == DeviceStatus.DISABLED) {
            throw new ApiException(409, "当前设备状态不可预约");
        }
    }

    private void ensureReservableForApproval(DeviceEntity device, ReservationEntity reservation) {
        if (device.getStatus() == DeviceStatus.BORROWED
                || device.getStatus() == DeviceStatus.REPAIRING
                || device.getStatus() == DeviceStatus.DAMAGED
                || device.getStatus() == DeviceStatus.DISABLED) {
            throw new ApiException(409, "当前设备状态不可审核通过");
        }
        if (device.getStatus() == DeviceStatus.RESERVED) {
            boolean reservedByOtherReservation = reservationRepository.findAll().stream()
                    .filter(item -> !Objects.equals(item.getReservationId(), reservation.getReservationId()))
                    .anyMatch(item -> Objects.equals(item.getDeviceId(), device.getDeviceId())
                            && item.getStatus() == ReservationStatus.PICKUP_PENDING);
            if (reservedByOtherReservation) {
                throw new ApiException(409, "当前设备已被其他预约锁定");
            }
        }
    }

    private void createBorrowRecord(ReservationEntity reservation) {
        boolean exists = borrowRecordRepository.findByReservationId(reservation.getReservationId()).isPresent();
        if (exists) {
            return;
        }

        BorrowRecordEntity record = new BorrowRecordEntity();
        record.setRecordId(borrowRecordRepository.nextRecordId());
        record.setReservationId(reservation.getReservationId());
        record.setUserId(reservation.getApplicantId());
        record.setDeviceId(reservation.getDeviceId());
        record.setStatus(BorrowStatus.PICKUP_PENDING);
        record.setExpectedReturnTime(reservation.getEndTime());
        borrowRecordRepository.save(record);
    }

    private void ensureNoTimeConflict(Long deviceId, LocalDateTime startTime, LocalDateTime endTime, Long excludeReservationId) {
        boolean conflict = reservationRepository.findAll().stream()
                .filter(item -> !Objects.equals(item.getReservationId(), excludeReservationId))
                .filter(item -> Objects.equals(item.getDeviceId(), deviceId))
                .filter(item -> item.getStatus() != ReservationStatus.REJECTED
                        && item.getStatus() != ReservationStatus.CANCELLED
                        && item.getStatus() != ReservationStatus.EXPIRED)
                .anyMatch(item -> startTime.isBefore(item.getEndTime()) && endTime.isAfter(item.getStartTime()));
        if (conflict) {
            throw new ApiException(409, "同一时间段内设备已存在预约");
        }
    }

    private ReservationEntity getExistingReservation(Long reservationId) {
        return reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ApiException(404, "预约不存在"));
    }

    private DeviceEntity getExistingDevice(Long deviceId) {
        return deviceRepository.findById(deviceId)
                .orElseThrow(() -> new ApiException(404, "设备不存在"));
    }

    private DeviceEntity findDevice(Long deviceId) {
        return deviceRepository.findById(deviceId).orElse(null);
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

    private boolean visibleTo(UserEntity currentUser, ReservationEntity reservation) {
        return switch (currentUser.getRoleCode()) {
            case SUPER_ADMIN, ADMIN -> true;
            case TEACHER -> {
                UserEntity applicant = getExistingUser(reservation.getApplicantId());
                yield applicant.getRoleCode() == RoleCode.STUDENT
                        && Objects.equals(applicant.getManagerId(), currentUser.getUserId());
            }
            case STUDENT -> Objects.equals(reservation.getApplicantId(), currentUser.getUserId());
        };
    }

    private Map<String, Object> toReservationSummary(ReservationEntity reservation) {
        Map<String, Object> result = new LinkedHashMap<>();
        UserEntity applicant = getExistingUser(reservation.getApplicantId());
        DeviceEntity device = findDevice(reservation.getDeviceId());
        result.put("reservationId", reservation.getReservationId());
        result.put("deviceId", reservation.getDeviceId());
        result.put("deviceName", device == null ? "已删除设备 #" + reservation.getDeviceId() : device.getDeviceName());
        result.put("applicantId", reservation.getApplicantId());
        result.put("applicantName", applicant.getName());
        result.put("startTime", formatDateTime(reservation.getStartTime()));
        result.put("endTime", formatDateTime(reservation.getEndTime()));
        result.put("purpose", reservation.getPurpose());
        result.put("status", reservation.getStatus());
        return result;
    }

    private Map<String, Object> toReservationDetail(ReservationEntity reservation) {
        Map<String, Object> result = toReservationSummary(reservation);
        result.put("createdAt", formatDateTime(reservation.getCreatedAt()));
        result.put("reviewComment", reservation.getReviewComment());
        result.put("reviewerId", reservation.getReviewerId());
        if (reservation.getReviewerId() != null) {
            result.put("reviewerName", getExistingUser(reservation.getReviewerId()).getName());
        } else {
            result.put("reviewerName", null);
        }
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

    private String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
