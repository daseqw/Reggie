package all.service.impl;

import all.common.CustomException;
import all.mapper.CategoryMapper;
import all.pojo.Category;
import all.pojo.Dish;
import all.pojo.Setmeal;
import all.service.CategoryService;
import all.service.DishService;
import all.service.SetmealService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CategoryServiceimpl extends ServiceImpl<CategoryMapper, Category> implements CategoryService {
    @Autowired
    private DishService dishService;
    @Autowired
    private SetmealService setmealService;
    @Override
    public void remove(Long ids) throws CustomException {
        //查询 dish 是否 关联
        LambdaQueryWrapper<Dish> dishLambdaQueryWrapper = new LambdaQueryWrapper<Dish>();
        dishLambdaQueryWrapper.eq(Dish::getCategoryId, ids);
        int count = dishService.count(dishLambdaQueryWrapper);
        //判断是否有已有关联
        if(count > 0){
            //已有关联，抛出CustomException异常，由GlobalExceptionHandler捕捉
            throw new CustomException("分类下菜品有关联，不能删除");
        }
        //查询setmeal 是否 关联
        LambdaQueryWrapper<Setmeal> setmealLambdaQueryWrapper = new LambdaQueryWrapper<Setmeal>();
        setmealLambdaQueryWrapper.eq(Setmeal::getCategoryId, ids);
        int count1 = setmealService.count(setmealLambdaQueryWrapper);
        //判断是否有已有关联
        if(count1 > 0){
            //已有关联，抛出CustomException异常，由GlobalExceptionHandler捕捉
            throw new CustomException("分类下套餐有关联，不能删除");
        }
        //调用删除
        super.removeById(ids);
    }
}
