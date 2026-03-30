package com.lab.booking.service;

import com.lab.booking.common.ApiException;
import com.lab.booking.infrastructure.cache.AppCacheService;
import com.lab.booking.model.BorrowRecordEntity;
import com.lab.booking.model.BorrowStatus;
import com.lab.booking.model.DeviceEntity;
import com.lab.booking.model.DeviceStatus;
import com.lab.booking.model.RankScope;
import com.lab.booking.model.RepairStatus;
import com.lab.booking.model.ReservationStatus;
import com.lab.booking.model.RoleCode;
import com.lab.booking.model.UserEntity;
import com.lab.booking.repository.AuthRepository;
import com.lab.booking.repository.BorrowRecordRepository;
import com.lab.booking.repository.DeviceRepository;
import com.lab.booking.repository.RepairRepository;
import com.lab.booking.repository.ReservationRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class StatisticsService {

    private static final String OVERVIEW_CACHE_KEY = "statistics:overview";

    private final DeviceRepository deviceRepository;
    private final BorrowRecordRepository borrowRecordRepository;
    private final ReservationRepository reservationRepository;
    private final RepairRepository repairRepository;
    private final AuthRepository authRepository;
    private final AuthService authService;
    private final AppCacheService cacheService;

    public StatisticsService(
            DeviceRepository deviceRepository,
            BorrowRecordRepository borrowRecordRepository,
            ReservationRepository reservationRepository,
            RepairRepository repairRepository,
            AuthRepository authRepository,
            AuthService authService,
            AppCacheService cacheService
    ) {
        this.deviceRepository = deviceRepository;
        this.borrowRecordRepository = borrowRecordRepository;
        this.reservationRepository = reservationRepository;
        this.repairRepository = repairRepository;
        this.authRepository = authRepository;
        this.authService = authService;
        this.cacheService = cacheService;
    }

    public List<Map<String, Object>> hotDevices(String startDate, String endDate, RankScope rankScope, Integer topN) {
        requireAdmin();
        DateRange range = resolveRange(startDate, endDate, rankScope);
        int actualTopN = normalizeTopN(topN);

        return borrowRecordRepository.findAll().stream()
                .filter(record -> inRange(record.getPickupTime(), range)
                        || inRange(record.getExpectedReturnTime(), range)
                        || inRange(record.getReturnTime(), range))
                .collect(Collectors.groupingBy(BorrowRecordEntity::getDeviceId, Collectors.counting()))
                .entrySet().stream()
                .sorted(Map.Entry.<Long, Long>comparingByValue().reversed().thenComparing(Map.Entry.comparingByKey()))
                .limit(actualTopN)
                .map(entry -> {
                    DeviceEntity device = getDevice(entry.getKey());
                    Map<String, Object> result = new LinkedHashMap<>();
                    result.put("deviceId", device.getDeviceId());
                    result.put("deviceName", device.getDeviceName());
                    result.put("deviceCode", device.getDeviceCode());
                    result.put("imageUrl", device.getImageUrl());
                    result.put("borrowCount", entry.getValue());
                    result.put("rankScope", range.scope().name());
                    return result;
                })
                .toList();
    }

    public List<Map<String, Object>> deviceDamageStatistics(RankScope rankScope, Integer topN) {
        requireAdmin();
        DateRange range = resolveRange(null, null, rankScope);
        int actualTopN = normalizeTopN(topN);

        return deviceRepository.findAll().stream()
                .map(device -> toDamageView(device, range))
                .filter(Objects::nonNull)
                .sorted(Comparator.<Map<String, Object>, Long>comparing(item -> ((Number) item.get("damageCount")).longValue()).reversed()
                        .thenComparing(item -> ((Number) item.get("deviceId")).longValue()))
                .limit(actualTopN)
                .toList();
    }

    public List<Map<String, Object>> userViolationStatistics(RankScope rankScope, Integer topN) {
        requireAdmin();
        DateRange range = resolveRange(null, null, rankScope);
        int actualTopN = normalizeTopN(topN);

        return authRepository.getUsers().values().stream()
                .filter(user -> !user.isDeleted())
                .filter(user -> user.getRoleCode() == RoleCode.STUDENT)
                .map(user -> toViolationView(user, range))
                .filter(Objects::nonNull)
                .sorted(Comparator.<Map<String, Object>, Long>comparing(item -> ((Number) item.get("violationCount")).longValue()).reversed()
                        .thenComparing(item -> ((Number) item.get("userId")).longValue()))
                .limit(actualTopN)
                .toList();
    }

    public Map<String, Object> overview() {
        requireAdmin();
        return cacheService.get(OVERVIEW_CACHE_KEY, LinkedHashMap.class)
                .map(map -> (Map<String, Object>) map)
                .orElseGet(this::computeAndCacheOverview);
    }

    private Map<String, Object> computeAndCacheOverview() {
        long deviceTotal = deviceRepository.findAll().size();
        long availableDeviceTotal = deviceRepository.findAll().stream()
                .filter(device -> device.getStatus() == DeviceStatus.AVAILABLE)
                .count();
        long borrowingTotal = borrowRecordRepository.findAll().stream()
                .filter(record -> record.getStatus() == BorrowStatus.BORROWING || record.getStatus() == BorrowStatus.OVERDUE)
                .count();
        long pendingReservationTotal = reservationRepository.findAll().stream()
                .filter(reservation -> reservation.getStatus() == ReservationStatus.PENDING)
                .count();
        long pendingRepairTotal = repairRepository.findAll().stream()
                .filter(repair -> repair.getStatus() == RepairStatus.PENDING)
                .count();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("deviceTotal", deviceTotal);
        result.put("availableDeviceTotal", availableDeviceTotal);
        result.put("borrowingTotal", borrowingTotal);
        result.put("pendingReservationTotal", pendingReservationTotal);
        result.put("pendingRepairTotal", pendingRepairTotal);
        cacheService.put(OVERVIEW_CACHE_KEY, result);
        return result;
    }

    private Map<String, Object> toDamageView(DeviceEntity device, DateRange range) {
        long damageCount = repairRepository.findAll().stream()
                .filter(repair -> Objects.equals(repair.getDeviceId(), device.getDeviceId()))
                .filter(repair -> inRange(repair.getCreatedAt(), range) || inRange(repair.getUpdatedAt(), range))
                .count();

        boolean deviceCurrentlyDamaged = device.getStatus() == DeviceStatus.DAMAGED || device.getStatus() == DeviceStatus.REPAIRING;
        if (damageCount == 0 && !deviceCurrentlyDamaged) {
            return null;
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("deviceId", device.getDeviceId());
        result.put("deviceName", device.getDeviceName());
        result.put("deviceCode", device.getDeviceCode());
        result.put("imageUrl", device.getImageUrl());
        result.put("category", device.getCategory());
        result.put("damageCount", Math.max(1L, damageCount));
        result.put("status", device.getStatus());
        result.put("rankScope", range.scope().name());
        return result;
    }

    private Map<String, Object> toViolationView(UserEntity user, DateRange range) {
        long overdueCount = borrowRecordRepository.findAll().stream()
                .filter(record -> Objects.equals(record.getUserId(), user.getUserId()))
                .filter(record -> record.getStatus() == BorrowStatus.OVERDUE)
                .filter(record -> inRange(firstNonNull(record.getExpectedReturnTime(), record.getReturnTime(), record.getPickupTime()), range))
                .count();

        long damagedCount = borrowRecordRepository.findAll().stream()
                .filter(record -> Objects.equals(record.getUserId(), user.getUserId()))
                .filter(record -> record.getDeviceCondition() != null && !"NORMAL".equals(record.getDeviceCondition()))
                .filter(record -> inRange(firstNonNull(record.getReturnTime(), record.getExpectedReturnTime(), record.getPickupTime()), range))
                .count();

        long violationCount = overdueCount + damagedCount;
        if (violationCount == 0) {
            return null;
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("userId", user.getUserId());
        result.put("name", user.getName());
        result.put("jobNoOrStudentNo", user.getLoginId());
        result.put("overdueCount", overdueCount);
        result.put("damageCount", damagedCount);
        result.put("violationCount", violationCount);
        result.put("rankScope", range.scope().name());
        return result;
    }

    private int normalizeTopN(Integer topN) {
        return topN == null || topN < 1 ? 10 : topN;
    }

    private DeviceEntity getDevice(Long deviceId) {
        return deviceRepository.findById(deviceId)
                .orElseThrow(() -> new ApiException(404, "设备不存在"));
    }

    private DateRange resolveRange(String startDate, String endDate, RankScope rankScope) {
        if ((startDate == null || startDate.isBlank()) && (endDate == null || endDate.isBlank())) {
            RankScope actualScope = rankScope == null ? RankScope.TOTAL : rankScope;
            LocalDate today = LocalDate.now();
            return switch (actualScope) {
                case TOTAL -> new DateRange(LocalDate.of(2000, 1, 1).atStartOfDay(), LocalDate.of(2999, 12, 31).plusDays(1).atStartOfDay(), RankScope.TOTAL);
                case HALF_YEAR -> new DateRange(today.minusMonths(6).atStartOfDay(), today.plusDays(1).atStartOfDay(), RankScope.HALF_YEAR);
                case MONTH -> new DateRange(today.minusMonths(1).atStartOfDay(), today.plusDays(1).atStartOfDay(), RankScope.MONTH);
            };
        }

        LocalDate start = parseDate(startDate, "startDate");
        LocalDate end = parseDate(endDate, "endDate");
        if (end.isBefore(start)) {
            throw new ApiException(400, "结束日期不能早于开始日期");
        }
        return new DateRange(start.atStartOfDay(), end.plusDays(1).atStartOfDay(), rankScope == null ? RankScope.TOTAL : rankScope);
    }

    private boolean inRange(LocalDateTime dateTime, DateRange range) {
        return dateTime != null && !dateTime.isBefore(range.start()) && dateTime.isBefore(range.endExclusive());
    }

    private LocalDateTime firstNonNull(LocalDateTime... dateTimes) {
        for (LocalDateTime dateTime : dateTimes) {
            if (dateTime != null) {
                return dateTime;
            }
        }
        return null;
    }

    private LocalDate parseDate(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new ApiException(400, fieldName + " 不能为空");
        }
        try {
            return LocalDate.parse(value.trim());
        } catch (DateTimeParseException ex) {
            throw new ApiException(400, fieldName + " 日期格式错误，需要 yyyy-MM-dd");
        }
    }

    private void requireAdmin() {
        UserEntity currentUser = authService.currentUser();
        if (currentUser.getRoleCode() != RoleCode.ADMIN && currentUser.getRoleCode() != RoleCode.SUPER_ADMIN) {
            throw new ApiException(403, "无权限访问");
        }
    }

    private record DateRange(LocalDateTime start, LocalDateTime endExclusive, RankScope scope) {
    }
}
