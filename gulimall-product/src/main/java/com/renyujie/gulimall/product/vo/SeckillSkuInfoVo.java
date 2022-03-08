package com.renyujie.gulimall.product.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
  秒杀信息
 */
@Data
public class SeckillSkuInfoVo {


    /**
     * 活动id
     */
    private Long promotionId;
    /**
     * 活动场次id
     */
    private Long promotionSessionId;
    /**
     * 商品id
     */
    private Long skuId;
    /**
     * 秒杀价格
     */
    private BigDecimal seckillPrice;
    /**
     * 秒杀总量
     */
    private BigDecimal seckillCount;
    /**
     * 每人限购数量
     */
    private BigDecimal seckillLimit;
    /**
     * 排序
     */
    private Integer seckillSort;

    //当前sku的秒杀开始时间
    private Long startTime;

    //当前sku的秒杀结束时间
    private Long endTime;

    //秒杀随机码
    private String randomCode;

}
