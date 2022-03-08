package com.renyujie.gulimall.seckill.config;

import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * mq简单配置
 */
//开启RabbitMQ消息队列 不监听消息可以不加
//@EnableRabbit
@Configuration
public class MyRabbitMQConfig {

    @Bean
    public MessageConverter messageConverter() {

        return new Jackson2JsonMessageConverter();
    }
}
