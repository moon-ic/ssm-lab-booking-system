package com.lab.booking.mapper;

import com.lab.booking.model.BorrowRecordEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface BorrowRecordMapper {

    Long selectNextRecordId();

    void upsertBorrowRecord(BorrowRecordEntity record);

    BorrowRecordEntity selectById(@Param("recordId") Long recordId);

    BorrowRecordEntity selectByReservationId(@Param("reservationId") Long reservationId);

    List<BorrowRecordEntity> selectAll();
}
