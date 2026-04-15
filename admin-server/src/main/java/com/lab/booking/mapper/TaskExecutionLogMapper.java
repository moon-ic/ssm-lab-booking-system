package com.lab.booking.mapper;

import com.lab.booking.model.TaskExecutionLogEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface TaskExecutionLogMapper {

    Long selectNextLogId();

    void upsertLog(TaskExecutionLogEntity log);

    TaskExecutionLogEntity selectById(@Param("logId") Long logId);

    List<TaskExecutionLogEntity> selectAll();
}
