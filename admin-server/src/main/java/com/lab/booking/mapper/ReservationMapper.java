package com.lab.booking.mapper;

import com.lab.booking.model.ReservationEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ReservationMapper {

    Long selectNextReservationId();

    void upsertReservation(ReservationEntity reservation);

    ReservationEntity selectById(@Param("reservationId") Long reservationId);

    List<ReservationEntity> selectAll();
}
