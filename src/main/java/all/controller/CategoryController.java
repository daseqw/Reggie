package all.controller;

import all.common.CustomException;
import all.common.R;
import all.pojo.Category;
import all.pojo.Employee;
import all.service.CategoryService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;


/**
 * 分类管理
 */
@Slf4j
@RestController
@RequestMapping("/category")
public class CategoryController {
    @Autowired
    private CategoryService categoryService;

    /**
     * 新增菜品分类与新增套餐分类
     * @param category
     * @return
     */
    @PostMapping
    public R<String> save(@RequestBody Category category){
        log.info(category.toString());
        categoryService.save(category);
        return R.success("新增分类成功");
    }

    /**
     * 分页功能
     * @param page
     * @param pageSize
     * @return
     */
    @GetMapping("/page")
    public R<Page> page(int page, int pageSize){
        log.info("页数"+page+"每页条数"+pageSize);
        //分页构造器
        Page pageInfo = new Page(page,pageSize);
        //查询条件
        LambdaQueryWrapper<Category> queryWrapper = new LambdaQueryWrapper<Category>();
        //根据sort进行排序
        queryWrapper.orderByDesc(Category::getSort);

        //执行查询
        categoryService.page(pageInfo,queryWrapper);
        //返回结果
        return R.success(pageInfo);
    }

    /**
     * 删除菜品分类与新增套餐分类
     * @param ids
     * @return
     */
    @DeleteMapping
    public R<String> delete(Long ids) throws CustomException {
        log.info(String.valueOf(ids));
        //categoryService.removeById(ids);
        categoryService.remove(ids);
        return R.success("删除成功");
    }

    /**
     * 修改分类
     * @param category
     * @return
     */
    @PutMapping
    public R<String> update(@RequestBody Category category){
        log.info(category.toString());
        categoryService.updateById(category);
        return R.success("修改成功");
    }

    /**
     * 新增菜品里的菜品分类选项
     * @param category
     * @return
     */
    @GetMapping("/list")
    public R<List<Category>> list (Category category){
        //根据type查询对应的数据返回给 新增菜品 里的 菜品分类
        LambdaQueryWrapper<Category> categoryLambdaQueryWrapper = new LambdaQueryWrapper<Category>();
        categoryLambdaQueryWrapper.eq(category.getType()!=null,Category::getType,category.getType());
        //查询
        List<Category> list = categoryService.list(categoryLambdaQueryWrapper);
        //查询到的数据返回给页面
        return R.success(list);
    }
}
