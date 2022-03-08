package com.renyujie.gulimall.seckill.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

/**
 * @Description: redisson配置
 */
@Configuration
public class MyRedissonConfig {

    /**
     * 所有对Redisson的使用都是通过RedissonClient对象
     */
    @Bean(destroyMethod="shutdown")
    public RedissonClient redisson() throws IOException {
        //1 创建配置
        Config config = new Config();
        //单节点模式
        config.useSingleServer().setAddress("redis://123.57.180.71:6379").setPassword("ryj123456");

        //2 根据Config创建出RedissonClient实例
        RedissonClient redissonClient = Redisson.create(config);
        return redissonClient;
    }
}
