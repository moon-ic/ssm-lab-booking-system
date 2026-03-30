package com.lab.booking.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class SystemTaskScheduler {

    private final SystemTaskService systemTaskService;

    public SystemTaskScheduler(SystemTaskService systemTaskService) {
        this.systemTaskService = systemTaskService;
    }

    @Scheduled(cron = "0 0/30 * * * ?")
    public void expireReservationsTask() {
        systemTaskService.expirePickupPendingReservations(LocalDateTime.now());
    }

    @Scheduled(cron = "0 0/30 * * * ?")
    public void overdueBorrowRecordsTask() {
        systemTaskService.markOverdueBorrowRecords(LocalDateTime.now());
    }

    @Scheduled(cron = "0 0 8 * * ?")
    public void reminderSummaryTask() {
        systemTaskService.generateReminderSummary(LocalDateTime.now());
    }
}
