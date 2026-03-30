package com.lab.booking.service;

import com.lab.booking.model.BorrowRecordEntity;
import com.lab.booking.model.BorrowStatus;
import com.lab.booking.model.DeviceEntity;
import com.lab.booking.model.DeviceStatus;
import com.lab.booking.model.NotificationType;
import com.lab.booking.model.ReservationEntity;
import com.lab.booking.model.ReservationStatus;
import com.lab.booking.repository.BorrowRecordRepository;
import com.lab.booking.repository.DeviceRepository;
import com.lab.booking.repository.ReservationRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class SystemTaskService {

    private static final String EXPIRE_TASK_CODE = "EXPIRE_PICKUP_PENDING_RESERVATIONS";
    private static final String EXPIRE_TASK_NAME = "失效超时未取用预约";
    private static final String OVERDUE_TASK_CODE = "MARK_OVERDUE_BORROW_RECORDS";
    private static final String OVERDUE_TASK_NAME = "标记逾期借用记录";
    private static final String REMINDER_TASK_CODE = "GENERATE_BORROW_REMINDERS";
    private static final String REMINDER_TASK_NAME = "生成借用提醒";

    private final ReservationRepository reservationRepository;
    private final BorrowRecordRepository borrowRecordRepository;
    private final DeviceRepository deviceRepository;
    private final NotificationService notificationService;
    private final TaskExecutionLogService taskExecutionLogService;

    public SystemTaskService(
            ReservationRepository reservationRepository,
            BorrowRecordRepository borrowRecordRepository,
            DeviceRepository deviceRepository,
            NotificationService notificationService,
            TaskExecutionLogService taskExecutionLogService
    ) {
        this.reservationRepository = reservationRepository;
        this.borrowRecordRepository = borrowRecordRepository;
        this.deviceRepository = deviceRepository;
        this.notificationService = notificationService;
        this.taskExecutionLogService = taskExecutionLogService;
    }

    public Map<String, Object> runAllTasks(LocalDateTime now) {
        int expiredReservationCount = expirePickupPendingReservations(now);
        int overdueRecordCount = markOverdueBorrowRecords(now);
        Map<String, Integer> reminderSummary = generateReminderSummary(now);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("expiredReservationCount", expiredReservationCount);
        result.put("overdueRecordCount", overdueRecordCount);
        result.put("aboutToExpireReminderCount", reminderSummary.get("ABOUT_TO_EXPIRE"));
        result.put("overdueReminderCount", reminderSummary.get("OVERDUE"));
        return result;
    }

    public int expirePickupPendingReservations(LocalDateTime now) {
        LocalDateTime startedAt = LocalDateTime.now();
        try {
            int count = 0;
            int notificationCount = 0;
            for (ReservationEntity reservation : reservationRepository.findAll()) {
                if (reservation.getStatus() != ReservationStatus.PICKUP_PENDING) {
                    continue;
                }
                if (reservation.getEndTime() == null || reservation.getEndTime().isAfter(now)) {
                    continue;
                }
                BorrowRecordEntity record = borrowRecordRepository.findByReservationId(reservation.getReservationId()).orElse(null);
                if (record == null || record.getStatus() != BorrowStatus.PICKUP_PENDING) {
                    continue;
                }
                DeviceEntity device = deviceRepository.findById(reservation.getDeviceId()).orElse(null);
                if (device == null || device.getStatus() != DeviceStatus.RESERVED) {
                    continue;
                }

                reservation.setStatus(ReservationStatus.EXPIRED);
                record.setStatus(BorrowStatus.OVERDUE);
                device.setStatus(DeviceStatus.AVAILABLE);
                reservationRepository.save(reservation);
                borrowRecordRepository.save(record);
                deviceRepository.save(device);

                boolean created = notificationService.createSystemNotificationIfAbsent(
                        reservation.getApplicantId(),
                        NotificationType.RESERVATION_EXPIRED,
                        "预约已失效",
                        "您预约的设备 " + device.getDeviceName() + " 已超过领取时间，系统已自动失效该预约。",
                        "RESERVATION",
                        reservation.getReservationId(),
                        now
                );
                if (created) {
                    notificationCount++;
                }
                count++;
            }

            taskExecutionLogService.recordSuccess(EXPIRE_TASK_CODE, EXPIRE_TASK_NAME, startedAt, LocalDateTime.now(), Map.of(
                    "expiredReservationCount", count,
                    "notificationCount", notificationCount
            ));
            return count;
        } catch (RuntimeException ex) {
            taskExecutionLogService.recordFailure(EXPIRE_TASK_CODE, EXPIRE_TASK_NAME, startedAt, LocalDateTime.now(), ex);
            throw ex;
        }
    }

    public int markOverdueBorrowRecords(LocalDateTime now) {
        LocalDateTime startedAt = LocalDateTime.now();
        try {
            int count = 0;
            int notificationCount = 0;
            for (BorrowRecordEntity record : borrowRecordRepository.findAll()) {
                if (record.getStatus() != BorrowStatus.BORROWING) {
                    continue;
                }
                if (record.getExpectedReturnTime() == null || !record.getExpectedReturnTime().isBefore(now)) {
                    continue;
                }
                record.setStatus(BorrowStatus.OVERDUE);
                borrowRecordRepository.save(record);

                DeviceEntity device = deviceRepository.findById(record.getDeviceId()).orElse(null);
                boolean created = notificationService.createSystemNotificationIfAbsent(
                        record.getUserId(),
                        NotificationType.BORROW_OVERDUE,
                        "借用已逾期",
                        "您借用的设备 " + (device == null ? record.getDeviceId() : device.getDeviceName()) + " 已逾期，请尽快归还。",
                        "BORROW_RECORD",
                        record.getRecordId(),
                        now
                );
                if (created) {
                    notificationCount++;
                }
                count++;
            }

            taskExecutionLogService.recordSuccess(OVERDUE_TASK_CODE, OVERDUE_TASK_NAME, startedAt, LocalDateTime.now(), Map.of(
                    "overdueRecordCount", count,
                    "notificationCount", notificationCount
            ));
            return count;
        } catch (RuntimeException ex) {
            taskExecutionLogService.recordFailure(OVERDUE_TASK_CODE, OVERDUE_TASK_NAME, startedAt, LocalDateTime.now(), ex);
            throw ex;
        }
    }

    public Map<String, Integer> generateReminderSummary(LocalDateTime now) {
        LocalDateTime startedAt = LocalDateTime.now();
        try {
            int aboutToExpireCount = 0;
            int overdueCount = 0;
            int notificationCount = 0;
            for (BorrowRecordEntity record : borrowRecordRepository.findAll()) {
                if (record.getExpectedReturnTime() == null) {
                    continue;
                }
                DeviceEntity device = deviceRepository.findById(record.getDeviceId()).orElse(null);
                String deviceName = device == null ? String.valueOf(record.getDeviceId()) : device.getDeviceName();
                if (record.getStatus() == BorrowStatus.BORROWING
                        && !record.getExpectedReturnTime().isBefore(now)
                        && !record.getExpectedReturnTime().isAfter(now.plusDays(3))) {
                    aboutToExpireCount++;
                    boolean created = notificationService.createSystemNotificationIfAbsent(
                            record.getUserId(),
                            NotificationType.ABOUT_TO_EXPIRE_REMINDER,
                            "设备即将到期",
                            "您借用的设备 " + deviceName + " 将于 " + record.getExpectedReturnTime() + " 到期，请按时归还。",
                            "BORROW_RECORD",
                            record.getRecordId(),
                            now
                    );
                    if (created) {
                        notificationCount++;
                    }
                }
                if (record.getStatus() == BorrowStatus.OVERDUE
                        || (record.getStatus() == BorrowStatus.BORROWING && record.getExpectedReturnTime().isBefore(now))) {
                    overdueCount++;
                    boolean created = notificationService.createSystemNotificationIfAbsent(
                            record.getUserId(),
                            NotificationType.OVERDUE_REMINDER,
                            "设备归还提醒",
                            "您借用的设备 " + deviceName + " 当前已逾期，请尽快处理归还。",
                            "BORROW_RECORD",
                            record.getRecordId(),
                            now
                    );
                    if (created) {
                        notificationCount++;
                    }
                }
            }

            Map<String, Integer> result = new LinkedHashMap<>();
            result.put("ABOUT_TO_EXPIRE", aboutToExpireCount);
            result.put("OVERDUE", overdueCount);
            taskExecutionLogService.recordSuccess(REMINDER_TASK_CODE, REMINDER_TASK_NAME, startedAt, LocalDateTime.now(), Map.of(
                    "aboutToExpireReminderCount", aboutToExpireCount,
                    "overdueReminderCount", overdueCount,
                    "notificationCount", notificationCount
            ));
            return result;
        } catch (RuntimeException ex) {
            taskExecutionLogService.recordFailure(REMINDER_TASK_CODE, REMINDER_TASK_NAME, startedAt, LocalDateTime.now(), ex);
            throw ex;
        }
    }
}
