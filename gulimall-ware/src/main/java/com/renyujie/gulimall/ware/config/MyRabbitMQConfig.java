package com.renyujie.gulimall.ware.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.HashMap;
import java.util.Map;

/**
 RabbitMQ配置
 */
//开启RabbitMQ消息队列
@EnableRabbit
@Configuration
public class MyRabbitMQConfig {

    RabbitTemplate rabbitTemplate;

    /**
     * ******************下面是基础配置  消息内容序列化为json*******************
     *
     */
    @Bean
    public MessageConverter messageConverter() {

        return new Jackson2JsonMessageConverter();
    }

    //没有这个方法， 不能创建RabbitMQ的交换机啊，队列啊  因为创建规则是有消费者接收RabbitListener，mq去容器中找，没找到才会创建
    //然后之前设置chanal,queue的时候的持久化选项都是true  所以该方法执行一次后即可注释
    //@RabbitListener(queues = "stock.release.stock.queue")
    //public void handle(Message message) {
    //
    //}



    /**
     * **************下面全都是配置mq消息不丢失*****************
     *
     */
    @Primary
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        this.rabbitTemplate = rabbitTemplate;
        rabbitTemplate.setMessageConverter(messageConverter());
        return rabbitTemplate;
    }

//    @PostConstruct  //MyRabbitConfig对象创建完成以后执行这个方法
//    public void initRabbitTemplate() {
//
//        //设置确认回调 消息到了队列
//        rabbitTemplate.setConfirmCallback(new RabbitTemplate.ConfirmCallback() {
//
//            /**
//             * 1、消息抵达服务器 ack=true
//             * @param correlationData 当前消息唯一关联的数据 这个是消息爱的唯一id
//             * @param ack 消息是否成功收到
//             * @param cause 失败的原因
//             */
//            @Override
//            public void confirm(CorrelationData correlationData, boolean ack, String cause) {
//                //服务器收到了
//                System.out.println("消息抵达服务器confirm....correlationData[" + correlationData + "]==>ack[" + ack + "]cause>>>" + cause);
//            }
//        });
//
//        //设置消息队列的确认回调 发送了，但是队列没有收到
//        rabbitTemplate.setReturnCallback(new RabbitTemplate.ReturnCallback() {
//
//            /**
//             * 只要消息没有投递给指定的队列 就触发这个失败回调
//             * @param message  投递失败的消息详细信息
//             * @param replyCode 回复的状态码
//             * @param replyText 回复的文本内容
//             * @param exchange 当时这个消息发给那个交换机
//             * @param routingKey 当时这个消息用那个路由键
//             */
//            @Override
//            public void returnedMessage(Message message, int replyCode, String replyText, String exchange, String routingKey) {
//                //报错误 未收到消息
//                System.out.println("Fail!! Message[" + message + "]==>[" + exchange + "]==>routingKey[" + routingKey + "]");
//            }
//        });
//    }



    /**
     * **************下面解决订单与仓储服务  分布式事务中用到的mq*****************
     */
    @Bean
    public Exchange stockEventExchange() {

        // String name, boolean durable, boolean autoDelete, Map<String, Object> arguments
        //普通交换机
        return new TopicExchange("stock-event-exchange", true, false);
    }

    @Bean
    public Queue stockDelayQueue() {

        Map<String, Object> arguments = new HashMap<>();
        //死信交换机
        arguments.put("x-dead-letter-exchange", "stock-event-exchange");
        //死信路由键
        arguments.put("x-dead-letter-routing-key", "stock.release");
        //消息过期时间 ms 2分钟
        arguments.put("x-message-ttl", 120000);
        return new Queue("stock.delay.queue", true, false, false, arguments);
    }

    @Bean
    public Queue stockReleaseOrderQueue() {

        //普通队列
        return new Queue("stock.release.stock.queue", true, false, false);
    }

    @Bean
    public Binding stockCreateStockBinding() {

        //和延时队列绑定
        return new Binding(
                "stock.delay.queue",
                Binding.DestinationType.QUEUE,
                "stock-event-exchange",
                "stock.locked",
                null);
    }

    @Bean
    public Binding stockReleaseOrderBinding() {

        //和普通队列绑定
        return new Binding(
                "stock.release.stock.queue",
                Binding.DestinationType.QUEUE,
                "stock-event-exchange",
                "stock.release.#",
                null);
    }

}
