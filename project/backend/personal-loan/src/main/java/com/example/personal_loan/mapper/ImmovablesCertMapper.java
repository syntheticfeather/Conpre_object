package com.example.personal_loan.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.example.personal_loan.entity.ImmovablesCert;

@Mapper
public interface ImmovablesCertMapper {
    @Insert("INSERT INTO immovables_cert (property_cert_path, car_cert_path, total_value) " +
            "VALUES (#{propertyCertPath}, #{carCertPath}, #{totalValue})")
    @Options(useGeneratedKeys = true, keyProperty = "immovableCertId", keyColumn = "immovable_cert_id")
    int insert(ImmovablesCert record);

    @Update("<script>" +
            "UPDATE immovables_cert " +
            "<set>" +
                "<if test='propertyCertPath != null'>property_cert_path = #{propertyCertPath},</if>" +
                "<if test='carCertPath != null'>car_cert_path = #{carCertPath},</if>" +
                "<if test='totalValue != null'>total_value = #{totalValue}</if>" +
            "</set>" +
            "WHERE immovable_cert_id = #{immovableCertId}" +
            "</script>")
    int update(ImmovablesCert record);

    @Select("SELECT immovable_cert_id, property_cert_path, car_cert_path, total_value " +
            "FROM immovables_cert " +
            "WHERE immovable_cert_id = #{id}")
    ImmovablesCert selectById(Integer id);
}
