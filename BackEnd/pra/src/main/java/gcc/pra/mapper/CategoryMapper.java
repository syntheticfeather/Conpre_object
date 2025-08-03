package gcc.pra.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import gcc.pra.pojo.Category;

@Mapper
public interface CategoryMapper {

    @Insert("insert into Category ( category_name, category_alias, create_user, create_time, update_time) "
            + "values ( #{categoryName}, #{categoryAlias}, #{createUser}, #{createTime}, #{updateTime})")
    public void add(Category category);

    @Select("select * from Category where create_user = #{id}")
    public List<Category> getAll(Integer id);

    @Select("select * from Category where id = #{id}")
    public Category getDetail(Integer id);

    @Update("update Category set category_name = #{categoryName}, category_alias = #{categoryAlias}, update_time = #{updateTime} where id = #{id}")
    public void update(Category category);

    @Delete("delete from Category where id = #{id}")
    public void delete(Integer id);

}
