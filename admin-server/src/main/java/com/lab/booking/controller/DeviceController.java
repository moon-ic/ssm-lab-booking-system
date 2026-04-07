package com.lab.booking.controller;

import com.lab.booking.common.Result;
import com.lab.booking.dto.DeviceDtos;
import com.lab.booking.model.DeviceStatus;
import com.lab.booking.service.DeviceService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/devices")
public class DeviceController {

    private final DeviceService deviceService;

    public DeviceController(DeviceService deviceService) {
        this.deviceService = deviceService;
    }

    @GetMapping
    public Result<Map<String, Object>> listDevices(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) DeviceStatus status,
            @RequestParam(required = false) Integer pageNum,
            @RequestParam(required = false) Integer pageSize
    ) {
        return Result.success(deviceService.listDevices(keyword, category, status, pageNum, pageSize));
    }

    @GetMapping("/{deviceId}")
    public Result<Map<String, Object>> getDeviceDetail(@PathVariable Long deviceId) {
        return Result.success(deviceService.getDeviceDetail(deviceId));
    }

    @PostMapping
    public Result<Map<String, Object>> createDevice(@Valid @RequestBody DeviceDtos.SaveDeviceRequest request) {
        return Result.success(deviceService.createDevice(request));
    }

    @PutMapping("/{deviceId}")
    public Result<Map<String, Object>> updateDevice(@PathVariable Long deviceId, @Valid @RequestBody DeviceDtos.SaveDeviceRequest request) {
        return Result.success(deviceService.updateDevice(deviceId, request));
    }

    @PutMapping("/{deviceId}/status")
    public Result<Void> updateDeviceStatus(@PathVariable Long deviceId, @Valid @RequestBody DeviceDtos.UpdateDeviceStatusRequest request) {
        deviceService.updateDeviceStatus(deviceId, request);
        return Result.success();
    }

    @DeleteMapping("/{deviceId}")
    public Result<Void> deleteDevice(@PathVariable Long deviceId) {
        deviceService.deleteDevice(deviceId);
        return Result.success();
    }

    @PostMapping("/import")
    public Result<Map<String, Object>> importDevices(
            @RequestParam String deviceName,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) String description,
            @RequestParam MultipartFile image
    ) {
        return Result.success(deviceService.importDevice(deviceName, category, location, description, image));
    }
}
