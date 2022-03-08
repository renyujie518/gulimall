//package com.renyujie.gulimall.seckill.config;
//
//import com.alibaba.csp.sentinel.adapter.servlet.callback.UrlBlockHandler;
//import com.alibaba.csp.sentinel.adapter.servlet.callback.WebCallbackManager;
//import com.alibaba.csp.sentinel.slots.block.BlockException;
//import com.alibaba.fastjson.JSON;
//
//import com.renyujie.common.exception.BizCodeEnum;
//import com.renyujie.common.utils.R;
//import org.springframework.context.annotation.Configuration;
//
//import javax.servlet.http.HttpServletRequest;
//import javax.servlet.http.HttpServletResponse;
//import java.io.IOException;
//
///**
//
// * @description sentinel自定义返回方法
// */
//@Configuration
//public class MySeckillSentinelConfig {
//
//    public MySeckillSentinelConfig() {
//        WebCallbackManager.setUrlBlockHandler(new UrlBlockHandler() {
//            @Override
//            public void blocked(HttpServletRequest request, HttpServletResponse response, BlockException e) throws IOException {
//                R r = R.error(BizCodeEnum.TO_MANY_REQUEST.getCode(), BizCodeEnum.TO_MANY_REQUEST.getMsg());
//                //解决response乱码
//                response.setCharacterEncoding("utf-8");
//                response.setContentType("application/json");
//                response.getWriter().write(JSON.toJSONString(r));
//            }
//        });
//    }
//}
