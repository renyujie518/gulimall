package com.renyujie.common.dto.mq;

import lombok.Data;

import java.math.BigDecimal;

/**
  秒杀订单数据
 */
@Data
public class SeckillOrderTo {

    private String OrderSn;
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
    private Integer num;

    //会员id
    private Long memberId;

}
