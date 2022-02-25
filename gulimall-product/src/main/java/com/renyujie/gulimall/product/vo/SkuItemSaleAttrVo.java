package com.renyujie.gulimall.product.vo;

import lombok.Data;
import lombok.ToString;

import java.util.List;

/**
   详情页 的  sku的销售属性组合
 */
@ToString
@Data
public class SkuItemSaleAttrVo {
    private Long attrId;
    private String attrName;
    private List<AttrValueWithSkuIdVo> attrValues;
}
