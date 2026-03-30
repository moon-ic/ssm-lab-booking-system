package com.lab.booking.model;

import java.time.LocalDateTime;

public class BorrowRecordEntity {

    private Long recordId;
    private Long reservationId;
    private Long userId;
    private Long deviceId;
    private BorrowStatus status;
    private LocalDateTime pickupTime;
    private LocalDateTime expectedReturnTime;
    private LocalDateTime returnTime;
    private String deviceCondition;

    public Long getRecordId() { return recordId; }
    public void setRecordId(Long recordId) { this.recordId = recordId; }
    public Long getReservationId() { return reservationId; }
    public void setReservationId(Long reservationId) { this.reservationId = reservationId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getDeviceId() { return deviceId; }
    public void setDeviceId(Long deviceId) { this.deviceId = deviceId; }
    public BorrowStatus getStatus() { return status; }
    public void setStatus(BorrowStatus status) { this.status = status; }
    public LocalDateTime getPickupTime() { return pickupTime; }
    public void setPickupTime(LocalDateTime pickupTime) { this.pickupTime = pickupTime; }
    public LocalDateTime getExpectedReturnTime() { return expectedReturnTime; }
    public void setExpectedReturnTime(LocalDateTime expectedReturnTime) { this.expectedReturnTime = expectedReturnTime; }
    public LocalDateTime getReturnTime() { return returnTime; }
    public void setReturnTime(LocalDateTime returnTime) { this.returnTime = returnTime; }
    public String getDeviceCondition() { return deviceCondition; }
    public void setDeviceCondition(String deviceCondition) { this.deviceCondition = deviceCondition; }
}
