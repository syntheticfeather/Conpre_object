package com.example.personal_loan.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.example.personal_loan.entity.ImmovablesCert;

@Mapper
public interface ImmovablesCertMapper {
    @Insert(
        "INSERT INTO immovables_cert (" +
        "  property_cert_path, " +
        "  car_cert_path, " +
        "  total_value " +
        ") VALUES (" +
        "  #{propertyCertPath}, " +
        "  #{carCertPath}, " +
        "  #{totalValue} " +
        ")"
    )
    @Options(useGeneratedKeys = true, keyProperty = "immovableCertId", keyColumn = "immovable_cert_id")
    int insert(ImmovablesCert record);

    int update(ImmovablesCert record);

    @Select(
        "SELECT " +
        "  immovable_cert_id, " +
        "  property_cert_path, " +
        "  car_cert_path, " +
        "  total_value " +
        "FROM immovables_cert " +
        "WHERE immovable_cert_id = #{id}"
    )
    ImmovablesCert selectById(Integer id);
}
