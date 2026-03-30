package com.lab.booking.service;

import com.lab.booking.common.ApiException;
import com.lab.booking.dto.DeviceDtos;
import com.lab.booking.model.DeviceEntity;
import com.lab.booking.model.DeviceStatus;
import com.lab.booking.model.RoleCode;
import com.lab.booking.model.UserEntity;
import com.lab.booking.repository.DeviceRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class DeviceService {

    private final DeviceRepository deviceRepository;
    private final AuthService authService;

    public DeviceService(DeviceRepository deviceRepository, AuthService authService) {
        this.deviceRepository = deviceRepository;
        this.authService = authService;
    }

    public Map<String, Object> listDevices(String keyword, String category, DeviceStatus status, Integer pageNum, Integer pageSize) {
        authService.currentUser();
        List<Map<String, Object>> filtered = deviceRepository.findAll().stream()
                .filter(device -> keyword == null || matchesKeyword(device, keyword))
                .filter(device -> category == null || category.equals(device.getCategory()))
                .filter(device -> status == null || status == device.getStatus())
                .sorted(Comparator.comparing(DeviceEntity::getDeviceId))
                .map(this::toDeviceSummary)
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

    public Map<String, Object> getDeviceDetail(Long deviceId) {
        authService.currentUser();
        return toDeviceDetail(getExistingDevice(deviceId));
    }

    public Map<String, Object> createDevice(DeviceDtos.SaveDeviceRequest request) {
        requireAdmin();
        ensureDeviceCodeUnique(request.deviceCode(), -1L);
        DeviceEntity device = new DeviceEntity();
        device.setDeviceId(deviceRepository.nextDeviceId());
        device.setDeviceName(request.deviceName());
        device.setDeviceCode(request.deviceCode());
        device.setCategory(request.category());
        device.setStatus(DeviceStatus.AVAILABLE);
        device.setLocation(request.location());
        device.setImageUrl(request.imageUrl());
        device.setDescription(request.description());
        deviceRepository.save(device);
        return toDeviceDetail(device);
    }

    public Map<String, Object> updateDevice(Long deviceId, DeviceDtos.SaveDeviceRequest request) {
        requireAdmin();
        DeviceEntity device = getExistingDevice(deviceId);
        ensureDeviceCodeUnique(request.deviceCode(), deviceId);
        device.setDeviceName(request.deviceName());
        device.setDeviceCode(request.deviceCode());
        device.setCategory(request.category());
        device.setLocation(request.location());
        device.setImageUrl(request.imageUrl());
        device.setDescription(request.description());
        deviceRepository.save(device);
        return toDeviceDetail(device);
    }

    public void updateDeviceStatus(Long deviceId, DeviceDtos.UpdateDeviceStatusRequest request) {
        requireAdmin();
        DeviceEntity device = getExistingDevice(deviceId);
        device.setStatus(request.status());
        deviceRepository.save(device);
    }

    public Map<String, Object> importDevice(String deviceName, String category, String location, String description, MultipartFile image) {
        requireAdmin();
        if (deviceName == null || deviceName.isBlank()) {
            throw new ApiException(400, "deviceName 不能为空");
        }
        if (image == null || image.isEmpty()) {
            throw new ApiException(400, "image 不能为空");
        }

        long deviceId = deviceRepository.nextDeviceId();
        DeviceEntity device = new DeviceEntity();
        device.setDeviceId(deviceId);
        device.setDeviceCode(generateDeviceCode(deviceId));
        device.setDeviceName(deviceName.trim());
        device.setCategory(category == null || category.isBlank() ? "Imported" : category.trim());
        device.setStatus(DeviceStatus.AVAILABLE);
        device.setLocation(location == null || location.isBlank() ? "TBD" : location.trim());
        device.setImageUrl(buildImageUrl(deviceId, image.getOriginalFilename()));
        device.setDescription(description);
        deviceRepository.save(device);
        return toDeviceDetail(device);
    }

    private DeviceEntity getExistingDevice(Long deviceId) {
        return deviceRepository.findById(deviceId)
                .orElseThrow(() -> new ApiException(404, "设备不存在"));
    }

    private void ensureDeviceCodeUnique(String deviceCode, Long excludeId) {
        if (deviceRepository.existsByDeviceCode(deviceCode, excludeId)) {
            throw new ApiException(409, "设备编号已存在");
        }
    }

    private void requireAdmin() {
        UserEntity currentUser = authService.currentUser();
        if (currentUser.getRoleCode() != RoleCode.ADMIN && currentUser.getRoleCode() != RoleCode.SUPER_ADMIN) {
            throw new ApiException(403, "无权限访问");
        }
    }

    private boolean matchesKeyword(DeviceEntity device, String keyword) {
        return matchesText(device.getDeviceName(), keyword)
                || matchesText(device.getDeviceCode(), keyword)
                || matchesText(device.getCategory(), keyword)
                || matchesText(device.getLocation(), keyword)
                || matchesText(device.getDescription(), keyword);
    }

    private boolean matchesText(String text, String keyword) {
        if (text == null) {
            return false;
        }
        if (text.contains(keyword)) {
            return true;
        }

        Charset gbk = Charset.forName("GBK");
        String textUtf8ToGbk = recode(text, StandardCharsets.UTF_8, gbk);
        String textGbkToUtf8 = recode(text, gbk, StandardCharsets.UTF_8);
        String keywordUtf8ToGbk = recode(keyword, StandardCharsets.UTF_8, gbk);
        String keywordGbkToUtf8 = recode(keyword, gbk, StandardCharsets.UTF_8);

        return textUtf8ToGbk.contains(keyword)
                || textGbkToUtf8.contains(keyword)
                || text.contains(keywordUtf8ToGbk)
                || text.contains(keywordGbkToUtf8)
                || textUtf8ToGbk.contains(keywordUtf8ToGbk)
                || textGbkToUtf8.contains(keywordGbkToUtf8);
    }

    private String recode(String value, Charset sourceCharset, Charset targetCharset) {
        return new String(value.getBytes(sourceCharset), targetCharset);
    }

    private Map<String, Object> toDeviceSummary(DeviceEntity device) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("deviceId", device.getDeviceId());
        result.put("deviceName", device.getDeviceName());
        result.put("deviceCode", device.getDeviceCode());
        result.put("category", device.getCategory());
        result.put("status", device.getStatus());
        result.put("location", device.getLocation());
        result.put("imageUrl", device.getImageUrl());
        return result;
    }

    private Map<String, Object> toDeviceDetail(DeviceEntity device) {
        Map<String, Object> result = toDeviceSummary(device);
        result.put("description", device.getDescription());
        return result;
    }

    private String generateDeviceCode(long deviceId) {
        return "EQ-" + LocalDate.now().getYear() + "-" + String.format("%04d", deviceId);
    }

    private String buildImageUrl(long deviceId, String originalFilename) {
        String safeName = (originalFilename == null || originalFilename.isBlank())
                ? "device.png"
                : originalFilename.replaceAll("[^A-Za-z0-9._-]", "_");
        return "/uploads/devices/" + deviceId + "-" + safeName;
    }
}
