package all.filter;

import all.common.BaseContext;
import all.common.R;
import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.AntPathMatcher;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Slf4j
@WebFilter(filterName = "LoginCheckFilter",urlPatterns = "/*")
public class LoginCheckFilter implements Filter {
    //路径匹配器，支持通配符
    public static final AntPathMatcher PATH_MATCHER=new AntPathMatcher();

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest request=(HttpServletRequest)servletRequest;
        HttpServletResponse response=(HttpServletResponse)servletResponse;
        //获取请求uri
        String requestURI = request.getRequestURI();
        log.info("拦截到请求：{}",requestURI);
        //定义不需要处理请求路径
        String[] urls=new String[]{
                "/employee/login",
                "/employee/logout",
                "/backend/**",
                "/front/**",
                "/user/sendMsg",
                "/user/login",
                "/common/**"

        };
        boolean check = check(urls, requestURI);
        if(check){
            log.info("本次请求不用处理:{}",requestURI);
            filterChain.doFilter(request, response);
            return;
        }
        //判断employee登录状态
        if(request.getSession().getAttribute("employee")!=null){
            log.info("用户已登录，用户ID为：{}",request.getSession().getAttribute("employee"));
            //已登录，获取当前线程用户的id
            Long empId = (Long) request.getSession().getAttribute("employee");
            //传给BaseContext，通过set方法设置当前线程的id为操作用户id
            BaseContext.setCurrentId(empId);
            filterChain.doFilter(request, response);
            return;
        }
        //判断user登录状态
        if(request.getSession().getAttribute("user")!=null){
            log.info("用户已登录，用户ID为：{}",request.getSession().getAttribute("user"));
            //已登录，获取当前线程用户的id
            Long userId = (Long) request.getSession().getAttribute("user");
            //传给BaseContext，通过set方法设置当前线程的id为操作用户id
            BaseContext.setCurrentId(userId);
            filterChain.doFilter(request, response);
            return;
        }
        //还未登录通过输出流，返回NOTLOGIN信息，前端响应拦截器进行页面跳转
        log.info("用户未登录！");
        response.getWriter().write(JSON.toJSONString(R.error("NOTLOGIN")));
        return;
    }

    /**
     * 路径匹配检查此次请求是否放行
     * @param urls
     * @param requestURI
     * @return
     */
    public static boolean check(String[] urls, String requestURI){
        //查看requestURI是否属于 不需要处理的urls
        for (String url : urls) {
            boolean match = PATH_MATCHER.match(url, requestURI);
            if(match){
                return true;
            }
        }
        return false;
    }
}

