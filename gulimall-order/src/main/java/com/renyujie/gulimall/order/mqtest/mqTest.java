package com.renyujie.gulimall.order.mqtest;

import com.rabbitmq.client.Channel;
import com.renyujie.gulimall.order.entity.OrderEntity;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;

import java.io.IOException;

/**
 * @author renyujie518
 * @version 1.0.0
 * @ClassName mqTest.java
 * @Description 测试延迟队列
 * http://127.0.0.1:9000/test/creatPOrder
 * @createTime 2022年03月05日 21:39:00
 */

//@RabbitListener(queues = "order.release.order.queue")
//public class mqTest {
//
//    @RabbitHandler
//    public void listening(OrderEntity entity, Channel channel, Message message) throws IOException {
//        System.out.println("1分钟后，收到过期的订单，准备关闭订单。order："+entity.getOrderSn());
//        channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
//    }
//}
