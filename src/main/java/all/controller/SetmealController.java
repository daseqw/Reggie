package all.controller;

import all.common.R;
import all.dto.SetmealDto;
import all.pojo.Category;
import all.pojo.Dish;
import all.pojo.Setmeal;
import all.pojo.SetmealDish;
import all.service.CategoryService;
import all.service.DishService;
import all.service.SetmealDishService;
import all.service.SetmealService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.web.bind.annotation.*;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/setmeal")
@Slf4j
public class SetmealController {
    @Autowired
    private DishService dishService;
    @Autowired
    private SetmealService setmealService;
    @Autowired
    private SetmealDishService setmealDishService;
    @Autowired
    private CategoryService categoryService;
    /**
     * 分页查询功能
     * @param page
     * @param pageSize
     * @return
     */
    @GetMapping("/page")
    public R<Page> page(int page, int pageSize,String name){
        //构造分页构造器对象
        Page<Setmeal> pageInfo = new Page<>(page,pageSize);
        Page<SetmealDto> setmealDtoPage = new Page<>();

        //条件构造器
        LambdaQueryWrapper<Setmeal> queryWrapper = new LambdaQueryWrapper<Setmeal>();
        //添加过滤条件
        queryWrapper.like(name != null,Setmeal::getName,name);
        //添加排序条件
        queryWrapper.orderByDesc(Setmeal::getUpdateTime);

        //执行分页查询
        setmealService.page(pageInfo,queryWrapper);

        //对象拷贝
        BeanUtils.copyProperties(pageInfo,setmealDtoPage,"records");

        List<Setmeal> records = pageInfo.getRecords();

        List<SetmealDto> list = records.stream().map((setmealRecords) -> {
            SetmealDto setmealDto = new SetmealDto();
            //把pageInfo里的records属性拷贝给setmealDto
            BeanUtils.copyProperties(setmealRecords,setmealDto);

            Long categoryId = setmealRecords.getCategoryId();//分类id
            //根据id查询分类对象
            Category category = categoryService.getById(categoryId);

            if(category != null){
                String categoryName = category.getName();
                setmealDto.setCategoryName(categoryName);
            }
            return setmealDto;
        }).collect(Collectors.toList());
        //设置setmealDtoPage里的records属性
        setmealDtoPage.setRecords(list);

        return R.success(setmealDtoPage);
    }

    /**
     * 修改状态
     * @param status
     * @param ids
     * @return
     */
    @PostMapping("/status/{status}")
    public R<String> changeStatus(@PathVariable Integer status,Long[] ids){
        for (Long id: ids) {
            Setmeal byId = setmealService.getById(id);
            byId.setStatus(status);
           setmealService.updateById(byId);
        }

        return R.success("修改售卖状态成功");
    }

    /**
     * 删除
     * @param ids
     * @return
     */
    @DeleteMapping
    //清理缓存，allEntries=true要写，默认是false 意思是删除 setmealCache 这个分类下 所有的缓存数据
    @CacheEvict(value = "setmealCache",allEntries = true)
    public R<String> delete(Long[] ids){
        List<Long> list = Arrays.asList(ids);
        LambdaQueryWrapper<Setmeal> setmealLambdaQueryWrapper = new LambdaQueryWrapper<Setmeal>();
        LambdaQueryWrapper<SetmealDish> setmealDishLambdaQueryWrapper = new LambdaQueryWrapper<SetmealDish>();
        for (Long id: ids) {
            //删除setmeal对应的id
            setmealLambdaQueryWrapper.eq(Setmeal::getId,id);
            setmealService.remove(setmealLambdaQueryWrapper);
            //删除setmeal_dish对应的id
            setmealDishLambdaQueryWrapper.eq(SetmealDish::getDishId,id);
            setmealDishService.remove(setmealDishLambdaQueryWrapper);
        }
        return R.success("删除成功");
    }

    /**
     * 点击修改后回弹数据
     * @param setmealId
     * @return
     */
    @GetMapping("/{setmealId}")
    //清理缓存，allEntries=true要写，默认是false 意思是删除 setmealCache 这个分类下 所有的缓存数据
    @CacheEvict(value = "setmealCache",allEntries = true)
    public R<SetmealDto> update(@PathVariable Long setmealId){
        //接受SetmealDto
        SetmealDto setmealDto = new SetmealDto();
        //按setmealId查询出setmeal表对应的信息
        Setmeal byId =  setmealService.getById(setmealId);
        //把byId的信息复制给setmealDto
        BeanUtils.copyProperties(byId,setmealDto);
        log.info(setmealDto.toString());
        //根据setmealId查询套餐下的dish(setmeal_dish表)
        LambdaQueryWrapper<SetmealDish> setmealDishLambdaQueryWrapper = new LambdaQueryWrapper<SetmealDish>();
        setmealDishLambdaQueryWrapper.eq(SetmealDish::getSetmealId,setmealId);
        List<SetmealDish> setmealDishList = setmealDishService.list(setmealDishLambdaQueryWrapper);

        //根据dish_id查询dish表中菜品的售卖状态，如果停售则不回显
        List<Long> dishIds  = new ArrayList<Long>();
        for (SetmealDish setmealDish: setmealDishList) {
            Long dishId = setmealDish.getDishId();
            log.info(dishId.toString());
            LambdaQueryWrapper<Dish> dishLambdaQueryWrapper = new LambdaQueryWrapper<Dish>();
            dishLambdaQueryWrapper.eq(Dish::getId,dishId);
            Dish one = dishService.getOne(dishLambdaQueryWrapper);
            log.info(one.toString());
            //判断one里对应的dish的状态,如果status为1就添加到setmealDishList1中进行回显
            if(one.getStatus() == 1){
                dishIds.add(one.getId());
            }
        }
        //把status为1的dish添加到setmealDishList1中
        List<SetmealDish> setmealDishList1 = new ArrayList<SetmealDish>();
        for (int i = 0; i < dishIds.size(); i++) {
            LambdaQueryWrapper<SetmealDish> setmealDishLambdaQueryWrapper1 = new LambdaQueryWrapper<SetmealDish>();
            setmealDishLambdaQueryWrapper1.eq(SetmealDish::getDishId,dishIds.get(i));
            SetmealDish one = setmealDishService.getOne(setmealDishLambdaQueryWrapper1);
            setmealDishList1.add(one);
        }
        log.info(setmealDishList1.toString());
        setmealDto.setSetmealDishes(setmealDishList1);
        return R.success(setmealDto);
    }

    /**
     * 修改保存
     * @param setmealDto
     * @return
     */
    @PutMapping
    public R<String> updateSave(@RequestBody SetmealDto setmealDto){
        log.info(setmealDto.toString());
        //接收Setmeal
        Setmeal setmeal = new Setmeal();
        BeanUtils.copyProperties(setmealDto,setmeal);
        setmealService.updateById(setmeal);
        log.info(setmeal.toString());
        //接收SetmealDish
        SetmealDish setmealDish =new SetmealDish();
        List<SetmealDish> setmealDishes = setmealDto.getSetmealDishes();
        //先删除原有的setmeal_id
        Long setmealDishId = setmealDto.getId();
        LambdaQueryWrapper<SetmealDish> setmealDishLambdaQueryWrapper = new LambdaQueryWrapper<SetmealDish>();
        setmealDishLambdaQueryWrapper.eq(SetmealDish::getSetmealId,setmealDishId);
        setmealDishService.remove(setmealDishLambdaQueryWrapper);
        //给setmealDishes设置setmealid
         setmealDishes = setmealDishes.stream().map((sd)->{
           sd.setSetmealId(setmeal.getId());
           return sd;
        }).collect(Collectors.toList());
        //新增套餐菜品
        setmealDishService.saveBatch(setmealDishes);
        log.info(setmealDish.toString());

        return R.success("保存成功");
    }

    /**
     * 新增保存
     * @param setmealDto
     * @return
     */
    @PostMapping
    //清理缓存，allEntries=true要写，默认是false
    @CacheEvict(value = "setmealCache",allEntries = true)
    public R<String> save(@RequestBody SetmealDto setmealDto){
        //将setmealDto中的信息拷贝给setmeal
        Setmeal setmeal = new Setmeal();
        BeanUtils.copyProperties(setmealDto,setmeal);
        log.info(setmeal.toString());
        //保存到setmeal表中
        setmealService.save(setmeal);
        //将setmealDto里的setDishes集合取出来
        List<SetmealDish> setmealDishes = setmealDto.getSetmealDishes();
        //把setmeal中的id给setmealDishes依次赋值
         setmealDishes = setmealDishes.stream().map((sd)->{
            sd.setSetmealId(setmeal.getId());
            return sd;
        }).collect(Collectors.toList());
        //保存到setmeal_dish表中
        setmealDishService.saveBatch(setmealDishes);
        return R.success("保存成功");
    }

    /**
     * 移动端展示套餐
     * @param setmeal
     * @return
     */
    @GetMapping("/list")
    //使用注解缓存
    //                                       固定写法，#setmeal表示的是参数的哪个setmeal
    @Cacheable(value = "setmealCache",key = "#setmeal.categoryId")
    public R<List<Setmeal>> showSetmeal(Setmeal setmeal){
        LambdaQueryWrapper<Setmeal> setmealLambdaQueryWrapper = new LambdaQueryWrapper<Setmeal>();
        setmealLambdaQueryWrapper.eq(setmeal.getCategoryId()!= null,Setmeal::getCategoryId,setmeal.getCategoryId());
        setmealLambdaQueryWrapper.eq(setmeal.getStatus()!=null,Setmeal::getStatus,setmeal.getStatus());
        List<Setmeal> list = setmealService.list(setmealLambdaQueryWrapper);
        return R.success(list);
    }


}
