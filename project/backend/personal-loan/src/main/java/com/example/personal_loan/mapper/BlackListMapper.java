package com.example.personal_loan.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.example.personal_loan.dto.BlackListDto;
import com.example.personal_loan.entity.BlackUser;

@Mapper
public interface BlackListMapper {
    
    @Insert("INSERT INTO black_list (user_id, black_level) " +
            "VALUES (#{userId}, #{blackLevel} )")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(BlackUser blackUser);
    
    @Delete("DELETE FROM black_list WHERE user_id = #{userId}")
    int deleteByUserId(@Param("userId") Long userId);
    
    @Select("""
        SELECT
            id AS id, 
            user_id AS userId, 
            black_level AS blackLevel, 
            create_time AS createTime, 
            update_time AS updateTime, 
            remove_time AS removeTime 
        FROM black_list 
        WHERE user_id = #{userId} 
        """)
    BlackUser selectByUserId(@Param("userId") Long userId);

    @Select("""
        SELECT id, 
            user_id AS userId, 
            black_level AS blackLevel, 
            create_time AS createTime, 
            update_time AS updateTime, remove_time AS removeTime 
        FROM black_list 
        WHERE user_id = #{userId} AND remove_time IS NULL 
        """)
    BlackUser selectActiveByUserId(@Param("userId") Long userId);
        
    @Select("""
        SELECT
            b.id AS id,
            b.user_id AS userId,
            u.user_name AS userName,
            u.phone AS phone,
            b.black_level AS blackLevel,
            b.create_time AS createTime,
            b.update_time AS updateTime,
            b.remove_time AS removeTime
        FROM black_list b
        INNER JOIN users u ON b.user_id = u.id
        ORDER BY b.update_time DESC
        """)
    List<BlackListDto> selectAll();
    
    @Update("""
        UPDATE black_list
        SET
            black_level = #{blackLevel},
            remove_time = #{removeTime},
            update_time = #{updateTime}
        WHERE id = #{id}
        """)
    void update(BlackUser blackUser);
}