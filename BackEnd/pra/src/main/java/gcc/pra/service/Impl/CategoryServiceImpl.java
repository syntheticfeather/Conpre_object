package gcc.pra.service.Impl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import gcc.pra.Utils.ThreadLocalUtil;
import gcc.pra.mapper.CategoryMapper;
import gcc.pra.pojo.Category;
import gcc.pra.service.CategoryService;

@Service
public class CategoryServiceImpl implements CategoryService {

    @Autowired
    private CategoryMapper categoryMapper;

    @Override
    public void add(Category category) {
        Integer id = ThreadLocalUtil.getId();
        category.setCreateUser(id);
        category.setCreateTime(LocalDateTime.now());
        category.setUpdateTime(LocalDateTime.now());
        categoryMapper.add(category);
    }

    @Override
    public List<Category> getAll() {
        Integer id = ThreadLocalUtil.getId();
        List<Category> categories = categoryMapper.getAll(id);
        return categories;
    }

    @Override
    public Category getDetail(Integer id) {
        return categoryMapper.getDetail(id);
    }

    @Override
    public void update(Category category) {
        category.setUpdateTime(LocalDateTime.now());
        categoryMapper.update(category);
    }

    @Override
    public void delete(Integer id) {
        categoryMapper.delete(id);
    }

}
