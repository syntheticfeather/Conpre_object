package com.example.personal_loan.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.example.personal_loan.entity.PostponeRequest;

@Mapper
public interface PostponeRequestMapper {
    
    @Insert("INSERT INTO postpone_request (order_id, user_id, current_term, status, created_at) " +
            "VALUES (#{orderId}, #{userId}, #{currentTerm}, #{status}, #{createdAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(PostponeRequest request);
    
    @Update("UPDATE postpone_request SET status = #{status}, reject_reason = #{rejectReason}, reviewed_at = #{reviewedAt} " +
            "WHERE id = #{id}")
    int updateStatus(PostponeRequest request);
    
    @Select("SELECT * FROM postpone_request WHERE order_id = #{orderId} ORDER BY created_at DESC LIMIT 1")
    PostponeRequest selectByOrderId(@Param("orderId") Long orderId);
    
    @Select("SELECT * FROM postpone_request WHERE status = '待审核' ORDER BY created_at")
    List<PostponeRequest> selectPendingRequests();
    
    @Select("SELECT * FROM postpone_request WHERE user_id = #{userId} ORDER BY created_at DESC")
    List<PostponeRequest> selectByUserId(@Param("userId") Long userId);
    
    @Select("SELECT * FROM postpone_request WHERE id = #{id}")
    PostponeRequest selectById(@Param("id") Long id);
    
    @Select("SELECT * FROM postpone_request WHERE order_id = #{orderId} AND current_term = #{currentTerm}")
    PostponeRequest selectByOrderIdAndCurrentTerm(@Param("orderId") Long orderId, @Param("currentTerm") Integer currentTerm);
}
