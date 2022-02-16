package com.renyujie.gulimall.ware.vo;

import lombok.Data;

/**
 批量查询sku是否有库存 的返回结果 只需要知道如下变量

 */
@Data
public class SkuHasStockVo {
    private Long skuId;
    private Boolean hasStock;
}
