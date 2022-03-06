package com.renyujie.gulimall.ware.listener;


import com.rabbitmq.client.Channel;
import com.renyujie.common.dto.mq.OrderTo;
import com.renyujie.common.dto.mq.StockLockedTo;
import com.renyujie.gulimall.ware.service.WareSkuService;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;

/**
   订单解锁
 */
@Service
//@RabbitListener(queues = "stock.release.stock.queue")
public class StockReleaseListener {

    @Autowired
    WareSkuService wareSkuService;

    /**
     * 库存解锁的场景1
       下订单成功，库存锁定成功，接下来的业务调用失败，导致订单回滚。之前锁定的库存就要自动解锁
     */
//    @RabbitHandler
    @RabbitListener(queues = "stock.release.stock.queue")
    public void handleStockLockedRelease(StockLockedTo to, Message message, Channel channel) throws IOException {

        System.out.println("收到库存解锁消息...");
        try {
            //当前消息是否第二次及以后(重新)派发过来了 (做法过于暴力)
//            Boolean redelivered = message.getMessageProperties().getRedelivered();
            wareSkuService.unLockStock(to);
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        } catch (Exception e) {
            //有异常  拒绝拒绝后重新放回队列 继续被被人消费解锁
            channel.basicReject(message.getMessageProperties().getDeliveryTag(), true);
        }
    }


    /**
     * 防止订单服务卡顿，导致库存订单状态一直改变不了为4（已取消），库存消息优先到，查订单状态，查到的是新建状态0
     * 这会导致上面的解锁服务什么都不做就走了，但是该mq已消费，导致这个取消的但是卡顿到的订单，永远不能解锁库存
     *
     * 即在处理OrderServiceImpl#closeOrder问题
     */
    //@RabbitListener
    @RabbitListener(queues = "stock.release.stock.queue")
    public void handleOrderCloseRelease(OrderTo orderTo, Message message, Channel channel) throws IOException {

        System.out.println("收到订单关闭，准备解锁库存消息...");
        try {
            wareSkuService.unLockStock(orderTo);
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        } catch (Exception e) {
            channel.basicReject(message.getMessageProperties().getDeliveryTag(), true);
        }
    }
}
