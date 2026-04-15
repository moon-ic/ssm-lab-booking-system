package com.lab.booking.mapper;

import com.lab.booking.model.RepairEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface RepairMapper {

    Long selectNextRepairId();

    void upsertRepair(RepairEntity repair);

    RepairEntity selectById(@Param("repairId") Long repairId);

    List<RepairEntity> selectAll();
}
