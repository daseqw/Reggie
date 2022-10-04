package all.service;

import all.common.CustomException;
import all.pojo.Orders;
import com.baomidou.mybatisplus.extension.service.IService;

public interface OrdersService extends IService<Orders> {
    public void submit(Orders orders) throws CustomException;
}
