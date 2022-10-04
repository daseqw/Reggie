package all.controller;

import all.common.R;
import all.dto.DishDto;
import all.pojo.Category;
import all.pojo.Dish;
import all.pojo.DishFlavor;
import all.service.CategoryService;
import all.service.DishFlavorService;
import all.service.DishService;
import all.service.SetmealService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 菜品管理
 */
@RestController
@RequestMapping("/dish")
@Slf4j
public class DishController {
    @Autowired
    private DishService dishService;

    @Autowired
    private DishFlavorService dishFlavorService;

    @Autowired
    private CategoryService categoryService;


    @Autowired
    private SetmealService setmealService;

    @Autowired
    private RedisTemplate redisTemplate;


    /**
     * 新增菜品
     * @param dishDto
     * @return
     */
    @PostMapping
    public R<String> save(@RequestBody DishDto dishDto){
        log.info(dishDto.toString());

        dishService.saveWithFlavor(dishDto);
        //精确清理Redis中的缓存
        redisTemplate.delete(dishDto.getCategoryId().toString());
        return R.success("新增菜品成功");
    }

    /**
     * 菜品信息分页和查询
     * @param page
     * @param pageSize
     * @param name
     * @return
     */
    @GetMapping("/page")
    public R<Page> page(int page,int pageSize,String name){

        //构造分页构造器对象
        Page<Dish> pageInfo = new Page<>(page,pageSize);
        Page<DishDto> dishDtoPage = new Page<>();

        //条件构造器
        LambdaQueryWrapper<Dish> queryWrapper = new LambdaQueryWrapper<>();
        //添加过滤条件
        queryWrapper.like(name != null,Dish::getName,name);
        //添加排序条件
        queryWrapper.orderByDesc(Dish::getUpdateTime);

        //执行分页查询
        dishService.page(pageInfo,queryWrapper);

        //对象拷贝
        BeanUtils.copyProperties(pageInfo,dishDtoPage,"records");

        List<Dish> records = pageInfo.getRecords();

        List<DishDto> list = records.stream().map((dishRecords) -> {
            DishDto dishDto = new DishDto();
            //把pageInfo里的records属性拷贝给dishDto
            BeanUtils.copyProperties(dishRecords,dishDto);

            Long categoryId = dishRecords.getCategoryId();//分类id
            //根据id查询分类对象
            Category category = categoryService.getById(categoryId);

            if(category != null){
                String categoryName = category.getName();
                dishDto.setCategoryName(categoryName);
            }
            return dishDto;
        }).collect(Collectors.toList());
        //设置dishDtoPage里的records属性
        dishDtoPage.setRecords(list);

        return R.success(dishDtoPage);
    }

    /**
     * 根据id查询菜品信息和对应的口味信息
     * @param id
     * @return
     */
    @GetMapping("/{id}")
    public R<DishDto> get(@PathVariable Long id){
        DishDto dishDto = dishService.getByIdWithFlavor(id);

        return R.success(dishDto);
    }

    /**
     * 修改菜品
     * @param dishDto
     * @return
     */
    @PutMapping
    public R<String> update(@RequestBody DishDto dishDto){
        log.info(dishDto.toString());

        dishService.updateWithFlavor(dishDto);

        //精确清理Redis中的缓存
        redisTemplate.delete(dishDto.getCategoryId().toString());

        return R.success("新增菜品成功");
    }

    /**
     * 根据id(批量)删除菜品信息
     * @param ids
     * @return
     */
    @DeleteMapping
    public R<String> delete(Long[] ids){
        List<Long> list = Arrays.asList(ids);
        LambdaQueryWrapper<DishFlavor> queryWrapper = new LambdaQueryWrapper<DishFlavor>();
        //删除dish对应的dish_flavor
        for (Long id: ids) {
        LambdaQueryWrapper<DishFlavor> dishFlavorLambdaQueryWrapper = new LambdaQueryWrapper<DishFlavor>();
        dishFlavorLambdaQueryWrapper.eq(DishFlavor::getDishId,id);
        DishFlavor one = dishFlavorService.getOne(dishFlavorLambdaQueryWrapper);
        log.info(one.toString());
        dishFlavorService.remove(dishFlavorLambdaQueryWrapper);
        }

        log.info("删除菜品，id为：{}",ids);

        //获取ids对应的dish
        for (Long id: ids) {
        LambdaQueryWrapper<Dish> dishLambdaQueryWrapper = new LambdaQueryWrapper<Dish>();
        dishLambdaQueryWrapper.eq(Dish::getId,id);
        Dish one = dishService.getOne(dishLambdaQueryWrapper);
        //获取到dish对应的categoryId
        Long categoryId = one.getCategoryId();
        //再在Redis中精确删除
        redisTemplate.delete(categoryId.toString());
        }
        //在数据库中删除对应的dish
        dishService.removeByIds(list);

        return R.success("菜品信息删除成功");
    }

    /**
     * 批量和单个修改售卖状态
     * @param status
     * @param ids
     * @return
     */
    @PostMapping("/status/{status}")
    public R<String> changeStatus(@PathVariable("status") Integer status,Long[] ids) {

        for (Long id: ids) {
            Dish byId = dishService.getById(id);
            byId.setStatus(status);
            dishService.updateById(byId);
        }

        return R.success("修改售卖状态成功");

    }



    /**
     * 套餐管理里的添加菜品功能,移动端的页面展示
     * 缺少在添加菜品内的查询功能
     * @param categoryId
     * @return
     */
    @GetMapping("/list")
    public R<List<DishDto>> dishList(@RequestParam("categoryId") Long categoryId){
        log.info(categoryId.toString());

        List<DishDto> dishDtoList = null;

        //从Redis中查询菜品信息
        String key = categoryId.toString();
        dishDtoList = (List<DishDto>) redisTemplate.opsForValue().get(key);
        //如果存在直接返回
        if(dishDtoList!=null){
            return R.success(dishDtoList);
        }

        //在dish中查找符合categortId的dish集合
        LambdaQueryWrapper<Dish> dishLambdaQueryWrapper = new LambdaQueryWrapper<Dish>();
        dishLambdaQueryWrapper.eq(Dish::getCategoryId,categoryId);
        //查询dish必须是起售状态
        dishLambdaQueryWrapper.eq(Dish::getStatus,1);
        //得到dish的list
        List<Dish> dishList = dishService.list(dishLambdaQueryWrapper);

        //把dishList copy 到 dishDtoList
            dishDtoList = dishList.stream().map((dl) -> {
            DishDto dishDto = new DishDto();
            BeanUtils.copyProperties(dl,dishDto);
            Long categoryId1 = dl.getCategoryId();//分类id
            //根据id查询分类对象
            Category category = categoryService.getById(categoryId1);
            if(category != null){
                String categoryName = category.getName();
                dishDto.setCategoryName(categoryName);
            }
            //当前菜品的id
            Long dishId = dl.getId();
            //查询对应的口味
            LambdaQueryWrapper<DishFlavor> lambdaQueryWrapper = new LambdaQueryWrapper<DishFlavor>();
            lambdaQueryWrapper.eq(DishFlavor::getDishId,dishId);
            //SQL:select * from dish_flavor where dish_id = ?
            //获取到对应的口味
            List<DishFlavor> dishFlavorList = dishFlavorService.list(lambdaQueryWrapper);
            dishDto.setFlavors(dishFlavorList);
            return dishDto;
        }).collect(Collectors.toList());

        //不存在就先获取再存入Redis中再返回
        redisTemplate.opsForValue().set(key,dishDtoList,60,TimeUnit.MINUTES);
        //把查询到的dishDtoList返回到页面上
        return R.success(dishDtoList);
    }



/*    @GetMapping("/list")
    public R<List<Dish>> selctDish(@RequestParam("name") String name){
        LambdaQueryWrapper<Dish> dishLambdaQueryWrapper = new LambdaQueryWrapper<Dish>();
        dishLambdaQueryWrapper.eq(name!=null,Dish::getName,name);
        List<Dish> list = dishService.list(dishLambdaQueryWrapper);
        return R.success(list);
    }*/

}
