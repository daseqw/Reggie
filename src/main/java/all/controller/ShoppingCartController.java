package all.controller;

import all.common.BaseContext;
import all.common.R;
import all.pojo.Setmeal;
import all.pojo.ShoppingCart;
import all.service.ShoppingCartService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/shoppingCart")
@Slf4j
public class ShoppingCartController {
    @Autowired
    private ShoppingCartService shoppingCartService;

    /**
     * 查看购物车
     * @return
     */
    @GetMapping("/list")
    public R<List<ShoppingCart>> showShoppingCart(){
        //获取当前用户的id
        Long currentId = BaseContext.getCurrentId();
        //查询shopping_cart中对应的id
        LambdaQueryWrapper<ShoppingCart> shoppingCartLambdaQueryWrapper = new LambdaQueryWrapper<ShoppingCart>();
        shoppingCartLambdaQueryWrapper.eq(ShoppingCart::getUserId,currentId);
        //根据时间排序
        shoppingCartLambdaQueryWrapper.orderByAsc(ShoppingCart::getCreateTime);

        List<ShoppingCart> list = shoppingCartService.list(shoppingCartLambdaQueryWrapper);
        return R.success(list);

    }

    /**
     * 添加dish和setmeal到shoppingCart
     * @param shoppingCart
     * @return
     */
    @PostMapping("/add")
    public R<ShoppingCart> save(@RequestBody ShoppingCart shoppingCart){
        log.info("购物车数据:{}",shoppingCart);

        //设置用户id，指定当前是哪个用户的购物车数据
        Long currentId = BaseContext.getCurrentId();
        shoppingCart.setUserId(currentId);

        Long dishId = shoppingCart.getDishId();

        LambdaQueryWrapper<ShoppingCart> queryWrapper = new LambdaQueryWrapper<ShoppingCart>();
        queryWrapper.eq(ShoppingCart::getUserId,currentId);

        LambdaQueryWrapper<ShoppingCart> queryWrapper1 = new LambdaQueryWrapper<ShoppingCart>();

        if(dishId != null){
            //添加到购物车的是菜品
            queryWrapper.eq(ShoppingCart::getDishId,dishId);
            //判断口味
            //前端对应不上
            //queryWrapper1.eq(shoppingCart.getDishFlavor() != null,ShoppingCart::getDishFlavor,shoppingCart.getDishFlavor());

        }else{
            //添加到购物车的是套餐
            queryWrapper.eq(ShoppingCart::getSetmealId,shoppingCart.getSetmealId());
        }

        //查询当前菜品或者套餐是否在购物车中
        //SQL:select * from shopping_cart where user_id = ? and dish_id/setmeal_id = ?
        ShoppingCart cartServiceOne = shoppingCartService.getOne(queryWrapper);

        if(cartServiceOne != null){
//            前端对应不上    口味不一样，就新存一个
//            ShoppingCart one = shoppingCartService.getOne(queryWrapper1);
//            if(one == null){
//                shoppingCart.setNumber(1);
//                shoppingCart.setCreateTime(LocalDateTime.now());
//                shoppingCartService.save(shoppingCart);
//                cartServiceOne = shoppingCart;
//            }else {
                //如果已经存在且口味一样，就在原来数量基础上加一
                Integer number = cartServiceOne.getNumber();
                cartServiceOne.setNumber(number + 1);
                shoppingCartService.updateById(cartServiceOne);
            //}
        }else{
            //如果不存在，则添加到购物车，数量默认就是一
            shoppingCart.setNumber(1);
            shoppingCart.setCreateTime(LocalDateTime.now());
            shoppingCartService.save(shoppingCart);
            cartServiceOne = shoppingCart;
        }

        return R.success(cartServiceOne);
    }

    /**
     * 清除购物车
     * @return
     */
    @DeleteMapping("/clean")
    public R<String> cleanShoppingCart(){
        //获取当前用户id
        Long currentId = BaseContext.getCurrentId();
        //根据id清空购物车
        LambdaQueryWrapper<ShoppingCart> shoppingCartLambdaQueryWrapper = new LambdaQueryWrapper<ShoppingCart>();
        shoppingCartLambdaQueryWrapper.eq(ShoppingCart::getUserId,currentId);
        shoppingCartService.remove(shoppingCartLambdaQueryWrapper);
        return R.success("清除购物车成功");
    }

    /**
     * 减少dish或者setmeal的数量
     * @return
     */
    @PostMapping("/sub")
    public R<ShoppingCart> reduceGood(@RequestBody ShoppingCart shoppingCart){
        log.info(shoppingCart.toString());
        ShoppingCart shoppingCart1 = new ShoppingCart();
        LambdaQueryWrapper<ShoppingCart> shoppingCartLambdaQueryWrapper = new LambdaQueryWrapper<ShoppingCart>();
        shoppingCartLambdaQueryWrapper.eq(ShoppingCart::getDishId,shoppingCart.getDishId());
        ShoppingCart one = shoppingCartService.getOne(shoppingCartLambdaQueryWrapper);
        if(one != null){
            //需要减少的是dish
            Integer number = one.getNumber();
            if(number - 1 == 0){
                shoppingCartService.removeById(one);
            }else {
                one.setNumber(number - 1);
                shoppingCartService.updateById(one);
                shoppingCart1 = one;
            }
        }else {
            //需要减少的是setmeal
            LambdaQueryWrapper<ShoppingCart> shoppingCartLambdaQueryWrapper1 = new LambdaQueryWrapper<ShoppingCart>();
            shoppingCartLambdaQueryWrapper1.eq(ShoppingCart::getSetmealId,shoppingCart.getSetmealId());
            ShoppingCart one1 = shoppingCartService.getOne(shoppingCartLambdaQueryWrapper1);
            Integer number = one1.getNumber();
            if(number -1 == 0){
                shoppingCartService.removeById(one1);
            }else {
                one1.setNumber(number - 1);
                shoppingCartService.updateById(one1);
                shoppingCart1 = one1;
            }
        }
        return R.success(shoppingCart1);
    }
}
