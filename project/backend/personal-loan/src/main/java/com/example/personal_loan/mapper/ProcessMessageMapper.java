package com.example.personal_loan.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface ProcessMessageMapper {

    @Select("SELECT COUNT(1) > 0 FROM processed_message WHERE message_id = #{messageId}")
    boolean isProcessMessage(String message_id);

    @Insert("INSERT INTO processed_message (message_id) (business_id) (business_type) " +
            "VALUES (#{messageId}) (#{businessType}) (#{Id})")
    void insertMessage(String message_id, String businessType, Long Id);

}
