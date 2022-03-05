package com.renyujie.gulimall.ware.vo;

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
     * 订单中需要锁住的sku的商品项信息
     */
    private List<OrderItemVo> locks;
}
