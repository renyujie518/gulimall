package com.renyujie.gulimall.order.listener;

import com.rabbitmq.client.Channel;
import com.renyujie.common.dto.mq.SeckillOrderTo;
import com.renyujie.gulimall.order.service.OrderService;
import com.renyujie.gulimall.order.utils.AlipayTemplate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
   秒杀的监听器
 */
@Slf4j
@RabbitListener(queues = "order.seckill.order.queue")
@Component
public class OrderSeckillListener {

    @Autowired
    AlipayTemplate alipayTemplate;

    @Autowired
    OrderService orderService;

    @RabbitHandler
    public void listening(SeckillOrderTo seckillOrderTo, Channel channel, Message message) throws IOException {

        try {
            log.info("创建秒杀单的相信信息. ...");
            orderService.creatSeckillOrder(seckillOrderTo);
            //手动调用支付宝收单 p310 暂时不用手动
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        } catch (IOException e) {
            //true 重新回到消息队列
            channel.basicReject(message.getMessageProperties().getDeliveryTag(), true);
        }
    }
}
