package gcc.pra.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;

import gcc.pra.pojo.Article;

@Mapper
public interface ArticleMapper {

    @Insert("insert into article(title, content, cover_img, state, category_id, create_user, create_time, update_time)"
            + " values(#{title}, #{content}, #{coverImg}, #{state}, #{categoryId}, #{createUser}, #{createTime}, #{updateTime})")
    public void insert(Article article);

    public List<Article> list(Integer categoryId, String state, Integer id);

}
