package com.renyujie.gulimall.order.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.renyujie.common.dto.mq.SeckillOrderTo;
import com.renyujie.common.utils.PageUtils;
import com.renyujie.gulimall.order.entity.OrderEntity;
import com.renyujie.gulimall.order.vo.*;

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


    /**
     * @description 远程库存服务调用  按照订单号查询订单详情
     */
    OrderEntity getOrderByOrderSn(String orderSn);

    /**
     * @description 收到过期的订单，关闭订单
     */
    void closeOrder(OrderEntity entity);

    /**
     * 构建当前订单的支付信息 PayVo
     */
    PayVo getPayOrder(String orderSn);

    /**
     * 给远程服务使用的
     * 查询当前登录用户的所有订单详情数据（分页）
     * @RequestBody 远程传输必须用这个
     */
    PageUtils queryPageWithItem(Map<String, Object> params);


    /**
     * 处理支付宝返回的数据
     * <p>
     * 只要我们收到了，支付宝给我们的一步的通知，告诉我订单支付成功
     * 返回success，支付宝就再也不通知
     */
    String handlePayResult(PayAsyncVo payAsyncVo);

    /**
     * @description 创建秒杀单
     */
    void creatSeckillOrder(SeckillOrderTo seckillOrderTo);
}

