package com.renyujie.common.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
sku在前端的"保存商品"界面的最后一步  设置了很多sku的优惠、满减，会员价格等信息
 这时候需要调用远程的coupon服务    把这些信息统统打包成SkuReductionTo
 */

@Data
public class SkuReductionTo {

    private Long skuId;
    private int fullCount;
    private BigDecimal discount;
    private int countStatus;
    private BigDecimal fullPrice;
    private BigDecimal reducePrice;
    private int priceStatus;
    private List<MemberPrice> memberPrice;
}
