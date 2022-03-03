package com.renyujie.gulimall.order.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;

/**
 * @author 孟享广
 * @date 2021-02-06 11:47 上午
 * @description创建RabbitMQ 队列 交换机
 * 运行之前，一定要小心，否则要删除队列/交换机重新运行 麻烦！
 *
 * 解决消息丢失(最怕)
 *  1 做好消息确认机制（publisher，consumer【手动ack】）
 *  2 每一个发送的消息都在数据库做好记录。定期将失败的消息再次发送一次
 * 解决消息重复
 *  1 幂等性
 *  2 防重表
 *  3 RabbitMQ自带redelivered (做法过于暴力)
 * 解决消息积压
 *  1 增加更多的消费者
 *  2 上线专门的队列消费服务，取出来，记录到数据库，离线慢慢处理
 */
//开启RabbitMQ消息队列 不监听消息可以不加
@EnableRabbit
@Configuration
public class MyRabbitMQConfig {
    @Autowired
    RabbitTemplate rabbitTemplate;

    /**
     * ******************8下面全都是基础配置*******************
     */

    /**
     * @Description: json类型的转换  content_type:application/json
     */
    @Bean
    public MessageConverter messageConverter() {

        return new Jackson2JsonMessageConverter();
    }


    /**
     * **************下面全都是配置mq消息不丢失*****************
     */
    /**
     * 定制rabbitTemplate
     */
    //MyRabbitConfig对象创建完成以后执行这个方法
    @PostConstruct
    public void initRabbitTemplate() {
        //设置确认回调 消息到了队列
        rabbitTemplate.setConfirmCallback(new RabbitTemplate.ConfirmCallback() {

            /**
             * 1、服务收到消息就会回调
             *      1、spring.rabbitmq.publisher-confirms: true
             *      2、设置确认回调 confirm
             * @param correlationData 当前消息唯一关联的数据 这个是消息爱的唯一id
             * @param ack 消息是否成功收到
             * @param cause 失败的原因
             *
             * broker收到  就会自动回到执行confirm   消息抵达服务器 ack=true
             */
            @Override
            public void confirm(CorrelationData correlationData, boolean ack, String cause) {
                //服务器收到了
                System.out.println("消息抵达服务器confirm....correlationData[" + correlationData + "]==>ack[" + ack + "]cause>>>" + cause);
            }
        });

        rabbitTemplate.setReturnCallback(new RabbitTemplate.ReturnCallback() {

            /**
             *只要消息没有投递给指定的队列就会进行回调（成功不会触发）
             *  1、spring.rabbitmq.publisher-returns: true
             *     spring.rabbitmq.template.mandatory: true
             *  2、设置确认执行回调ReturnCallback
             *
             * @param message  投递失败的消息详细信息
             * @param replyCode 回复的状态码
             * @param replyText 回复的文本内容（错误提示）
             * @param exchange 当时这个消息发给那个交换机
             * @param routingKey 当时这个消息用那个路由键
             *
             */
            @Override
            public void returnedMessage(Message message, int replyCode, String replyText, String exchange, String routingKey) {
                //报错误 未收到消息
                System.out.println("Fail!! Message[" + message + "]==>[" + exchange + "]==>routingKey[" + routingKey + "]");
            }
        });

    }


}
