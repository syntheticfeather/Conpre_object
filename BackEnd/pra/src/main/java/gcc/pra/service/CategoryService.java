package gcc.pra.service;

import java.util.List;

import gcc.pra.pojo.Category;

public interface CategoryService {

    public void add(Category category);

    public List<Category> getAll();

    public Category getDetail(Integer id);

    public void update(Category category);

    public void delete(Integer id);

}
