package com.example.personal_loan.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.example.personal_loan.entity.UserCert;

@Mapper
public interface UserCertMapper {
    @Select("""
            SELECT 
                user_id,
                id_card,
                credit_score,
                work_cert_id,
                tri_cert_id,
                bank_card_id,
                immovable_cert_id
            FROM user_certification 
            WHERE user_id = #{userId}
            """)
    UserCert selectByUserId(@Param("userId") Long userId);
}
