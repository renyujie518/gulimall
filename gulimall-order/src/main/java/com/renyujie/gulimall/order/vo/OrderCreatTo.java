package com.renyujie.gulimall.order.vo;


import com.renyujie.gulimall.order.entity.OrderEntity;
import com.renyujie.gulimall.order.entity.OrderItemEntity;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
   创建订单时生成的对象
 */
@Data
public class OrderCreatTo {

    //订单实体
    private OrderEntity order;

    //订单中的商品项
    private List<OrderItemEntity> orderItems;

    //应付价格
    private BigDecimal payPrice;

    //运费
    private BigDecimal fare;
}
