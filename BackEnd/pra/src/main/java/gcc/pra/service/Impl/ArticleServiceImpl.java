package gcc.pra.service.Impl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.apache.catalina.mapper.Mapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;

import gcc.pra.Utils.ThreadLocalUtil;
import gcc.pra.mapper.ArticleMapper;
import gcc.pra.pojo.Article;
import gcc.pra.pojo.PageBean;
import gcc.pra.service.ArticleService;

@Service
public class ArticleServiceImpl implements ArticleService {

    @Autowired
    private ArticleMapper articleMapper;

    @Override
    public void add(Article article) {
        Map<String, Object> map = ThreadLocalUtil.get();
        article.setCreateUser((Integer) map.get("id"));
        article.setCreateTime(LocalDateTime.now());
        article.setUpdateTime(LocalDateTime.now());
        articleMapper.insert(article);
    }

    @Override
    public PageBean<Article> list(Integer pageNum, Integer pageSize, Integer categoryId, String state) {
        PageBean<Article> pageBean = new PageBean<>();
        PageHelper.startPage(pageNum, pageSize);

        Integer id = ThreadLocalUtil.getId();
        List<Article> articles = articleMapper.list(categoryId, state, id);
        Page<Article> page = (Page<Article>) articles;

        pageBean.setTotal(page.getTotal());
        pageBean.setItems(page.getResult());
        return pageBean;
    }

}
