package com.lab.booking.service;

import com.lab.booking.model.BorrowStatus;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class ProfileService {

    private final AuthService authService;
    private final BorrowRecordService borrowRecordService;

    public ProfileService(AuthService authService, BorrowRecordService borrowRecordService) {
        this.authService = authService;
        this.borrowRecordService = borrowRecordService;
    }

    public Map<String, Object> getProfile() {
        return authService.me();
    }

    public Map<String, Object> listMyBorrowRecords(BorrowStatus status, Integer pageNum, Integer pageSize) {
        return borrowRecordService.listCurrentUserRecords(status, pageNum, pageSize);
    }
}
