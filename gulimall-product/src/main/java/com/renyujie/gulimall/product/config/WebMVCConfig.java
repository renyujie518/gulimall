package com.renyujie.gulimall.product.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

/**
 * @author renyujie518
 * @version 1.0.0
 * @ClassName WebMVCConfig.java
 * @Description P136如果静态资源中的JS、CSS效果没有达到。可以在项目中添加一个配置类，再重启项目即可。
 * @createTime 2022年02月17日 16:23:00
 */
@Configuration
public class WebMVCConfig extends WebMvcConfigurerAdapter {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/static/**").addResourceLocations("classpath:/static/");
    }
}