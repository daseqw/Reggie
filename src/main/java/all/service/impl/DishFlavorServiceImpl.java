package all.service.impl;

import all.mapper.DishFlavorMapper;
import all.pojo.DishFlavor;
import all.service.DishFlavorService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import org.springframework.stereotype.Service;

@Service
public class DishFlavorServiceImpl extends ServiceImpl<DishFlavorMapper, DishFlavor> implements DishFlavorService {
}
