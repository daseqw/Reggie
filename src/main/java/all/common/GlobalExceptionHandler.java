package all.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.sql.SQLIntegrityConstraintViolationException;

/**
 * 全局异常处理
 */
@ControllerAdvice(annotations = {RestController.class, Controller.class})
@ResponseBody
@Slf4j
public class GlobalExceptionHandler {
    /**
     * mysql中的 ---》 唯一字段重复  异常 异常处理方法
     * @param e
     * @return
     */
    @ExceptionHandler({SQLIntegrityConstraintViolationException.class})
    public R<String> exceptionHandler(SQLIntegrityConstraintViolationException e){
        //输出日志
        log.error(e.getMessage());
        //判断抛出的异常信息里是否包含这两个单词（这种索引唯一发生重复时的异常信息里都包含这两个单词）
        if (e.getMessage().contains("Duplicate entry")){
            //如果是这种异常，就把用户输入的重复字段的值提取出来，然后返回。
            //根据空格来取值
            String[] split = e.getMessage().split(" ");
            log.error(split[2]+"已存在");
            return R.error(split[2]+"已存在");
        }
        //如果不是这种异常，就返回未知错误。
        return R.error("未知错误");
    }

    /**
     * 删除有关联的菜品或套餐 异常
     * @param e
     * @return
     */
    @ExceptionHandler({CustomException.class})
    public R<String> CustomExceptionHandler(CustomException e){
        log.info(e.getMessage());
        //如果不是这种异常，就返回未知错误。
        return R.error(e.getMessage());
    }



}
