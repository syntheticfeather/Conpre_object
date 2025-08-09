package gcc.pra.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import gcc.pra.pojo.User;

@Mapper
public interface UserMapper {

    @Select("SELECT * FROM user WHERE username = #{username}")
    User findUserByName(String username);

    @Insert("INSERT INTO user (username, password, create_time, update_time)"
            + " VALUES (#{username}, #{md5Password}, NOW(), NOW())")
    void addUser(String username, String md5Password);

    @Update("update user set nickname = #{nickname}, email = #{email}, update_time = #{updateTime} where id = #{id}")
    void updateUser(User user);

    @Update("update user set user_pic = #{avatarUrl}, update_time = now() where id = #{id}")
    void updateUserAvatar(String avatarUrl, Integer id);

    @Update("update user set password = #{PWD}, update_time = now() where id = #{id}")
    void updatePWD(String PWD, Integer id);

}
