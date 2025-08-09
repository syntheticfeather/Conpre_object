package gcc.pra.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import gcc.pra.pojo.Category;
import gcc.pra.pojo.Result;
import gcc.pra.service.CategoryService;

@RestController
@RequestMapping("/category")
public class CategoryController {

    @Autowired
    private CategoryService categoryService;

    // http://localhost:8080/category
    @PostMapping
    public Result add(@RequestBody @Validated(Category.add.class) Category category) {
        categoryService.add(category);
        return Result.success();
    }

    @GetMapping
    public Result<List<Category>> getAll() {
        List<Category> categories = categoryService.getAll();
        return Result.success(categories);
    }

    @GetMapping("/detail")
    public Result<Category> getDetail(Integer id) {
        return Result.success(categoryService.getDetail(id));
    }

    @PutMapping
    public Result update(@RequestBody @Validated(Category.update.class) Category category) {
        categoryService.update(category);
        return Result.success();
    }

    // 删除文章
    @DeleteMapping
    public Result delete(Integer id) {
        categoryService.delete(id);
        return Result.success();
    }
}
