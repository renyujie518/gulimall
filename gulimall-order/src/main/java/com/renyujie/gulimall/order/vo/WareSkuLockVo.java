package com.renyujie.gulimall.order.vo;

import lombok.Data;

import java.util.List;

/**
   sku锁定库存信息
 */
@Data
public class WareSkuLockVo {

    /**
     * 订单号
     */
    private String orderSn;

    /**
     * 需要锁住的所有库存信息
     */
    private List<OrderItemVo> locks;
}
