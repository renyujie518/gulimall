package com.renyujie.gulimall.order.vo;

import lombok.Data;

/**
   远程查询库存服务 获得是否有库存  来自SkuHasStockVo.class
 */
@Data
public class SkuStockVo {

    private Long skuId;
    private Boolean hasStock;
}
