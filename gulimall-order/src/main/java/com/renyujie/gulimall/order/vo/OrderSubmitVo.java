package com.renyujie.gulimall.order.vo;

import lombok.Data;
import lombok.ToString;

import java.math.BigDecimal;

/**
 "确认页"点击提交订单 提交的内容对象
 */
@ToString
@Data
public class OrderSubmitVo {

    //收货地址的id
    private Long addrId;
    //支付方式
    private Integer payType;

    //无需提交购买的商品，去购物车在获取一遍（防止点击提交订单前 又在购物车中添加商品 jd是算新添加的）

    //优惠、发票....

    //防重令牌
    private String orderToken;

    //应付价格 校验价格
    BigDecimal payPrice;

    //订单备注
    private String node;

    //用户相关信息，直接去session取出登录用户
}
