package com.example.personal_loan.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.example.personal_loan.entity.TriCert;

@Mapper
public interface TriCertMapper {
    @Insert(
        "INSERT INTO tri_cert (" +
        "  social_security_path, " +
        "  credit_report_path " +
        ") VALUES (" +
        "  #{socialSecurityPath}, " +
        "  #{creditReportPath} " +
        ")"
    )
    @Options(useGeneratedKeys = true, keyProperty = "triCertId", keyColumn = "tri_cert_id")
    int insert(TriCert record);

    int update(TriCert record);

    @Select(
        "SELECT " +
        "  tri_cert_id, " +
        "  social_security_path, " +
        "  credit_report_path " +
        "FROM tri_cert " +
        "WHERE tri_cert_id = #{id}"
    )
    TriCert selectById(Integer id);
}
