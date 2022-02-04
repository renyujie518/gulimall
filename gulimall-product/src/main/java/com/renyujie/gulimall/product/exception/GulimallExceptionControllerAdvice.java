package com.renyujie.gulimall.product.exception;

import com.renyujie.common.exception.BizCodeEnum;
import com.renyujie.common.utils.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

/**
 * @author renyujie518
 * @version 1.0.0
 * @ClassName GulimallExceptionControllerAdvice.java
 * @Description 商品服务接口层的统一异常处理
 * @createTime 2022年01月29日 21:52:00
 */
@RestControllerAdvice(basePackages = "com.renyujie.gulimall.product.controller")
@Slf4j
public class GulimallExceptionControllerAdvice {
    //（为了学习精准异常处理）  专门处理@RequestBody上validate失败后的异常
    @ExceptionHandler(value = MethodArgumentNotValidException.class)
    public R handleValidException(MethodArgumentNotValidException e) {
        log.error("请求数据校验问题: {}，异常类型：{}", e.getMessage(), e.getClass());
        BindingResult bindingResult = e.getBindingResult();
        Map<String, String> errorMap = new HashMap<>();
        bindingResult.getFieldErrors().forEach((item) -> {
            errorMap.put(item.getField(), item.getDefaultMessage());
        });
        return R.error(BizCodeEnum.VALID_EXCEPTION.getCode(),
                BizCodeEnum.VALID_EXCEPTION.getMsg()).put("data", errorMap);
    }

    //    公共异常  这里做统一抓取 所以在controller不需要紧跟着每个接口带bindingResult判断 直接异常在这里被捕获
    @ExceptionHandler(value = Throwable.class)
    public R handleException(Throwable throwable) {
        log.error("未知异常{},异常类型{}",throwable.getMessage(),throwable.getClass());
        return R.error(BizCodeEnum.UNKNOWN_EXCEPTION.getCode(),
                BizCodeEnum.UNKNOWN_EXCEPTION.getMsg());
    }
}
