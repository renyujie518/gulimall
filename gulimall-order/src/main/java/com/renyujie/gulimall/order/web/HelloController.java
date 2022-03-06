package com.renyujie.gulimall.order.web;


import com.renyujie.gulimall.order.entity.OrderEntity;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Date;

/**
   测试页面
 */
@Controller
public class HelloController {

    @Autowired
    RabbitTemplate rabbitTemplate;

    /**
     * @Description: 测试页面
     */
    @GetMapping("/{page}.html")
    public String returnPage(@PathVariable("page") String page) {
        //System.out.println("http://127.0.0.1:9000/" + page + ".html");
        //return new StringBuilder().append("http://127.0.0.1:9000/").append(page).append(".html").toString();
        return page;
    }



    /**
     * @Description: 使用延迟mq  模拟测试
     */
    @ResponseBody
    @GetMapping("/test/creatPOrder")
    public String creatPOrderTest() {
        OrderEntity entity = new OrderEntity();
        entity.setOrderSn("10010");
        entity.setModifyTime(new Date());

        //给MQ发消息
        rabbitTemplate.convertAndSend(
                "order-event-exchange",
                "order.create.order",
                entity);
        return "ok";
    }
}
