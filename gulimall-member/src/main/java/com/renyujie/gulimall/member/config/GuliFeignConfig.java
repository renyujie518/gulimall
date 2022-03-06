package com.renyujie.gulimall.member.config;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;

/**
 * @description 解决Feign远程调用请求头丢失 p268

原因：feign发送请求时构造的RequestTemplate没有请求头，请求参数等信息【cookie没了】
导致在cart服务中，拦截器拦截获取session中的登录信息，获取不到userId【没有cookie】
这样会在com.renyujie.gulimall.cart.service.impl.CartServiceImpl#getUserCartItems()的时候空指针异常
其真实原因就是由于feign没带cookie给cart服务  cart服务的拦截器获取不到session信息  就会认定此时没登录
CartInterceptor.threadLocal.get()得到的就是null

解决：
通过源码分析可得 在feign中真正到达远程cart服务 的会后会经过一层过滤器：RequestInterceptor
将这个拦截器注入bean生效  在feign源码的Factory构造的手会把容器中的拦截器放进来
再通过请求上下文RequestContextHolder同步新、老请求（老请求就是/toTrade请求，带有Cookie数据）的cookie
新请求：template       老请求：attributes.getRequest()

DEBUG可以查看到会调用 拦截器
 */
@Configuration
public class GuliFeignConfig {

    @Bean("requestInterceptor")
    public RequestInterceptor requestInterceptor() {

        return new RequestInterceptor() {

            @Override
            public void apply(RequestTemplate template) {
                System.out.println("feign远程调用外部服务之前先会执行RequestInterceptor.apply方法");
                //由于feign构造的时候是单线程，可以在RequestContextHolder请求上下文中获得浏览器进来的 请求属性（头和cokkie都有）
                ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
                if (attributes != null) {
                    HttpServletRequest request = attributes.getRequest();
                    if (request != null) {
                        //同步请求头数据 主要同步Cookie
                        String cookie = request.getHeader("Cookie");
                        //给新请求同步的老请求Cookie
                        template.header("Cookie", cookie);
                    }
                }
            }
        };
    }
}
