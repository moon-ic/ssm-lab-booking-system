package com.lab.booking.mapper;

import com.lab.booking.model.DeviceEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface DeviceMapper {

    List<DeviceEntity> selectAll();

    DeviceEntity selectById(@Param("deviceId") Long deviceId);

    Integer countByDeviceCode(@Param("deviceCode") String deviceCode, @Param("excludeId") Long excludeId);

    Long selectNextDeviceId();

    void upsertDevice(DeviceEntity device);

    void deleteById(@Param("deviceId") Long deviceId);
}
