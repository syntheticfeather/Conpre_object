package com.example.personal_loan.dao;

import java.util.List;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.example.personal_loan.entity.User;

@Mapper
public interface UserMapper {

    @Insert("INSERT INTO users (user_name, password, id_card, phone,credit_score) VALUES ( #{name}, #{password}, #{idCard},#{phone},#{creditScore} )")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(User user);

    @Delete("DELETE FROM users WHERE id = #{id}")
    int delete(@Param("id") Long id);

    @Update("UPDATE users SET user_name = #{name}, phone = #{phone}, id_card = #{idCard} ,password=#{password} WHERE id = #{id}")
    int update(User user);

    // 根据ID查询用户
    @Select("SELECT * FROM users WHERE id = #{id}")
    User findById(@Param("id") Long id);

    // 根据手机号和密码查询用户
    @Select("SELECT * FROM users WHERE phone = #{phone} AND password = #{password}")
    User findByPhoneAndPassword(@Param("phone") String phone, @Param("password") String password);

    // 查询所有用户
    @Select("SELECT * FROM users")
    List<User> findAll();

    // 根据手机号查询用户
    @Select("SELECT * FROM users WHERE phone = #{phone}")
    User findByPhone(@Param("phone") String phone);

    // 检查手机号是否已存在
    @Select("SELECT COUNT(*) FROM users WHERE phone = #{phone} AND id != #{id}")
    int findByPhoneExcludeId(@Param("phone") String phone, @Param("id") Long id);

    // 检查身份证号是否已存在
    @Select("SELECT COUNT(*) FROM users WHERE id_card = #{id_card} AND id != #{id}")
    int findByIdCardExcludeId(@Param("id_card") String idCard, @Param("id") Long id);

    // 根据身份证号查询用户
    @Select("SELECT * FROM users WHERE id_card = #{id_card}")
    User findByIdCard(@Param("id_card") String idCard);
}
