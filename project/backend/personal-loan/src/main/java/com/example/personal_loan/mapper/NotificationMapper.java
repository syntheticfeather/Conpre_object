package com.example.personal_loan.mapper;

import java.time.LocalDateTime;
import java.util.List;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.example.personal_loan.entity.Notification;

@Mapper
public interface NotificationMapper {

    @Insert(
        "INSERT INTO notifications (" +
        "  user_id, " +
        "  business_id, " +
        "  business_type, " +
        "  title, " +
        "  content, " +
        "  read_flag, " +
        "  created_at, " +
        "  read_at " +
        ") VALUES (" +
        "  #{userId}, " +
        "  #{businessId}, " +
        "  #{businessType}, " +
        "  #{title}, " +
        "  #{content}, " +
        "  #{readFlag}, " +
        "  #{createdAt}, " +
        "  #{readAt} " +
        ")"
    )
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int insert(Notification notification);

    @Select(
        "SELECT " +
        "  id, " +
        "  user_id as userId, " +
        "  business_id as businessId, " +
        "  business_type as businessType, " +
        "  title, " +
        "  content, " +
        "  read_flag as readFlag, " +
        "  created_at as createdAt, " +
        "  read_at as readAt " +
        "FROM notifications " +
        "WHERE user_id = #{userId} " +
        "ORDER BY created_at DESC " +
        "LIMIT #{limit}"
    )
    List<Notification> selectLatestByUserId(@Param("userId") Long userId, @Param("limit") int limit);

    @Update(
        "UPDATE notifications " +
        "SET read_flag = 1, read_at = #{readAt} " +
        "WHERE id = #{id} AND user_id = #{userId}"
    )
    int markAsRead(@Param("readAt") LocalDateTime readAt, @Param("userId") Long userId, @Param("id") Long id);
}

