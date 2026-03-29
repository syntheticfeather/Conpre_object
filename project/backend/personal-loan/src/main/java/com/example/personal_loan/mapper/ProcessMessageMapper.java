package com.example.personal_loan.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ProcessMessageMapper {

    @Select("SELECT COUNT(1) > 0 FROM processed_message WHERE message_id = #{messageId}")
    boolean isProcessMessage(@Param("messageId") String messageId);

    @Insert("INSERT INTO processed_message (message_id, business_id, business_type) " +
            "VALUES (#{messageId}, #{Id}, #{businessType})")
    void insertMessage(@Param("messageId") String messageId, @Param("businessType") String businessType, @Param("Id") Long Id);

}
