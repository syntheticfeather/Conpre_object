package com.example.personal_loan.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.example.personal_loan.entity.UserCert;

@Mapper
public interface UserCertMapper {
    @Select(
        "SELECT " +
        "    user_id, " +
        "    id_card, " +
        "    credit_score, " +
        "    work_cert_id, " +
        "    tri_cert_id, " +
        "    bank_card_id, " +
        "    immovable_cert_id " +
        "FROM user_certification " +
        "WHERE user_id = #{userId}"
    )
    UserCert selectByUserId(@Param("userId") Long userId);

    @Insert(
        "INSERT INTO user_certification (" +
        "  user_id, " +
        "  id_card, " +
        "  credit_score, " +
        "  bank_card_id, " +
        "  work_cert_id, " +
        "  tri_cert_id, " +
        "  immovable_cert_id " +
        ") VALUES (" +
        "  #{userId}, " +
        "  #{idCard}, " +
        "  #{creditScore}, " +
        "  #{bankCardId}, " +
        "  #{workCertId}, " +
        "  #{triCertId}, " +
        "  #{immovableCertId} " +
        ")"
    )
    @Options(useGeneratedKeys = false) // user_id 是主键，由业务传入，无自增
    int insert(UserCert cert);

    int update(UserCert cert);
}
