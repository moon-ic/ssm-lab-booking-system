package com.lab.booking.controller;

import com.lab.booking.common.Result;
import com.lab.booking.service.DeviceService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/device-imports")
public class DeviceImportController {

    private final DeviceService deviceService;

    public DeviceImportController(DeviceService deviceService) {
        this.deviceService = deviceService;
    }

    @PostMapping
    public Result<Map<String, Object>> importDevice(
            @RequestParam String deviceName,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) String description,
            @RequestParam MultipartFile image
    ) {
        return Result.success(deviceService.importDevice(deviceName, category, location, description, image));
    }
}
