package all.service.impl;

import all.mapper.EmployeeMapper;
import all.pojo.Employee;
import all.service.EmployeeService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
/*
 * 用mybatis-plus实现Service
 * */
@Service
public class EmployeeServiceimpl extends ServiceImpl<EmployeeMapper, Employee> implements EmployeeService {
}
