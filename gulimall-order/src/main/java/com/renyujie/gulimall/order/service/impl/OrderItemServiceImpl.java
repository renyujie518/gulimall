package com.renyujie.gulimall.order.service.impl;

import com.rabbitmq.client.Channel;
import com.renyujie.gulimall.order.entity.OrderEntity;
import com.renyujie.gulimall.order.entity.OrderReturnReasonEntity;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Map;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.renyujie.common.utils.PageUtils;
import com.renyujie.common.utils.Query;

import com.renyujie.gulimall.order.dao.OrderItemDao;
import com.renyujie.gulimall.order.entity.OrderItemEntity;
import com.renyujie.gulimall.order.service.OrderItemService;

@RabbitListener(queues = {"hello-java-queue"})
@Service("orderItemService")
public class OrderItemServiceImpl extends ServiceImpl<OrderItemDao, OrderItemEntity> implements OrderItemService {

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<OrderItemEntity> page = this.page(
                new Query<OrderItemEntity>().getPage(params),
                new QueryWrapper<OrderItemEntity>()
        );

        return new PageUtils(page);
    }

    /**
     * @Description: 与业务无关  实验性质 监听消息
     * RabbitListener注解必须在容器内  所以要去一个impl中实现

     * 参数可以写一下类型
     * 1、Message essage: 原生消息详细信息。头+体
     * 2、发送的消息的类型: OrderReturnReasonEntity content; 当时发送的时候的类型
     * 3、Channel channel:当前传输数据的通道
     *
     * Queue:可以很多人都来监听,只要收到消息,队列删除消息,而且只能有一个收到此消息
     * 1)、订单服务启动多个：同一个消息,只能有一个客户端收到
     * 2)、只有一个消息完全处理完,方法运行结束，我们就可以接收到下一个消息
     */
   @RabbitHandler
    public void receiveMessage1(Message message,
                               OrderReturnReasonEntity orderReturnReasonEntity,
                               Channel channel) throws InterruptedException {
        //拿到消息体
        //byte[] body = message.getBody();
        //拿到消息头
        //MessageProperties properties = message.getMessageProperties();
        System.out.println("接收到消息" + message
                + "消息的类型" + message.getClass()
                + "消息的对象" + orderReturnReasonEntity
                + "管道" + channel
        );
        //模拟消息处理
        //Thread.sleep(3000);
        System.out.println("消费完成");

    }

    /**
     *与业务无关  实验性质 监听消息
     * @RabbitListener(queues={"hello-java-queue"})放在类上 说明监听哪些队列
     * @RabbitHandler：标在方法上【作用：重载处理不同类型的数据】
     */
    @RabbitHandler
    public void receiveMessage2(Message message,
                                OrderEntity orderEntity,
                                Channel channel) throws InterruptedException {
        //拿到消息体
        //byte[] body = message.getBody();
        //拿到消息头
        //MessageProperties properties = message.getMessageProperties();
        System.out.println("接收到消息" + message
                + "消息的类型" + message.getClass()
                + "消息的对象" + orderEntity
                + "管道" + channel
        );
        //模拟消息处理
        //Thread.sleep(3000);
        System.out.println("消费完成");
    }


    /**
     * @Description:
     * spring.rabbitmq.listener.simple.acknowledge-mode=manual
     * 消费者手动签收实验(保证每个消息都被正确消费，此时才可以broker删除这个消息)
     * 注意 一旦接收消息  会从队列中拿取所有ready的的消息  如果没ack 队列会把这些消息放到unack中，
     * 下次有接受的请求，再会把unack中的全部发过来
     *
     * channeL.basicAck(deliveryTag,false);签收；非批量，业务成功完成就应该签收
     * channel.basicNack(deliveryTag,false,true);拒签，非批量；业务失敗，拒签,但消息不丢失  变unack,还可能被人消费
     * channel.basicNack(deliveryTag,false,false);拒签，非批量；业务失敗，拒签,但消息直接丢弃
     */
    @RabbitHandler
    public void receiveMessage3(Message message,
                                OrderEntity orderEntity,
                                Channel channel) throws InterruptedException {
        //拿到消息体
        //byte[] body = message.getBody();
        //拿到消息头
        //MessageProperties properties = message.getMessageProperties();
        System.out.println("接收到消息" + message
                + "消息的类型" + message.getClass()
                + "消息的对象" + orderEntity
                + "管道" + channel
        );
        //模拟消息处理
        //Thread.sleep(3000);

        //消息处理完 手动确认  deliveryTag在Channel内按顺序自增
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        System.out.println("deliveryTag->" + deliveryTag);

        try {
            if (deliveryTag % 2 == 0) {
                //确认签收 队列删除该消息 false非批量模式
                channel.basicAck(deliveryTag, false);
                System.out.println("接收了消息" + deliveryTag);
            } else {
                //拒收退货 第三个参数 -> true:重新入队 false:丢弃
                channel.basicNack(deliveryTag, false, true);
                System.out.println("没有接收到消息" + deliveryTag);
            }
        } catch (IOException e) {
            //说明出现网络中断
        }
        System.out.println("消费完成" + deliveryTag);

    }

}