package com.example.personal_loan.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.example.personal_loan.entity.BlackUser;

@Mapper
public interface BlackListMapper {
    
    @Insert("INSERT INTO black_list (user_id, black_level) " +
            "VALUES (#{userId}, #{blackLevel} )")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(BlackUser blackUser);
    
    @Delete("DELETE FROM black_list WHERE user_id = #{userId}")
    int deleteByUserId(@Param("userId") Long userId);
    
    @Select("SELECT * FROM black_list WHERE user_id = #{userId}")
    BlackUser selectByUserId(@Param("userId") Long userId);
    
    @Select("SELECT * FROM black_list ")
    List<BlackUser> selectAll();
    
    @Update("UPDATE black_list SET black_level = #{level} WHERE user_id = #{userId}")
    int updateLevel(@Param("userId") Long userId, @Param("level") int level);
    
}