package all.service.impl;

import all.common.BaseContext;
import all.common.CustomException;
import all.mapper.OrdersMapper;
import all.pojo.*;
import all.service.*;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
public class OrdersServiceimpl extends ServiceImpl<OrdersMapper, Orders> implements OrdersService {
    @Autowired
    private ShoppingCartService shoppingCartService;

    @Autowired
    private UserService userService;

    @Autowired
    private OrdersService ordersService;

    @Autowired
    private OrderDetailService orderDetailService;

    @Autowired
    private AddressBookService addressBookService;


    /**
     * 用户下单
     * @param orders
     */
    @Override
    public void submit(Orders orders) throws CustomException {
        //获取当前用户Id
        Long userId = BaseContext.getCurrentId();
        //根据当前用户Id查询对应的购物车
        LambdaQueryWrapper<ShoppingCart> shoppingCartLambdaQueryWrapper = new LambdaQueryWrapper<ShoppingCart>();
        shoppingCartLambdaQueryWrapper.eq(ShoppingCart::getUserId,userId);
        List<ShoppingCart> shoppingCarts = shoppingCartService.list(shoppingCartLambdaQueryWrapper);

        //判断用户购物车是否为空，为空则不能下单
        if(shoppingCarts == null || shoppingCarts.size() == 0){
            throw new CustomException("当前购物车为空,不能下单");
        }

        //查询用户数据
        User user = userService.getById(userId);
        //根据当前用户Id查询对应的地址
        Long addressBookId = orders.getAddressBookId();
        AddressBook addressBook = addressBookService.getById(addressBookId);
        //判断地址，为空则不能下单
        if(addressBook == null){
            throw new CustomException("当前地址为空,不能下单");
        }

        //生成订单号
        long orderId = IdWorker.getId();
        //用来计算总金额
        AtomicInteger amount = new AtomicInteger(0);
        //设置orderDetail和计算总金额
        List<OrderDetail> orderDetails = shoppingCarts.stream().map((orderD) -> {
            OrderDetail orderDetail = new OrderDetail();
            orderDetail.setOrderId(orderId);
            orderDetail.setNumber(orderD.getNumber());
            orderDetail.setDishFlavor(orderD.getDishFlavor());
            orderDetail.setDishId(orderD.getDishId());
            orderDetail.setSetmealId(orderD.getSetmealId());
            orderDetail.setName(orderD.getName());
            orderDetail.setImage(orderD.getImage());
            orderDetail.setAmount(orderD.getAmount());
            amount.addAndGet(orderD.getAmount().multiply(new BigDecimal(orderD.getNumber())).intValue());
            return orderDetail;
        }).collect(Collectors.toList());
        //设置orders对应的数据
        orders.setId(orderId);
        orders.setOrderTime(LocalDateTime.now());
        orders.setCheckoutTime(LocalDateTime.now());
        orders.setStatus(2);
        orders.setAmount(new BigDecimal(amount.get()));//总金额
        orders.setUserId(userId);
        orders.setNumber(String.valueOf(orderId));
        orders.setUserName(user.getName());
        orders.setConsignee(addressBook.getConsignee());
        orders.setPhone(addressBook.getPhone());
        orders.setAddress((addressBook.getProvinceName() == null ? "" : addressBook.getProvinceName())
                + (addressBook.getCityName() == null ? "" : addressBook.getCityName())
                + (addressBook.getDistrictName() == null ? "" : addressBook.getDistrictName())
                + (addressBook.getDetail() == null ? "" : addressBook.getDetail()));
        //向订单表插入数据，一条数据
        this.save(orders);
        //向订单明细表(order_detail)中插入多条数据
        orderDetailService.saveBatch(orderDetails);
        //清空购物车
        shoppingCartService.remove(shoppingCartLambdaQueryWrapper);
    }
}
