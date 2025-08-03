package gcc.pra.service;

import gcc.pra.pojo.Article;
import gcc.pra.pojo.PageBean;

public interface ArticleService {

    void add(Article article);

    PageBean<Article> list(Integer pageNum, Integer pageSize, Integer categoryId, String state);

}
