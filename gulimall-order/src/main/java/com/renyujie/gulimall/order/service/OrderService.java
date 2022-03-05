package com.renyujie.gulimall.order.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.renyujie.common.utils.PageUtils;
import com.renyujie.gulimall.order.entity.OrderEntity;
import com.renyujie.gulimall.order.vo.OrderConfirmVo;
import com.renyujie.gulimall.order.vo.OrderSubmitVo;
import com.renyujie.gulimall.order.vo.SubmitOrderResponseVo;

import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * 订单
 *
 * @author renyujie518
 * @email renyujie518@gmail.com
 * @date 2022-01-20 21:37:57
 */
public interface OrderService extends IService<OrderEntity> {

    PageUtils queryPage(Map<String, Object> params);

    /**
     * @Description: 购物车  点击去结算
     * 订单确认页 confire.html 需要返回的数据
     */
    OrderConfirmVo confirmOrder() throws ExecutionException, InterruptedException;

    /**
     * "确认页"点击提交订单   去"支付选择页"  下单失败重新回到"确认页"
     * 前端以表单的方式提交 orderSubmitVo
     */
    SubmitOrderResponseVo submitOrder(OrderSubmitVo orderSubmitVo);
}

