package all.controller;

import all.common.R;
import all.pojo.Employee;
import all.service.EmployeeService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
/*公共接口类HttpServletRequest继承自ServletRequest。
客户端浏览器发出的请求被封装成为一个HttpServletRequest对象。
对象包含了客户端请求信息包括请求的地址，请求的参数，提交的数据，
上传的文件客户端的ip甚至客户端操作系统都包含在其内。*/
@Slf4j
@RestController
@RequestMapping("/employee")
public class EmployeeController {
    @Autowired
    private EmployeeService employeeService;

    /**
     * 员工登录
     * @param request 用来获取 Session
     * @param employee 登录传入的对应参数
     * @return
     */
    @PostMapping("/login")
    public R<Employee> login(HttpServletRequest request, @RequestBody Employee employee){
        //1.将页面提交的密码进行md5加密
        String password = employee.getPassword();
        password = DigestUtils.md5DigestAsHex(password.getBytes());

        //2.根据页面提交的用户名username查询数据库
        LambdaQueryWrapper<Employee> queryWrapper = new LambdaQueryWrapper<>();
        //调用.eq方法进行username的对比
        queryWrapper.eq(Employee::getUsername,employee.getUsername());
        Employee emp = employeeService.getOne(queryWrapper);

        //3.判断用户名username是否存在
        if(emp == null){
           return R.error("登录失败!");
        }

        //4.比对用户名username的密码
        if(!emp.getPassword().equals(password)){
            return R.error("登录失败!密码错误！");
        }

        //5.查看员工状态是否启用
        if(emp.getStatus() == 0){
            return R.error("员工状态已禁用!");
        }

        //6.登录成功，将员工id放入Session中
        request.getSession().setAttribute("employee",emp.getId());


        //返回从数据库中查询出来的员工
        return R.success(emp);
    }

    /**
     * 员工登出
     * @param request
     * @return
     */
    @PostMapping("/logout")
    public R<String> logout(HttpServletRequest request){
        //移除掉Session中的员工id
        request.getSession().removeAttribute("employee");
        return R.success("登出成功");
    }

    /**
     * 新增员工
     * @param request
     * @param emp
     * @return
     */
    @PostMapping
    public R<String> save(HttpServletRequest request,@RequestBody Employee emp){
        //设置初始密码为123456
        emp.setPassword(DigestUtils.md5DigestAsHex("123456".getBytes()));

        //设置 create_time 和 update_time-----》通过公共填充完成
        //emp.setCreateTime(LocalDateTime.now());
        //emp.setUpdateTime(LocalDateTime.now());

        //获取当前登录的id
        //Long empid = (Long) request.getSession().getAttribute("employee");

        //设置 create_user 和 update_user-----》通过公共填充完成
        //emp.setCreateUser(empid);
        //emp.setUpdateUser(empid);

        //新增员工
        employeeService.save(emp);
        return R.success("新增员工成功");
    }

    /**
     * 分页查询
     * @param page 页数
     * @param pageSize 每页条数
     * @param name 查询的员工姓名
     * @return
     */
    @GetMapping("/page")
    public R<Page> page(int page,int pageSize,String name){
        log.info("页数"+page+"每页条数"+pageSize+"查询的姓名"+name);
        //分页构造器
        Page pageInfo = new Page(page,pageSize);
        //查询条件
        LambdaQueryWrapper<Employee> queryWrapper = new LambdaQueryWrapper<Employee>();
        //判断name是否为空，若为空就不查询
        boolean flag;
        if(name == null){
             flag = false;
        }else {
             flag = true;
        }
        queryWrapper.like(flag,Employee::getName,name);
        //排序条件
        //根据Updatetime排序
        queryWrapper.orderByDesc(Employee::getUpdateTime);

        //执行查询
        employeeService.page(pageInfo,queryWrapper);
        return R.success(pageInfo);
    }

    /**
     * 编辑和控制状态
     * @param request
     * @param emp
     * @return
     */
    @PutMapping
    public R<String> update(HttpServletRequest request,@RequestBody Employee emp){
        log.info(emp.toString());

        //获取修改人的id--------》通过公共填充完成
        //Long empId = (Long) request.getSession().getAttribute("employee");
        //修改update_time和update_user--------》通过公共填充完成
        //emp.setUpdateUser(empId);
        //emp.setUpdateTime(LocalDateTime.now());

        //修改对应信息
        employeeService.updateById(emp);


        return R.success("修改成功");
    }

    /**
     * 根据id查询对应的员工信息
     * @param id
     * @return
     */
    @GetMapping("/{id}")
    public R<Employee> getbyId(@PathVariable Long id){
        Employee byId = employeeService.getById(id);
        if(byId == null){
            return R.error("查询失败");
        }
        return R.success(byId);
    }
}
