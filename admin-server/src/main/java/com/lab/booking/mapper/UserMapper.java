package com.lab.booking.mapper;

import com.lab.booking.model.UserEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface UserMapper {

    UserEntity selectByLoginId(@Param("loginId") String loginId);

    UserEntity selectById(@Param("userId") Long userId);

    Long selectNextUserId();

    List<UserEntity> selectAll();

    void upsertUser(UserEntity user);
}
