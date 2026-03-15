package com.example.personal_loan.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.example.personal_loan.entity.WorkCert;

@Mapper
public interface WorkCertMapper {
    @Insert(
        "INSERT INTO work_cert (" +
        "  employment_cert_path, " +
        "  salary_cert_path " +
        ") VALUES (" +
        "  #{employmentCertPath}, " +
        "  #{salaryCertPath} " +
        ")"
    )
    @Options(useGeneratedKeys = true, keyProperty = "workCertId", keyColumn = "work_cert_id")
    int insert(WorkCert record);

    int update(WorkCert record);

    @Select(
        "SELECT " +
        "  work_cert_id, " +
        "  employment_cert_path, " +
        "  salary_cert_path " +
        "FROM work_cert " +
        "WHERE work_cert_id = #{id}"
    )
    WorkCert selectById(Integer id);
}
