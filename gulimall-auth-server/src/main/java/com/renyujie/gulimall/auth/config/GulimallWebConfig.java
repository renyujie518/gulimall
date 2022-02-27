package com.renyujie.gulimall.auth.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 web的配置类
 */
@Configuration
public class GulimallWebConfig implements WebMvcConfigurer {

    /**
     * @Description: 视图映射  重写这个方法  无需自己写页面渲染的controller,直接渲染
     */
    @Override
    public void addViewControllers(ViewControllerRegistry registry) {

        registry.addViewController("/login.html").setViewName("login");
        registry.addViewController("/reg.html").setViewName("reg");
    }
}
